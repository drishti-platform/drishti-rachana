# Copyright (c) 2026 Ashutosh Sinha <ajsinha@gmail.com>. All rights reserved.
# Drishti Rachana. Proprietary: see the LICENSE file at the root of this repository.
"""Every relative Markdown link in the repository's own documents resolves to a file (or folder)."""
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LINK = re.compile(r"\]\(([^)\s]+)\)")
FENCE = re.compile(r"^\s*```")
SKIP = ("js/vendor/", "node_modules/", "compat/", "/target/", "CHANGELOG.md")


def _documents():
    try:
        out = subprocess.run(["git", "ls-files", "*.md"], cwd=ROOT, capture_output=True, text=True, check=True).stdout
        names = out.splitlines()
    except Exception:
        names = [str(p.relative_to(ROOT)) for p in ROOT.rglob("*.md")]
    return [n for n in names if not any(s in n for s in SKIP) and (ROOT / n).is_file()]


def test_relative_markdown_links_resolve():
    broken = []
    for name in _documents():
        path = ROOT / name
        in_fence = False
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if FENCE.match(line):
                in_fence = not in_fence
                continue
            if in_fence:
                continue
            for target in LINK.findall(line):
                if re.match(r"^([a-z][a-z0-9+.-]*:|#|/)", target):
                    continue
                file_part = target.split("#", 1)[0]
                if file_part and not (path.parent / file_part).exists():
                    broken.append(f"{name}:{number}: {target}")
    assert not broken, "broken relative links:\n" + "\n".join(broken)
