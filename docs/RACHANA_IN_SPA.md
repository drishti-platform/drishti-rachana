<!-- Copyright (c) 2026 Ashutosh Sinha. Proprietary. All rights reserved. -->
# Rachana in a JavaScript SPA

You will render a Sutra inside a single-page app with `<rachana-view>`, give it your data, and listen to its events. The package is the same
code the Python wheel serves. **The JavaScript facade is EXPERIMENTAL at 1.x**: the names below may change in a minor release;
the Sutra language and the ViewModel are stable.

## Prepare your environment

Node 20 or later and a bundler (Vite, webpack, esbuild) or none. Two ways to get the files, both from the release you were given:

- **npm package** `drishti-rachana-1.1.1.tgz`: `npm install ./drishti-rachana-1.1.1.tgz` (it is not on a public registry). It holds the
  source modules (`node_modules/drishti-rachana/src/`) and the compiler bundles under **`node_modules/drishti-rachana/dist/`**
  (`rachana.runtime.js`, `rachana.tools.js`). It does **not** hold Drishti Elements or Apache Arrow: take `vendor/` from the browser zip.
- **Browser zip** `drishti-rachana-1.1.1-browser.zip`: it unpacks into one folder, `drishti-rachana-1.1.1-browser/`. **Move the
  contents of that folder** (not the folder) into a folder your server serves, for example `public/rachana/`, so that
  `public/rachana/rachana.runtime.js`, `public/rachana/src/rachana_view.js` and `public/rachana/vendor/` exist. It holds the compiler
  bundles (`rachana.runtime.js`, `rachana.tools.js`), the source modules (`src/`), Drishti Elements and Apache Arrow (vendored) and the LICENSE.

The facade is ES modules only (no UMD build, no type declarations yet). Drishti Elements (the painter) is a classic script; load it before the module. Serve every file from your own origin.
The file in the examples below is `public/app.js`; the Sutra's rows come from your own Data Protocol address (`/api/rdp` below) or a record.

## Step 1: render a Sutra

With the browser zip (no build step). `public/index.html`:

```html
<link rel="stylesheet" href="/rachana/vendor/drishti-elements/3.3.1/drishti-elements.css">
<script src="/rachana/vendor/drishti-elements/3.3.1/drishti-elements.js"></script>
<script type="module" src="/rachana/src/rachana_view.js"></script>      <!-- registers <rachana-view> -->
<script type="module" src="/app.js"></script>                           <!-- your page's code: an external file, never inline -->

<rachana-view id="board" runtime="/rachana/rachana.runtime.js"></rachana-view>
```

Every script is an external file served from your own origin, so the page runs under the strict policy of step 3 (an inline script
would be refused by it).

With the npm package, serve `node_modules/drishti-rachana/src/` and `node_modules/drishti-rachana/dist/rachana.runtime.js` as static
assets (or import the element from your bundler: `import 'drishti-rachana/src/rachana_view.js';`) and serve the two Elements files and
`vendor/apache-arrow/` from the browser zip's `vendor/` folder, as above.

Give it the Sutra as text and the rows as a record or a Data Protocol host. `public/app.js`:

```js
const view = document.getElementById('board');
view.sutra = `
rachana: 1.2
sutra: sales
version: 1
title: { id: "'Sales overview'" }
data:
  sales: { table: sales, key: [region] }
panels:
  - { id: revenue, kind: metric, label: Revenue, value: "sum($.sales, 'revenue')", fmt: amount0, span: 3 }
`;
view.setAttribute('data', '/api/rdp');                    // your Data Protocol base (JSON rows); or src="..." for the Sutra by address
```

Attributes: `src` (the Sutra's address), `data` (the Data Protocol base), `runtime`, `live` (follow live tables). Properties: `sutra`,
`record`, `controls`, `linkResolver`, `host`, `fetch`, `files`. Setting `sutra` or changing an attribute repaints.

## Step 2: events

```js
view.addEventListener('rachana-view:ready',  (e) => console.log('painted', e.detail.viewModel));
view.addEventListener('rachana-view:change', (e) => console.log('live change', e.detail));
view.addEventListener('rachana-view:error',  (e) => console.error(e.detail.error));
view.addEventListener('rachana-view:write',  (e) => console.log('edit', e.detail));    // a live table's cell or record saved
```

A link in the view gets its address only from `view.linkResolver = (link) => absoluteUrlOrNull`; without one it is a button that raises
`elements-view:link`, so the page decides where the user goes. `view.host = { notify(effect, where), run(effect, where) }` handles a
Sutra's own `notify` and `host` effects (the painter runs none itself).

## Step 3: a strict Content-Security-Policy

Serve the page with `default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self' data:;
connect-src 'self'; worker-src 'self' blob:; base-uri 'none'; form-action 'none'; object-src 'none'`. The bundles use no `eval`
and no inline script; add the origin of a separate Data Protocol host to `connect-src` only if you use one. The Python side: [Rachana in your Flask app](RACHANA_IN_FLASK.md).

## Next

- Without a framework wrapper, mount `<rachana-view>` in a component and set its `sutra` property from state (React: a `ref`; Vue: a template ref).
- The Data Protocol: [guide](../spec/1/PROTOCOL.md). The element's full attribute list is the header of `src/rachana_view.js`.
