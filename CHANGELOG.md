<!-- Copyright (c) 2026 Ashutosh Sinha. Proprietary: see LICENSE. -->
# Changelog

Entries are written per release; the regime's unreleased work is listed after the releases it follows.

## v1.1.1 (2026-11-28)

- **Core fragments take the panel keys a screen needs** (library `core` 1.1.0, language unchanged): `core/data-table` gains optional `key` (F1 to F12), `code`, `empty`, `when`, `span` and `where` (a function over the row: `lambda r: r.AMT_USD > 0`); `core/record-card` gains `title`, `key`, `code`, `when` and `span`, and its head is optional (`name`, `subtitle` and `status` may each be missing; with a `title` it is a plain record view, the headless record list); the new `core/tabbed-views` is a tabs panel over a list (`each`, a `tab-title` function, `layout`, and a `body` slot holding the panels every tab shows, `@` being the tab's record). `source:` and `waits:` stay what a use already carries (`use:` takes `source`, `when`, `waits`); `pivot` is a host profile feature and not a Core parameter. Every earlier use compiles to the same ViewModel (the goldens are unchanged); the new fragment has an example and a golden; the other uses are proved in `CoreFragmentParamsTest` and `sutra/fragments-panel-keys`.
- **A use of `core/data-table`, `core/record-card` or `core/tabbed-views` may keep its own panel id** (library `core` 1.1.0): the optional parameter `id` names the fragment's first panel exactly (`as: holidays` with `id: holidays` gives the panel `holidays`, not `holidays-table`), so function keys, `maskedPanels`, events and `panel:` links keep working on a converted screen; the fragment's other panels stay `<as>-<id>` (record-card's status panel is `<as>-state`). A fragment of any library gets the same by declaring `id: { type: id, optional: true }` . A use that does not give it compiles to the same ViewModel as before (the goldens are unchanged). Conformance: `sutra/fragments-panel-keys` (one more case); `CoreFragmentParamsTest`.
- **Compiler** (`TypedFragment`): a panel key of `key`, `code`, `empty`, `when`, `where`, `span`, `height`, `pivot` or `waits` that a fragment writes as a whole `arg.x`, `%{x}` or `arg.f(@)` of an optional parameter the use leaves out is left out of the panel (it was written as the text `null`); a slot placed inside a panel's own `panels:` list (a tabs panel) is filled with the use's panels. Conformance: `sutra/fragments-panel-keys.yaml` (three cases).
- **The GenUI kit's MCP tools ship only as a sajhamcpserver plugin** (owner, 2026-11-28): the `js/packages/rachana-mcp` placeholder and the "generic Rachana MCP server" are retired (DECISIONS D31 withdrawn, ROADMAP and AI_GENUI reworded). Version 1.1.1 (poms, Facade, Python package, npm packages, plugin dependency).
- **Fixed: the examples did not run from a zip of the platform.** `pip install -e python` failed (`python/README.md` was missing), and the browser runtime bundle (`rachana.runtime.js`) and the compiler bundle (`rachana.tools.js`) were git-ignored build output, so the live-orders page was blank (404) and the `rachana` command stopped with "missing". Both bundles and `SHA256SUMS` are now committed in `python/src/drishti_rachana/bundle/` (the `rachana` command falls back to them when `dist/` is not built); a page whose bundle is missing says so. A README in each example (`live-orders`, `static-sales`, `server-demo`), `examples/live-orders/requirements.txt`, `flask_app.py [port]`, `rachana dev` now serves a page that paints (the static-files host was installed for `data/` beside `/page/<name>`, so the static-sales dev page stayed blank), the server jar takes its runtime bundle from the committed folder (a build from a zip had none in the jar), and `python/tests/test_examples.py` (both apps in process: page, bundle, data answer 200; both bundles match their recorded hash).

## v1.1.0 (2026-11-22)

- **The doc block is part of the language** (SPEC 2.9): `$defs/doc` of `sutra.schema.json` (summary, description, category, tags, use-when, avoid-when, data {table, in-words, sample-row}, example, see-also, keywords, and the earlier docstring keys), `docLine`, `docOf`, and the typed fragment file with its parameter, column and slot; the Sutra's and a panel's `doc` refer to them (a panel takes `doc:`; a parameter's, slot's or event's doc may be a block). New lint **`RCH-2093`** (`DocBlockLint`): a missing or thin doc block, field by field, placed, with its JSON Pointer; library gate LG3 fails on it; guard `LibraryDocBlocksTest`. Every fragment of `libraries/core` and `libraries/finance` carries the new fields (MANIFEST.sha384 rewritten). The spec files are LangFiles' output for 1.2. **A Sutra's and a panel's `doc` take the loose form** (`docLoose`, `docLooseOf`: an object, nothing required, other keys allowed, no length limit: exactly what 1.0.1 accepted), so no Sutra that 1.0.1 accepted is refused by the schema; the strict `doc` is the typed fragment file's and a library fragment's, and thinness is the lint `RCH-2093`, never a schema refusal (`schema_cases.py` holds three cases). **For library authors: library gate LG3 now fails a library whose fragments lack the new doc fields** (summary, description, use-when, data, example and the rest), so a library built for 1.0.x needs its doc blocks before it passes the gate; a Sutra without a doc block is unaffected (the lint is a warning by default).
- **GenUI kit lite for SAJHA** (`tools/sajha/`): the plugin with `search_fragments`, `get_fragment`, `check_sutra`, `publish_ui`; the catalogue from doc blocks (text and data-shape search); the check (kit rules `RGK-1xx`, then the compiler on V8; check tokens); rows fetched by the kit as the user, never through the model; boards pushed over SSE to a pure-JS page with `<rachana-view>` (strict CSP, no CDN); audit hooks; the risk mash-up example; README, guide, tool contracts; pytest. Design: `docs/GENUI_KIT.md`.
- **Two customer guides**: `docs/RACHANA_IN_FLASK.md` (wheel, compile a Sutra on the server, the page, a strict Content-Security-Policy) and `docs/RACHANA_IN_SPA.md` (npm package or browser zip, `<rachana-view>`, events, strict CSP; ES modules only, no type declarations yet). Python 3.10 or later (tested on 3.11 and 3.13).
- **`Rachana.close()`** (Python): frees the V8 isolate; also registered at interpreter exit, because a live isolate kept the interpreter from exiting on Python 3.13.
- **The plugin zip is reproducible**: `tools/sajha/package.py` builds it from the tree (INSTALL.md is `tools/sajha/INSTALL.md`; links that leave the kit's folder are reduced to text). The board server has no sign-in of its own: a board's link is the access (stated in INSTALL.md, the kit README and `GENUI_KIT.md`).

## v1.0.1 (2026-11-22)

Patch: the published Sutra schema only (no language change, no case changed, no API change). A Sutra the 1.2 compiler accepts did not validate against `spec/1/sutra.schema.json`.

- **Fixed: `spec/1/sutra.schema.json` (and `Rachana.schema()`, the schema every editor reads) lacked constructs the 1.2 grammar accepts.** The file is generated from the grammar by `RachanaSchema`, which wrote Core's keys by hand and took only a profile's keys from `Grammar`; the keys a later level brings (`Grammar.LEVEL_KEYS`) were never written into it. Now in the schema: the top-level `data`, `libraries`, `let`, `functions` and `state`; a panel's `on` and `actions` and a control's `on` (effects by trigger); the panel option `form` for every kind; a use's `as`, `slots`, `on` and its wiring keys; a panel or a use anywhere panels are listed (a `use` item is no longer refused); a tab's `body` without an `id`; `point-click`, `submit` and `cancel` as an event's `on`; the pivot `mode`; an expression option given as a number or true/false (a gauge's `max`); a table `limit` given as an expression (`ctl.top`); a library `use` of the form `lib@1/fragment@1`. The per-kind rules now apply only to a panel that names its kind (they applied to a panel without one, so every kind's required options were demanded of a `use`).
- **Guard: the schema cannot drift again.** `tools/conformance/schema_cases.py` validates every Sutra of the conformance cases that is not expected to fail, and every Sutra of `examples/` and `libraries/`, against the published schema (351 Sutras); `SchemaCasesGuardTest` (rachana-guards) runs it in `./mvnw verify` (skipped, with the reason printed, on a machine without python3, PyYAML and jsonschema). A Sutra that compiles and does not validate fails the build. The Drishti profile's constructs are left out (the schema is Core's); three places where the compiler ignores a value are listed in the script with the reason (the list only shrinks).
- **Fixed (patch gate): the schema was looser than the compiler in two places.** `rows`, `each`, `nodes`, `edges`, `source` and `children` are text only again (`rows: 5` is refused, as RCH-2029 does), and a table `limit` is a whole number of 1 or more or text holding `ctl.` (`limit: -5`, `0` and `many` are refused again). `schema_cases.py` now also proves the other direction: `MUST_REFUSE` names conformance cases the compiler refuses and the schema must refuse; the number of refused cases that validate (semantic problems) may not grow past a committed ceiling (194), nor may `LENIENT` (3).

## v1.0.0 (2026-11-21)

- **Level retirement: one language level, 1.2** (owner decision UP-0). `rachana: 1` and `rachana: 1.2` are 1.2; a Sutra or fragment that declares 1.0 or 1.1 is refused (`RCH-2009`) with "use rachana 0.12 upgrade". `Grammar.LEVEL` is the one level (`LEVELS` holds it alone, `MAJOR_ONLY` is it), every compiler, printer, format table and checker defaults to it, and the 1.0 and 1.1 readings are gone: the replay of level 1.0 (`compat/levels`, `moved-cases.yaml`, `level-switches.yaml`, `tools/regime/levels.py`, `LevelReplayTest`), the frozen 1.0 language files, the per-level cases and the 1.0 printer forms. `spec/1/rachana.lang.json` and `sutra.schema.json` are generated at 1.2.
- **`rachana upgrade` is removed from 1.0.0** (the Java `Upgrade`, `UpgradeCommand`, `UpgradeRule`, the Node and Python commands, `docs/guides/UPGRADE.md`, `compat/upgrade`). It lives in Rachana 0.12.0: run `rachana upgrade` from 0.12 on a Sutra written at 1.0 or 1.1, then use 1.0.0.
- **Corpus re-recorded at 1.2** (`compat/customers/drishti/2.9.11.i7b`, 481 Sutras, none with a problem): the Sutras are Drishti's own, upgraded to 1.2; the documents are compiled by Core (`tools/regime/CorpusCompileCore.java`, `corpus_export.py`). The reference's recorded binds ended with level 1.0 and are not recorded again (Core is the one implementation; the conformance suite holds the binder).
- **Fixed**: from level 1.1 the regular-expression functions ran on the default table, so an instance's own caps (`RachanaOptions.withRegexLimits`) did not hold; they run on the instance's.
- **Function catalogue examples** write a condition on an aggregate as `calc(sum(x), where = ...)`, the only form the language reads at 1.2.
- **Elements 3.3.1** (release commit 20661f0) vendored: `js/vendor/drishti-elements/3.3.1/` (SHA-384 manifest), the server pom, Python assets, Studio and docs name it.
- **Cases pinned to levels 1.0 and 1.1 are removed** (the level-declaration suites are rewritten for the one level; `until:` cases and the 1.0-only syntax and number cases are gone; `FROZEN.sha384` rewritten).
- **API removals** (logged in `java/api-removals.txt`): `tools.Upgrade*`, `Grammar.LEVEL_1_0` (now `Grammar.LEVEL`), `Printer.print(Expr, boolean)`.
- **`LiveTables.undeclare(name)`** (G4 gate): a host that releases a table it made (Drishti: a search pushed out of its live sources) takes the declaration out, so its spec no longer stays in the engine. Idempotent (answers whether it was declared); the table's tray, edit switch and open table go with it; every open subscription of it (and of its tray) ends with one `gap` frame `recoverable: false, expired: true` (the bridge passes `expired` on); a later read, write or subscription is `404 rdp.table.unknown` with `expired: true`, never a row held before, until the name is declared again (PROTOCOL 16, `error.json`, `patch.json`). A replaced declaration now also drops its cached edit form. Test: `UndeclareAndReusedIdTest`.
- **A reused change id with other content is refused** (G4 gate): a repeated write id was answered from `ChangeSink.prior(id)`, which carries no content, so the same id with a different change was answered as the first change. New SPI `ChangeSink.priorDigest(id)` (default `null`) gives the `Change.digest()` (new: SHA-256 of the canonical `table`, `op`, `key`, `basedOn`, `set`, `from`, `reason`; numbers by value) the sink kept with its first answer; `Outbox` gives it from its journal; the guard also keeps it in its own memory for a sink that keeps none. Same id and content: the same answer; other content: `409 rdp.write.conflict` with `reused: true`, audited `write-reused` (the host's audit sees it), never passed to the sink. Tests: `UndeclareAndReusedIdTest` (through the Data Protocol write path, also after an outbox restart), conformance `protocol/write#a-reused-change-id-with-other-content-is-refused`.
- **One level and the whole text at every parse entry point** (G4 gate F25, a silent wrong result): `MeasureFunctions` (a BI model's `functions:`) parsed at the retired level 1.0 with no end-of-input check, so `net(x) if c else 0` expanded to `net(x)`'s body alone and `net(x) total rubbish )(` was accepted; `CheckedMode.parse(text)`, `CheckedMode.require` (checked at 1.2, returned a tree read at 1.0), `FormRules`/`FormExpr` and `Parser`'s one-argument entries defaulted to 1.0 too. Now every entry goes through the one parse (`Parser.parseWithSpans(src, limits, table, macros)`), at `Grammar.LEVEL`, and a token after a complete expression is `RCH-2101` `unexpected '<token>'` at that token (its end included). The level-1.0 lexer and parser branches are gone; `ElCompiler` and `CheckedMode` refuse a level other than 1.2. `MeasureFunctions` expands calls anywhere in the tree (records, lists, slices, forms, lambdas, `??`, `**`; a `let` or lambda name is not taken for a parameter), refuses a call by name, and words a problem at its place (the token, or the call whose function is at fault: the cause is an `ElException`). `Json.read` also refuses text after the value (it dropped it). SPEC 4.1 rule 6 (one level, the whole text) and 4.10 rule 9 (measure functions); FORMAT 5.2: an `expr` or `expr-error` case may carry `functions`; 15 cases `el/measure-functions`. API removals: `ElSource` (the removed `rachana upgrade`'s reader, no caller), `Checker.check(..., boolean)`.
- **The last unverified case flipped**: `protocol/write#a-reused-change-id-with-other-content-is-refused` (read and run by the G4 gate); no case below `conformance/1/` is unverified.

## v0.12.0 (2026-11-21)

- **IDE launchers follow the platform standard**: one `run-configs/` at the repository root, every launcher named `Rachana: <verb> <what>` (IntelliJ folder `Rachana`), the same names as the VS Code tasks and launches; missing launchers added; the check `python/tests/test_launchers.py` (a test) keeps names, files and workspace in step.

- **`ProtocolHost.keys` is public**: a host that mounts the Data Protocol on its own routes (Drishti's Spring mount of a windowed
  stream's `POST /changes/{sub}/window`) reads a window's keys as the host does, a bare value as a one-part key, instead of copying the
  reader (G4 pass 3, live tables LV5). Case: `ProtocolHostTest.keysAreReadAsTheProtocolSendsThem`.
- **`rachana-server` reads and writes CSV with Drishti Tools (owner decision O6)**: the server's own RFC 4180 parser and writer are
  replaced by `tools-csv` 1.1.0 (`CsvReader`, `CsvWriter`; `drishti.tools.version` in `java/pom.xml`); Core and every other module
  stay free of Tools (`DependencyAllowListTest` allows `com.ash.tools:tools-csv` in `rachana-server` only). The install order is now
  Drishti Tools → Rachana → Lake → Drishti. The table writer writes LF line ends and no formula guard (the file is the table's own
  source and is read back as written). A CSV that holds only its header now keeps that column order when the first row is added
  (the old header read returned no names).
- **The same bytes on every run (G3 gate F1)**: a ViewModel's `layout.split` and `layout.sections` were written from `Map.of`, so
  their key order changed from one JVM run to the next and differed on Node; the server's home page listed Sutras from `Map.copyOf`
  (another order at every start), and the pending tables, a table source's column types, the bundle's `levels()` answer, the live
  reference server's answers and an audit event's fields had the same flaw. Every main source of every module now builds its fixed
  maps and sets with `com.ash.rachana.model.Ordered` (`map`, `set`, `copy`, `copySet`: in the order written, unmodifiable, no nulls,
  no duplicate keys), its working ones as `LinkedHashMap`/`LinkedHashSet`, and walks a concurrent map sorted. The guard
  `OrderedCollectionsGuardTest` (rachana-guards) refuses multi-key `Map.of`, `Map.ofEntries`, multi-item `Set.of`, `Map.copyOf`,
  `Set.copyOf`, `HashMap`, `HashSet`, the collectors that make them and an unsorted walk over a `ConcurrentHashMap` or
  `IdentityHashMap` in any main source (it replaces the narrower ViewModel-only guard). New cases `sutra/binder/layout` (areas with a
  split, tabs, sticky, phone, tablet; a flow grid with sections) and a server test of the home page with several Sutras over three
  starts; the conformance lane runs the JVM a second time at the newest level and compares the bytes.
- **A fragment of another level is refused (G3 gate F2, owner decision O1)**: the compiler accepted a fragment's `rachana:` key and
  expanded the fragment at the level of the Sutra using it, so a 1.0 fragment's `0.1 + 0.2` silently became exact inside a 1.2
  Sutra. A use of a fragment or block whose level (its key; none is 1.0) is not the Sutra's is now `RCH-2008` naming both levels and
  `rachana upgrade` (SPEC 2.1 rule 4). The conformance cases' fragments declare the level of the Sutras that use them (90 keys raised;
  the synthetic `demo` library's manifests and pins follow: `6f945cc7…` → `e5087749…`); cases `problem/level-declaration` (another,
  a missing and a later level refused; the same level read) and `sutra/level-declaration` (1.0 keeps doubles, 1.2 is exact, each
  through a fragment of its own level).

- **Duplicate leftovers (i7b audit S10, D6)**: `Signatures.Signature` loses its `legacy` component and `signatures.yaml` its four `del:` lines
  (DEL's names for functions; DEL is folded away and nothing read them; logged in `java/api-removals.txt`). The deprecated function
  spellings (`startsWith`, `oneOf`, `matchesCase`, `days`, `today` ...) are part of the frozen level 1.0 (SPEC 5.10, 7.2 rule 3: read, never
  offered, warning `RCH-2090`), so `conformance/1/el/coverage-old-spellings.yaml` stays as that baseline; they leave with level 1.0 at G4.
  `docs/design/SPEC.md` and `docs/design/DATA_PROTOCOL.md` keep decisions only (the specification is `spec/1/`); links to them corrected.
- **Product documentation**: the README describes the built product (parts, ways to use it, build, install order Rachana → tools →
  Lake → Drishti from release tags, IDE support) with front matter. One guide per major feature under `docs/guides/` (the language at
  level 1.2 with fragments as functions, libraries and `rachana lib`, the Data Protocol, live tables with editing and approvals, the
  server, Studio, the command line and SDKs, `rachana upgrade`) and four tutorials under `docs/tutorials/` (first dashboard, a library,
  live edits with approvals, a Flask live board), each run against the build. The planning pages `docs/OVERVIEW.md`, `MODES.md`,
  `ROADMAP.md`, `AI.md` and `ARCHITECTURE.md` are removed (the designs stay in `docs/design/`); module and example READMEs describe
  what is built.

- **Known duplicates folded** (i7b G3, I7B_DUPLICATES Q1 to Q3): the library manifest's exports and the `list` format write their
  JSON strings through `Json.quote` (a control character other than tab, newline and return is now written `\b`, `\f` or `\u00XX`;
  no committed file holds one); `median`/`percentile` share `Holistics.OfValues` with the other holistic functions, `count` and `nmiss`
  one count base, `stderr`/`cv`/`skewness`/`kurtosis` and `product` one encoder of a state of doubles (state bytes and numbers
  unchanged; API lines of the inherited members logged in `java/api-removals.txt`); the live-table declaration and the Data Protocol
  filter use `Checker.distance`; the form and state browser runners share `runners/static_page.py`.

- **Library doc texts are no longer cut at a comma; the 1.0.0 libraries' content hashes change** (G4, lead decision: no customers
  yet). An unquoted text in a fragment's flow mapping ends at its first comma: `delta: { type: number, optional: true, doc: The change
  against a reference, written beside the figure }` is the doc "The change against a reference" and a key "written beside the figure",
  and `rachana lib build` carried exactly that into the manifest (`core/key-figure`'s `delta`, and 45 more parameter and column docs
  across the two libraries). The cause is fixed where it starts: a parameter declaration takes only its keys (`type`, `doc`, `default`, `required`,
  `optional`, `columns`, `contract`, `min`, `max`, `options`, `pattern`, `max-rows`, `live`, `key`, `window`, `dynamic`, `of`) and a
  column only `type`, `doc`, `optional`; any other is `RCH-2048`, and a key with spaces says to quote the text (LIBRARIES.md 4.2 rule
  7). The 46 docs are quoted in the fragment files; the manifests, READMEs and `MANIFEST.sha384` of `core` and `finance` are rebuilt
  (`rachana lib build`, gates LG1-LG5 and LG8 pass, the gallery paints). `FragmentDocTextTest`: a cut doc is refused with the fix; a
  quoted doc with commas, colons, double and single quotes, `#`, a backslash, accents, CJK and an emoji goes through the manifest
  writer and back unchanged, and the manifest is not stale. Content hashes (SHA-384 of the canonical archive, signature left out):
  `core@1.0.0` `6736932f…5857fa7b1` → `f77b54a5…1610d2501`, `finance@1.0.0` `cc79698c…c23a30e68` → `87bd4ca2…178cc9c20`; a host
  that pinned them (a `rachana.lock`) pins the new ones.

- **`Final`'s state is public API, so a host keeps no copy** (G4 binder pass R-2). `com.ash.rachana.agg.Final` gains `encode()`,
  `decode(byte[])` and `isEncoded(byte[])` (the tag test Core's own holistic states use too), marked `[experimental]` in
  `java/api-surface.txt` (additions only). Drishti's `bicompute.functions.Final` (a copy kept because these were package-private) can be
  deleted: its once-per-parent figures and native measures store and read Core's bytes. `FinalTest`.

- **SPEC 9 items 11 and 12** (G4 binder pass, lead decisions): `and` is the canonical form from level 1.2 and a formula written with `and`
  stays as written (P10: nothing to migrate); `geomean`'s state and `skewness_pop`'s figure may differ in the last place from Drishti's
  retired kernel, and Core's kernel is authoritative (8.3h). Item 7 notes the 1.1 catalogue check. Upgrade rule `SAME_AT_EVERY_LEVEL`
  covers both.

- **The served ViewModel schema names the 1.2 `form` kind's data record** (G4 binder pass R-1; contract 1.2, append-only, CG4).
  `ViewModelSchema.DATA_BY_KIND` had no entry for `form`, so the schema Core serves (`ViewModelSchema.build()`, the text a host's schema
  test validates against) gave a form panel's `data` no record; it is now `FormPanel`, in the published order (after `chart`, before
  `live`), so the generated kind rules equal `spec/1/viewmodel.schema.json`'s, which already held it (the contract file is unchanged).
  `ViewCodeGeneratedTest` holds every panel kind to its data record. Elements' pinned copy of the contract file already has the record;
  the next Elements sync re-pins it with this release's file (no change expected).

- **Literal arguments checked against the function catalogue, the one place** (G4 binder pass P14; SPEC 4.10 rule 2). From level
  1.1 an expression's literal arguments are checked against `functions.yaml` when it compiles (`CatalogueArgs`), wherever a function
  has no check of its own words: a text outside its parameter's `enum` (`group(..., 'decade')`), a parameter typed `boolean-literal`,
  `integer-literal`, `number-literal` or `string-literal` given something else (`rank(agg, desc = 'sideways')`, `lag(agg, 1.5, order)`,
  `measure($.x)`), and an engine function's keyword-only parameter given by place (`rank(sum(x), 'sideways')`, which compiled before).
  Each is `RCH-2101` at the argument. Level 1.0 is frozen and keeps the reference's acceptance; the audit found every other enumerated
  text argument (`pad`, `datedif`, `yearfrac`, `intck`, `shift_date`, `fillmissing`, `interp1`) already held by its own check, which keeps
  its words. Suite `el/catalogue-arguments-1-1` (11 cases, verified); upgrade rule `CATALOGUE_ARGUMENTS` (nothing to rewrite: at 1.0 these
  are refused when they run).

- **Stack guard: Rachana never crashes on a deep input** (G4 binder pass P15). The depth bound (SPEC 4.12, 200 levels) refuses a deep
  expression with `RCH-2101` before any stack ends on a normal thread, but a host thread with less stack (Drishti's profile test thread)
  overflowed while still inside the bound: `coalesce(` 300 deep was a `StackOverflowError` there and `RCH-2101` on the reference. The
  parser, the checker, the printer and the binder now run again, from the start, on a thread of Core's own with the stack the bounds
  need (`internal.Deep`; the Sutra declarations in force carried over), so the answer is the bound's own refusal, the same text and
  place on every thread; only past that thread's stack is it the walk's refusal at the place reached. The JavaScript bundles replace
  `Deep` (an engine has no second thread): an exhausted stack (`RangeError`, `InternalError`) is the refusal at once. `ElLimitsTest`
  runs the deep case on 64, 128 and 256 KB stacks at levels 1.0 and 1.2. Drishti can drop its known difference
  `el/limits#call-chain-past-the-depth`.

- **Elements 3.2.0 vendored** (drishti-elements commit 787f97d, label `3.2.0`): `js/vendor/drishti-elements/3.2.0/` replaces the `3.2.0-dev+a40ca7e` folder (SHA-384 manifest; `ELEMENTS_VERSION`, the server pom, the Python assets, Studio and the docs name it); the pack's `data/formats.yaml` and `world-regions.yaml` are the bundled ones.

- **`rachana upgrade` reads and writes a fragment's level as a Sutra's** (Gate 2 review 10.5; SPEC 2.1 rule 4). A fragment's own
  `rachana:` key is its level (`1` / `1.0` is 1.0, `1.N` is 1.N); a fragment without the key is 1.0 and gains `rachana: <to>` as its
  first key. The tool upgrades a level-1.0 fragment and writes the new level into its key; a file already at the target level is left
  untouched (exit 0). `--from` is gone (`Upgrade.upgrade(file, text, to, table)`). Before, a fragment's key was never read or written:
  a second run refused a fragment the first had rewritten (exit 1), and a third rewrote a valid 1.2 `sum(range(1, 4))` to
  `sum(span(1, 4))` (exit 0). The three runs are regression tests (`UpgradeTest`), with fragments at 1.0, at 1.2 and undeclared. The
  corpus proof is rerun: the 42 fragments now end at `rachana: 1.2` (520 files changed, 520 declarations); everything else in the
  report is identical.

- **The expression language's aggregates follow the host's on decimals** (lead decision L-3; SPEC 4.16 rule 7, PROTOCOL 3.1;
  conformance/1/VERIFIED.md). From level 1.1 `avg` over integers and decimals is the exact sum divided as `/` divides (decimal128, 34
  digits half to even, no trailing zeros: `avg([1, 2, 2])` → `1.666666666666666666666666666666667`), the digits a host gives for a
  decimal column; `min(list)` and `max(list)` give the exact element. A double among the elements keeps the double aggregate; `median`,
  `percentile` and the statistics stay real-number aggregates (as the host's `median` and `percentile`); `sum` of nothing was already
  `0`. Level 1.0 unchanged. 12 cases added (`el/numbers-exact`), the same bytes on the JVM, both JS bundles and the Python V8 engine.

- **The spec-gap code fixes CF-1 to CF-5 and the host aggregates decided** (conformance/1/VERIFIED.md, the third pass). A `table`
  entry whose table the host does not have is a failed entry: each panel that reads it is `error: "RCH-2070 the host has no table
  '<t>' (data '<e>')"` with `empty: true`, never read as no rows (`LiveScreen.missing`, called by `TableClient.load` and the JS `Screen` on `rdp.table.unknown`; the JS bundles' screen
  operation `missing`; a view built without a host is unchanged); the case
  `sutra/data#missing-table-reads-as-no-rows` is replaced by `#a-missing-table-fails-its-panels`. Checked mode joins the types of a
  value chosen among several in any order (SPEC 4.14 rule 1: `?:`, `??`, `switch`, `if`, `coalesce`, `least`, `greatest`; `switch`,
  `if` and `coalesce` values that do not fit are `RCH-9127`) and accepts a timestamp text ending in `Z` or `±hh:mm`; these change
  checked mode at level 1.0 in corners no case pinned before (listed in VERIFIED.md). The Python host's decimal `sum` has the digits of
  the values it adds and `min`/`max` give the cell as held (PROTOCOL 3.1). A table paged from its source no longer lists `group` in
  `can`. **Decided** (PROTOCOL 3.1, 6.3): `sum` over no value is `0` (`"0"` for a decimal) on every runtime (the Python host gave
  `null`); `avg` of a decimal column is the exact sum divided to 34 digits half to even, trailing zeros dropped, the same digits
  everywhere (the JVM host gave 16 digits, the Python host rounded to the column's scale). 16 cases added; the 12 cases of
  `el/aggregates-host-values` verified on an independent review against SPEC 5.1; no case of the suite is unverified.

- **A Sutra declares its language level in its `rachana:` key** (owner, 2026-11-21; SPEC 2.1 rule 4): `rachana: 1` (or `1.0`) is level
  1.0, `rachana: 1.2` is 1.2. A host reads each Sutra at the level it declares, at most its own (`Grammar.declaredAt`,
  `Declarations.atLevel`, `SutraParser.declaredLevel`, `Sutra.level()`): a host of 1.2 reads a `rachana: 1` Sutra exactly as a host of
  1.0. A level above the host's is `RCH-2008`, an unknown minor `RCH-2009` naming the host's newest. The key is the one place: `--level`
  is gone from `lint`, `build` and `preview` (Node and Python), `<rachana-view level>` is gone, the server refuses `project.level`;
  `rachana new` writes the newest level. Level-1.1 and 1.2 conformance cases, the libraries' and the examples' Sutras declare their
  level. One switch (`Grammar.MAJOR_ONLY`) retires levels 1.0 and 1.1 later. Suites `problem/level-declaration`, `sutra/level-declaration`.
- **`rachana upgrade`** (`java/rachana-tools` `Upgrade`, `UpgradeRule`, `UpgradeCommand`; Node `rachana upgrade`, `python -m
  drishti_rachana upgrade` and `java ... Main upgrade` run it): a level-1.0 Sutra or fragment rewritten for a later level with the same
  meaning: `range(list, field)` becomes `span`, a named argument takes the catalogue's name, the file declares `rachana: 1.2`; exact
  arithmetic, number-like text of Java's grammar, `standardize`, texts beyond U+FFFF and run-time regular expressions are flagged; a
  bare `lambda` blocks the file. Only rewritten words change (comments, layout, quoting kept; checked by reading the file again);
  idempotent; dry run by default, `--diff`, `--write`, `--report`, `--rules`. The rule list is complete against the documents
  (`UpgradeRulesTest`: 373 differences); 14 verified cases (`compat/upgrade/cases.yaml`, each bound at both levels on one host).
  **Proof** (`UpgradeCorpusTest`, report `compat/upgrade/drishti-compat-corpus-2.9.11.i7a.report.json`): the 478 Sutras and 42
  fragments of the Drishti corpus upgraded to 1.2 compile to the recorded documents and bind to the recorded ViewModels byte for byte
  (stamp aside); 0 refused, 0 blocked; 18 review and 6 info flags, none of which changed a recorded view.
- **Ready for a host's move onto Core (G4 rehearsal, pass 2)**: aggregates over a host's nodes give the figure (the rehearsal's P4: the
  0 came from the host test's own oracle, which read only its node type) and are never a silent 0: a value Core cannot read is
  `RCH-2102` at every level, a first argument that is not a list from level 1.1 (level 1.0 as recorded); a host scalar without
  `value()` is read through its accessors. `NodeFactory` (`Node.nodes()`): lists and records an expression builds come out in the
  host's node type. `Rachana.formats()` is live (`Formats.live`, `reload`). `Printer.print(e, level)`: 1.0 writes `&&`, `where = `
  (unchanged); 1.1 and later Python's `and`, `or`, `not`, `a if c else b`, `where=`. A Sutra's record components are its JSON
  (`domain`, `match` in their places; `extensions` lists only profile data). `Rachana.at(level, options, profiles...)`. A profile
  function (or grouping) with a Core name is refused with the reason. Conformance: `el/aggregates-host-values`, the host-node fixture
  (`JvmRuntime --host-nodes`, `HostNodeConformanceTest`).
- **The grid's edit UI for live tables (LIVE_TABLES 8.1)**: the vendored Elements pack is re-packed with the grid's live-mode edit (the table's
  `write` columns and rules, cells and beside modes, one write per row with a ULID, a pending mark until the patch, 409/422/403/405/404 shown, the
  held-changes tray decided in cells, the paused banner and "N new"); `liveTables(client)` answers `schema()` beside `page`, `follow` and `write`;
  `LiveReferenceServer` takes `--static <dir>` (a page and its table on one origin); the runner `runners/live_edit_browser.py` (in the browser lane)
  paints an editable live table and edits a cell against the reference host.

- **Live tables, the screen and four eyes (L9, L10 file writers; experimental)**: the live panel takes `edit: none | cells | beside`
  (language 1.2; contract 1.2 `LiveRef.edit`); core library fragments `core/live-table@1` and `core/held-changes@1`;
  `LiveTables.screen(name)` gives the Sutra a declaration needs. The approvers' tray: `Authoriser.approves`/`mayDecide`,
  `ChangeSink.holds`; an approver's `<name>.changes` lists others' held changes in scope and decides one by writing `state`
  (`released`/`rejected`); suite `protocol/approve` (10, unverified). `rachana-server`: file-table writers (CSV, JSON, JSON lines:
  atomic replace, visible journal, each change id applied once), `edits.hold` and `access.tables.<name>.approve`.

- **G2 golden fixes**: a problem inside a fragment file carries `file` and `path` (SPEC 6.1); one program is named `rachana` (the Node
  CLI; Python runs as `python -m drishti_rachana`, no console script); a size guard on both bundles (`tools/js/bundle-size-guard.json`,
  Brotli 11, measured + 5%, changed deliberately with a reason); the vendored Elements pack says the version of its folder; the
  default-level rule stated (SPEC 7.2) (the project-level `level:` of `rachana.yaml` it added is superseded the same day: the Sutra's `rachana:` key declares the level); level 1.0's
  UTF-16 text order stated (SPEC 4.7, 4.13); `FROZEN.sha384` lists every case file.

- **Live tables, the edit path (L7, L8 client, L10 writes; experimental)**: writes are Data Protocol v1 additions under `can: write`
  (`POST {base}/tables/{name}/write`, bridge `write`): a change `{id, op, key, basedOn, set, from?, reason?}` is answered `{change,
  state: applied | queued | held | refused, message, version?}`; refusals `405 rdp.write.unsupported`, `403 rdp.write.forbidden`
  (audited), `404` for a row outside the reader's scope, `409 rdp.write.conflict` with `current {version, by, at}`, `422
  rdp.write.invalid` with `fields [{column, rule, message}]` from the declaration's form rules (Core's checker). The host's
  `ChangeSink` takes checked changes (Drishti binds its write-back, LT-8); `Outbox` + `FileOutboxStore` give exactly-once for hosts
  without one; `held` and `release`/`reject` are the host's decisions; the tray `<name>.changes` lists the caller's pending changes.
  `TableClient.write`, `DataClient.write`, the `<rachana-view>` edit hook. `rachana-server`: the CSRF token on writes (beside its Host and Origin checks),
  `access.tables.<name>.edit`, `edits:`, `tables.<name>.write` (JDBC, change id journal table). Suite `protocol/write` (20, unverified).

- **Fragment column rename knows the scope of `@`** (decision L-21, bug fix): in the `emits` payloads and the `on:` effects of a
  `pivot` or `surface` panel, `@` is the clicked cell `{row, path, column, value}` or point `{x, y, z}`, so its fields are never
  renamed (before, a fragment's local column named `row`, `column`, `value`, `x`, `y` or `z` mapped by the caller renamed the payload
  field, a silent wrong read); a local column read there is `RCH-2121`. `FragmentColumns.markRow`. Cases `fire/fragment-marks`,
  `problem/fragment-marks`.
- **The conformance runner binds pivots**: `CasePivots`, a small pivot engine (sum, count, avg, min, max with subtotals and totals) in
  the runner every runtime shares (JVM, both bundles, Python on V8), handed to `LiveScreen.pivots`; pivot-cell clicks run as `fire`
  cases (`fire/pivot-cells`), not only in JUnit.
- **Elements pack re-vendored `3.2.0-dev+2f21158`** (Elements g2/integration with g3/one-escape): one `escapeHtml` in drishti-core (& < > " ') replaces six hand-written copies, one of which left out the double quote; references moved.
- **Elements pack re-vendored `3.2.0-dev+b466b87`** (Elements g2/integration 6972989 with g3/surface-click): the surface heatmap's tooltip no longer asks for ECharts' arrow, whose inline
  style a strict CSP refused (found by Elements' new Chromium test that clicks a surface point, heatmap and 3D); references moved.
- **Elements pack 3.2.0-dev+6972989** vendored (the painter's `requires` fails closed: a button naming a permission is enabled only when the host's `allowed` answers `true`; G2 gate F10, O8); the server refuses `auth: users` on a non-loopback bind unless `server.behind-tls-proxy: true` (F17); a test run leaves no temporary folder (`tools/gate/tmp_leaks.py`, F16).
- **G2 gate fixes (B)**: `./mvnw verify` runs what G2 built (`tools/gate/verify_lanes.py`, four executions of `rachana-guards`, nothing skipped):
  conformance at every language level on the JVM and on the Node bundle with identical bytes, the protocol cases on the JVM reference, the
  Python host and the live server, the Python SDK tests (the case reader takes the `fire` and `live` kinds), and a Chromium lane
  (`<rachana-view>` live, forms, state, a built site, the Studio); `-DskipTests` leaves them out, `rachana-guards` honours it. The two
  `arrow` expectations of the live declaration cases (F5). The static-files host evaluates through the runtime bundle's `query` (Core's
  `RowQuery`), with no filter evaluator of its own (F6). The server answers only its own `Host` names (bind address, `localhost`,
  `server.origins`) in every auth mode, and a link in `static/` cannot leave it (F8). `levels.py` refuses a deleted recorded case that
  is not a listed withdrawal (F9). One default language level, `Grammar.DEFAULT_LEVEL`, handed out by the bundles' `levels()` (F12;
  guard `DefaultLevelGuardTest`). One Elements pin (`ElementsPinGuardTest`). The reference server serves a path with a `+`.
- **finance library on the new formats and events**: rates default to `pct4` (curve-table, curve-overview, term-structure,
  curve-compare, fixing-ladder, leg-table), changes and spreads to `bp1` (fixing-ladder; curve-compare's new `spread-fmt`), discount
  factors to `df4`, prices to `price4` (depth-ladder, fx-scenario-chart, trade-ticket, fills-summary); the two trees take `fmt`
  (risk-hierarchy-tree keeps its share used in `pct0` through a column); correlation-matrix, scenario-matrix, sensitivity-heatmap and
  vol-surface raise `cell` on a click (their catalogue events). Goldens rewritten (22), gate green, gallery pages paint. Elements
  pack `3.2.0-dev+6b4a3b2` (pivot cells and surface points as marks).
- **Events on pivot cells and surface points** (level 1.2, contract 1.2 appended): `pivot` and `surface` fire `point-click`. A pivot
  cell with a figure is a mark, `@` the cell `{row, path?, column, value}`; a surface point is `{x, y, z}`; `#index` the place row by
  row. `Cell.ev`, `set`, `on` (a pivot's cells) and `Surface.ev`, `set`, `on` (lists beside `z`); `ev` only on a panel that emits on
  point-click. A click fires the panel's event with the payload read over the rebound mark (`ScreenWire`), `Fire` finds the mark by its
  place, the painter draws the cells and points as clickable marks. Cases `fire/surface-points`, `PointCellsTest` (a host's pivot engine).
- **Gate 2 fixes A (compiler and engine)**:
  - **F4 / O7, the `check` and `grammar` kinds run**: the 49 frozen cases no runtime answered are answered on the JVM, both JavaScript
    bundles (facade `checked`, `grammar`; the tools bundle's harness) and Python on V8. Corrected cases that had never run: checked
    mode reports every problem (`el/check#check-mtm-n`, `#check-status-in-1-2`: two each); `expected` of SPEC 6.1 is deferred
    (`el/examples/check#unknown-name-did-you-mean`). `grammar/reserved`: the seven refusals level 1.1 reads (`|>`, `??`, spread, `{}`,
    comments, digit separators, `??` in a template part) carry `until: "1.0"`, each with a level 1.1 twin that is accepted
    (`pipe-refused` was wrong at 1.1: it passed only because `f` is no function).
  - **F7 / O4, problems carry `path`, a full `range` and severity** (SPEC 6.1): `SutraProblem` is `(code, severity, message, location,
    path, range)` (the three-argument constructor stays); the parser places every problem of the file it read (`parse.ProblemPlaces`:
    the node's JSON Pointer and start and end with line, column and UTF-16 offset), live-table declarations too; advisories are
    `warning`. Conformance answers carry them from level 1.1 (level 1.0 keeps its recorded shape, so its replay is identical). From
    level 1.1 a duplicate panel id is reported at the id (`/panels/<n>/id`). `doc`, `fix` on compile problems and `expected` are
    deferred to a later minor (SPEC 6.1 says so). `path` restored in `problem/examples/problem` (`-l11`, `profile-key-without-the-profile`).
  - **O6, `-x ^ y` refused at level 1.1** (`RCH-2101`, the message names `-(x ^ n)` and `(-x) ^ n`); `-x ** n` keeps Python's rule and
    the checked-mode hint `RCH-2091`; level 1.0 has no infix `^`. Cases `el/sign-power-1-1`; DECISIONS EL1-2 amended.
  - **O5, `range` rename**: complete since lane B (`span` at 1.1); the SPEC's list of pending pivot names says so.
  - **F11 / O9, CG5 over all seven modules, removals logged, stability marked**: `ApiSurfaceTest` reads rachana-api, -core, -tools,
    -data, -live, -server and -cli (Arrow's jars from rachana-data's `runtime-classpath.txt`); each baseline line is `[stable]` (the
    entry surface of gate 2 section 4) or `[experimental]`. The write flag refuses to drop a line (or demote a stable one) unless
    `java/api-removals.txt` logs it with a date and a reason; a released `java/api-surface-<minor>.txt` (copied at the tag) is checked on
    every run and never written. README, the Python SDK and the JavaScript package say what is experimental.

- **Tree formats, promoted formats, hyphenated column names** (level 1.2): a `tree` takes `fmt`, the format of every measure whose
  column names none (a column's own `fmt` still wins; `fmt` at 1.1 is `RCH-2008`). Core bundles `pct4`, `bp1`, `df4` and `price4`
  from level 1.2: reserved Drishti names promoted in `compat/promotions.yaml` with Drishti's definitions unchanged
  (`ReservedNamesTest` prints probe values with both tables; below 1.2 Core alone does not know them). The format table is
  level-gated (`Formats.atLevel`, `since` per entry; a pack's or site's name holds at every level), in the JVM and both bundles; the
  Elements pack now carries the table (`3.2.0-dev+2b87fcd`, `pack --data`; `drishti.elements.version` names the pack folder and
  `2.4.0/` is gone). A `{ from, columns }` mapping takes a caller column such as `mtm-usd`: `@.amount` and a `bind` read
  `@['mtm-usd']`, a fragment's own `@['bid-size']` is renamed too; such a name in a field-name option is `RCH-2121`, never a minus.
  Cases: `format/promoted`, `sutra/binder/tree-formats`, `problem/tree-format-level`, `sutra/fragments-hyphens`,
  `problem/fragments-hyphens`.
- **The corpus's bind proof binds real data** (gate finding F2, owner decision O2): the recorder (`tools/regime/corpus_binds.py`,
  `CorpusBind.java`) now makes its synthetic data from each Sutra as the reference parsed it (fragments and blocks expanded: rows for
  timelines, charts' named fields, field-lists, histograms, graphs) and gives the reference a **recording source router** (one source
  plugin serving every kind: linked documents, wired `source:`s, map lookups, control lookups), the record's links and the host's
  pivot engine; every answer is a host call in `binds/<file>.json` (`source.fetch`, `source.search`, `source.freshness`,
  `pivot.cells`, `pivot.key`). Core's replay answers its `TableProvider` and `PivotEngine` from the same recording
  (`CoreBinds.tables`, `CoreBinds.pivots`; the links panel replays its items). Before: 463 of 478 bound, 454 compared, 24 listed,
  65% of data panels empty (1,312 of 1,447 tables, 788 of 863 line charts, every timeline and links panel). Now: **478 of 478
  bind on the reference and are identical on Core, none listed**; 3 of 5,569 data panels empty (0.1%: panels waiting for a
  selection), 6 of 400 links panels empty (the record names no entity), every panel kind filled somewhere.
  `corpus_binds.py --report` prints the count by kind.
- **Corpus binds no longer flaky**: `CustomerCorpusTest.recordedBindsReplay…` flipped between "differs" and "listed but identical now"
  because both the reference (i7a) and Core wrote some objects from `Map.of`, whose key order is drawn afresh in every JVM run. Core now
  writes a form's `rules`, `steps` and a field's `options` (and a problem's `at`, a grouped query's columns) in declaration order, and a
  guard (`ScreenWireOrderTest`) refuses a multi-key `Map.of` in the ViewModel writers. The replay compares the objects the reference
  scrambled (a wire's `emits`/`children`, a form's `rules`/`steps`/`options`) in sorted key order (`CoreBinds.unscrambled`), so the nine
  Sutras listed only for key order are now compared, not listed; 20 runs in a row identical.
- **Painter: forms, actions, control changes**: the vendored Elements pack re-packed from drishti-elements d1db672 (folder `3.2.0-dev+d1db672`): the
  `form` panel (drishti-inputs' Form), panel `actions:` buttons and a control's `on.change`. `<rachana-view>` answers the painter's `check` hook with the
  runtime's own `fire` (`Screen.check`: a form's rules run in the bundle, never in the page) and sends a control's change through `Screen.controls`;
  `runners/form_browser.py` (Chromium): a submit that breaks a rule shows the message and sets no state, one that passes sets state and hands the order to
  the host, an action button fires. **The Node suite cannot skip quietly any more**: `rachana-js` is a module of every reactor build (before `rachana-guards`;
  the `js` profile stays, empty) and copies the bundles to `js/packages/rachana/dist`; `tools/js/node_tests.mjs` fails when the bundles are missing or any test
  was skipped (`-Drachana.node.skip=true` still leaves the run out).

- **Node tests in the build**: the Node suite (`js/packages/*/test`, 33 tests) now runs in the Maven reactor: `rachana-guards` runs
  `node tools/js/node_tests.mjs js/packages/*/test` in its `test` phase (exec-maven-plugin; `-Dnode=<path>` names Node, `-Drachana.node.skip=true`
  leaves it out), so `./mvnw verify` fails when they do. Fixed: the CLI tests named an old Elements pack folder (they use `ELEMENTS_VERSION`
  now), the Studio test did not count the bundled `finance` library, and the `liveTables` follow answer has `ready` (resolves when the host
  opened the subscription): a change made before that is a gap for a window (PROTOCOL 16), which made the windowed follow test flaky.

- **IDE launchers**: IntelliJ Java programs (`Rachana CLI (lib)`, `Rachana reference server`, `Rachana server serve|check (server-demo)`, `Rachana server hash-password`), Maven launchers for the JavaScript side (`js/pom.xml`, exec-maven-plugin: runtime bundle, Node tests, Studio, dev server; no bash, so IntelliJ Community on Windows runs them), PyCharm configurations (`python/run-configs/`) and `drishti-rachana.code-workspace` for VS Code; the server example `examples/server-demo` (CSV and JSON tables, a users file with the development-only user `dev`); [java/README, Build and run in the IDE](java/README.md#build-and-run-in-the-ide).

- **Kinds batch** (level 1.2, contract 1.2 appended; SPEC 2.3, VIEWMODEL; API additions only):
  - **the `live` panel kind**: a grid over a live host table read a window at a time (`rows: $.<table entry>`, `window`, `top`,
    `sort`, `group`, `bars`); the dataset carries its fields and `live` (`table`, `window`, `top`, the conditions as one filter tree),
    never rows; the plan reads such a table one window at a time; `<rachana-view>` hands the painter `options.tables` (`liveTables`:
    one page, a windowed follow in seq order, the window moved, a gap). Cases `sutra/binder/live`, `problem/live`, `plan/live`.
  - **cell bars**: `bars` on `grid` (level-gated: `RCH-2008` below 1.2, `PanelKind.since(option)`) and `live` (`GridColumn.bar`).
  - **several series on a `line`**: `layers` (fields of wide rows, each with its `mark` line, bar, area or point, `tone` and `axis`) and
    `by` (one series per value of a field of long rows, missing points null, `Series.ev` per series on a panel that emits on
    point-click). Cases `sutra/binder/series`, `problem/series`, `problem/levels#line-layers-need-level-1-2`.
  - **`treemap`** (cells sized by a figure, nested by `group`, toned by the sign of `color`; `point-click` cells with `ev` and `set`) and
    **`bullet`** (one figure on a band against a `limit` or between `lower` and `upper`, warning zones within `warnAt`). Case
    `sutra/binder/treemap-bullet`; problems in `problem/series`. `control.Fire` reads a treemap's `cells`.
  - **`finance` library: seven more fragments** the batch unblocks, each with a minimal and a full example, a golden ViewModel and a
    gallery page (`rachana lib gate finance` green): `live-blotter` (a live panel: the table read a window at a time, new rows on top),
    `depth-ladder` (a grid with cell bars and the touch), `curve-compare` (curves split by name, the spreads to a base), `backtest-chart`
    (P&L bars against the VaR on the loss side, exceptions marked), `performance-chart` (rebased portfolio and benchmark, the drawdown),
    `concentration-treemap`, `limit-band` (a two-sided bullet). `order-ticket` and `what-if-shocks` are lane A8's (forms and effects).
  - The vendored Elements pack is re-packed from drishti-elements f8b8619 (folder `3.2.0-dev+f8b8619`, replacing `3.2.0-dev+824d83b`):
    the live kind through `options.tables`, cell bars, series marks with a right axis and a legend, treemap and bullet panels, the
    `point` event with a mark's row.

- **Server, second step**: live tables answer Arrow pages (`can` gains `arrow`; the one `ArrowRows` of rachana-data); `*.live.yaml` declarations in the project
  folder (or its `live/` folder) are served, each `snapshot` naming a configured table (a declaration that does not is listed with the pending tables);
  a JDBC table pushes the table's condition into its query (`FilterSql`: bound values only, column names from the table's own columns, anything that is
  not exactly the same meaning left to the engine, which filters again). The frozen case `protocol/examples/bridge#bridge-rows-request-answered-once` is
  withdrawn (it sorted a table that does not declare `sort`; `protocol/host#bridge-rows-of-a-sortable-table` is the corrected case), so `protocol_check`
  passes in every mode. The ViewModel schema's `x-contract` is the highest minor it defines (now `1.2`; a document's own `contract` stays the smallest
  that describes it), checked by `ContractAppendOnlyTest`.

- **Elements pack re-packed from drishti-elements 824d83b** (folder `3.2.0-dev+824d83b`, SHA-384 manifest): the hbar donut fires `bar-click` like the bar form,
  and the panel charts' tooltips are DOM elements, so no chart needs `'unsafe-inline'` for styles; `runners/state_browser.py` still passes.
- **The painter's state and effects in `<rachana-view>`** (Elements 3.2.0-dev+824d83b, contract 1.2): a click on a mark that writes state sends the state
  back (`Screen.setState`, the bundle's `plan` and `view` operations now take `state`), the view is built again and the painter draws only the panels
  the changed states' `reads` name; `notify` and `host` effects go to the page's `host` property of `<rachana-view>` (`{notify, run}`), never run by
  Elements. The vendored pack is re-packed from drishti-elements 282ee8a (folder `3.2.0-dev+824d83b`, SHA-384 manifest); `runners/state_browser.py` clicks
  a bar in Chromium and checks the dependent panels.
- **The standalone server, step 1** (`java/rachana-server`; COMPILER_AND_RUNTIMES 10, MODES 4): one JVM process (the JDK's HTTP
  server on virtual threads, no framework) serving a project folder: its Sutras as pages (`/v/<sutra>`, `<rachana-view>` live over the
  Data Protocol; checked at load and on change, `/problems`), the libraries of `rachana_libs/`, `libraries:` and the jar, the page assets
  from the jar (the element, the runtime, the vendored Elements pack). Tables come from files (CSV, JSON, JSON lines, Arrow), a JDBC
  query (one `select`, `:name` parameters bound through a prepared statement, a read-only connection rolled back after each read, the
  operator's driver jars) or a REST JSON address (`path`, secret headers from the environment), and are kept live by `rachana-live`
  (a polling diff per `refresh`) behind a fail-closed `Authoriser` (roles per table and per Sutra, masked columns, a refused table answered
  as unknown). Sign-in by a users file (PBKDF2-SHA-256 salted hashes, `hash-password`) or a trusted reverse proxy's header; sessions
  (HttpOnly, SameSite=Lax), CSRF tokens on sign-in and sign-out and an origin check on every `POST`, a sign-in throttle, a strict
  Content-Security-Policy, loopback by default (`auth.mode: none` only there). One checked configuration file
  (`rachana-server.example.yaml` shows every key): every problem at once, explicit paths only (absolute, `${config}`, `${project}`), no secret
  in the file or the logs (structured JSON lines). `java -jar rachana-server-<version>-all.jar serve|check|hash-password`, and
  `rachana serve` in the Node CLI. Tests over HTTP and in Chromium (sign in, the view paints, a changed file arrives live).
- **One HTTP binding of the Data Protocol on the JVM**: `ProtocolHttp` (rows as JSON, Arrow or NDJSON, the minimal SSE stream, windowed
  streams) taken out of `ReferenceServer`, which serves through it as the standalone server does.
- Guards: nothing depends on `rachana-server`, which depends on `rachana-live` alone; Core requires no HTTP server, HTTP client or JDBC;
  the bundled libraries and the Elements pack are the repository's. The dependency allow-list admits a JDBC driver (H2) in the server's
  tests only.

## Unreleased (G2)

- **The last unverified conformance cases closed (the spec-gap pass)**: 24 decisions (`SG-1` to `SG-24`, `conformance/1/VERIFIED.md`)
  written into the specification: SPEC 2.2 and 2.4 (`data` is a key of 1.1, a data name is a field name, `as: record` is the first
  row, a table the host does not have fails its panels), 4.2 and 4.16 (a decimal cell prints the digits the host sent), 4.10 (real-number
  results and the tolerance), 4.14 "Checked mode in detail" (the join of types, the 36 functions typed, `in`, literal parameters);
  PROTOCOL 3.1 (the text of a decimal and of its aggregates), 4 and 6.2 (a capability not declared is refused), 7 (what the runtime
  writes), 8.4 (a conflated `remove` stays), 10.1 (the `fields` of `rdp.write.invalid`), 15 (the adapter's clock, source, writer, audit
  events), 16 (a live table's `can`, a table paged from its source, a repeated change id answers the change as it stands, the tray,
  four eyes); FORMAT 5.2, 5.9 (the frames of a step), 5.10 (the plan document); VIEWMODEL (frame header). 51 cases flipped to
  verified (three `el/determinism` cases first given a tolerance in place of their text); one left (`sutra/data#missing-table-reads-as-no-rows`),
  with five code fixes listed for a builder lane (`CF-1` to `CF-5`). `FROZEN.sha384` regenerated (pre-release).
- **F13, the binder comparison over the customer corpus** (lane A10): `compat/customers/drishti/2.9.11.i7a/binds/` holds, per shipped Sutra (478), a recording (`<file>.json`: a synthetic record made from the Sutra's own paths, the business date, and the answers of the host's own panel binders, `provenance` and `links`) and the i7a reference binder's ViewModel (`<file>.view.json`) or why it could not bind (`<file>.error.txt`); written by `tools/regime/corpus_binds.py` with `CorpusBind.java` over Drishti's engine jars at `v2.9.11.i_7a` (a scratch clone, read only). `CustomerCorpusTest` replays every recording through a `RecordedHost` into Core's binder (`CoreBinds`) and compares byte for byte: 445 identical; 33 listed in `tools/split/known-differences.txt` with their reasons (the reference's `Map.of` key order, its null source router, the host's pivot engine); no Core bug found. VERSION.json `binds`, manifest rewritten.
- **A control's `change` trigger and a Screen test harness** (I1, level 1.2, lane A10): `on: { change: [effects] }` on a control, the one effect model (the effects of a panel's list, evaluated over the screen; `event` the new value read whole; `event.<name>`, `set: <control>` and any other trigger refused, `RCH-2012`/`RCH-2023`; state names checked, `RCH-2020`); carried as `ControlView.on` (contract 1.2 appended; the stamp follows); `ScreenWire.change` runs them for a host; `Fire.change` and the conformance step `{ control, trigger: change, value }` (FORMAT 5.15, case schema); `Binder.controlOn`, `Fire.runEvent`; `ViewBuilder.screen(Request)`, the screen `build` binds, so `ScreenWire`'s paths have JUnit coverage of their own (`ScreenHarness`, `ScreenWireTest`: submit, cancel, change, row-click events, children, downstream). Cases fire/change, problem/control-change; SPEC 2.8 item 7, VIEWMODEL. API additions only.
- **Form fragments** (lane A9): `core/entry-form` and `core/stepped-form` (fields as `list<record>` placed whole, rules, submit/cancel events carrying `values`), `finance/order-ticket` (side, quantity, type, limit price, time in force; checks before send; `submit {order}`) and `finance/what-if-shocks` (shocks as a form beside the caller's mapped sensitivities; `change {shocks}`; the estimate is the host's); examples, goldens, gallery pages; differences from the catalogue in `libraries/README.md`. A use's `on:` now takes any built effect (`toggle`, `clear`, `notify`, `host`, with `when`), its `event.<name>` reads put in as the payload's expression; a form panel emits on `submit`/`cancel` with payload `event` (the sent values); a fragment's `actions:` never name a host action (`RCH-2117`). The gallery reads examples with YAML 1.2 booleans (`on:` stays a key).
- **The standalone form panel and panel buttons** (I1, SUTRA_EXPANSION 3.6, level 1.2, lane A9): `kind: form` (its `form:` the panel itself, `initial:` starting values over state and controls, `submitLabel`, `cancelLabel`); `on: { submit, cancel }` whose effects run only after the form's own checks pass and read the sent values as `event.<field>` (`EffectView.event`, `withEvent`); `ScreenWire.submit` (checks, then runs the effects; `Submitted`); `actions:` buttons on any panel (`{id?, label, tone?, confirm?, requires?, do}`, `PanelView.actions`, `ActionView`); `Fire` presses buttons (`action`) and sends forms (`values`, refusals `RCH-5003`); contract 1.2 appended (`FormPanel`, `ActionView`); cases fire/forms, fire/actions, problem/actions-forms; SPEC 2.8 items 5 and 6, VIEWMODEL, FORMAT 5.15.
- **Language files generated from Rachana's own tables** (lane A9): `rachana-tools` `LangFiles <level> <out dir>` writes `rachana.lang.json` and `sutra.schema.json` of a level from the parsers' key sets (`parse.KeyTables`), `PanelKind`, `ControlKind`, `Functions` + `functions.yaml`, `Formats`, `Tones`, `RachanaSchema` at the level, and the new `spec/1/language-tables.yaml` (problem codes with `since` and `raisedBy`, the Drishti profile's functions, the released wording); a name of a later level is left out of an earlier level's files. Level 1.0 reproduces `compat/levels/1.0` byte for byte (`LangFilesTest`, which also fails on a problem code the sources raise that the table lacks). `tools/regime/levels.py write` writes a level's two files this way (and copies them to `spec/1`); `tools/conformance/spec_files.py` checks `spec/1` against the newest level; `LangDump.java` and the reference jars are no longer needed. `spec/1` still holds level 1.0 (levels 1.1 and 1.2 are written at release).
- **The four old unverified `examples` cases settled**: `problems-in-position-order-l11` (code: from level 1.1 a panel refused for its kind still holds its id, so the duplicate is reported, SPEC 6.1; verified, expectation without `path`), `profile-key-without-the-profile` (code and case: from level 1.1 a Core instance answers `match`/`domain` with `RCH-2060`, SPEC 2.2; the case runs with `given: { profiles: [] }`, `since: "1.1"`), and `edit/examples/edit` (both cases withdrawn: placeholder operations, pinned by `edit/ops`). Level 1.0 replay unchanged; the three listed in `compat/levels/moved-cases.yaml`.
- **Trees and drill tables write state** (I1, level 1.2, lane A8): every node of a table with `children:` or `levels:` carries its row-click `set` / `on`, `@` the node at its level; a fire counts the rows depth first. Cases `fire/trees`.
- **The general effect list** (I1, level 1.2, lane A8): beside `set`, the effects `toggle: <boolean state>`, `clear: <state>` (its default stands), `notify: <template>` (`tone:`) and `host: <action>` (`with:`; the host's own action, handed over and never run by Rachana; a typed fragment naming one is `RCH-2117`), and `when:` on any effect (false leaves it out for that mark); a list with more than plain state writes is carried as the mark's `on` (contract 1.2 appended: `on` on `Row`, `Bar`, `Point`, `Step`, `Event`, `Candle`, `Metric`, `Gauge`, `Fields`, `Chart.on`; `EffectView`); `Fire` runs set/toggle/clear and hands notify/host over; `LiveScreen.state` with null forgets a value. Cases `fire/effects`, `problem/state#the-general-effects-checked`, `problem/fragments-state#a-fragment-never-calls-the-host`. Not yet: panel `actions:` (buttons), `go`/`fire`/`refresh`/`export`/`copy`, control `change`.
- **`finance` library 1.0.0** (`libraries/finance/`): 48 typed fragments in seven categories (market data 11, trading 9, P&L 4, market risk 10, limits 2, counterparty and collateral 9, funds 3), each with a minimal and a full example that map caller columns (`from:` / `columns:`), a golden ViewModel, a generated README and a gallery page; `rachana lib gate finance` and `LibrariesCheckTest` pass. Synthetic data only. Not built, waiting for compiler features: `curve-compare`, `backtest-chart`, `performance-chart` (multi-series chart grammar, marks), `concentration-treemap`, `limit-band` (treemap and bullet kinds), `live-blotter`, `depth-ladder` (live windowed table, cell bars), `order-ticket`, `what-if-shocks` (state and effects). Events are declared where the panel can raise them (table and bar clicks, and `point-click` on lines, areas, candles, waterfalls and timelines); pivot cells and surface points have none yet. Column names use underscores (`pay_date`), formats are Core's. `vol-surface` takes one row per expiry and named strike columns (the surface kind has fixed columns). The gallery now accepts category ids with hyphens.
- **Vendored Elements pack with the grid, graph and tree bundles**: `js/vendor/drishti-elements/3.2.0-dev+475232e/` is re-packed from the same commit with `--libs core,views,events,chart,grid,graph,tree` (SHA-384 manifest kept), so core's `network-graph`, `tree-table` and tables no longer request files the pack lacks; the library gallery paint test no longer tolerates any failed request.
- **`point-click` writes state** (I1, level 1.2, lane A8): `on: { point-click: [ { set: { state, to } } ] }` on `line`, `area`, `scatter`, `waterfall`, `timeline`, `candlestick` (`@` the mark's row, `#index` its place before `where`) and `metric`, `gauge`, `kv`, `status` (one mark, the panel's context); contract 1.2 appended: `set` on `Point`, `Step`, `Event`, `Candle`, `Metric`, `Gauge`, `Fields`, `Chart.set` beside `x`; cases `fire/points`, `problem/state#point-click-only-on-its-marks`; SPEC 2.8, VIEWMODEL.
- **Conformance kind `fire`** (FORMAT 5.15, lane A8): a case fires triggers on marks as a painter does (`steps: [{panel, trigger, mark?, values?}]`) and checks per step the screen's `state`, the `effects` run, the refusals (`problems`, `RCH-5001`) and `at` into the ViewModel after it; Core's `control.Fire` (marks by trigger: rows depth first, bars, points/steps/events/candles/x positions, the one mark of a figure or record); JVM, both JS bundles and the Python runner; cases `fire/clicks`.
- **Library quality gates and gallery**: `rachana lib gate <lib> [--strict]` (one implementation, `LibraryGates` over `LibraryTool`, run by `LibrariesCheckTest` on every bundled library and by `tools/libraries/build.py`) reports LG1 valid, LG2 frozen goldens, LG3 definition of done, LG4 level replay (`RCH-2008` one level below), LG5 interface append-only and LG8 budgets; `rachana lib freeze <lib>` records the released baseline (`FROZEN.sha384`, `released/*.interface.json`). Notes, not failures: a second example per fragment (`--strict` fails it), edge tests, live/window cases, accessibility summaries and table samples are not built; LG6, LG7 and LG9 run in release. `lib build` writes each fragment's full interface (doc, parameters, contract, slots, events, budget, needs) into `exports`, checked for staleness. `tools/libraries/build.py --gallery` builds the library gallery with Rachana itself (`gallery.py`: a Sutra per fragment over the manifest, the live example and its source; `gallery_check.py`: Chromium paint test). `rachana preview` resolves libraries as `build` does. The tools bundle checks expressions at the Sutra's language level.
- **Studio on the new Drishti Elements widgets**: the vendored pack is now `js/vendor/drishti-elements/3.2.0-dev+475232e/` (packed from drishti-elements commit 475232e, SHA-384 manifest kept). Studio's frame is `SplitPane` (palette | preview | a vertical split of source, properties and changes), the palette a `NavList` grouped by library, the column mapping the `columnmap` form field; `<rachana-view complete>` asks the painter to complete the view, so the console no longer warns that `ref` is missing. No hand-rolled widget is left in Studio.
- **`rachana build` sites load libraries in the browser**: a page that uses `use:` gets the files of the libraries it names (project, `--libs`, `RACHANA_LIBS`, bundled) copied to `rachana_libs/<library>/` and a `libraries/<page>.libraries.json` manifest that `<rachana-view libraries="...">` reads (a Sutra with `libraries:` builds at `--level 1.2`); tested in Node and by the Chromium `paint_check.py`.
- **Fragment libraries moved into this repository** (owner decision, L-12 superseded): `libraries/<name>/` by domain, `libraries/README.md`
  the index, `tools/libraries/` the build and check (`build.py`, `LibCheck.java`; the jars come from this repository's own build);
  `LibrariesCheckTest` runs the check in `./mvnw verify` (and proves an external folder and an `.rlib` beside bundled `core`); Studio, `lint` and `build` (`--libs`, `RACHANA_LIBS`) and the Python `lint` take extra library paths beside the bundled ones; a same-name conflict is refused (`LibraryResolver.first` too). The old repository's history is merged in.
  **core 1.0.0** (baseline LB1, language level 1.2): 28 typed fragments (figures, records, tables, charts, states, history) with examples
  and ViewModel snapshots, and `compare-ab` (lane A6: two records field by field over a `list<record{field, label, fmt?}>`, the
  differences highlighted, `only-different`); not built: `entry-form`, `stepped-form` (no standalone form panel yet). Differences from the catalogue are listed in `libraries/core/README.md`.

- **Rachana Studio, first slice** (`js/packages/rachana-studio`, `rachana studio <folder> [--libs ...]`; decision MK-1): three panes (the fragments of the
  project's libraries, the live `<rachana-view>`, the Sutra source with its live problems, the parameter form of the chosen fragment and the
  unsaved changes) under a command bar; the form inserts a `use:` block with a column mapping; local save. The AI prompt is a disabled
  placeholder. Built from Drishti Elements 3.1.0 widgets only; `<rachana-view>` takes a `files` property (the libraries). Vendored js-yaml 4.1.0.

### Nested stateful fragments, the rest of the effect list (G2 lane A7)

- **Nested uses are instances of the outer use** (SPEC 2.5; LIBRARIES 7.2): a use named `x` inside a fragment used as `a`
  is `a-x` (ids `a-x-<id>`, private state `a-x.<name>`, events `@a-x.<event>`); before, two uses of a fragment holding a
  stateful use collided. The outer fragment reads only the inner use's outputs, and the screen no nested instance
  (`RCH-2117`). Cases `sutra/fragments-nested-state` (2), `problem/fragments-nested-state` (3).
- **State name parameters** (LIBRARIES 4.2, 6.3; SPEC 2.5): `{ type: state, of: text }` hands a fragment one name of the
  screen's state; the fragment writes only it, as `set: { state: "%{into}" }` (`RCH-2048` elsewhere); the name must be held
  and of type `of` (`RCH-2116`); `selection` parameters wait for I2 (`RCH-2023`). Cases `sutra/fragments-state-params` (1),
  `problem/fragments-state-params` (4).
- **`submit` and `cancel` join I1's closed trigger list** (for the standalone form panel; its effects run only once the
  form's rules pass); refused (`RCH-2023`) until that panel is built. At 1.2 an emits trigger message no longer calls
  `point-click` later (`problem/wiring#emits-on-tick` now `until: "1.1"`, its 1.2 answer a new case); case files take
  `until:` (FORMAT 4, the runner skips a case above it).

### Typed fragments, the gaps the core library found (G2 lane A6)

- **Renames over derived rows** (L-21, never silent): a panel whose `rows` is the table parameter filtered, sorted, limited or piped
  (`take(sort(arg.rows, lambda r: -r.amount), arg.top)`, `arg.rows[?@.amount > 0] |> sort(...)`) has its field-name options and
  `bind`s renamed to the caller's columns (they were left as written, with the advisory `RCH-2120`); over `map(arg.rows, {k: ...})`
  the panel reads the fixed names; a local column named over rows derived any other way is `RCH-2121`. `FragmentColumns.rowsOf`
  (API addition). Suites `sutra/fragments-gaps`, `problem/fragments-gaps`.
- **`list<record{...}>` and written-out records**: a `list<T>`, `record{f: T, ...}` or `list<record{...}>` parameter may be written
  at the use as YAML (a list, a mapping), checked item by item and field by field (`RCH-2116`; `max`/`min` count the items); as a
  whole value it is placed as written (`fields: arg.fields`), in an expression it is the EL literal. A text literal inside a fragment
  is now quoted with a backslash escape (was a doubled quote, which Rachana-EL does not read).
- **Dynamic text parameters**: `type: text, dynamic: true` takes a template at the use (`"Limit used ${fmt(..., 'pct0')}"`), read in
  the caller's context: the template as a whole value, `concat(...)` of its parts inside an expression; `@`, `^`, `#index` refused.
  A plain `text` keeps its text as written. `FragmentColumns.templateParts` (API addition).
- **Events beyond rows and bars** (I1 `point-click`, level 1.2): `emits: [{ on: point-click }]` on `line`, `area`, `scatter`,
  `waterfall`, `timeline`, `candlestick` (each mark a row of `rows`; its `ev` the row before `where`) and on `metric`, `gauge`, `kv`,
  `status` (the panel one mark, `ev` "0", the payload read in the panel's context); `RCH-2057` on other kinds. Contract 1.2 appended:
  `ev` on `Point`, `Step`, `Event`, `Candle`, `Metric`, `Gauge`, `Fields`, `Chart.ev` beside `x`; the stamp is 1.2 when one is
  present. A fragment can now return events from its charts and figures. `Wiring.POINT_CLICK`, `POINT_KINDS`, `ONE_MARK`,
  `Wiring.fires`, the records' `ev` (API additions; old constructors kept). Not yet: `on: { point-click: [ set ... ] }` (the effect
  list's trigger table, lane A5's `Effects.BUILT`), graph nodes, the painter's side (Elements).

### Screen state and the effect list's set (G2 lane A5)

- **S1 + I1 (first step), level 1.2** (SPEC 2.8; decision S1-OPEN): a Sutra's `state:` (typed `text`, `number`, `boolean`,
  `date`, `list`; a default; at most 40 with fragments' private state; `url` and `remember` refused until S4), read as
  `state.<id>` (`Expr.State`, parsed only while a Sutra that holds state is in force; an undeclared id does not compile), and
  a panel's `on:` (`row-click` on tables and ladders, `bar-click` on hbars) with the effect `set`: `{ state, to }` evaluated
  per row (`Row.set`, `Bar.set`), or `set: <control>` (with `field:`), which is the panel's `on-select`. The bind request
  carries `StateValues` beside `ControlValues` (`state.<id>=...`; `ViewBuilder.Request.withState`, `LiveScreen.state`), and
  the ViewModel lists `state[] {id, value, url, reads}` (contract 1.2, `ViewModel.NEWEST`; an older painter ignores it).
  Fragments: private `state:` held as `<as>.<name>` (a stateful fragment needs `as:`, `RCH-2115`), `outputs:` read by the
  screen as `state.<as>.<name>`, never written (`RCH-2117`); a use's `on:` takes an effect list that writes the screen's
  state from the event's payload (`event.<name>`). Cases `sutra/state` (6), `problem/state` (9), `sutra/fragments-state`
  (2), `problem/fragments-state` (4); conformance `given.state`. Not yet: the painter side (Elements lane), the other
  triggers and effects, `state` name parameters (LIBRARIES 6.3), S4.

### Named arguments by the catalogue; stable wire keys; Sutra let and functions, library functions (G2 lane A4)

- **X41 + S2 + X42, level 1.2** (SPEC 2.7): a Sutra's `let:` (named values, each reading the ones before it, no current
  row, evaluated once per bind) and `functions:` (pure: a body reads only its parameters and other functions; no recursion;
  named and piped calls), and a library's functions (manifest `functions:` and `functions/*.yaml`, called `<lib>.<f>(...)`
  from the Sutra and from the library's fragments). Inlined by the parser while the Sutra's `Declarations` are in force
  (`Declarations.within`, opened by `SutraExpressions.check`, `ViewBuilder.build`, `ScreenWire.event`/`children`,
  `DataRecord.record`, `DataPlan.plan`, `ControlResolver.resolve`); compiled expressions are cached per declarations.
  `Sutra.declarations()`, `Library.functions()`. Cases `sutra/declarations` (2), `problem/declarations` (7); the lib case
  that refused functions now checks a library function with a Core name. Not yet: `examples` run as tests (X43).

- **The function catalogue is the one source of Core's parameter names** (lead's decision): `spec/1/functions.yaml` is built into
  the jar and both bundles (`rachana/el/functions.yaml`); `CoreParams` reads names, repeatable parameters and `role: column` from
  it (`FragmentColumns.COLUMN_PARAMS` too). From level 1.1 named arguments bind by those names (`avg($.p, field = 'x')`; the old
  kernel names `x, where` are refused; a repeatable parameter only by place). Level 1.0 unchanged. `CoreParamsTest` guards that
  every implemented function has a catalogue entry and vice versa; `el/named-arguments-1-1` (10 cases). Bundles grow by the
  catalogue (Brotli runtime 463 KB, tools 483 KB).
- **Deterministic ViewModel bytes**: an event's `wire.emits` keys (`name`, `on`) and a child group's keys in declaration order (were
  `Map.of`'s, varying from run to run); the same for a plan's `&&` filter and the chart binder's engine view. `ScreenWireOrderTest`.
- **L-21, never silent over derived rows** (library finding): `difference` and `intersect` keep their first list's rows (`union` when
  both lists are one table's), so a filter's `@.<column>` over them is renamed; a filter over rows the mapping cannot follow
  (`map(arg.rows, @)[?@.amount > 0]`) is `RCH-2121`, never a silent read of nothing. Cases sutra +1, problem +1.

### Live tables, read side (G2 lane L, LIVE_TABLES L1 to L6)

- **L11 (G2 half), conformance**: the live test adapter `LiveReferenceServer` (`rachana-live`: `/_test/live` loads a declaration and
  its source's rows, `/_test/as` acts as a reader with a view, `/_test/stream` changes the source and pushes the stream, `/_test/upkeep`
  moves the clock, `/_test/audit` reads the events; PROTOCOL 15), run by `protocol_check.py --live`; a fixture with a `declaration`
  (FORMAT 5.12, `case.schema.json`) runs only there (other hosts: unsupported; the Python host skips it, LT-14). Suites
  `protocol/window` 12, `protocol/narrow` 6, `live/merge` 9, `live/resync` 5, `live/retention` 5, `live/overlay` 11, `live/readers` 11,
  all unverified, all green on the reference; the ported-case mapping table is LIVE_TABLES 9.1.
- **Windows in the minimal SSE form and the clients**: `POST /changes` takes `window: {keys, top}`, answers `subd` first and is moved by
  `POST /changes/{sub}/window` (only the stream's own caller: else `rdp.table.unknown`); a gap ends it. `TableClient.follow(…, window,
  top, …)` and `TableClient.window`; JavaScript `DataClient.follow(…, {window})` and `DataClient.window` over HTTP and the bridge (`win`,
  a window's `gap` routed). `ReferenceServer` gains `actAs`, `testPath`, `reset`, `onReset` (one bridge channel per caller);
  `ProtocolHost.remove`. API additions only.
- **Paging without O(offset)**: a paged table orders by the whole sort list (`dir`, `nulls`), then the key values; its cursor is keyset
  (the last row sent, found by binary search in the kept index and the live side: rows changing before it neither repeat nor skip),
  and an offset is placed by counting the live side and the shadowed positions.
- **Declarations in Studio and lint**: the tools bundle's `live(request)` checks a `.live.yaml` (`{ok, problems, schema}`, FORMAT 5.14);
  `rachana lint` lints `*.live.yaml` beside Sutras (the Drishti profile's keys with `--profiles core,drishti`). The runtime bundle stays
  without the checker.
- The four unverified JVM `examples` cases that fail at level 1.2 (`edit/examples/edit#set-a-title-keeps-comments`,
  `#remove-a-required-key-is-refused`, `problem/examples/problem#problems-in-position-order-l11`, `#profile-key-without-the-profile`)
  fail the same on fc74ffd, before the live tables work: not caused by it.

- **L3 to L6, the engine** (new JVM module `rachana-live`, never in Core or the bundles: `LiveModuleGuardTest`): `LiveTables` is a
  Data Protocol provider over `SnapshotSource` (whole, columns, query tiers) and `LiveStream`; merge by key and record version
  (`KeyedRows.versionOrder`), new epoch on a gap, paused streams, retention, `PollingDiff`; `StoreOverlay` pages columns-tier sources
  through kept indexes merged with the live side; `narrow` and heavy columns; `Subscription` gains key windows (`top`, `total`,
  coalescing, capacity gap), redaction and a lookup-based offer, and the bridge `win`; `Authoriser`/`ReaderView`/`AuditSink` keep every
  page, total and patch to the reader (fail closed). `LiveTablesTest` ports the liveset scenarios (merge, snapshot race, burst, paged
  window, newest-first top, retention, merged pages, two audiences).
- **L1, the declaration** (`rachana-core`): `LiveTableSpec` reads and checks `<name>.live.yaml` (`rachana: 1`, `live-table:`, snapshot,
  stream, `where` pushed down as a filter tree, key, version, `changed-by`, `changed-at`, columns with `width`, `fmt`, `heavy`, order,
  grouping, subtotals, `narrow`, `retention`, `tray`, `edit:` as a Core form with `reason`), every problem at once in position order;
  `LiveTableSpec.of` for a host's default declarations in code; `schema()` answers the Data Protocol schema document. New codes
  `RCH-2080` (edit needs a key and a version), `2081` (not editable), `2082` (heavy column where a list needs it), `2083` (retention).
  PROTOCOL section 16 (schema fields, windowed subscriptions, patch `total`/`top`, `narrow`, writes, `win`/`write` bridge messages,
  five error codes) and the JSON schemas; section 14 now says writes travel only under `can: write`. Conformance kind `live`
  (FORMAT 5.14) with `live/declaration` (9 cases, unverified; JVM and the tools bundle agree). API surface: additions only.
- **L2, one protocol host over providers** (`rachana-data`): `ProtocolHost` answers the routes of PROTOCOL 4 and the bridge of 12 over
  any number of `TableProvider`s (a table is answered by the first provider that has it; the listing is every provider's) and checks
  every query document (`RowQuery.validate`) before a provider sees it. `RowQuery` is the one evaluation of a query document over rows
  held in memory (filter as Rachana-EL, sort, cursor and offset, group). `MemoryHost` is now a provider (its own `handle` and `bridge`
  delegate to its `protocol()`); `ReferenceServer` serves `host().protocol()`, so other providers can be added to it. A caller object
  (opaque) travels from the transport to the provider. No behaviour change: protocol cases 54 of 55 as before (the frozen
  `examples/bridge` sort case).

### Python SDK parity with the JVM data host (G2)

- **Arrow IPC and NDJSON** from `DataHost` (`drishti_rachana.rows_forms`): `Accept: application/vnd.apache.arrow.stream` (official pyarrow, optional extra `drishti-rachana[arrow]`; `406` + `rdp.query.unsupported` when it is not installed or the table does not declare `arrow`, as the JVM) and `application/x-ndjson`, by the rules of PROTOCOL 3.1, 3.2, 5.2 (metadata, masked `<name>$masked`, big integers, decimals), 5.3; a reader that also accepts JSON or `*/*` gets JSON.
- **Filtered and conflated subscriptions** (`drishti_rachana.subscription`, port of `Subscription.java`): `changes.filter` restates each patch (`remove` / `update`, an empty patch keeps the numbering), `conflate: {everyMs}` merges by key with `first` and `seq`; `GET` or `POST /changes` (`table`, `after`, `filter`, `conflate`); an unresumable position (and any filtered stream not at the table's position) is one `gap` frame with `recoverable: false`. The bridge moved from the test host to `drishti_rachana.bridge` (`sub`, `unsub`, `msg`, `res.arrow`).
- **`protocol_check.py --python` runs every `protocol` case** (the `--python` skips of `protocol/forms` and `protocol/live` are gone): 55 cases, 54 pass; the one failure is the frozen `bridge-rows-request-answered-once`, which the JVM reference fails the same way.
- **The adapters serve the real `<rachana-view>`**: `drishti_rachana.assets` serves the element and its modules, the vendored Drishti Elements 3.1.0 pack and the runtime bundle (and Apache Arrow JS) under `<mount>/static/...` with correct media types, as package data of the wheel (`pyproject.toml` force-include) or from `js/` in a checkout; the placeholder `DEFAULT_VIEW_SCRIPT` and `view_script=` are deleted. `@table(..., schema=)` declares `arrow`. The FastAPI change stream no longer fails on an async table function (the first read ran inside the event loop). `examples/live-orders/paint_check.py` (Chromium): paints, changes while open, no console error.

### CLI and Python SDK, second pass (G2)

- **Language level for `lint` / `check`**: the bundle's `check` already read `since` (as `open` does), so no facade change; `rachana lint`,
  `python -m drishti_rachana lint` and `Rachana.check(level=)` take `--level` / `level` (default **1.1**; `1.2` for typed fragments and
  libraries). `examples/static-sales` and `live-orders` lint clean at 1.1 and 1.2 (controls read `options:`, not `from:`; `regions` is
  a `table:`).
- **`rachana build` / `preview` use the real `<rachana-view>`**: the element, its modules, the vendored Elements pack and the runtime
  bundle are copied into the site; `static_files_host.js` answers the Data Protocol in the page from `data/` (static-files mode); the
  warning and the TODO are gone. Tests: node (`build` of static-sales), `test/paint_check.py` (Chromium: every page
  paints, no console error). 
- **Python `DataHost` and the JVM reference answer the same `protocol` cases**: `drishti_rachana.testhost` (the PROTOCOL 15 test
  adapter over the standard library) and `protocol_check.py --python`; the host first passed 39 of the 40 cases it ran (superseded by the parity entry above; the one failing case is the
  frozen example that sorts a table without `can: sort`, which the JVM reference fails the same way). The host now enforces `can` (`rdp.query.unsupported` with `operation`), checks filter columns
  (`rdp.column.unknown`) and operations (`rdp.query.invalid`), refuses masked columns (`rdp.forbidden`), orders by the key last with
  empty values last, compares decimals exactly, answers `group` (count, sum, min, max, avg, distinct, totals) and sends `t: msg` frames
  and `gap` with `recoverable: false` when there is no replay. `python/tests/test_protocol_cases.py` runs the cases.
- The three `test_conformance.py` failures could not be reproduced on `g2/integration` (6 pass, 26 in the package); two stale
  expectations in `test_server_adapters.py` were corrected to the spec.

### Fragments are functions: column texts reach the mapping (decision L-21 rule 3, lead), level 1.2 (G2 lane A3)

- **Column parameters** (SPEC 2.5, `functions.yaml`): every Core parameter whose argument names a column of the list's rows is
  marked `role: "column"` (`field` of `sum`, `count`, `avg`, `median`, `variance`, `stddev`, `variance_pop`, `stddev_pop`,
  `distinct`, `percentile`, `span`; `key` and `aggregate` of `group`; `b` and `c` of `wavg`); `roles_since: "1.2"` says when roles
  arrived, so `levels.py write` and `LevelReplayTest` write a level before it without them (level 1.0's file is unchanged).
  `functions.schema.json` describes both keys.
- **Renamed at compile time like `@.col`** (`com.ash.rachana.el.FragmentColumns`, used by `TypedFragment.bind` before the
  arguments are put in): the text of a column parameter over a table parameter (`sum(arg.rows, 'amount')`), `@.<column>` where
  `@` is a row of the table (a panel that reads it, a filter `arg.rows[?...]`, the per-element argument of `map`, `sort`, `reduce`,
  `group`), a lambda element's field (`lambda r: r.amount`), the field of `first`/`last`/an index of the rows, and the same inside a
  template's `${...}`; the pipe and `sort`/`take`/`drop`/`unique` keep the table's rows. The spelling is kept; only the names change.
  `RCH-2120` now also counts these reads.
- **`RCH-2121`** (error): a column parameter over the table given a computed text, a text naming a local column over a list that is
  not the table, a column the table does not declare, or a bare name that is a local column; the message names the local column and
  says to write `@.<name>` or the column as text.
- **`fn` parameters** (LIBRARIES 4.2, X13 lambdas of lane B): a value parameter of type `fn(T, ...) -> U` (one name or more;
  `any` allowed in it) takes a lambda written at the use in the user's context (`score: "lambda x: x / 1000"`, or a lambda
  `default`); the count of names and a body free of `@`, `^`, `#index` are checked (`RCH-2116`); inside, `arg.f(x)` becomes
  `let(fn_f = <lambda>, ... fn_f(x) ...)` around the expression (around each `${...}` in a template) and `arg.f` passed whole
  to `map`/`sort`/`reduce`/`group` becomes the lambda. Cases: `sutra/fragments-calls` (+2), `problem/fragments-calls` (+4).
- **`rachana lib contracts` and `rachana lib check-host`** (LIBRARIES 5.2, 22; `java/rachana-cli` `Main` over `LibraryTool`, the
  one implementation; the npm launcher passes them through): `contracts <dir|rlib> [--json]` lists each published contract
  (title, grain, key, columns with type and required/optional) and the fragments (`name@N`) whose table parameters name it;
  `check-host <dir|rlib> --base <url> [--table CONTRACT=TABLE]... [--json]` reads each table's schema from a Data Protocol host
  (`GET <base>/tables/<name>`, the JDK's HTTP client) and reports every column `ok`, `type`, `missing`, `absent` (optional),
  `masked` or the table `no-table`, the rows of `ops/contract-fit`; exit 1 when a column breaks a fragment or the host does not
  answer. `LibraryTool.contracts`, `checkHost`, `report`; `Library.majors(fragment)`; `TypedFragment.fits` public. MainTest.
- **Conformance**: `sutra/fragments-calls` (+2: every renamed form, identity), `problem/fragments-calls` (+4: one per refusal),
  verified; the `sutra` kind now builds the view at the case's level (it built at 1.0, so 1.1 syntax in a 1.2 case did not
  read). `FragmentColumnsTest` checks the Java table of column parameters equals the role marks. API additions only
  (`FragmentColumns`, `SutraExpressions.expressionOptions(String)`, `TypedFragment.COLUMN_TEXT`).

### Fragments are functions (decision L-21, owner 2026-11-21), level 1.2 (G2 lane A2)

- **Binding like a call** (SPEC 2.5, LIBRARIES 5 to 7, DECISIONS L-21): a table parameter names its local columns or a
  published contract (`contract: <name>`; `contracts/<name>.json` in a library, `Library.contract`, or served by the host,
  `FragmentResolver.contract`); the use binds it as `rows: $.t` (identity) or `{ from, columns: { <local>: <caller> } }`,
  renamed at compile time in the panels that read it (field options, `bind`, `@.<column>`). With the caller's columns known
  (`data:` `fields` or written rows) problems name both sides: `RCH-2114` unknown local column, `RCH-2115` required column
  missing, `RCH-2116` unknown caller column or type misfit; `RCH-2120` advisory for a mapping never read (severity warning,
  `Sutra.advisories()`). Every typed fragment is hermetic (`RCH-2117`).
- **Instances and return values**: `as:` scopes ids (`<as>-<id>`); `emits: [{name, payload, doc}]` declared and checked
  against the inner panel that raises it; the screen reads `@<as>.<event>.<field>`; `on: {<event>: {set, field}}` sets a
  control from the event's row (`on-select`). `TypedFragment.bind`, `TypedFragment.Caller`, `SutraBlocks.Calls`.
- **Resolvers for the carriers** (LIBRARIES 10, 11.2): `LibraryResolver.classpath(ClassLoader)` reads
  `META-INF/rachana/libs/<name>/` by its `MANIFEST.sha384` (index `META-INF/rachana/libraries`);
  `LibraryResolver.packages(roots)` reads npm packages declaring `"rachana": {"library", "folder"}` and Python distributions
  declaring the entry point group `rachana.libraries`, as data (nothing is run). Same bytes, same SHA-384 as the folder.
- **Compile result records library hashes**: `Sutra.libraries()` (`UsedLibrary`: name, exact version, SHA-384), from
  `LibrarySet.fragments(..., Consumer<Library>)`; the `problem` case output carries `libraries` (FORMAT 5.8, schema).
- **Conformance**: `sutra/fragments-calls` (6), `problem/fragments-calls` (11), verified; JVM and both JavaScript bundles
  agree at 1.0, 1.1, 1.2; level-1.0 replay and the customer corpus unchanged. API additions only (`api-surface.txt`).

### IntelliJ launchers and installing a release

- **Build launchers**: `java/run-configs/` holds four IntelliJ Maven configurations in the folder **Rachana** (build all, test, install release for a release tag only, JS bundles), the build guide is [java/README](java/README.md#build-in-intellij-idea), and a release is installed into `~/.m2` for Drishti from its tag only (decision R-1).

### Rachana-EL level 1.1: the syntax batch and exact numbers (lane B: EL phase 1)

- **Syntax at language level 1.1** (SPEC 4.17, EBNF part D; EL_EXPANSION X6, X7, X11 to X14; DECISIONS PY1 to PY8): record literals
  `{a: 1, 'k-2': x, ...r}` and list spread `[...a, b]`; Python slices `t[a:b:c]` and `range(start, stop, step)` (end-exclusive; `..` stays
  reserved; `range` is only the sequence at 1.1, the aggregate max − min is `span`, EL1-1); the pipe `x |> f(a)`; `a ?? b`; `a if c else b`; lambdas `lambda x: …`,
  second class (map, sort, reduce, group, `let`; no recursion; `=>` stays reserved); `**` and infix `^` (right-associative, `-2 ** 2` is
  `-4`); comments `/* … */`; digit separators `1_000_000`. Reserved names are refused by name at 1.1.
- **Numbers built at 1.1** (SPEC 4.16, X2, XD3): literals without an exponent are exact integers and decimals (34 digits), `+ - * %`
  exact, `/` half-even to 34 digits, a double on either side a double, overflow of the integer part `RCH-2102`; `sum` exact over
  integers and decimals. **Texts by code point** at 1.1 (SPEC 4.13, XD4): `left`, `right`, `mid`, `find`, `search`, `pad`, ordering,
  `sort`, `least`, `greatest`. The N5 level-1.1 variants: problem messages `expression '<source>': …` with the token's end,
  `standardize` a number, a refused pattern at evaluation `RCH-2102`.
- **Level 1.0 unchanged**: the replay of `compat/levels/1.0` is identical; the level is the compiler's (`ElCompiler(…, level)`,
  `Rachana.language`). API additions only (`Expr.Op`, `Minus`, `Power`, `Coalesce`, `RecordLit`, `Pair`, `Spread`, `Slice`,
  `Lambda`, `FnRef`, `LocalCall`; `Printer.print(e, v11)`; level-aware `CheckedMode.check`/`parse` and `Checker.check`;
  `ElException.end()`). The checker types `??`, `**` and the exact operators (X8, first part). `FilterTree` reads level 1.1 trees.
- **Conformance**: `el/syntax-1-1` (114 cases) and `el/syntax-at-1-0` (13), verified; `el/numbers-exact`, `el/text-code-points`,
  `el/reserved` and `el/messages-level-1-1` verified, their cases that failed at 1.0 moved to `since: "1.1"` (9 reserved-token
  cases withdrawn: 1.1 defines the tokens; two `**` message cases withdrawn: PY6). `tools/regime/levels.py check` skips a recorded
  case that moved to a later level or was withdrawn only when `compat/levels/moved-cases.yaml` lists it (EL1-4, below). JVM, both JavaScript bundles
  and the Python V8 engine agree.

### The EL1 decisions built (DECISIONS "EL1 decisions taken")

- **EL1-1**: at level 1.1 `range(...)` is only Python's sequence (the dispatch on the first argument is gone); the aggregate max − min is
  `span(list, field?)` (functions.yaml, `since: "1.1"`; Core's `range` kernel); a list or a text written as `range`'s argument does not
  compile (`RCH-2101`), one met at evaluation is `RCH-2102`, both naming `span`; a masked argument masks. Level 1.0 keeps the `range`
  aggregate and has no `span`. Cases in `el/syntax-1-1` and `el/syntax-at-1-0`.
- **EL1-4**: `compat/levels/moved-cases.yaml` lists every recorded 1.0 case that moved to a later level (46, with `since`) or was withdrawn
  (9, with the reason); `tools/regime/levels.py check` skips only those and fails on any other difference and on a wrong entry
  (`levels.py compare`, tested by `LevelReplayTest`). A level's `functions.yaml` is spec/1's less the entries of a later `since`.
- **EL1-2**: checked mode at 1.1 warns `RCH-2091` on `-x ** n` or `-x ^ n` without parentheses (`-(x ** n)` by Python's rule, `(-x) ^ n`
  in Excel), with both spellings as the fix (SPEC 4.17, 6.2).

### `data:` and the Data Protocol; `<rachana-view>` (lane C: SPLIT_PLAN steps 15 and 16, B1 to B3)

- **`data:`** at language level 1.1 (SPEC 2.4, D10): entries `{ table }`, `{ url }` (or an address), `{ rows }`, `{ record }`, each a
  field of `$`; `key`, `fields`, `as`, `where` (reads controls), `limit`, `refresh`, `live`, `mode`, `format`, `path`. Problems
  `RCH-2073` (not exactly one source, a bad name), `RCH-2072` (live without a key), `RCH-2011`, `RCH-2012`. At level 1.0 `data` stays an
  unknown key (`RCH-2011`): the level-1.0 replay is identical.
- **Core, `com.ash.rachana.table`** (one implementation; the JavaScript bundles are the same code): `TableSchema` (cells decoded by type:
  decimals exact, big integers, masked), `KeyedRows` (patches in the order of PROTOCOL 8.1; keys compared as JSON text; append windows),
  `FilterTree` (a row condition as the protocol's filter tree with controls and constants folded; a received tree checked and spelt as
  the Rachana-EL condition of the same meaning), `DataPlan` (the plan document: per table the query, the readers and what stays local;
  a panel's `where` is pushed down only for the table's sole reader; `RCH-2070`, `RCH-2071`), `DataRecord`, `LiveScreen`; `ViewDiff`
  (ViewModel frames: `strip`, `panel`, `rows` patches).
- **`java/rachana-data`**: `MemoryHost` (the protocol transport-free: schema, rows, query with filter, sort with the key as tie-breaker,
  cursor paging, grouping with exact decimal totals, the change log, gaps, the bridge), `ReferenceServer` (JDK HTTP server, the minimal
  SSE stream, the test adapter of PROTOCOL 15, static files for a page), `TableClient` (the JVM client: the plan's queries, paging, the
  stream into a `LiveScreen`).
- **JavaScript** (`js/packages/rachana/src`): the bundles export `patch`, `plan` and `screen(op, request)`; `DataClient` (transport only:
  fetch, cursor paging, the SSE stream or a host's `subscribe` hook, the origin allow-list), `Screen`, and `<rachana-view>` painting with
  `<elements-view>` from Drishti Elements 3.1.0, vendored (`js/vendor/drishti-elements/3.1.0`, the release's `pack` of core, views and
  events, with its SHA-384 manifest).
- **Conformance**: the runner answers the kinds `patch` and `plan` (`steps` compared per step); new cases `sutra/data` (9),
  `problem/data` (8), `plan/plan` (8), `patch/patch` (5), `protocol/host` (10); the frozen example `plan/examples/plan` corrected as its
  note said (`select` is not a control kind). `tools/conformance/protocol_check.py --reference` runs the 40 protocol cases on the
  reference server: 39 pass; `protocol/examples/bridge#bridge-rows-request-answered-once` sorts a table that does not declare `sort`,
  which PROTOCOL 4 refuses (unverified; `protocol/host#bridge-rows-of-a-sortable-table` is the corrected case). JVM and JavaScript agree
  on all 2,160 cases at levels 1.0 and 1.1; budgets: runtime 379 KB, tools 399 KB Brotli. Tests: Core `TablesTest`, `MemoryHostTest`,
  `TableClientTest` (JVM end to end), `js/packages/rachana/test/data_client.test.mjs` (Node end to end),
  `runners/view_browser.py` (`<rachana-view>` live in Chromium).
- Not yet (lane C2 below builds the protocol's rest and checked mode over table schemas): exact decimal arithmetic in aggregates (the
  decimals item, X2).

### The Data Protocol completed: row forms, bridge subscriptions, conflation, filters (lane C2)

- **Arrow IPC and NDJSON** (PROTOCOL 5.2, 5.3, 12). `java/rachana-data`: `ArrowRows` writes a page as an Arrow IPC stream and reads one
  back with the official Apache Arrow Java library (19.0.0, `arrow-vector` with the `arrow-memory-unsafe` allocator; allowed in this
  module only, Core stays free of it; a JVM that runs it opens `java.nio` to `org.apache.arrow.memory.core`), types by PROTOCOL 3.1,
  masked cells as `<name>$masked`, the protocol facts as schema metadata (`rachana.totals` and `rachana.schema` added); `NdjsonRows`
  (header, then one line per row). The reference server answers both (Arrow only for tables declaring `arrow`, else `406`); the bridge
  answers `accept: arrow` with `res.arrow` (base64). `TableClient.read(table, query, Form)`. JavaScript: `arrowPage` (the vendored
  Apache Arrow JS 21.1.0, `js/vendor/apache-arrow`, the same file Drishti Elements vendors), `ndjsonPage`, `DataClient({form, arrow})`,
  `<rachana-view form="arrow|ndjson">`. Every form gives the same rows (`RowFormsTest`, `data_client.test.mjs`). Cases
  `protocol/forms` (7); `protocol_check.py` compares Arrow and NDJSON answers as the JSON page they stand for (Arrow with pyarrow).
- **Subscriptions** (PROTOCOL 8.2 to 8.4, 12). `MemoryHost.subscribe(table, after, filter, everyMs, sink)` gives a `Subscription`:
  the backlog then live frames in seq order; **filtered** (`changes.filter`): each patch restated for the rows the reader sees (a row
  that stops matching is a `remove`, one that starts an `update` with every field, an untouched patch an empty one so the numbering has
  no holes; resumable only from the current position, else `recovered: false`); **conflated** (`everyMs`): one message per window,
  merged by key (`Subscription.merge`: later remove cancels, later fields win, `reset` restarts), carrying `first` and `seq`. The
  bridge: `MemoryHost.bridge(push)` opens a channel (`sub` answered by `res {recovered, version}`, `msg` with the `sub` id, `unsub`);
  the reference server's SSE stream runs on a subscription and takes `POST /changes {table, after, filter?, conflate?}`; the test
  adapter gains `GET /_test/bridge/out` (PROTOCOL 15). `TableClient.follow(..., filter, everyMs, ...)`, and a gap now stops the follow.
  JavaScript: `BridgeChannel` (requests, `res.arrow`, `sub`/`msg`) behind `DataClient({bridge})`, `follow(..., {filter, everyMs})`;
  `Screen` follows a table read through a pushed-down filter with a filtered subscription where the table offers one, and takes a
  conflated message's `first`. Specification additions: PROTOCOL 8.2 (the POST form), 8.4 (`first`), 12 (`sub.conflate`, `msg.sub`,
  `msg.first`, the `sub` answer), 15; `bridge.json`, `patch.json`. Cases `protocol/live` (8); tests `SubscriptionTest`, the Node bridge
  test.
- **Chart panels in `<rachana-view>`**: the vendored Elements 3.1.0 pack now has `drishti-chart` (with `drishti-inputs`, Apache
  ECharts in the script, and its run-time files under `vendor/`), packed from the `v3.1.0` tag with its SHA-384 manifest. The browser
  test's Sutra has a line panel; `runners/view_browser.py` passes in Chromium 156, Firefox 157 and WebKit 27.2 (Playwright 1.64.0).
- **Checked mode over table schemas** (SPEC 4.14, EL_EXPANSION X8 where it touches tables), from level 1.2: `DataPlan.plan(...,
  level)` reads each row condition over a table (the entry's `where`, a reading panel's `where`) against the table's column types and
  masks (`TableFields`): `RCH-9127` for a type refusal (no implicit casts, no text arithmetic), `RCH-9161` for a filter on a masked
  column, each problem with `entry`, `panel`, `where`; an unknown column stays `RCH-2071`; controls are values. Levels 1.0 and 1.1 are
  unchanged. `LiveScreen` and the conformance runner pass the level. Cases `plan/checked` (5); `TablesTest`.

### The `rachana` command and the Python SDK basics (G2, CLI and SDK lane)

- **CLI (Node, `js/packages/rachana-cli`)**: `rachana new` (starter project), `lint` (compile and problems; `--format text|json|sarif`,
  `--strict`; exit 0 / 1 errors / 2 usage), `build` (lints, then a static site: pages, Sutras, data files, protocol index,
  runtime bundle), `preview` and `dev` (local server). `lib build|sign|keygen|pack|verify|lock` is the Java `LibraryTool` behind
  `com.ash.rachana.cli.Main` (the jar launcher; the Node launcher spawns it: one implementation). Not yet: the painter step of
  `build` and `preview` (`<rachana-view>` is on g2/data), live reload, `fix`, `fmt`, `explain`.
- **Python SDK**: `python -m drishti_rachana lint` / `rachana lint` on V8 (`drishti-rachana[v8]`), same output formats and exit
  codes; `drishti_rachana.server.DataHost`, a minimal Data Protocol v1 host (JSON; schema, rows, query filter tree, cursor paging,
  keyed patches, SSE changes); `RachanaBlueprint` (Flask) and `RachanaRouter` (FastAPI) replace the stubs; the live-orders example
  uses them. Reuse of lane C's server shapes is pending the g2/data merge; the host follows DATA_PROTOCOL.md directly.

### Language level 1.2: typed fragments (C1) and libraries (X42), pulled forward by decision L-19

- **Typed fragments** (SPEC 2.5, LIBRARIES 4): `TypedFragment`; a fragment whose `params:` are declarations with a `type` is
  checked at each use: value parameters (an expression in the user's context, read as `arg.<name>`; literals checked against
  the type and `min`/`max`; `text` taken as written), table parameters (`columns`, `optional`, `when-bound`), name parameters
  as checked tokens (`%{name}` only for those; `id`, `format`, `tone`, `icon`, `variant`, `area`, `palette`, `key`, `enum`,
  `columns`), `slots` of panels. Problems `RCH-2114`..`2118`; a typed use below level 1.2 is `RCH-2008`. The text form is
  untouched: every Drishti fragment expands to the same bytes (CG3, 478 Sutras, byte-identical; level-1.0 replay identical).
- **Libraries** (SPEC 2.5, LIBRARIES 3, 7, 9 to 12; package `com.ash.rachana.lib`): the top-level key `libraries:` (level 1.2;
  `RCH-2008` below), `use: <library>/<fragment>@N` and the long form `<library>@M/<fragment>@N`; `LibraryResolver` (memory,
  folder `rachana_libs/<name>/`, a folder of `.rlib` files, `first` of several, a conflict refused) and `LibrarySet` (resolver +
  `rachana.lock` + trust; libraries cached by content hash; dependencies loaded, cycles and depth 4 checked); the
  `FragmentResolver` hook keeps its signature, served through an adapter (`LibrarySet.fragments`). `Library.load` checks the
  file-type rule, the manifest, `MANIFEST.sha384`, `exports` staleness, the uses inside a library and the scope of a public
  library's typed fragments (`RCH-2117`). `LibraryLock` reads and writes the lock. `Rlib`: the deterministic `.rlib`
  (sorted, stored, fixed time) and `Sha384` in plain Java, so the JVM and the JavaScript build compute one identity.
  `Grammar.withFragments`, `Grammar.withLibraries`, `Rachana.libraries`. Codes `RCH-2110`..`2113`.
- **Tools** (`rachana-tools`): `LibraryTool` (`build`: exports and hashes; `sign`; `pack`; `verify`; `lockEntry`) and `Ed25519`
  (the JDK's Ed25519; a `SignatureVerifier` for JVM hosts; keys are plain `ed25519:<hex>`). `LibraryToolTest`.
- **Conformance**: the new kind `lib` (FORMAT 5.13, schema); `files` now read by the runtimes for `sutra` and `problem` cases
  (fragment files, `rachana_libs/`, `rachana.lock`); `sutra/fragments-typed` (5), `problem/fragments-typed` (18),
  `lib/libraries` (7), all verified on the JVM; the JVM and both JavaScript bundles answer every case byte-identically at
  1.0, 1.1 and 1.2 (`build_js.py` now checks level 1.2; runtime 383,760 B, tools 404,588 B Brotli, within the budgets).
- API baseline: additions only. Not yet: `as:`, `emits`/`on:`, private state, `let`, outputs, `fn` parameters, column mapping
  `{from, columns}`, library functions (X41), the classpath and package resolvers, the `rachana lib` commands of the CLI, the
  generated `rachana.lang.json`/`sutra.schema.json` at level 1.2.

## v0.11.0 (2026-11-21): the JavaScript build, ViewModel contract 1.1 and the G1 gate

### ViewModel contract 1.1 and language level 1.1 (datasets; grid, tree, chart)

- **Contract 1.1**, appended to 1.0 (SUTRA_EXPANSION 6.3; VIEWMODEL "Contract 1.1"): `datasets` (`Dataset`, `DataField`,
  `ArrowRef`, `PagesRef`, `ServerRef`: inline `values` or `rows`, `name`, Arrow, REST pages, a server-side row model; raw values
  formatted by the painter, masked as `{"$masked": true}`), the panel kinds `grid` (`Grid`, `GridColumn`, `SortKey`, `GridPivot`),
  `tree` (`Tree`) and `chart` (`DataChart`, `Measure`), map `points` (`GeoPoint`, bubbles), and the patch op `rows` (reset, remove,
  update, add by key). Every addition is left out when unused; the **stamp is the smallest minor that describes the document**
  (`1.1` only with datasets, a dataset kind or map points), so every existing ViewModel keeps its bytes (the level-1.0 replay is
  identical; Drishti stays at level 1.0). `spec/1/released/viewmodel-1.0.schema.json` written (`freeze.py --contract 1.0`, the contract v0.9.0
  released): CG4 now checks the current schema contains it. `spec/1/viewmodel.schema.json` gains 13 definitions (`x-since: "1.1"`),
  structurally equal to the records' generated schema (`compare_viewmodel_schema.py`, which now ignores `x-since`).
- **Language level 1.1** (SPEC 2.3, 7.2 rule 6): the kinds `grid`, `tree` and `chart` put their rows into the view's datasets
  (`DatasetBinder`; options `dataset`, `rowKey`, `group`, `sort`, `limit`, `search`, `empty`; `node`, `parent`, `path`, `label`,
  `mode`, `open`; `x`, `y`, `by`, `agg`, `stack`, `type`, `horizontal`, `xLabel`, `yLabel`, `bins`, `top`, `fmt`). Levels are real
  in the grammar: `Grammar.atLevel`, `Rachana.language(level)`; every instance starts at 1.0, where the new kinds are not offered
  (`RCH-2021`'s list unchanged) and using one is `RCH-2008`. A conformance case runs at the level of its `since`.
- Conformance: `sutra/binder/datasets` (6 cases, since 1.1, verified on the JVM), `problem/levels` (1 case, since 1.0; a known
  difference from the reference, which has no level 1.1). the customer corpus (478 Sutras) byte-identical on Core; JVM and the JavaScript build agree on all 2,130 cases at levels 1.0 and
  1.1; 0 verified failing; `levels.py check`: level 1.0 identical. API baseline: additions only (view records, `Grammar` levels,
  `PanelKind.since/at`, `BindContext.datasets`, `Rachana.language`). `DatasetPanelsTest`. The generated `rachana.lang.json` and
  `sutra.schema.json` still describe level 1.0 (generated from the reference's tables); SPEC lists the 1.1 names.
### G1 re-check, R1: the runtime bundle

- **R1, the runtime bundle built at `52e112f` was broken** (named formats not applied, `round`, `shift_date`, `b.ni is not a
  function`; Node stopped on a null read). Root cause in TeaVM 0.16.0, not in the merge: the precise dependency analysis merges
  type-set domains (`Transition.mergeDomains`) in an order that varies between runs, and some orders lose types: no `Boolean`
  reached `Plain.asBoolean`, so `booleanValue()` stayed a virtual call with no implementation emitted. The merge only changed the
  order. Fix: a second patched TeaVM file, `PreciseDependencyAnalyzer` (`java/rachana-js/src/build/java`, Apache-2.0, marked
  "Drishti Rachana patch"): no domain merging. The bundles are now the same on every build (three builds, two heap sizes).
- **The build checks both bundles**: `tools/js/build_js.py` runs the conformance suite in Node on the runtime and tools bundles
  at levels 1.0 and 1.1 after every build (`--check-only` runs the check alone) and fails on any error or verified failing case,
  or when the runtime answers a case differently from the tools bundle (the runtime leaves out only the 43 `edit` cases). The Node
  runner counts a case that throws as that case's error instead of stopping.
- Results: both bundles 0 errors, 0 verified failing at 1.0 and 1.1; the runtime's 1,954 (1.0) and 2,006 (1.1) answers and the
  tools bundle's 1,997 and 2,049 are byte-identical to the JVM's. `./mvnw clean verify` (JDK 21): 912 tests, 0 failures, 0 skipped.
- Bundle SHA-256 (P1-3): `rachana.runtime.js` `45bbfa4a6e99b34627bd86f75d1bcaaf744e4f583630e6de6ddd2fb47388269e` (1,412,427
  bytes; Brotli 366,858), `rachana.tools.js` `e25dfc97e2b1dbbcc8d756097691fb25ad40b402420078fed0fc33f55c4cfb9e` (1,511,248
  bytes; Brotli 387,940).

### G1 gate fixes

Findings of the i7b gate 1 review (Drishti `docs/plans/I7B_GATE_1_REVIEW.md`).

- **F4, the block library per instance (A6)**: `parse.BlockLibrary` holds what `SutraBlocks` kept in static fields (the library,
  its problems, warnings and clashes, the UI kit folder, the modules' contributed sections). A host makes one per instance and hands
  it to its profile (`Entities.addTo(profile, kinds, blocks)`, or `Profile.Builder.fragments`); two instances never share blocks.
  `SutraBlocks` keeps the stateless half (`read`, `expand(..., resolver)`, `sizes(..., resolver)`, `packOf`, `flow`, `isUse`).
  Core alone resolves no block (`FragmentResolver.none()`, was the static folder library). API: removed the static `SutraBlocks.load`,
  `kit`, `fromKit`, `library`, `blocks`, `find`, `problems`, `warnings`, `clashes`, `contribute`, `withdraw`, `contributedUses`,
  `expand(3 args)`, `sizes(1 arg)`, `SutraBlocks.Contribution` (now `BlockLibrary.Contribution`), `FragmentResolver.folders()`;
  changed `BlockUses.of(text)` to `of(text, resolver)`; added `BlockLibrary`, `FragmentResolver.none()`, `Entities.addTo(…, blocks)`,
  `Entities.match(kinds, sections)` (DRISHTI_MIGRATION_TODO 8.3i).
- **F1, the customer corpus on Core (CG3)**: `CustomerCorpusTest` compiles the 478 Sutras of `compat/customers/drishti/2.9.11.i7a`
  on Core (`CoreRuntime`: one instance with the conformance suite's `drishti` profile, `conformance.Cases.profile`, and its own
  `BlockLibrary`), by default; every document is byte-identical to the recorded `compiled/`, and the test checks that all 478 were
  compared. A runtime that cannot run fails the test (it was skipped). The reference bridge runs only with
  `-Drachana.compat.runtime=bridge`. The Drishti profile's formats are not loaded: no format enters these documents. Not yet: the
  corpus's recorded ViewModels (`binds/`, F13), before G4.
- **F2, one regex budget per call**: `SafeRegex.replaceAll` draws every match on one `maxSteps` budget (it was one budget per match,
  so a pattern that rescans the value for each match cost the value's length times the budget: 0.6 to 1.7 s for one call on 8 KB).
  The review's three 8 KB cases now stop at `RCH-4011` within one budget (`SafeRegexBudgetTest`). `find` and `extract` were already
  one scan. A `regex_replace` over many matches can now reach the budget where it did not.
- **F3, a form field's `pattern` on `SafeRegex`** (one regex engine, DECISIONS G1-1): matched against the whole value by the new
  `SafeRegex.matches` (linear time; a value over the input cap is refused, never cut); look-around, back-references and the rest
  of what `SafeRegex` does not offer are refused at lint (`RCH-2058`, SafeRegex's words in brackets). No shipped Drishti Sutra
  (repository and the i7a corpus) has a form pattern. `internal.RegexText` deleted (Core and the JavaScript build).
- **F9**: `el/determinism#d-m01-a` expects Core's `exp(1)` (`2.7182818284590455`); its known-differences reason corrected.
  Level 1.0 re-recorded for this case and `problem/forms#pattern-not-a-regex` only (DECISIONS G1-2, for the owner); conformance
  `--jvm`: 2,123 cases, pass 1,938, fail 58 (all unverified, F8), 0 verified failing.

### The JavaScript build

Step A15. Not tagged: the tag waits for the owner (G1 gate).

- `java/rachana-js` (Maven profile `js`) builds `rachana.runtime.js` and `rachana.tools.js` with TeaVM 0.16.0 through its tool
  API; `tools/js/build_js.py` copies them into `js/packages/rachana/dist/` and the Python package and prints their sizes and
  SHA-256. **Rule (P1-3): the release notes of every release record both bundles' SHA-256.** At `28d01bf`: runtime
  `d0ced04fc29dd911c30aa11c8b8fd75b9952d8f4081e3e28cd01a52c5139d1bb` (361 KB Brotli), tools
  `b012b4111a5efb11a6c9a14eb6b6cd965a347e1bd1382da27524532d4a23c0cd` (381 KB Brotli); two clean builds give the same bytes.
- The view package reads no record by reflection: generated writers (`ViewRecords`) and the schema text generated at build time
  (`ViewModelSchemaText`), checked fresh by a test; ViewModel bytes unchanged (conformance, and 331 corpus Sutras built both ways).
- Runtimes agree: every conformance case byte-identical to the JVM in Node, Chromium, Firefox, WebKit and Python on V8
  (`--node`, `runners/run_browsers.py`, `python -m drishti_rachana.engine`); `conformance.Cases` is the one harness, no I/O.
- Python on V8 (W-1): `drishti_rachana.engine.Rachana` compiles and lints, evaluates, formats, builds views and edits (extra `v8`).
- Core: `exp`, `ln`, `log10` and the trigonometric functions through `CoreMath` (six fdlibm functions added); `NumberText.toDouble`,
  `CoreDecimal.round`, `internal.Bits`, `internal.RegexText`; the guard refuses `Math::`, `doubleValue()` and `BigDecimal.round`
  elsewhere. Three JVM results move by one unit in the last place (level 1.0 re-recorded, D15-4).
- One TeaVM file patched (`StatementRenderer`, Apache-2.0): its printer dropped the parentheses of `a * (b / c)` (D15-2).
- Re-measure (P1_RESULTS section 11): T1, T5 to T10 pass; T2 and T4 miss, for the owner (D15-5, D15-6).

## v0.10.0 (2026-11-21): the untangling

Steps A1 to A8b, A13, A14 and A7b are in (the binder moved and behind hooks, its API, Core's canonical writer, form expressions in
Rachana-EL). Not tagged: the tag waits for the owner (G1 gate). Proof: `./mvnw clean verify` green on JDK 21; conformance
`--jvm` 2,095 of 2,123 identical to the reference, the 28 differences each listed in `tools/split/known-differences.txt`, 0 verified
failing. API baseline changes since v0.9.0 to note: the binder packages (`bind`, `view`, `control`, `wire`), `Rachana.binder`/`views`,
`ViewBuilder`, `el.FormRules`, `RCH-5001`/`RCH-5002`, and the planned removals listed in the entries below.

- **A7b, form expressions in Rachana-EL**: `and`, `or` and `not` are Rachana-EL operators (DECISIONS PY1; `not` reads below the
  comparisons, as in Python); a form's rules, hints and `=` defaults are read by Rachana-EL's parser and evaluated by the `form`
  profile (`el/FormRules`): exact decimals with their written scale, `/` to 16 digits, `+` joining a text, empty values, three-valued
  `and`/`or`, a rule that cannot be told holds. `FormExpr` keeps its API; its own lexer and parser are gone. Every answer equals the old
  evaluator's (`FormRulesTest`, the old one kept in the tests); changed: the words of a syntax error, Rachana-EL's spellings read too,
  `_` separators taken out by the profile (DECISIONS A7b-1 to A7b-4, for review). Conformance: `el/python-words` (6 cases, unverified,
  known: the reference refuses the words); 2,095 of 2,123 identical to the reference, 28 known, 0 verified failing. The reference's
  `sutra` program is now `RefBridgeSutra.java` (the engine class path changes other kinds' answers). API: `el.FormRules`.
- **A14, the binder's API, writer and cases**: `rachana.binder(hooks, pivots)` and `rachana.views(hooks, pivots, tables, dates,
  budget)`: `ViewBuilder`, Core's view pipeline (controls, the date, panel sources through the `TableProvider`, map lookups, `when`,
  wired panels, strip, title, keys, controls and selects, screen grid, phone; a host's reference, mnemonic, provenance and timings left
  unset). `ViewJson` is the canonical writer: the conformance `sutra` kind (`JvmRuntime`, and the reference bridge over the host's
  engine) writes whole ViewModels with it, and the 5 views of the new `sutra/binder/panels` suite (harvested from the host's panel
  tests: chart kinds, drill levels and a timeline, tabs with a tree table, metrics, strip/keys/controls/selects) are byte-identical to
  the reference's Jackson output (verified). The driver checks `at` (JSON Pointers into the ViewModel). `patch` cases wait for `data:`
  (B1). The level replay (CG2) compares the cases a level recorded: cases added later and `unsupported` ones now answered are reported,
  not differences (3 sutra examples are now answered). Conformance: 2,112 of 2,117 identical to the reference, 5 known (the 2 of A1;
  3 Core-only Sutra examples the reference cannot read); 0 verified failing (one unverified example, `metric-over-a-list`, fails: its
  `sum($.sales, 'revenue')` is not a Core aggregate; the example `control-value-reaches-a-template` named a control kind `select` that
  does not exist, now `dropdown`). API baseline: `Rachana.binder`, `Rachana.views`, `ViewBuilder` and its `Request`.
- **A9, the aggregate kernel in Core**: the state and maths of the aggregates SPEC 5.7 and 5.8 list, and the list functions over
  them, moved here with their history from Drishti `c6bc1d1bc` (git filter-repo, `tools/split/a9-paths.txt`): `Builtins`, `Moments`,
  `MoreAggregates`, `Holistics`, `Comoments`, `Final`, `ListAggregates`, `ListAnalytics`, `ListGrouping`, now
  `com.ash.rachana.agg` (exported). Core's own contract `Aggregate` (start, add a columnar `Batch`, merge, finish, encode and decode
  with the engine's bytes, SQL state) and `Kind`; `Catalogue.of(name, …)` gives an aggregate by name; `CoreAggregates` is the first
  provider of every function table, so `sum`, `count`, `avg`, `min`, `max`, `median`, `percentile`, `distinct`, `wavg`, the variance
  family, the 32 statistics, the list analytics and `group(...)`'s grouping are computed by Core (the catalogue says `R`, source
  `Rachana Core`). What only a host computes stays with it and is refused in Core alone with the reason: the `_once` family,
  `measure`, `cell`, a model's measure-level functions (decision N7); the sketches and the `AggFunction` shells stay in the engine.
  The moved maths uses Core's own classes (A5's guard covers `agg/`; `CoreMath.hypot` added, equal to `StrictMath.hypot`). Results:
  16 aggregate cases that the reference refused now pass, seven determinism probes expect Core's values, and refusals that list
  the known functions now name the aggregates (56 known differences in all, 0 verified failing; 1,924 cases pass); level 1.0 written
  again (37 results). 855 unit tests (the moved `RachanaAggregatesTest`, `RachanaStatisticsTest`, `RachanaListAnalyticsTest`).
- **A13, the binder behind hooks**: the binder and its neighbours (Drishti's engine `bind`, `view`, `control`, `wire`, and the live
  `Frame` and `Patch` records) moved here with their history (84 commits, `tools/split/a13-paths.txt`) into
  `com.ash.rachana.{bind,view,control,wire}`, and every host type behind a hook: `LinkResolver` (which kind an id names, a kind's
  code), `TableProvider` (list, search, fetch, freshness; `computedId`), `Masker` (the viewer's masks and kinds), `PanelBinder` per
  profile kind (`links`, `provenance`), `PivotEngine` (a pivot panel's cells), `BusinessDays` (a request's business date and calendar),
  a label function; `BindHooks` holds them (`BindHooks.core()`: Core alone). Core's own `EntityRef`, `Entity` (with `Origin`),
  `EntityHit`, `AsOf`, `SourceFailures`, `Layout`, `PivotLimits`, `RelativeDates`. `BindContext` carries a `host` object Core never
  reads. The ViewModel records carry Core's `@Include` (Jackson's rules); `ViewJson` is Core's canonical writer (records in field order,
  the rules applied) and `MaskCount` and `ViewModelSchema` read it; the map cache is Core's `TtlCache` (no Caffeine). New codes
  `RCH-5001` (bad request) and `RCH-5002` (forbidden). Changed answers (for review): a panel too deep to evaluate names "the expression
  depth limit"; the ViewModel schema's `$id` is this specification's address. Host words in Core: still 2 files. Conformance
  byte-identical (2,110 of 2,112 identical to the reference, the 2 known; 0 verified failing); `BindHooksTest` and the 5 moved unit
  tests; the API baseline gains the four packages (104 types).
- **A5, the same bytes on every runtime**: Core no longer asks the JDK for anything whose answer differs between runtimes. Its
  own classes do it: `CoreMath` (an fdlibm port of `exp`, `log`, `log10`, `log1p`, `expm1`, `pow`, `tanh`, `atan2`, `sin`, `rint`
  and the saturating casts; equal to `StrictMath` on Java 21 bit for bit), `NumberText` (Core's one door between doubles and text;
  on the JVM Java's shortest round-trip printing and correctly rounded reading; the JavaScript build swaps in a same-named class),
  `CoreText` (upper and lower case, with the expansions and the final sigma, white space, strip and reversal by code point, from
  Java 21's Unicode 15.0 tables carried in Core), `CoreDecimal.sqrt` (whole-number arithmetic; equal to `BigDecimal.sqrt`), and
  `DateText` (English month and day names, and the `date` format's pattern printed by Core). `text(v, code)` prints Excel number
  codes with Core's own printer (`java.text` left Core; equal to the `DecimalFormat` it replaces for every code in its grammar,
  and two quoted texts side by side no longer print a stray quote). `String.format` is gone. The regular-expression budget is a
  count of steps, not a clock: `SafeRegex.Limits(maxPattern, maxProgram, maxInput, maxSteps)` (default 20 million steps) replaces
  `timeoutNanos` (planned; API baseline rewritten). The `date` format's `MMM` no longer follows the JVM's default locale, and
  `WorldRegions` no longer upper-cases with it. A guard (`CoreDeterminismGuardTest`) refuses these JDK calls anywhere else in Core,
  the binder moved by A13 included (its case mapping, strip, `rint` and number reading now go through Core's classes).
  Results: 17 conformance results move in the last place, all in the P2-1 list (`norm_s_dist`, `f_dist`, `binom_dist`,
  `hypgeom_dist`, `gamma_dist`, `npv`, `irr`, `mirr`, `rate`, `log`, `fisher`), listed in `tools/split/known-differences.txt`;
  the two determinism probes among them now expect Core's value; every other result is byte-identical (2,093 of 2,112 identical to
  the reference, the 19 known; 0 verified failing). Language level 1.0 (`compat/levels/1.0`, not yet released) was written again
  by `levels.py write 1.0`: 18 results (the 17, and `fisher_inv`, which had been recorded on Java 25) and the `fisher(0.5)` example.
  New tests: `CoreTextTest`, `CoreDecimalTest`, `DateTextTest`, `NumberCodeTest`, `SafeRegexBudgetTest`, the guard; 822 unit tests.
- **A7, the model and parser split along the Core/profile line**: each instance has a `Grammar` (`rachana.grammar()`, its parser
  `rachana.parser()`, its schema `rachana.schema(formats)`): Core's keys and kinds plus what its profiles add. A profile adds top-level
  keys (`SutraKey`: reader, the panels it brings, schema fragment), brings the profile panel kinds (`links`, `provenance`), the `kind`
  source form, `form.save` and a fragment resolver (`FragmentResolver`; Core's default is the folder library). What a profile key
  reads is kept in `Sutra.extensions()`; `match()` and `domain()` read it. `Entities.addTo(profile)` holds the entity additions
  (`match`, `domain`, `links`, `provenance`, kind sources, `form.save`) until the host's profile module takes them; Core alone reads a
  Sutra with none of them and refuses them (`RCH-2011`, `RCH-2021`, `RCH-2010`, `RCH-2058`); `setMatch` is refused without the `match`
  key. `new SutraParser()`, `new OpApplier()` and the editors' no-argument constructors are Core's grammar alone; the others take the
  instance's parser. Core `SutraStore` (`name@version`, newest wins, listeners). Removed (planned):
  `RachanaSchema.build(formats, kinds)`, now `build(formats, grammar)`; the schema's `$id` is `urn:rachana:schema:sutra:1`. Host words
  left in Core: 2 files (the limit messages' setting names, which change conformance words, and `signatures.yaml`). Conformance
  byte-identical to before (2,110 of 2,112 identical to the reference, the 2 known; 0 verified failing); `GrammarTest` new.
- **A6, registries per instance**: `Rachana.with(options, profiles…)` builds one instance (its own `FunctionTable`, compiler and
  cache, format table); `Profile.core()` and `Profile.named(name).functions(…).formats(…).calendars(…).build()`. Nothing is
  process-wide: `Functions.register` and `Calendars.use` are gone, providers are no longer found by `ServiceLoader` (a profile hands
  them over), and two instances in one JVM never see each other's functions, calendars or patterns. A profile cannot redefine a Core
  name (refused when the instance is built), nor can two providers give one name. `RachanaOptions.regexLimits` now reaches the text
  functions, per instance. A host whose functions come and go rebuilds and swaps its instance. The static `Functions` methods read
  the Core table (`FunctionTable.core()`). Conformance results byte-identical to before A6 (2,110 of 2,112 identical to the reference,
  the 2 known; 0 verified failing); 840 unit tests (`RachanaInstanceTest` new).
- **A1, errors**: `RachanaException` (code, words; message is the code and the words) and `RachanaCode` replace the host's exception
  and error codes; every code is `RCH-` with the number it always had (problems, editing refusals, evaluation and syntax errors,
  checked-mode refusals). A host maps them to its own by the number at its edge. The accepted options in an editing refusal are
  named sorted (they were in an unordered set's order, which changed between runs).
- **A8a, A8b, Core's own YAML and JSON**: `PositionalYamlReader` is now Core's own reader (no library, no regular expressions):
  the same tree, types, positions, issues and error texts as before (`tools/yaml-diff`: 168,923 inputs, every Drishti and
  Rachana YAML file, case texts, edge inputs and mutations; `PositionalYamlReaderTest` on 1,113 recorded vectors). New: an
  anchor or alias is an issue (`RCH-2033` in a Sutra). `Json` reads and writes JSON. Jackson and SnakeYAML leave the build.
  API: `PositionalYamlReader.read` throws the unchecked `YamlSyntaxException` (message, line, column) instead of an
  `IOException`; `Ops.parse` takes a plain JSON value and `Ops.toJson` returns maps and lists; `Signatures.parse` takes a
  plain value; `PNode.plain()` added. Conformance results byte-identical before and after.
- **A2, the value interface**: `com.ash.rachana.api.Node` (zero dependencies): kind, fields, elements, scalar value, masked and
  missing, path navigation, and Rachana's own nodes behind `Node.of`; the evaluator, the formats and the checker read only `Node`, so a
  host passes its own document type by implementing it (`ForeignNodeTest`), never by copying. The host's `DataNode`, `NodeType`,
  `PathWalker` left `hostcompat`; `ListValues` and `IdText` are now `com.ash.rachana.format`'s own.
- **A3, small dependencies out**: `SafeRegex` (the linear-time regular expressions) and `BusinessCalendar` are Core's own
  (`com.ash.rachana.el`); the date functions read business days through the `Calendars` hook (Core's calendars unless a host
  installs its resolver); the compiler's cache is Core's own LRU, so **Caffeine is gone**; the regular-expression caps are read
  from `rachana.regex.*` (they were the host's setting names). The temporary `hostcompat` package is gone.
- **A4, settings**: `RachanaOptions`, every setting of Core in one value (expression cache size and limits, the site and profile
  format files, the regular-expression caps), with `compiler()` and `formats()`; Core reads no system property any more (the
  regular-expression caps are plain defaults until A6 hands the options to each instance). Spring never moved.
- `tools/split/compare_results.py` and `known-differences.txt`: the proof of each split step against the reference bridge.

## v0.9.0 (2026-11-19): the compiler moves here

- **The compiler, with its history** (split step A10): Drishti's `compute/drishti-rachana` (and its earlier paths) at Drishti
  `main` `c6bc1d1bc`, less the profile part, extracted with `git filter-repo` from a fresh clone under a reviewed path allow-list
  (`tools/split/a10-paths.txt`), 138 commits kept; packages renamed `com.ash.drishti.rachana` → `com.ash.rachana`; into
  `java/rachana-core`. Behaviour is unchanged.
- `rachana-core` builds and tests alone (JDK 21, a named module): 783 unit tests. The host types it still names are held in
  `com.ash.rachana.hostcompat`, and Jackson and Caffeine are allowed, each until a named step ([java/SPLIT_EXEMPTIONS.md](java/SPLIT_EXEMPTIONS.md)).
- `rachana-conformance`: `JvmRuntime`, the conformance runtime over this code; `rachana_conformance.py --jvm` runs it. Over all
  2,112 cases its results are **byte-identical** to the reference bridge over Drishti's jars of the same commit: 1,908 pass,
  74 unverified failing (recorded), 130 unsupported, **0 verified failing**.
- The format table and world regions come from the pinned Drishti Elements `2.4.0` data (`js/vendor/drishti-elements/2.4.0/data`).
- Guards: split exemptions and the host types read from `SPLIT_EXEMPTIONS.md` and fail when stale; host words in `rachana-core`
  counted and may only go down.
- Changes to the moved code, all without a change of behaviour: `Formats` no longer implements the host's Spring reload hook;
  `Signatures` reads its own module's resource first.

## Unreleased (M1, the compatibility regime)

- The customer-corpus export (`tools/regime/corpus_export.py`, `CorpusCompile.java`) and `compat/customers/drishti/2.9.11.i7a/`.
- `java/rachana-compat`: `CustomerCorpusTest` (CG3), `RecordedHost`, `FrozenCasesTest` (CG1), `ContractAppendOnlyTest` (CG4),
  `ReservedNamesTest` (CG6).
- **CG5 and CG2 skeletons**: `java/api-surface.txt` (1,661 lines: the public and protected API of `rachana-api` and `rachana-core`)
  with `ApiSurfaceTest`; `compat/levels/1.0/` (results of the JVM runtime at level 1.0 and the four generated definition files),
  `tools/regime/levels.py` (`write` once, `check` replays) and `LevelReplayTest`.
- `conformance/1/FROZEN.sha384` (pre-release), `compat/reserved-names.yaml`, `compat/promotions.yaml`, `compat/level-switches.yaml`.
- `tools/gate/drishti_gate.py` (CG8, dry run), `compat/forward-vectors/FILES.md` (CG7).
