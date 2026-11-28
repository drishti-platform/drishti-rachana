---
# Copyright (c) 2026 Ashutosh Sinha <ajsinha@gmail.com>. All rights reserved. Drishti Rachana. Proprietary: see LICENSE.
product: Drishti Rachana
summary: A declarative UI platform - describe a screen in a short YAML Sutra, supply data over the Rachana Data Protocol, get a fast, dense, professional screen painted by Drishti Elements.
language: "rachana: 1 (levels 1.0 to 1.2)"
parts: [specification, conformance suite, Java compiler, JavaScript build, Python package, Data Protocol, live tables, server, CLI, Studio, libraries]
runtimes: [JVM (Java 21), Node 20+, browsers, Python 3.10+ on V8]
install-order: [tools, Rachana, Lake, Drishti]  # the platform build order
licence: proprietary (LICENSE); specification and conformance suite under LICENSE-SPEC.md (draft)
author: Ashutosh Sinha
---

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="brand/drishti_rachana-logo.svg">
    <img src="brand/drishti_rachana-logo-light.svg" alt="Drishti Rachana" width="300">
  </picture>
</p>

# Drishti Rachana

**Describe a screen. Supply the data. Get a fast, professional UI.** Screens become assembly, not writing.

You write a **Sutra**, a short YAML file in the **Rachana** language that says what a screen shows and where each value comes from.
Rachana compiles it with your data into a **ViewModel** (plain JSON: every title, figure, column and row worked out) and paints it with
**Drishti Elements**: grids, charts, maps, pivots, forms and live updates. A Sutra holds no code and no pixels, so it is safe to review,
to keep in version control and to have an AI assistant write.

```yaml
rachana: 1.2
sutra: sales-overview
version: 1
title: { id: "'Sales overview'" }
data:
  sales: { table: sales, key: [region, product, month] }
panels:
  - { id: revenue, kind: metric, label: Revenue, value: "sum($.sales, 'revenue')", fmt: amount0, span: 3 }
  - { id: by-region, kind: hbar, title: Revenue by region, rows: "group($.sales, 'region', 'revenue')", label: label, value: value, span: 9 }
```

## What it is made of

| Part | What it does | Where |
|---|---|---|
| **Specification** | the language (`rachana: 1`, levels 1.0 to 1.2), the expression language Rachana-EL, the function catalogue, formats, the translation rules and the ViewModel schema | [SPEC.md](spec/1/SPEC.md) |
| **Data Protocol** | tables as JSON, Arrow or NDJSON rows; paging, filters, grouping; live keyed patches over SSE or WebSocket; an in-process bridge | [PROTOCOL.md](spec/1/PROTOCOL.md) |
| **Conformance suite** | 2,604 cases (a Sutra plus data in, the exact expected result out); any runtime that passes them is Rachana | [CONFORMANCE.md](conformance/CONFORMANCE.md) |
| **Compiler** | one reference implementation in Java; built for JavaScript (TeaVM); Python runs that bundle on V8; byte-identical answers everywhere | [DESIGN.md](docs/DESIGN.md), [DEVELOPING.md](docs/DEVELOPING.md) |
| **`<rachana-view>`** | compiles and binds a Sutra in the page and paints it with Drishti Elements | `js/packages/rachana` |
| **Live tables, server, CLI, Studio, SDKs** | tables kept current by key and version with guarded edits and approvals; a server over files, JDBC and REST; `rachana new`, `lint`, `preview`, `build`, `studio`, `lib`, `serve`; a visual editor | [IN_YOUR_APP.md](docs/IN_YOUR_APP.md), [WRITING_SUTRAS.md](docs/WRITING_SUTRAS.md) |
| **Libraries** | typed fragments to assemble screens from: `core` (34 fragments) and `finance` (57); your own with `rachana lib` | [libraries/core](libraries/core/README.md), [libraries/finance](libraries/finance/README.md) |
| **Flask and SPA guides** | the short customer paths: [Rachana in your Flask app](docs/RACHANA_IN_FLASK.md) (wheel, compile a Sutra, the page, strict CSP) and [Rachana in a JavaScript SPA](docs/RACHANA_IN_SPA.md) (npm or browser zip, render, events); the JavaScript facade is experimental at 1.x | [Flask](docs/RACHANA_IN_FLASK.md), [SPA](docs/RACHANA_IN_SPA.md) |

## Ways to use it

| Mode | For | How |
|---|---|---|
| Static | analysts | Sutras plus CSV, JSON or Arrow files; `rachana build` writes a site that needs no backend |
| Embedded | app developers | `<rachana-view>` plus a Sutra inside any web app, on your own Data Protocol endpoint |
| Python app | Flask and FastAPI teams | `drishti_rachana`: mount Sutras, expose tables, stream changes |
| Server | teams | the Rachana server: Sutras over files, databases and REST, live updates, sign-in, edits with approvals |
| Drishti | enterprises | the Drishti product, built on Rachana |

## Install from the checkout

Rachana is installed from this checkout, or from a wheel or jar file you built from it; never from a package registry. There is no `pip install <name>`, `npm install <name>` or Maven Central step.

| Need | Version | For |
|---|---|---|
| JDK | 21 | the Java build, the compiler, the server, the CLI (`java/mvnw` downloads Maven) |
| Node | 20 or later | the JavaScript packages, Studio, the dev server |
| Python | 3.13 (3.10 is the package's minimum) | the Python SDK, the JavaScript bundle build, the conformance runner and the checks |

```bash
python3 tools/js/build_js.py                       # optional: rebuild the compiler bundles (JDK 21); committed copies are in python/src/drishti_rachana/bundle
cd java && ./mvnw clean verify -DskipTests && cd ..   # the jars (server, library tool)
python3 -m pip install -e 'python[v8,flask]'       # the Python package, editable, from this checkout, into the Python you chose
```

Rachana never makes a virtual environment and never installs anything for you. Every build and test command, the IDE launchers and installing a release tag for another product are in [DEVELOPING.md](docs/DEVELOPING.md).

## Five-minute start

```bash
alias rachana="node $PWD/js/packages/rachana-cli/bin/rachana.js"
rachana new my-dash && cd my-dash
rachana lint                                       # 1 file(s), 0 failing problem(s)
rachana preview sutras/overview.sutra.yaml --data data --port 4400    # open http://localhost:4400
rachana build --out site                           # a static site any web server serves
```

Then [the tutorial](docs/TUTORIAL.md) writes a real dashboard, a library, a live table with approvals and a Python app.

## The documents

| Document | For |
|---|---|
| [TUTORIAL.md](docs/TUTORIAL.md) | four hands-on parts: a dashboard from a CSV file, a typed library, a live table with approvals, a live board in Flask |
| [WRITING_SUTRAS.md](docs/WRITING_SUTRAS.md) | the language (layout, controls, forms, recipes, expressions step by step, an annotated example, common mistakes), libraries and `rachana lib`, Studio |
| [PANEL_KINDS.md](docs/PANEL_KINDS.md) | every panel kind: when to use it, when not, what each option does; row groups and totals |
| [IN_YOUR_APP.md](docs/IN_YOUR_APP.md) | Flask and FastAPI integration, the server, live tables and editing, the Data Protocol in use, the CLI and SDKs |
| [examples/README.md](examples/README.md) | the three runnable examples |
| [SPEC.md](spec/1/SPEC.md) | the normative specification, the translation rules, the ViewModel contract |
| [PROTOCOL.md](spec/1/PROTOCOL.md) | the Data Protocol |
| [CONFORMANCE.md](conformance/CONFORMANCE.md) | the case format, the runner contract, known differences |
| [DESIGN.md](docs/DESIGN.md) | the design as built: Core and profiles, compiler and runtimes, libraries, live tables, the GenUI boundary |
| [DECISIONS.md](docs/DECISIONS.md) | the decisions behind it |
| [ROADMAP.md](docs/ROADMAP.md) | what is planned |
| [DEVELOPING.md](docs/DEVELOPING.md) | build and test every runtime, the IDE launchers, the tools |
| [libraries/core](libraries/core/README.md), [libraries/finance](libraries/finance/README.md) | the fragments of each library |
| [CHANGELOG.md](CHANGELOG.md), [LICENSE-SPEC.md](LICENSE-SPEC.md) | what changed when; the licence of the specification |

## Build and run in the IDE

Every launcher is checked in under `run-configs/` at the repository root (`run-configs/*.run.xml`, named `Rachana: <verb> <what>`), with no bash. Open the **repository root** in IntelliJ IDEA or PyCharm (in IntelliJ link `java/pom.xml` in the Maven tool window); both read the same files, and VS Code reads `drishti-rachana.code-workspace`. The launcher groups are IntelliJ-rachana-build, -test, -check, -run and -install, and PyCharm-rachana-test, -check and -run. Prerequisites, the Windows `PKIX path building failed` fix and the launcher table: [DEVELOPING.md](docs/DEVELOPING.md#build-and-run-in-the-ide).

## Where it fits the Drishti platform

Rachana is the second product of the platform build: Drishti Tools, Drishti Rachana, Drishti Lake, Drishti, in that order, each built against the released tag of the one before, never a branch. Only `rachana-server` uses Drishti Tools (`tools-csv`); the other modules use nothing of it. The install steps are in [DEVELOPING.md](docs/DEVELOPING.md).

## Licence

Author: Ashutosh Sinha. Proprietary, all rights reserved ([LICENSE](LICENSE)). The specification and the conformance suite (`spec/`,
`conformance/`) are intended to be under the permissive [LICENSE-SPEC.md](LICENSE-SPEC.md), still a draft and not yet in force.

## Rachana fragment libraries

Typed fragments, grouped by domain, built and released with Rachana. Each library is a folder here (`<name>.rachana-lib.yaml`,
`fragments/`, `examples/`, `goldens/`, a generated `README.md`).

| Library | Status | What it holds |
|---|---|---|
| [core](libraries/core/README.md) | built, 1.1.0 (34 fragments) | generic screens for any domain: figures, records, tables, basic charts, forms, states, history |
| [finance](libraries/finance/README.md) | built, 1.0.0 (57 fragments, two examples each) | trading, P&L, market risk, limits, counterparty and collateral, market data, funds; generic terms, every figure arrives as data |
| genomics | planned | |
| economy | planned | |
| geo | planned | |
| ops | planned | |
| ai | planned | |

Build, test, sign and pack: `python3 tools/libraries/build.py [--update-goldens] [LIBRARY ...]` (after `cd java && ./mvnw clean package -DskipTests`;
the jars come from this repository's build, nothing is installed). The check alone, `tools/libraries/LibCheck.java`, also runs
in the Maven build (`LibrariesCheckTest` in rachana-cli). `rachana studio` offers these libraries by default.

### Adding your own

The libraries here are one source among others, never the only one. Drop a library folder or an `.rlib` into a folder and name it:
`rachana lint|build|studio --libs <path>[,<path>]` (`python -m drishti_rachana lint --libs` too), or `RACHANA_LIBS=<path>[:<path>]`, or
`<project>/rachana_libs`. Nothing in Rachana is modified or rebuilt. A name found twice with different contents is refused with a
problem, never silently shadowed (rename or remove one).

### Notes on a few fragments

- `core/entry-form`, `core/stepped-form`: no `initial` (a form panel's `initial:` names fields, which a list parameter cannot know) and no draft kept between visits; `stepped-form` takes `steps: list<record{id, label}>` and fields that each name their `step`. Both take `rules` and name both buttons.
- `finance/order-ticket`: no `max-qty` (a form rule cannot read a parameter) and no slots; `side` is an expression (`"'BUY'"`); the ticket raises `submit {order}` with every field, `instrument` included.
- `finance/curve-table`: `df-fmt` defaults to `df4`; `finance/order-ticket` and `finance/performance-chart` keep their own format parameters.
- `finance/what-if-shocks`: a form of number fields (`shocks`, each `{id, label, type: number, min, max}`); `change {shocks}` hands the shocks to the screen, and the estimate is the host's (`estimate`), never computed by Rachana.

## JavaScript packages

The npm workspace of Drishti Rachana's JavaScript packages (Node 20+). How to use them: [IN_YOUR_APP.md](docs/IN_YOUR_APP.md#command-line-and-sdks).

| Package | npm name | What |
|---|---|---|
| `packages/rachana` | `drishti-rachana` | the compiler bundles (`dist/`, built by `python3 tools/js/build_js.py`), the facade, `<rachana-view>`, the Data Protocol client (HTTP and the in-process bridge), the static-files host |
| `packages/rachana-cli` | `drishti-rachana-cli` | the `rachana` command: new, lint, preview, build, studio, lib, serve |
| `packages/rachana-studio` | `drishti-rachana-studio` | Rachana Studio ([guide](docs/WRITING_SUTRAS.md#rachana-studio)) |
| `packages/rachana-electron` | `drishti-rachana-electron` | placeholder for the Electron host SDK; no code yet |
| `vendor/drishti-elements/<version>/` | (the pinned painter, copied unchanged) | Drishti Elements, with its SHA-384 manifest |

**One compiler.** The JavaScript compiler is generated from the Java reference (`java/rachana-js`, TeaVM) into `packages/rachana/dist`;
nothing in `js/` re-implements the language. The conformance suite proves the generated JavaScript matches the JVM byte for byte.

Tests: `java/mvnw -f js/pom.xml exec:exec@node-tests` (every `packages/*/test/*.test.mjs` through `node --test`), or the IDE launcher
**Rachana JS Node tests**.

## Python package

**EXPERIMENTAL at Rachana 1.0 (gate 2, O9):** every module of `drishti_rachana` may change in a minor release; the frozen
contract is the documents it carries (the ViewModel schemas, the Data Protocol, the conformance case format).

Python package for Drishti Rachana; import name `drishti_rachana`: the engine on V8, `lint`, a Data Protocol host, the
Flask and FastAPI adapters, the protocol types and the conformance runner. How to use it: [IN_YOUR_APP.md](docs/IN_YOUR_APP.md#python)
and the tutorial [A live board in Flask](docs/TUTORIAL.md#part-4-a-live-board-in-flask). It holds no language semantics: no parser, operator table or
formatter: compiling, linting and evaluating run the JavaScript bundle on V8 (`drishti_rachana.engine`).

| Module | What it is |
|---|---|
| `drishti_rachana.protocol` | Data Protocol v1 messages as dataclasses with `to_json` / `from_json`: table schema, rows page, query and filter tree, keyed patch, error. Standard library only. |
| `drishti_rachana.conformance` | the runner of [the runner contract](conformance/CONFORMANCE.md#the-runner-contract): loads and validates case files, runs an `Adapter`, compares (FORMAT 6, 7), writes `results/<runtime>.jsonl` and the summary, exit codes 0/1/2/3. `NoneAdapter` marks every case `unsupported` |
| `drishti_rachana.server`, `.rows_forms`, `.subscription`, `.bridge` | `DataHost`: a framework-neutral Data Protocol v1 host as the JVM reference is (schema, rows, query with the filter tree, sort, cursor paging, `group` with totals, keyed patches, declared schemas with `can` enforced, masked columns, key-ordered stable pages). Rows as JSON, as **Arrow IPC** (the official pyarrow, extra `arrow`; `406` when it is not installed or the table does not declare `arrow`) and as **NDJSON**; the SSE change stream (`GET` or `POST /changes`) with **filtered** (`changes.filter`, PROTOCOL 8.2) and **conflated** (`conflate: {everyMs}`, 8.4) subscriptions and gap frames; `Bridge` answers the in-process bridge incl. `sub`/`unsub`/`msg` and `res.arrow`. `drishti_rachana.testhost` serves it with the test adapter of PROTOCOL 15: `python tools/conformance/protocol_check.py --python` runs all 55 `protocol` cases against it as `--reference` does against the JVM (54 pass; the frozen `bridge-rows-request-answered-once` fails on both) |
| `drishti_rachana.assets` | the browser assets of `<rachana-view>`: the element and modules, the vendored Drishti Elements 3.3.1 pack, the runtime bundle (and Apache Arrow JS), looked up in a fixed table (package data in a wheel, `js/` in a checkout; no path is joined from the caller) with their media types |
| `drishti_rachana.adapters.flask`, `.fastapi` | `RachanaBlueprint` / `RachanaRouter`: mount Sutras, `@table(..., schema=)`, `@live`; routes `/`, `/page/<name>` (the page with the real `<rachana-view>`), `/sutras/<name>.sutra.yaml`, `/static/...` (its assets), `/data/...` (the Data Protocol, `/data/changes` GET or POST) |
| `python -m drishti_rachana lint` | compile and lint Sutras on V8 (install `python[v8]` from this checkout): `--format text\|json\|sarif`, `--strict`; each Sutra at the level its `rachana:` key declares (SPEC 2.1); exit 0, 1 on errors, 2 usage |
| `drishti_rachana.cli` | `python -m drishti_rachana conformance run` (Python installs no `rachana` program: that name is the Node CLI) |
| `drishti_rachana.engine` | **Rachana without a JVM** (decision W-1): `Rachana()` runs the JavaScript bundle (A15) on V8 through `mini-racer`: `check(sutra)` compiles and lints (each Sutra at its declared level), `evaluate`, `template`, `format`, `view`, `edit`. The answers are the bundle's, byte-identical to the JVM on every conformance case. Extra `v8`; the bundle is built by `python3 tools/js/build_js.py` into `drishti_rachana/bundle/` |

```bash
pip install -e 'python[conformance]'          # PyYAML + jsonschema to read case files
python -m drishti_rachana conformance run --cases conformance/1 # adapter 'none': every case unsupported, exit 0
python -m drishti_rachana conformance run --adapter mypkg.mod:MyAdapter --only 'el/**'

python3 tools/js/build_js.py                  # the bundles, into drishti_rachana/bundle/
pip install -e 'python[v8]'
python -c "from drishti_rachana.engine import Rachana; print(Rachana().check(open('examples/static-sales/overview.sutra.yaml').read()))"
```

Not yet: `--cmd` (cases on stdin, RUNNER 5), per-case timeouts, `--record` (JVM reference only).

Optional extras: `flask`, `fastapi`, `arrow`, `polars`, `conformance`, `test`. Tests: `python -m pytest -q python/tests`. The live-orders board paints in Chromium: `python3 examples/live-orders/paint_check.py` (flask and playwright installed in your Python) (needs the runtime bundle, `python3 tools/js/build_js.py`).

Copyright (c) 2026 Ashutosh Sinha. Proprietary; see `LICENSE` at the repository root.
