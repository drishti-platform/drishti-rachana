#!/usr/bin/env python3
# Copyright (c) 2026 Ashutosh Sinha <ajsinha@gmail.com>. All rights reserved.
# Drishti Rachana. Proprietary: see LICENSE.
"""The conformance driver (conformance/CONFORMANCE.md): reads and validates the case files, starts a runtime program that speaks the
stdin protocol of RUNNER section 5, compares what it answers with `expect` (FORMAT 6 and 7), writes results/<runtime>.jsonl and
the summary, and exits by RUNNER 4.4. `--record` fills `expect` of unverified cases from the reference (RUNNER 6).

    python3 tools/conformance/rachana_conformance.py --cmd "java -cp ... tools/reference-bridge/RefBridge.java spec/1/formats.yaml"
    python3 tools/conformance/rachana_conformance.py --classpath-file CP.txt          # the bootstrap bridge over Drishti's jars

Needs PyYAML and jsonschema (pip install -r tools/conformance/requirements.txt)."""
from __future__ import annotations

import argparse
import fnmatch
import json
import math
import os
import re
import shlex
import subprocess
import threading
import sys
import time
from decimal import Decimal
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
DRISHTI_FORMATS = str(ROOT / "tools" / "conformance" / "profile" / "drishti-formats.yaml")   # the Drishti profile's format names, kept in this repository; RACHANA_DRISHTI_FORMATS names another file
SCHEMA = ROOT / "conformance" / "schema" / "case.schema.json"


# ---- reading cases: YAML 1.2 core schema, no repeated key ----------------------------------------------------------------

class CoreLoader(yaml.SafeLoader):
    pass


CoreLoader.yaml_implicit_resolvers = {}
for _tag, _rx, _first in [
    ("tag:yaml.org,2002:null", r"^(?:~|null|)$", list("~n") + [""]),
    ("tag:yaml.org,2002:bool", r"^(?:true|false)$", list("tf")),
    ("tag:yaml.org,2002:int", r"^(?:[-+]?[0-9]+|0o[0-7]+|0x[0-9a-fA-F]+)$", list("-+0123456789")),
    ("tag:yaml.org,2002:float", r"^(?:[-+]?(?:\.[0-9]+|[0-9]+(?:\.[0-9]*)?)(?:[eE][-+]?[0-9]+)?|[-+]?\.(?:inf|Inf|INF)|\.(?:nan|NaN|NAN))$", list("-+0123456789.")),
]:
    CoreLoader.add_implicit_resolver(_tag, re.compile(_rx), _first)


def _int(loader, node):
    v = loader.construct_scalar(node).replace("_", "")
    return int(v, 8) if v.startswith("0o") else int(v, 16) if v.startswith("0x") else int(v)


def _float(loader, node):
    v = loader.construct_scalar(node).lower()
    return {".inf": math.inf, "+.inf": math.inf, "-.inf": -math.inf, ".nan": math.nan}.get(v) or float(v)


def _map(loader, node):
    loader.flatten_mapping(node)
    out = {}
    for k, v in node.value:
        key = loader.construct_object(k, deep=True)
        if key in out:
            raise yaml.constructor.ConstructorError(None, None, f"repeated key {key!r}", k.start_mark)
        out[key] = loader.construct_object(v, deep=True)
    return out


CoreLoader.add_constructor("tag:yaml.org,2002:int", _int)
CoreLoader.add_constructor("tag:yaml.org,2002:float", _float)
CoreLoader.add_constructor("tag:yaml.org,2002:map", _map)


class Usage(Exception):
    pass


def load_files(cases_dir: Path):
    import jsonschema
    schema = json.loads(SCHEMA.read_text(encoding="utf-8"))
    validator = jsonschema.Draft202012Validator(schema)
    files = []
    for p in sorted(cases_dir.rglob("*.yaml"), key=lambda p: p.relative_to(cases_dir).with_suffix("").as_posix().encode("utf-8")):
        suite = p.relative_to(cases_dir).with_suffix("").as_posix()
        text = p.read_text(encoding="utf-8")
        try:
            doc = yaml.load(text, Loader=CoreLoader)
        except yaml.YAMLError as e:
            raise Usage(f"{p}: {e}")
        errs = sorted(validator.iter_errors(json.loads(json.dumps(doc, default=lambda o: None))), key=lambda e: list(e.path))
        if errs:
            raise Usage(f"{p}: {errs[0].message} at /{'/'.join(map(str, errs[0].path))}")
        if doc["suite"] != suite:
            raise Usage(f"{p}: suite {doc['suite']!r} is not {suite!r}")
        ids = [c["id"] for c in doc["cases"]]
        if len(ids) != len(set(ids)):
            raise Usage(f"{p}: a case id is repeated")
        files.append((p, suite, doc))
    return files


def merged(doc, case):
    c = dict(case)
    for k in ("since", "profile", "verified"):
        c.setdefault(k, doc.get(k, {"verified": False}.get(k)))
    g = dict(doc.get("given") or {})
    g.update(case.get("given") or {})
    if g:
        c["given"] = g
    c["verified"] = bool(c.get("verified"))
    return c


# ---- canonical JSON (FORMAT 8 and 9) -------------------------------------------------------------------------------------

def number_text(x) -> str:
    x = float(x)
    if math.isnan(x):
        return "NaN"
    if math.isinf(x):
        return "Infinity" if x > 0 else "-Infinity"
    if x == 0:
        return "0"
    if x == int(x) and abs(x) < 1e15:
        return str(int(x))
    sign, digits, exp = Decimal(repr(x)).as_tuple()
    ds = list(digits)
    while len(ds) > 1 and ds[-1] == 0:
        ds.pop()
        exp += 1
    e10 = len(ds) - 1 + exp
    neg = "-" if sign else ""
    if 1e-3 <= abs(x) < 1e7:
        s = format(Decimal((0, tuple(ds), exp)), "f")
        return neg + (s if "." in s else s + ".0")
    return neg + str(ds[0]) + "." + ("".join(map(str, ds[1:])) or "0") + "E" + str(e10)


_ESC = {'"': '\\"', "\\": "\\\\", "\b": "\\b", "\t": "\\t", "\n": "\\n", "\f": "\\f", "\r": "\\r"}


def string_text(s: str) -> str:
    out = ['"']
    for ch in s:
        o = ord(ch)
        out.append(_ESC.get(ch) or (f"\\u{o:04x}" if o < 0x20 or o == 0x7F or 0xD800 <= o <= 0xDFFF else ch))
    out.append('"')
    return "".join(out)


def canon(v) -> str:
    if v is None:
        return "null"
    if v is True:
        return "true"
    if v is False:
        return "false"
    if isinstance(v, (int, float)):
        return number_text(v)
    if isinstance(v, str):
        return string_text(v)
    if isinstance(v, list):
        return "[" + ",".join(canon(x) for x in v) + "]"
    if isinstance(v, dict):
        return "{" + ",".join(string_text(k) + ":" + canon(x) for k, x in v.items()) + "}"
    raise TypeError(type(v))


# ---- comparing (FORMAT 6, 7) ---------------------------------------------------------------------------------------------

def match_problem(exp, act, why):
    for k, v in exp.items():
        if k == "doc":
            continue
        if k == "messageStart":
            if not str(act.get("message", "")).startswith(v):
                why.append(f"message: expected to start {v!r}, actual {act.get('message')!r}")
                return False
        elif k in ("range", "expr"):
            a = act.get(k) or {}
            for kk, vv in v.items():
                if isinstance(vv, dict):
                    for k3, v3 in vv.items():
                        if (a.get(kk) or {}).get(k3) != v3:
                            why.append(f"{k}.{kk}.{k3}: expected {v3!r}, actual {(a.get(kk) or {}).get(k3)!r}")
                            return False
                elif a.get(kk) != vv:
                    why.append(f"{k}.{kk}: expected {vv!r}, actual {a.get(kk)!r}")
                    return False
        elif k == "severity" or k in act or v is not None:
            av = act.get(k, "error" if k == "severity" else None)
            if canon(av) != canon(v):
                why.append(f"{k}: expected {canon(v)}, actual {canon(av)}")
                return False
    if "severity" not in exp and act.get("severity", "error") != "error":
        why.append("severity: expected error")
        return False
    return True


def pointer(doc, ptr):
    """FORMAT 6 `at`: a JSON Pointer into the actual document; (found, value)."""
    cur = doc
    for raw in ptr.split("/")[1:] if ptr else []:
        key = raw.replace("~1", "/").replace("~0", "~")
        if isinstance(cur, dict) and key in cur:
            cur = cur[key]
        elif isinstance(cur, list) and key.isdigit() and int(key) < len(cur):
            cur = cur[int(key)]
        else:
            return False, None
    return True, cur


def match_at(exp, doc, why, where):
    for ptr, want in exp.items():
        found, got = pointer(doc, ptr)
        if not found:
            why.append(f"{where}at {ptr}: nothing there")
            return False
        if canon(want) != canon(got):
            why.append(f"{where}at {ptr}: expected {canon(want)}, actual {canon(got)}")
            return False
    return True


def compare(kind, expect, actual, tolerance, why):
    for field, exp in expect.items():
        if field == "tolerance":
            continue
        act = actual.get(field)
        if field in ("problems",):
            if len(exp) != len(act or []):
                why.append(f"problems: expected {len(exp)}, actual {len(act or [])}")
                return False
            if not all(match_problem(e, a, why) for e, a in zip(exp, act)):
                return False
        elif field == "error":
            if not isinstance(act, dict) or not match_problem({**exp, "severity": "error"} if False else exp, {**act, "severity": "error"}, why):
                why.append(f"error: expected {canon(exp)}, actual {canon(act)}")
                return False
        elif field == "at":
            doc = actual.get("viewModel", actual.get("plan", actual)) if isinstance(actual, dict) else actual
            if not match_at(exp, doc, why, ""):
                return False
        elif field == "steps" and kind == "patch":         # FORMAT 5.9: per step, `frames` exactly and/or `at` into that step's ViewModel
            got = act if isinstance(act, list) else []
            if len(exp) != len(got):
                why.append(f"steps: expected {len(exp)}, actual {len(got)}")
                return False
            for i, (e, g) in enumerate(zip(exp, got)):
                if "frames" in e and canon(e["frames"]) != canon(g.get("frames")):
                    why.append(f"steps[{i}].frames: expected {canon(e['frames'])}, actual {canon(g.get('frames'))}")
                    return False
                if "at" in e and not match_at(e["at"], g.get("viewModel"), why, f"steps[{i}] "):
                    return False
        elif field == "steps" and kind == "fire":          # FORMAT 5.15: per step, `state`, `effects` exactly, `problems` as matched, `at`
            got = act if isinstance(act, list) else []
            if len(exp) != len(got):
                why.append(f"steps: expected {len(exp)}, actual {len(got)}")
                return False
            for i, (e, g) in enumerate(zip(exp, got)):
                for f in ("state", "effects"):
                    if f in e and canon(e[f]) != canon(g.get(f)):
                        why.append(f"steps[{i}].{f}: expected {canon(e[f])}, actual {canon(g.get(f))}")
                        return False
                if "problems" in e:
                    gp = g.get("problems") or []
                    if len(e["problems"]) != len(gp):
                        why.append(f"steps[{i}].problems: expected {len(e['problems'])}, actual {len(gp)}")
                        return False
                    if not all(match_problem(x, y, why) for x, y in zip(e["problems"], gp)):
                        return False
                if "at" in e and not match_at(e["at"], g.get("viewModel"), why, f"steps[{i}] "):
                    return False
        elif field == "value" and tolerance and isinstance(exp, (int, float)) and isinstance(act, (int, float)):
            d = abs(float(exp) - float(act))
            if d > tolerance.get("abs", 0) and d > tolerance.get("rel", 0) * abs(float(exp)):
                why.append(f"value: expected {canon(exp)} within {tolerance}, actual {canon(act)}")
                return False
        elif canon(exp) != canon(act):
            why.append(f"{field}: expected {canon(exp)}, actual {canon(act)}")
            return False
    return True


# ---- running -------------------------------------------------------------------------------------------------------------

def wanted(cid, only):
    return not only or any(fnmatch.fnmatchcase(cid, g.replace("**", "*")) for g in only)


def run(a) -> int:
    t0 = time.time()
    cases_dir = Path(a.cases)
    try:
        files = load_files(cases_dir)
    except Usage as e:
        print(f"usage: {e}", file=sys.stderr)
        return 2
    only = [g for g in (a.only or "").split(",") if g]
    kinds = [k for k in (a.kinds or "").split(",") if k]
    todo = []                                                      # (path, suite, doc, case)
    for p, suite, doc in files:
        for c in doc["cases"]:
            cid = f"{suite}#{c['id']}"
            if wanted(cid, only) and (not kinds or c["kind"] in kinds):
                todo.append((p, suite, doc, merged(doc, c), cid))
    proc = subprocess.Popen(shlex.split(a.cmd), stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=sys.stderr, text=True, encoding="utf-8")
    proc.stdin.write(json.dumps({"rachana-conformance": 1, "profiles": a.profiles.split(",")} | ({"language": a.language} if a.language else {})) + "\n")
    proc.stdin.flush()
    hello = json.loads(proc.stdout.readline() or "{}")
    runtime = hello.get("runtime", a.runtime or "unknown")
    can_kinds, can_profiles = set(hello.get("kinds", [])), set(hello.get("profiles", ["core"]))
    lang = tuple(int(x) for x in (a.language or hello.get("language", "1.0")).split("."))
    sent, lines = [], []
    for p, suite, doc, c, cid in todo:
        profiles = c["profile"] if isinstance(c["profile"], list) else [c["profile"]]
        ok = c["kind"] in can_kinds and set(profiles) <= can_profiles | {"core"} and tuple(int(x) for x in str(c["since"]).split(".")) <= lang \
            and ("until" not in c or lang <= tuple(int(x) for x in str(c["until"]).split("."))) and set(profiles) - {"core"} <= can_profiles
        sent.append(ok)
        if ok:
            msg = {k: v for k, v in c.items() if k not in ("expect", "verified", "source", "note", "tags", "id")}
            msg["case"] = cid
            lines.append(json.dumps(msg, ensure_ascii=False) + "\n")

    def feed():                                   # a thread: the program answers while the cases are still being sent
        try:
            for ln in lines:
                proc.stdin.write(ln)
            proc.stdin.close()
        except OSError:
            pass
    threading.Thread(target=feed, daemon=True).start()
    answers = {}
    for line in proc.stdout:
        try:
            r = json.loads(line, parse_int=int, parse_float=float)
            answers[r["case"]] = r
        except Exception:
            pass
    proc.wait()
    results, failures, counts, recorded = [], [], {"pass": 0, "fail": 0, "error": 0, "unsupported": 0}, {}
    unverified_failing = verified_failing = 0
    for (p, suite, doc, c, cid), ok in zip(todo, sent):
        actual, status = None, "unsupported"
        why = []
        if ok:
            r = answers.get(cid)
            if r is None or "error" in r:
                status, why = "error", [(r or {}).get("error", "no answer")]
            else:
                actual = r["actual"]
                exp = c.get("expect") or {}
                status = "pass" if compare(c["kind"], exp, actual, (c.get("expect") or {}).get("tolerance") or c.get("tolerance"), why) else "fail"
                if a.record and not c["verified"]:
                    recorded.setdefault(p, {})[c["id"]] = {k: actual[k] for k in (exp or actual) if k in actual and k != "tolerance"}
        counts[status] += 1
        if status in ("fail", "error"):
            if c["verified"]:
                verified_failing += 1
            else:
                unverified_failing += 1
            failures.append({"case": cid, "status": status, "verified": c["verified"], "detail": "; ".join(why)[:300]})
            print(f"{status}: {cid}: {'; '.join(why)[:300]}", file=sys.stderr)
        results.append(canon({"case": cid, "kind": c["kind"], "status": status, "actual": actual}))
    out = Path(a.out or ROOT / "results" / f"{runtime}.jsonl")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("".join(r + "\n" for r in results), encoding="utf-8", newline="\n")
    summ = Path(a.summary or out.with_suffix("").with_suffix(".summary.json"))
    summ.write_text(json.dumps({"runtime": runtime, "runtimeVersion": hello.get("runtimeVersion"), "language": a.language or hello.get("language"), "profiles": a.profiles.split(","),
                                "cases": len(todo), **counts, "unverifiedFailing": unverified_failing, "failures": failures, "millis": int((time.time() - t0) * 1000)},
                               indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    if a.record:
        for p, per in recorded.items():
            n = rewrite_expects(p, per)
            print(f"recorded {n} expect line(s) in {p.relative_to(ROOT)}", file=sys.stderr)
    print(f"{runtime}: {len(todo)} cases: pass {counts['pass']}, fail {counts['fail']}, error {counts['error']}, unsupported {counts['unsupported']} "
          f"(unverified failing {unverified_failing}, verified failing {verified_failing})")
    return 1 if verified_failing else 3 if unverified_failing else 0


def flow(v) -> str:
    """A flow-style YAML mapping (JSON, which is YAML) of an expect: numbers as the case author reads them."""
    def f(x):
        if isinstance(x, float):
            return number_text(x).replace("E", "e") if "E" in number_text(x) else number_text(x)
        if isinstance(x, dict):
            return "{" + ", ".join(json.dumps(k, ensure_ascii=False) + ": " + f(y) for k, y in x.items()) + "}"
        if isinstance(x, list):
            return "[" + ", ".join(f(y) for y in x) + "]"
        return json.dumps(x, ensure_ascii=False)
    return f(v)


def rewrite_expects(path: Path, per) -> int:
    """RUNNER 6: only `expect` values change; a case whose expect is not one line is reported, never rewritten."""
    lines = path.read_text(encoding="utf-8").split("\n")
    n, i = 0, 0
    while i < len(lines):
        m = re.match(r'^\s*- id: "?([a-z0-9-]+)"?\s*$', lines[i])
        if m and m.group(1) in per:
            j = i + 1
            while j < len(lines) and not re.match(r"^\s*- id:", lines[j]):
                if re.match(r"^\s{4}expect: \{.*\}\s*$", lines[j]):
                    lines[j] = "    expect: " + flow(per[m.group(1)])
                    n += 1
                    break
                j += 1
        i += 1
    path.write_text("\n".join(lines), encoding="utf-8", newline="\n")
    return n


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--cases", default=str(ROOT / "conformance" / "1"))
    ap.add_argument("--runtime")
    ap.add_argument("--out")
    ap.add_argument("--summary")
    ap.add_argument("--profiles", default="core")
    ap.add_argument("--language", default=None, help="the language level run; default the runtime's own")
    ap.add_argument("--only")
    ap.add_argument("--kinds")
    ap.add_argument("--record", action="store_true")
    ap.add_argument("--cmd", help="the runtime program (RUNNER 5)")
    ap.add_argument("--classpath-file", help="a file holding the class path of Drishti's reference jars: runs tools/reference-bridge/RefBridge.java")
    ap.add_argument("--jvm", action="store_true", help="run Rachana's own JVM runtime (java/rachana-conformance, built by ./mvnw package)")
    ap.add_argument("--host-nodes", action="store_true", help="with --jvm: the documents as a host's own node type (HostNodes fixture)")
    ap.add_argument("--node", action="store_true", help="run the JavaScript build in Node (js/packages/rachana/dist, built by tools/js/build_js.py)")
    ap.add_argument("--bundle", help="with --node: the bundle to run (default js/packages/rachana/dist/rachana.tools.js)")
    a = ap.parse_args(argv)
    if a.jvm:
        target = ROOT / "java" / "rachana-conformance" / "target"
        cp_file = target / "runtime-classpath.txt"
        if not cp_file.is_file():
            ap.error(f"{cp_file} is missing: build first (cd java && ./mvnw clean package -DskipTests)")
        jars = sorted(str(j) for j in target.glob("rachana-conformance-*.jar") if not j.name.endswith(("-sources.jar", "-tests.jar")))
        mp = os.pathsep.join(jars + [cp_file.read_text().strip()])
        a.cmd = (f"java -p {shlex.quote(mp)} -m com.ash.rachana.conformance/com.ash.rachana.conformance.JvmRuntime "
                 f"{'--host-nodes ' if a.host_nodes else ''}--profiles core,drishti "
                 f"{ROOT / 'spec' / '1' / 'formats.yaml'} {os.environ.get('RACHANA_DRISHTI_FORMATS', DRISHTI_FORMATS)}")
    elif a.node:
        bundle = Path(a.bundle) if a.bundle else ROOT / "js" / "packages" / "rachana" / "dist" / "rachana.tools.js"
        if not bundle.is_file():
            ap.error(f"{bundle} is missing: build first (python3 tools/js/build_js.py)")
        a.cmd = (f"node {ROOT / 'js' / 'packages' / 'rachana' / 'runners' / 'conformance.mjs'} --bundle {shlex.quote(str(bundle))} "
                 f"--runtime {a.runtime or 'node'} --profiles core,drishti "
                 f"{ROOT / 'spec' / '1' / 'formats.yaml'} {os.environ.get('RACHANA_DRISHTI_FORMATS', DRISHTI_FORMATS)}")
    elif a.classpath_file:
        cp = Path(a.classpath_file).read_text().strip()
        a.cmd = f"java -cp {shlex.quote(cp)} {ROOT / 'tools' / 'reference-bridge' / 'RefBridge.java'} {ROOT / 'spec' / '1' / 'formats.yaml'} {os.environ.get('RACHANA_DRISHTI_FORMATS', DRISHTI_FORMATS)}"
    if not a.cmd:
        ap.error("give --cmd or --classpath-file")
    return run(a)


if __name__ == "__main__":
    sys.exit(main())
