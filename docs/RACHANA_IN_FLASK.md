<!-- Copyright (c) 2026 Ashutosh Sinha. Proprietary. All rights reserved. -->
# Rachana in your Flask app

You will add a Rachana screen to an existing Flask app: install the wheel, compile (check) a Sutra on the server, serve the page that
paints it with the browser bundle, and send a strict Content-Security-Policy with it. About fifteen minutes. The JavaScript facade
(`<rachana-view>` and the browser bundle) is **EXPERIMENTAL at 1.x**: its names may change in a minor release. The Python package
and the Sutra language are stable.

## Prepare your environment

Python 3.10 or later (tested on 3.11 and 3.13). The wheel is not on a public index: install the file you were given, with the `v8` extra (the compiler runs on
V8 inside Python) and the `flask` extra:

```bash
python3 -m venv venv && . venv/bin/activate          # Windows: venv\Scripts\activate
pip install 'drishti_rachana-1.1.1-py3-none-any.whl[v8,flask]'
python -c "import drishti_rachana; print(drishti_rachana.__version__)"      # 1.1.1
```

## Step 1: write and compile a Sutra

`sutras/sales.sutra.yaml`:

```yaml
rachana: 1.2
sutra: sales
version: 1
title: { id: "'Sales overview'" }
data:
  sales: { table: sales, key: [region, month] }
panels:
  - { id: revenue, kind: metric, label: Revenue, value: "sum($.sales, 'revenue')", fmt: amount0, span: 3 }
  - { id: by-region, kind: hbar, title: Revenue by region, rows: "group($.sales, 'region', 'revenue')", label: label, value: value, span: 9 }
```

Check it from Python before you serve it (the same compiler the browser runs, byte for byte):

```python
from pathlib import Path
from drishti_rachana.engine import Rachana

r = Rachana()
res = r.check(Path("sutras/sales.sutra.yaml").read_text(encoding="utf-8"))
assert res["ok"], res["problems"]
```

From the command line: `python -m drishti_rachana lint sutras`.

## Step 2: serve the Sutra, the data and the assets

```python
from flask import Flask, Response
from drishti_rachana.adapters.flask import RachanaBlueprint

app = Flask(__name__)
rachana = RachanaBlueprint(sutras="sutras")             # /sutras/<name>.sutra.yaml, /data/..., /static/... (the browser bundle)

@rachana.table("sales", key=["region", "month"])
def sales():
    return [{"region": "EMEA", "month": "2026-10", "revenue": 120_000},
            {"region": "APAC", "month": "2026-10", "revenue": 95_000}]    # your query here

app.register_blueprint(rachana, url_prefix="/rachana")
```

The blueprint serves, under its prefix: your Sutras, the Data Protocol (`/data`), and the assets the page loads: the compiler
bundle (`/static/rachana/rachana.runtime.js`), `<rachana-view>` (`/static/rachana/src/rachana_view.js`) and Drishti Elements
(`/static/vendor/drishti-elements/3.3.1/`). Nothing comes from a CDN.

## Step 3: the page, with a strict CSP

The blueprint has a ready page at `/rachana/page/sales` that you can use as it is. To own the page and keep scripts out of it (a strict policy
refuses inline script), write it with **external** scripts only:

```python
PAGE = """<!doctype html><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1">
<title>Sales</title>
<link rel=stylesheet href="/rachana/static/vendor/drishti-elements/3.3.1/drishti-elements.css">
<script src="/rachana/static/vendor/drishti-elements/3.3.1/drishti-elements.js"></script>
<script type="module" src="/rachana/static/rachana/src/rachana_view.js"></script>
<rachana-view src="/rachana/sutras/sales.sutra.yaml" data="/rachana/data"
              runtime="/rachana/static/rachana/rachana.runtime.js" live></rachana-view>"""

CSP = ("default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; "
       "font-src 'self' data:; connect-src 'self'; worker-src 'self' blob:; base-uri 'none'; form-action 'none'; "
       "frame-ancestors 'self'; object-src 'none'")

@app.get("/sales")
def sales_page():
    return Response(PAGE, mimetype="text/html", headers={"Content-Security-Policy": CSP})
```

Why each directive: `script-src 'self'` (the bundle, the element and Elements are files on your origin; there is no inline script and
no `eval`); `style-src 'unsafe-inline'` only for the style attributes the painter puts on charts; `connect-src 'self'` for the Data
Protocol (add the origin of another Data Protocol host only if you use one); `worker-src blob:` for the painter's workers.
Open `http://127.0.0.1:5000/sales`; the browser console shows no CSP violation.

## Step 4: run it

```bash
flask --app app run
```

The tiles and the bar chart come from `sales`; change the function's rows and reload. Make the table live with `@rachana.live("sales")`
(a generator of `(key, row)` changes) and the page updates without a reload: see [the live board tutorial](TUTORIAL.md#part-4-a-live-board-in-flask).

## Next

- The JavaScript side in a single-page app: [Rachana in a JavaScript SPA](RACHANA_IN_SPA.md).
- The Data Protocol your tables speak: [guide](../spec/1/PROTOCOL.md). FastAPI works the same way (`RachanaRouter`).
