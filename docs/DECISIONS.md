<!-- Copyright (c) 2026 Ashutosh Sinha. Proprietary. All rights reserved. -->
# Decisions

Each decision keeps its recommendation text and has a Status column. D16 is accepted only as a last resort (see its row). The design these decisions shaped is [DESIGN.md](DESIGN.md); what is still planned is [ROADMAP.md](ROADMAP.md).

## Already fixed (not asked again)

Name `drishti-rachana`; standalone repository; licence as Drishti Elements; Drishti is the first customer and
vendors pinned releases; usable from Java, JavaScript and Python; one compiler source (Java reference, JavaScript
build, WebAssembly build for Python), other languages proven by the conformance suite; open specification,
proprietary runtime; nothing Drishti-specific here (Drishti keeps a profile); generative UI built on Rachana, with a
tool pack for sajhamcpserver (the only MCP host); UI speed budgets; desktop hosts: Electron first, then Swing through JCEF, then .NET through WebView2.

## The Core and profile line

| # | Question | Recommendation | Why | If not | Status |
|---|---|---|---|---|---|
| **D1** | Where exactly is the line between Sutra Core and the Drishti profile? | As the table in [CORE_AND_PROFILES](DESIGN.md#sutra-core-and-profiles-design) section 1: Core is everything about a screen and rows (20 panel kinds, controls, events, forms without save, fragments, the expression language, list aggregates, binding). The profile is `match`/`domain`, kinds `links` and `provenance`, sources by entity kind, write-back, model functions, registry over packs, governance, context help, masking policy. | Most of what an author types is Core; what is left is "where the data comes from and who may see it", which is a host's business. | A thinner Core (expressions and parsing only) would leave the standalone product without a binder: not a product. | Accepted 2026-11-17 (owner) |
| **D2** | Do forms belong in Core? | **Yes for entry** (fields, rules, hints, steps, drafts); **no for saving** (`form.save` is the profile's). Core raises a `submit` action; the host saves. | A dashboard product needs parameter entry; write-back needs connectors and approvals. | Keeping all of forms in the profile is simpler at the split and can be relaxed later; choose this if the split must be smaller. | Accepted 2026-11-17 (owner) |
| **D3** | Is the list-aggregate kernel moved from Drishti's pivot engine into Core, with the engine then using Core's? | **Yes** ([CORE_AND_PROFILES](DESIGN.md#sutra-core-and-profiles-design) 2.4). | Core cannot work without `sum`; a second implementation would break the one-way-per-job rule. | Core without aggregates until later (the Overview example would not run), or two implementations. | Accepted 2026-11-17 (owner) |
| **D4** | Does the binder (Sutra + data → ViewModel) move into Core? | **Yes, in two steps after Drishti is already on a pin** (split steps 13, 14). | It is what makes Rachana a product; doing it after the pin keeps the i8 split safe. | Rachana ships only a compiler in R1 and binds in JavaScript by hand: dual machinery. | Accepted 2026-11-17 (owner) |
| **D5** | Catalogue-driven inference (screens with no Sutra)? | **Stays in Drishti** for the split. A small shape-only inference for Core ("three lines give a screen") is a later Core feature, designed separately. | Inference today reads Drishti's catalogue and semantic hints. | Moving it now enlarges the riskiest part of i8. | Accepted 2026-11-17 (owner) |

## Language and specification

| # | Question | Recommendation | Why | Status |
|---|---|---|---|---|
| **D6** | Where does the ViewModel specification live: here or in Drishti Elements? | **Here** (`spec/1/viewmodel.schema.json`), with the Data Protocol and the Sutra schema. The **painter** stays in Drishti Elements (`drishti-views`, `<elements-view>`), vendored here at a pin. | The ViewModel is the compiler's output contract; one home per fact. The painter is Elements code and names no host. | Accepted 2026-11-17 (owner) |
| **D7** | The element that takes a Sutra: name? | **`<rachana-view sutra="…" data="…">`** in this repository's npm package; it compiles, binds and hands the ViewModel to `<elements-view>`. (The earlier drafts used `<elements-view sutra=…>` and `<elements-sutra>`.) | Keeps Elements free of any Rachana dependency; the name says which product it is. | Accepted 2026-11-17 (owner) |
| **D8** | Problem codes: keep `DRS-` or introduce `RCH-`? | **`RCH-` in Core with the same numbers** (`RCH-2023` is today's `DRS-2023`); Drishti's API keeps answering `DRS-` through a one-to-one mapping in its profile. | Nothing Drishti-specific here; no renumbering to learn; Drishti's documents and tests do not change. | Accepted 2026-11-17 (owner) |
| **D9** | Source of truth for the language: the compiler's tables (today) or a hand-written definition file that generates the tables? | **The compiler's tables stay the truth for `rachana: 1`**; `rachana.lang.json` and the schema are generated from them and checked for staleness. | It works today and cannot drift; flipping the direction is a rewrite with no user benefit. | Accepted 2026-11-17 (owner) |
| **D10** | The `data:` keys (this settles the Overview's guesses). | Top-level **`data:`**; an entry is `{ table: <name> }` (host-resolved, Data Protocol), `{ url }`, `{ rows }` or `{ record }`; each entry is a field of `$` (`rows: $.sales`, `sum($.sales, 'revenue')`). The earlier design's `ref`, `pages`, `server`, `topic`, `fn` source types are **not** added: a host exposes any of them as a named table. | `table` already exists as a source key; one way to name data; the same Sutra runs in every mode. | Accepted 2026-11-17 (owner) |
| **D11** | May `version:` be left out (default 1) and `title:` be plain text? | **Yes to both**, as additions in language 1.1. | The first Sutra a newcomer or a model writes gets two lines shorter; existing files unaffected. | Accepted 2026-11-17 (owner) |
| **D12** | `match` is required today. In Core? | **Not a Core key.** A Core Sutra has no `match`; in Drishti the profile keeps it required for entity screens. | Matching a record family is a platform concept. | Accepted 2026-11-17 (owner) |
| **D13** | Major-version policy. | A runtime of major N reads N and N−1; N−1 supported at least 24 months; every breaking change ships with a `rachana migrate` rule; stricter checks are warnings for two minors first. | Customers' Sutras are assets; a break without a tool is not acceptable. | Accepted 2026-11-17 (owner) |
| **D14** | Is the specification's own licence different from the runtime's? | **Yes: publish `spec/` and `conformance/` under a permissive specification licence** (free to read, implement and test against; the name "Rachana" only for implementations that pass). The runtime stays proprietary. Needs a short LICENSE-SPEC text; draft for the owner to approve. | "Open specification" has to be stated in the licence, or the public LICENSE (no rights granted) contradicts it. | Accepted 2026-11-17 (owner) |

## Runtimes and packaging

| # | Question | Recommendation | Why | Status |
|---|---|---|---|---|
| **D15** | JavaScript build tool. | **TeaVM**, after the one-week proof (T1 to T10); J2CL as the named alternative. | Pure Java build tool, works with only a JDK on Windows and Linux. | Accepted 2026-11-17 (owner) |
| **D16** | If the proof fails on speed or size by a wide margin? | Fallback order in COMPILER_AND_RUNTIMES 4.3; the last one (a hand-written Core-only JavaScript evaluator held by conformance) **needs your explicit acceptance at that time**. | It would be two evaluators. | Accepted 2026-11-17 (owner) as a last resort; needs the owner's explicit OK if ever used |
| **D17** | WebAssembly route for Python. | **TeaVM's WebAssembly target run under `wasmtime`**, after its proof; fallback GraalVM native-image shared library; a JVM sidecar only as a development convenience. | One toolchain; no native code of ours to ship. | Accepted 2026-11-17 (owner); **superseded by W-1** (below) |
| **D18** | Jackson and Spring leave Core; Core gets its own small YAML reader. | **Yes.** | Required for the JavaScript and WebAssembly builds; smaller jar. Risk covered by a differential test over every shipped Sutra. | Accepted 2026-11-17 (owner) |
| **D19** | Java package and coordinates. | Package **`com.ash.rachana`**, Maven group `com.ash.rachana`, artifacts `rachana-api`, `rachana-core`, `rachana-tools`, `rachana-data`; npm **`drishti-rachana`**; PyPI **`drishti-rachana`** (import `drishti_rachana`). | The product name stays `drishti-rachana`; code names carry no product prefix that the "names no host" guard would trip on. | Accepted 2026-11-17 (owner) |
| **D20** | Primary CLI. | **npm (Node) is primary** (needs no JVM, serves `dev` and `build`); PyPI and jar launchers expose the same commands where they apply; identical `lint --format json` output is a conformance case. | Analysts and web developers have Node or Python, rarely a JVM. | Accepted 2026-11-17 (owner) |
| **D21** | Standalone server stack. | **Plain Java 21, no application framework**; configuration in one YAML file. Detailed design later (R2). | Small, starts fast, third consumer of the public API. | Accepted 2026-11-17 (owner) |
| **D22** | Where do Drishti's Rachana jars come from? | A release location the owner controls (GitHub Releases of this repository at first); Drishti holds the jars as it holds its other pinned jars, version in one Maven property. Public registries (Maven Central, npm, PyPI) wait for R3. | No dependency on a public registry before the licensing tiers are decided. | Accepted 2026-11-17 (owner) |

## Data, desktop and AI

| # | Question | Recommendation | Why | Status |
|---|---|---|---|---|
| **D23** | Live transport. | **Reuse the events wire protocol v1** designed for Drishti Elements (topics `rdp.<table>`, message kind `rows`), plus a minimal read-only SSE form for hosts with nothing. | One live mechanism across Elements, Views and Rachana. Depends on that design shipping; re-check then. | Accepted 2026-11-17 (owner) |
| **D24** | Filters sent to servers. | **Only as a JSON syntax tree** of a fixed push-down subset; never expression text. | Safe, checkable, translatable to SQL with bound parameters. | Accepted 2026-11-17 (owner) |
| **D25** | Desktop: where does compilation happen? | **In the web view (JavaScript build) for every host**; a Java host may optionally bind in process. | One path; .NET needs no compiler. | Accepted 2026-11-17 (owner) |
| **D26** | JavaFX WebView. | **Fallback only**, supported at "renders and updates", not at the performance budgets; JCEF is the Swing path. | Its engine trails current browsers. Confirm by a painter test run before promising anything. | Accepted 2026-11-17 (owner) |
| **D27** | Native (non-web) painters. | **Out of scope**; the ViewModel specification being open is the answer for anyone who needs one. | A second grid, pivot and chart stack per toolkit is dual machinery. | Accepted 2026-11-17 (owner) |
| **D28** | Where does Electron sit in the roadmap? | **First item of R2** (R2a), right after R1's JavaScript build, Views and Data Protocol exist; Swing/JCEF R2b; .NET/WebView2 R2c. | It needs all three R1 deliverables; putting it inside R1 would lengthen the first step of i8. | Accepted 2026-11-17 (owner) |
| **D29** | In AI mode, may a model-written Sutra use `url:` sources? | **No**: handles only (`data_to_table`). | Data access stays with the host's tools and permissions. | Accepted 2026-11-17 (owner) |
| **D30** | GenUI quality gate. | Ship the prompt kit only when first-try validity ≥ 85% and validity within three attempts ≥ 99% on the held-out tasks, for at least one current model; publish the figures with dates. | "It works" must be a number. Targets to confirm after the first run. | Accepted 2026-11-17 (owner) |
| **D31** | Generic MCP server runtime. | **Withdrawn 2026-11-28 (owner):** there is no generic Rachana MCP server; the kit's MCP tools ship only as a sajhamcpserver plugin (`tools/sajha/`). | One governed host (identity, roles, audit) instead of a second server to keep safe. | Superseded 2026-11-28 (owner) |
| **D32** | Until the WASM build exists (R2), how does the sajhamcpserver tool pack validate? | It **waits for R2**; the plugin of `tools/sajha/` runs on the Python SDK (V8) in the meantime. | Avoids a temporary JVM dependency in SAJHA. | Accepted 2026-11-17 (owner) |

## Sutra libraries (L-1 to L-20, open)

Status: **planning; open for the owner (2026-11-19).** Designs: [LIBRARIES](DESIGN.md#sutra-libraries-the-fragment-library-framework) (the framework) and
the library catalogue (seven libraries, 209 fragments, the backlog; `core` and `finance` are built, see [libraries](../README.md#rachana-fragment-libraries)). The plans in those documents assume
the recommendations.

| # | Question | Recommendation | Why | If not | Status |
|---|---|---|---|---|---|
| **L-1** | Is a "component" (SUTRA_EXPANSION C1) a second thing beside the fragment? | **No: one mechanism.** A fragment has a text form (today's `%{param}`, kept with identical output for all of major 1) and a typed form (C1). The header key stays `fragment:`; the sketch key `block:` of C1 is dropped; "component" is prose only. | One way per job; Drishti's 17 kit fragments and its pack fragments are already this mechanism. | Two file kinds, two resolvers and a migration that changes every Drishti fragment file. | Open |
| **L-2** | One library concept for functions (X42) and fragments? | **Yes.** One manifest (`<name>.rachana-lib.yaml`, X42's file extended), one `LibraryResolver`, one lock file; a library holds functions, fragments, formats, table contracts, samples, tests, messages. `FragmentResolver` keeps its signature and is served by an adapter. | `finance` and `geo` are both a function library (X28, X34) and a fragment library; two formats would split them. | A fragment-library format beside X42's: two pins to keep in step. | Open |
| **L-3** | How is a fragment referenced? | **`libraries: [finance@1]` once per Sutra, then `use: finance/var-tiles@1`** (library, fragment, fragment major). The long form `use: finance@1/var-tiles@1` is accepted in a Sutra with no `libraries:` key (model-written, playground) and `rachana fmt` rewrites it. Unqualified names keep today's meaning (the host's resolver). | It is the spelling Drishti's pack fragments already use (`<pack>/<name>@N`); the library's version line is written in one place. | Only the long form: every use repeats the library major; or only the header: a single-file Sutra cannot be self-contained. | Open |
| **L-4** | Versions and pins? | **Semver per library; a fragment major (`@N`) frozen in its output for the whole library major; exact version and SHA-384 per library in a committed `rachana.lock`; no "latest"; one version of a library per project.** | A host can take a library minor without re-testing every screen, and the gate can prove it (LG2). | Semver alone: "minor" changes move screens; or per-fragment versions alone: no unit to ship, sign or license. | Open |
| **L-5** | Parameter typing? | **Two classes.** Values (the EL type language of X8, including `fn` lambdas and `table`) are compiled in the user's context and read inside as `arg.<name>`; names (`format`, `tone`, `enum`, `columns`, `state`, `selection`, `message`, ...) are fixed at compile time and substituted as **checked tokens**. Free text substitution stays only in the text form. | Hygiene (a value cannot inject syntax), full checking at the use, and one context root shared with screen `args:`. | Text substitution everywhere, as today: no types, no slots, and a parameter can change a fragment's structure. | Open |
| **L-6** | May a library fragment read the screen around it? | **No: hermetic.** Only its parameters, its bound tables and its slots; no `$`, `ctl.`, `state.`, `sel.`, no table names. Defaults of value parameters are literals in a public library; a private library may write expression defaults (Drishti's kit does). | A public library must work on any host and must not reach data its author was not given. | Fragments that silently depend on a host's field names; a security review per library. | Open |
| **L-7** | How does a fragment state the data it needs? | **A table parameter is a contract**: named columns with types, key, live and window rules, `max-rows`; the user maps column names at the use (`{ from, columns }`), a rename in the plan with no copy. Shapes shared by several fragments are **published contract files** in the Data Protocol's schema form, with synthetic samples; `rachana lib check-host` checks a host against them. Computed columns are made by the user, upstream. | The host's data team gets an exact list of tables to serve; the compiler, Studio and the AI kit all rank fit from the same file. | Field-name parameters only (today's `x: TENOR`): no type check, no fit ranking, no agreement a data team can test. | Open |
| **L-8** | What can a fragment do outside itself? | **Emit declared events, nothing else.** No `host` or `go` effect with a literal name and no literal `requires:` in a Core-pure library; the screen's author wires events to host actions. A fragment writes the user's state only through a `state` or `selection` name the user hands over. A private library that declares a profile may name that profile's actions. | A library can never act for the reader or name a host; safety does not depend on trusting the library. | Action-name parameters or literals: a library that sends orders. | Open |
| **L-9** | May a user override the inside of a library fragment (C2 `override`)? | **No.** Variation is by parameters, slots, a variant fragment in the user's own library, or `eject` (one use written in place, proved identical). `extends`/`override` stays for Sutras. | Overriding by inner id makes a fragment's internals its interface and ends the frozen-output promise. | Overrides allowed: every library minor can break a customer's screen. | Open |
| **L-10** | Packaging, distribution and integrity? | **A deterministic `.rlib` archive (data files only: YAML, JSON, JSON Lines) as the one artefact; npm, PyPI and Maven packages as generated wrappers; the release location first, public registries at R3 (as D22). SHA-384 manifest and lock hash always; Ed25519 detached signature; the host's trust list and `require-signed`. No network at compile time.** | One thing to sign and hash; teams install through the tools and proxies they already audit. | Per-ecosystem packages built separately: three artefacts that can differ. | Open |
| **L-11** | Licensing per library? | **`core`, `geo`, `ops`, `ai` under a short permissive library licence, included with every distribution; `finance`, `genomics`, `economy` (and later domain libraries) under a proprietary domain-library licence, sold per library.** No technical lock. Needs two licence texts drafted for approval. | The general vocabulary drives adoption and is worthless without a runtime; the domain knowledge is the paid value. | All proprietary: every tutorial and customer variant sits on restricted text. All free: no domain product. | Open |
| **L-12** | Where do the first-party libraries live? | **Free ones in this repository under `libraries/`; proprietary domain libraries in a private companion repository until the licensing tiers of R3 are decided, with their catalogue, contracts and gallery published here.** | Publishing is the irreversible direction, so it is the one to delay; CI can check out a companion as it will for the customer corpus (Q6). | All here: domain content public before its licence exists. All private: no public examples to learn from. | Open |
| **L-13** | Library names? | **Keep `core`** (the owner's word), always written "the `core` library" to tell it from Sutra Core; first-party names reserved in `compat/reserved-libraries.yaml`; a private library carries its owner's prefix (`acme-risk`). | No private library can shadow a first-party one; the reference `core/key-figure@1` reads well. | Rename to `ui` or `kit` to avoid the two meanings of "core". | Open |
| **L-14** | Which libraries? | **Seven: `core`, `finance`, `genomics`, `economy`, `geo`, `ops`, `ai`.** One `finance` library with categories (not split into trading, risk, market data) until it passes about 80 fragments. A small eighth, `business` (sales and customer variants of `core`), after LB2 if wanted. | Each has a distinct audience and licence; `geo`, `ops` and `ai` keep `core` from becoming a catch-all. | Fewer, larger libraries (maps and operations inside `core`): one licence and one release cadence for unlike things. | Open |
| **L-15** | The catalogue's asks of the expansion (K1 to K8)? | **Accept K1, K2, K6, K7, K8 into SUTRA_EXPANSION and the Data Protocol now; K3 `tracks` and K5 `sequence` as Core kinds in V4's last batch; K4 `structure` (molecular 3D) as a separate life-science painter module of Drishti Elements with a vendored viewer, design and mock-ups first.** | Without them four `genomics` fragments cannot exist; K4 is the only one that brings a large third-party viewer. | `genomics` ships without tracks, sequence and structure: the three things the field expects to see. | Open |
| **L-16** | How do Drishti's existing fragments migrate? | **Steps 0 to 4 of LIBRARIES 21, none forced:** nothing changes at level 1.0; a manifest beside the kit and each pack makes them private libraries; `rachana migrate` types a fragment in place only with a per-use proof of identical output; a use moves to `core` or `finance` only when its ViewModel is byte-identical on the goldens; new screens use the first-party libraries first. The four kit fragments that draw profile kinds stay in Drishti's kit. | The compatibility regime's promise, applied to fragments; the equivalence test Drishti already has becomes the product's eject and migrate proof. | A flag-day move of 500 Sutras; or the kit never converging with the product's libraries. | Open |
| **L-17** | The definition of done and the gates LG1 to LG9? | **Yes, as merge and release blockers:** every exported fragment has its document, two examples with goldens, edge tests (empty, masked, denied, over `max-rows`, long text), an accessibility summary, a budget, samples, and the level it needs; frozen goldens are added, never edited. | "A fragment is a product": the libraries are what a newcomer and a model see first. | Libraries as a folder of snippets that drift. | Open |
| **L-18** | Models compose from fragments first? | **Yes.** Fragment cards and `with` schemas generated per host; two MCP tools (`list_fragments`, `get_fragment`); the system text says "fragments first"; targets as D30 plus at least 70% of panels from fragments and at most half the output tokens of the panel-only baseline. | Fewer tokens, fewer mistakes, and answers that look like the product. | Models write panels: libraries help only people. | Open |
| **L-19** | Order and size? | **Pull C1 and X42 forward to be the first items after the split, then LB0 the framework (54 days), LB1 `core` baseline (31 fragments), LB2 the other libraries' baseline (105) with the AI kit and Studio's palette, LB3 and LB4 as the expansion items they need land.** 254 builder-days in all, beside C1, X42 and T1. | Every fragment depends on those two items and nothing in them depends on the rest of R2; 136 fragments need only the baseline. | Libraries wait for all of R2: the first fragment ships after the last expansion item. | Open |
| **L-20** | Is the catalogue of 209 the target? | **Yes as the backlog; each library is released as 1.0 when its baseline wave is done** (`core` 1.0 with 31, `finance` 1.0 with 47), and grows by minors. A fragment that is not used by a gallery screen, a Drishti screen or a customer within two releases of its library is a candidate for removal at the next major. | A dated first release per library; a rule that keeps the libraries from growing without use. | All 209 before any release: the first library ships with the last. | Open |

## N5: the known differences between the reference and the specification

| # | Decision | Status |
|---|---|---|
| N5 | **Accepted (lead, 2026-11-19, from the compatibility regime Q1: byte for byte, including problem wording).** At language level 1.0 the reference (Drishti i7a behaviour) wins wherever it differs from the draft specification: the cases pin the reference and are `verified: true`. The specification's intended behaviour becomes the next level (1.1) and is pinned by cases with `since: "1.1"` and `verified: false` until the reference changes at that level. Applied to: the problem-message prefix, `RCH-4010` for a refused pattern, `standardize` as a decimal, problem paths, the dependent-problem rule. List aggregates, `min`/`max` over a list and the 14 `el/functions-*` aggregate examples are not differences of wording but missing behaviour: they stay unverified until the aggregates move into Core. | Accepted |

### Rachana-EL syntax follows Python (owner, 2026-11-21)

The owner's rule: **keep Rachana-EL's syntax as similar to Python as possible**, with Excel's function names. It amends EL_EXPANSION as follows (accepted 2026-11-21):

| # | Decision |
|---|---|
| PY1 | `and`, `or`, `not` are first-class operators everywhere (XD21's "refuse with a fix" is reversed); `&&`, `\|\|`, `!` stay accepted |
| PY2 | Conditional expression `a if c else b` (Python form); the Excel function `if(c, a, b)` stays |
| PY3 | Lambdas are written `lambda x: x * 2` (Python form), not `x => x * 2`; `=>` stays reserved |
| PY4 | Ranges via the function `range(start, stop[, step])` with Python's end-exclusive rule, not `..` syntax; `..` stays reserved |
| PY5 | Membership `x in [a, b]` and `x not in …` are added (`one_of` stays) |
| PY6 | Power `**` is accepted; `^` stays for Excel users |
| PY7 | `??` (first non-empty) and the pipe `\|>` are kept: Python has no equivalent |
| PY8 | Function names stay Excel's; Python built-ins that already overlap (`len`, `round`, `min`, `max`, `sum`, `abs`) keep their meaning |

A7b (FormExpr into Rachana-EL), owner 2026-11-21: form rules keep `and`/`or`/`not` (now normal syntax); A7b runs after A5's exact decimals, with a small `form` profile for FormExpr's 16-digit division, `+` joining text and undecided-rule-passes at level 1.0, and every changed answer recorded for review.

**A7b as built (2026-11-21), for the owner's review.** PY1 is in Rachana-EL's parser (`and`, `or`, `not`; `not` below the
comparisons). A form's expressions are read by Rachana-EL's parser and evaluated by the `form` profile (`el/FormRules`; `FormExpr`
keeps its API): exact decimals (literals keep their written scale), `/` to 16 digits, `+` joining a text, empty values, three-valued
`and`/`or`, a rule that cannot be told holds — the answers FormExpr gave (`FormRulesTest`: 39 expressions over 200 drafts each, value,
type and names equal to the old evaluator's, kept in the tests). A5 did not bring decimal arithmetic to Core's operators (D5-2: that
waits for level 1.1), so the decimals are the profile's. Changed answers:

| # | Was | Is |
|---|---|---|
| A7b-1 | A syntax error said FormExpr's words (`'x' is not expected in '…'`, `a text in '…' is not closed`, `a ')' is missing`, `the expression ends too soon`) | Rachana-EL's words with the expression (`unexpected ')' at 7 (in '…')`); kept as they were: an unknown function (`'f' is not a function here (empty, len, abs, min, max)`), a function's count of values, a path of three names, an empty or too long expression |
| A7b-2 | Only FormExpr's tokens | Rachana-EL's spellings are read too: `&&`, `\|\|`, `!` (tight, as in Rachana-EL), Java number forms; anything else of the language (lists, `? :`, `in`, `%`, `ctl.x`, other functions) is refused in words when the form is read |
| A7b-3 | `30_000_000` read by FormExpr's lexer | the form profile takes `_` out between digits before reading (Rachana-EL 1.0 has no separator; X14 brings it) |
| A7b-4 | `and`, `or`, `not` were FormExpr's only | they are Rachana-EL's (PY1): 6 conformance cases `el/python-words`, unverified (the reference refuses the words) |


### EL phase 1: level 1.1 as built (G2 lane B, 2026-11-21), for the owner's review

The syntax batch, exact numbers and code points are built at level 1.1 (SPEC 4.16, 4.17; EBNF part D); level 1.0 is unchanged
(replay identical). Choices made in building, each open to the owner:

| # | Decision as built | Alternative |
|---|---|---|
| EL1-1 | **`range` collides**: Core already has the aggregate `range(list, field?)` (max − min, split step A9); PY4 makes `range(start, stop, step)` Python's. At 1.1 the first argument decides: a list (or a field name as second argument) keeps the aggregate, numbers give Python's range. Recommendation: rename the aggregate in a later minor (`spread` or `value_range`) with a `rachana migrate` rule | refuse one of the two at 1.1 |
| EL1-2 | `**` and infix `^` share Python's precedence: right-associative and tighter than a sign on the left, so `-2 ^ 2` is `-4` (Excel gives `4`) | give `^` Excel's precedence (two rules for one operator), or refuse `-x ^ y` without parentheses with a fix |
| EL1-3 | A whole JSON number a host hands over as an integer is exact at 1.1 (`$.b * 0.5` with `b: 2` is the decimal `1`); a fractional JSON number is a double, as SPEC 4.16 rule 6 says. Only the kind differs, never the digits. Recommendation: amend rule 6 to say so | every JSON number a double |
| EL1-4 | The F8 cases that failed at 1.0 (26 `numbers-exact`, 9 `text-code-points`, 11 `reserved`) pin level 1.1 (`since: "1.1"`) and are verified there; 9 reserved-token cases are withdrawn because 1.1 defines the tokens, and two `**` message cases because PY6 made `**` the power. At 1.0 a 1.1 construct is refused with the reference's words (`el/syntax-at-1-0`), not `RCH-2008`: N5 and the byte-identical replay come first. `levels.py check` does not count a case recorded failing that moved to a later level or was withdrawn | re-record level 1.0 with the reserved-token words (the replay would change) |
| EL1-5 | Number-like text read by an operator at 1.1 follows the literal grammar (P2-2's decimal-only grammar): hexadecimal, `Infinity`, `NaN` and `d`/`f` suffixes are not numbers there; level 1.0 keeps Java's (D5-2) | keep Java's grammar at 1.1 |
| EL1-6 | Lambdas are taken by `map`, `sort` (each key), `reduce` (two names) and `group` (the key), and by `let`; a filter `[?p]` keeps `@`. The X45 budgets are not built (only the literal, record and `range` caps); the printer does not keep comments; X11's catalogue mark for functions whose first argument is not the subject is not built | build X45 and the comment-keeping printer in the next lane |

### A5: the same bytes on every runtime (2026-11-21)

| # | Decision |
|---|---|
| D5-1 | P2-1 accepted as recommended: Core's maths is its own fdlibm port (`CoreMath`, equal to `StrictMath` on Java 21) on every runtime, the JVM included. 17 conformance results move in the last place (all in P2-1's function list); they are listed in `tools/split/known-differences.txt`; the two determinism probes among them (`d-m04-c`, `d-m05-a`) expect Core's value. Language level 1.0, not yet released, was written again (`levels.py write 1.0`); **for the owner**: 18 results and the `fisher(0.5)` example changed (the 17, and `fisher_inv`, first recorded on Java 25) |
| D5-2 | Language level 1.0 keeps Java's number grammar (hexadecimal significands, `d`/`f` suffixes, `Infinity`, `NaN` read as numbers) for byte identity with the reference; P2-2's decimal-only grammar waits for level 1.1 and the owner. `NumberText` is Core's one door between doubles and text; on the JVM it is Java's own (shortest round trip, correctly rounded); the JavaScript build replaces the class |
| D5-3 | Case mapping, white space and reversal are Core's (`CoreText`: Java 21's Unicode 15.0 tables carried in Core, with Java's expansions and final sigma); the final sigma follows Java's word boundaries as far as `CoreTextTest` draws (letters, digits, spaces, hyphen, apostrophe, full stop, double quote, combining marks); other punctuation beside a sigma may differ from the JVM |
| D5-4 | Month and day names and the `date` format's pattern are Core's (`DateText`, English); a pattern letter beyond `y M d E` shows the date as it came. `java.time` itself stays (every D-T probe agreed); `java.text` and `java.time.format` leave Core |
| D5-5 | `text(v, code)` prints Excel number codes with Core's printer; a code outside its written grammar is refused with the message it always had. Two quoted texts side by side print as written (the reference printed a stray quote; no case had it) |
| D5-6 | The regular-expression budget is a count of steps (`SafeRegex.Limits.maxSteps`, default 20 million), not a clock: the same value stops at the same place on every runtime. API: `timeoutNanos` removed (planned; baseline rewritten) |
| D5-7 | `CoreDeterminismGuardTest` refuses, outside `CoreMath`, `NumberText`, `CoreText`, `CoreDecimal` and `DateText`: the transcendental `Math` calls and `rint`, `StrictMath`, `String.format`, `java.text`, `java.time.format`, locale names, the clock, `Double`/`Float` text and reading, `BigDecimal.sqrt`, the T1 gaps; and in the value code (`el/`, `format/`, the binder's `bind/`, `control/`, `view/`, `wire/`) the JDK's case mapping, strip, white space and `reverse`. Left for the JavaScript conformance run at A15 (JS_PROOF rows not in P2's closed list): `java.util.regex` for Core's fixed patterns (J12), hash iteration order (J17), implicit `"" + double` in messages |

### A9: the aggregate kernel in Core (2026-11-21)

| # | Decision |
|---|---|
| D9-1 | N7 as decided: Core takes the state and maths of the Core-listed aggregates and the list functions over them; it defines its own `Aggregate` contract (the engine's `AggFunction` shape, SQL state included, so the engine's shells delegate without changing bytes or SQL); the `_once` family, `measure`, `cell`, the sketches and the shells stay in the host |
| D9-2 | `CoreAggregates` is the first provider of every function table: a host's provider can no longer give these names (Drishti drops them at A11, DRISHTI_MIGRATION_TODO 8.3h); the catalogue marks them `R`, source `Rachana Core` |
| D9-3 | The moved maths follows A5: `CoreMath` (with `hypot` added) and `NumberText`; a geometric mean, skewness or kurtosis may move in the last place against today's engine until Drishti's shells run Core's classes |
| D9-4 | **For the owner**: level 1.0 (unreleased) written again for A9: 37 results change (16 aggregate cases now computed, 7 determinism probes now expect Core's values, 14 refusals whose list of known functions now names the aggregates); listed in `tools/split/known-differences.txt` |


### A15: the JavaScript build (2026-11-21)

| # | Decision |
|---|---|
| D15-1 | The build is `java/rachana-js` (Maven profile `js`): TeaVM 0.16.0 through its tool API in a JVM of its own, Rachana's classes unpacked into the module so that a same-named class of the JavaScript build replaces Core's (`internal.NumberText`, `internal.RegexText`, `internal.Bits`); everything else is Core's code as the JVM runs it. Two bundles, `rachana.runtime.js` and `rachana.tools.js`, over one JSON facade whose answers are the conformance documents (FORMAT 5) |
| D15-2 | TeaVM 0.16.0 prints `a * (b / c)` as `a * b / c` and `a + (b - c)` as `a + b - c` (its JavaScript printer treats + and * as associative, which doubles are not): the build carries one patched TeaVM file, `StatementRenderer` (Apache-2.0, the change marked); to report upstream with the reproduction (`asin(-0.7693625579023913)`) and drop the patch when a release fixes it |
| D15-3 | Core keeps one door for each runtime-dependent call it still makes: `NumberText.toDouble` (TeaVM's `BigDecimal.doubleValue` misrounds), `CoreDecimal.round` (TeaVM's `BigDecimal.round` leaves some values unrounded), `CoreMath` for every transcendental including the method references A5 missed (`exp`, `ln`, `log10`, the trigonometric functions); the guard refuses `doubleValue()`, `BigDecimal.round` and `Math::` elsewhere |
| D15-4 | **For the owner**: three JVM results move by one unit in the last place (`exp(1)` is fdlibm's `2.7182818284590455`): level 1.0 (unreleased) re-recorded for them, listed in `tools/split/known-differences.txt` |
| D15-5 | **For the owner (P1-1)**: the runtime bundle is 361 KB Brotli, the tools bundle 381 KB, against the accepted "about 200 KB" for the runtime: the runtime bundle carries the Sutra parser and checker and the view builder (P1's 247 KB entry evaluated expressions only); the weight left to remove is TeaVM's in-memory file system (about 7% of the code: Core reads format tables from paths; a format-text option would drop it) and `java.time.format` (3%: `LocalDate.parse`); recommendation: take both in A16 or B3 and re-measure, then decide between a lighter runtime entry (compile and evaluate only) and accepting the figure |
| D15-6 | **For the owner (P1-2)**: after fallback 2b, T4 is 8x to 100x the hand-written interpreter, median 14x (Node; P1 about 19x in Node): the remaining cost is structural (the expression tree over Java objects, boxed numbers, map lookups), not a hot spot an intrinsic removes. By P1-2's rule the owner chooses between fallback 3 (a hand-written JavaScript evaluator for Core, D3) and moving T4's threshold to the P4 budget (`@.price * @.qty - @.amount` is about 1.1 µs per evaluation in Node against about 170 ns for P4) |
| D15-7 | P1-3 holds and T9 now passes in this build: two clean builds of the same source gave the same SHA-256 (TeaVM run through its tool API in a fresh JVM); the release notes of every release record both bundles' SHA-256 (`tools/js/build_js.py` prints them) |

### Owner acceptances of 2026-11-19 (proofs W, P1, P2)

| # | Decision |
|---|---|
| W-1 | The Python path for compile, lint and bind is **the JavaScript bundle run on V8** (`mini-racer`, option C of the WebAssembly proof (DESIGN.md, Proofs)): no second build target, the bundle and facade are reused. A WebAssembly route (TeaVM with `wasmtime-py`) and a native-image library stay the alternatives; the proof script is re-run on each TeaVM and `wasmtime` release. Supersedes D17's route |
| P1-1 | Bundle size is measured, not promised: the budget is restated from the measured figure at each release (see D15-5 for the figure and the open point) |
| P1-2 | If evaluation speed misses the threshold by a wide margin, the owner chooses between a hand-written JavaScript evaluator for Core and moving the threshold (see D15-6) |
| P1-3 | Reproducible builds: two clean builds of the same source give the same bundle SHA-256; every release records both hashes (D15-7) |
| P2-1 | The same bytes on every runtime: Core's maths is its own fdlibm port on every runtime, the JVM included (D5-1) |

### G1 gate decisions (owner, 2026-11-21)

| # | Decision |
|---|---|
| O1 | The bundle sizes are accepted: runtime 361 KB, tools 381 KB Brotli. The P10 budgets are restated to 365 KB and 385 KB; about 10% is planned to be trimmed in G3 packaging |
| O1 amended (2026-11-21) | Bigger bundles are acceptable (owner 2026-11-21). The size budgets are guards against unintended growth, restated to the measured size + ~5% at each release |
| O2 | No second evaluator: fallback 3 (a hand-written JavaScript evaluator for Core) is rejected. T4 (14x the hand-written interpreter) is judged by the end-to-end screen budget P4; real screens are measured in G2 |
| O5 | The level 1.0 re-recordings D5-1, D9-4 and D15-4 are confirmed. Level 1.0 is frozen at the `v1.0.0` tag (`FROZEN.sha384` out of `pre-release` in the same commit) and never rewritten after it |
| O6 | The five API removals (`RachanaSchema.build`, `Calendars.use`, `Functions.register`, `Functions.liveSource`, `SafeRegex.Limits.timeoutNanos`) are confirmed. The new packages (`view`, `agg`, `bind`, `wire`, `profile`, `control`, the `parse` additions) are **EXPERIMENTAL until v1.0.0**; the API-surface baseline and the README say so |
| O7 | The pushes made before the G1 rule are confirmed as the approved state: Rachana `main` `d90176e` with `v0.9.0`, Elements `v3.0.0-rc.1`, Drishti `a816aa7c0` |
| O8 | PY1 to PY8 as built are confirmed |
| O10 | A form field's `pattern` moves onto `SafeRegex` (no look-around, no back-references); the F3 work first checks Drishti's forms for those constructs |
| O3 | Datasets and the grid, tree and chart kinds are built in G1 as ViewModel contract 1.1 (owner, 2026-11-21) |
| O4 | Map panels get a default geography helper in Elements; hosts may override (owner accepted the recommendation, 2026-11-21) |

### G1 gate fixes (2026-11-21)

| # | Decision |
|---|---|
| G1-1 | A form field's `pattern` runs on `SafeRegex` (one regex engine; gate finding F3, owner decision O10): matched against the whole value (`SafeRegex.matches`), linear time, Core's caps; look-around, back-references, word boundaries, possessive and atomic groups are refused at lint (`RCH-2058`). No shipped Sutra of Drishti (its repository and the 2.9.11.i7a corpus, 478 Sutras) has a form pattern at all, so no pattern keeps `java.util.regex`. `internal.RegexText` (Core's and the JavaScript build's) is gone |
| G1-2 | **For the owner**: level 1.0 (unreleased) re-recorded for two results, and the two case files edited before release: `problem/forms#pattern-not-a-regex` now says SafeRegex's words (`a '[' is not closed`, was Java's `Unclosed character class`; G1-1), and `el/determinism#d-m01-a` expects Core's `exp(1)`, `2.7182818284590455` (F9: D15-4 had re-recorded the result but not the probe). Only these two lines of `compat/levels/1.0/results.jsonl` changed; `FROZEN.sha384` (pre-release) rewritten, now also listing `el/python-words.yaml` and `sutra/binder/panels.yaml` and the earlier `sutra/examples/sutra.yaml` edit |
| G1-3 | The block library is per instance (`BlockLibrary`, F4): Core alone has no block; a host's library reaches the parser through its profile. The customer corpus is compiled on Core by default with the conformance suite's `drishti` profile (F1); the corpus's ViewModels (`binds/`) are recorded later, before G4 (F13) |
| G1-4 | TeaVM 0.16.0's precise dependency analysis merges type-set domains in an order that varies between runs, and some orders lose types (the runtime bundle at `52e112f`: no `Boolean` reached `Plain.asBoolean`, named formats failed; gate re-check R1). The build carries a second patched TeaVM file, `PreciseDependencyAnalyzer` (Apache-2.0, `java/rachana-js/src/build/java`): no domain merging; the bundles are the same on every build. `tools/js/build_js.py` runs the conformance suite on both bundles at every level after each build and fails on any error, verified failing case, or runtime answer that differs from the tools bundle's |

### L-21: fragments are functions (owner, 2026-11-21)

"Fragments should behave like functions. Outside callers supply data and attribute names, which map to whatever local names
the fragment has." The governing rule of the typed form (LIBRARIES 4 to 7; SPEC 2.5, level 1.2):

| # | Rule | As built |
|---|---|---|
| 1 | A fragment declares its **local names**: parameters, table parameters and each table's local columns (written, or a published contract); its body uses only those | `columns:` or `contract:` (`contracts/<name>.json`, Data Protocol schema form) on a table parameter |
| 2 | The caller **binds** them at the use like named arguments: value ← expression or literal; table ← the caller's table; each local column ← the caller's column, identity when the names already match | `with: { rows: { from: $.positions, columns: { amount: MTM_USD, desk: BOOK_DESK } } }`, or `rows: $.t` |
| 3 | The mapping is resolved at **compile time**: a rename in the expanded Sutra, so no run-time cost and the plan pushes down the caller's real names | field-name options and `bind:` of the panels that read the table, and in every expression `@.<column>` over its rows, a lambda element's field, the field of `first`/`last`/an index, and the text of a Core column parameter (`sum(arg.t, 'amount')`; `role: column` in functions.yaml) are renamed (lead, 2026-11-21); a computed column text, a local column's text over another list, an undeclared column or a bare local column name is `RCH-2121` |
| 4 | **Hermetic**: no reading of the caller's screen except through parameters; events are the fragment's return values, which the caller names (`as:`) and wires (`on:`) | `RCH-2117` for every typed fragment; `emits: [{ name, payload, doc }]` declared, raised by one inner panel; the caller reads `@<as>.<event>.<payload name>`; `on: { <event>: { set: <control>, field: <name> } }` (Core's one effect; host actions wait for I1) |
| 5 | Problems at the call site name **both sides** | unknown local column `RCH-2114`; required column missing `RCH-2115`; unknown caller column or type mismatch `RCH-2116`; a mapping the fragment never reads: advisory `RCH-2120` (severity warning) |
| 6 | Two uses with different mappings are **two instances** | `as: <name>` scopes every id the fragment brings (`<as>-<id>`, a dot is not an id character); without `as:` a second use collides (`RCH-2024`) |

### G2 mock-up choices (lead, under the owner's delegation, 2026-11-21)

The owner delegated mock-up choices to the lead's recommendation; the alternatives stay in docs/mockups/g2/ so the owner can override.

| # | Screen | Chosen | Alternatives kept |
|---|---|---|---|
| MK-1 | Rachana Studio | **A, the three-pane editor** (fragment palette · live `<rachana-view>` preview · Sutra/properties) **with B's command bar** for the AI "describe a screen" prompt on top; C's prose cell later as a documentation mode | B canvas-first, C notebook |
| MK-2 | Workspace document (L4) | **A, docked tiles**, extended with tabs of docks; channels link tiles | B tabs, C floating windows (later, in the desktop hosts only) |
| MK-3 | Docs site front page | **A, task-first**, with B's search box and contents tree on the inner pages | B reference-first |

### EL1 decisions taken (lead, under the owner's delegation, 2026-11-21)

| # | Decision | Why |
|---|---|---|
| EL1-1 | At level 1.1 `range(...)` is only Python's sequence. The A9 aggregate is renamed **`span(list, field?)`** (max − min) at 1.1; level 1.0 keeps the old `range` aggregate unchanged. No dispatch on the first argument's type. | One meaning per name (Python-like, owner PY4); choosing by argument type is fragile and surprises authors and the AI |
| EL1-2 | Accepted: `**` and `^` share Python's precedence; **amended at gate 2 (O6, 2026-11-21):** a sign right before a power spelled `^` without parentheses is refused at level 1.1 (`RCH-2101`, the message names both parenthesised forms); `-x ** n` keeps Python's rule with the checked-mode hint `RCH-2091`; level 1.0 has no infix `^` | A silent sign difference from Excel is the wrong default for a risk product; `**` stays Python's (owner, gate 2) |
| EL1-3 | Accepted: whole JSON numbers from a host are exact at 1.1; fractional ones stay doubles | Exact where it is safe without changing host contracts |
| EL1-4 | Accepted, with a condition: the level replay may skip a 1.0 case only when it is listed (moved with `since`, or withdrawn with a reason) in `compat/levels/moved-cases.yaml`; the G2 gate checks the list | Keeps "replay identical" auditable |
| EL1-5 | Accepted: number-like text at 1.1 follows the decimal-only literal grammar | One number grammar |
| EL1-6 | Accepted: lambdas in `map`, `sort`, `reduce`, `group`, `let`; second class, no recursion | Safe and bounded |

Alternatives stay recorded in the table above; the owner may override any of them.

### R-1: where Drishti gets the released jars (owner, 2026-11-21)

**Decided (owner):** released Rachana jars go into the **local Maven repository (`~/.m2`)** of each build machine; Drishti's parent POM pins `drishti.rachana.version`. A release is installed from its **tag** only (`git checkout vX.Y.Z && ./mvnw clean install`), never from a branch or a snapshot. Builders and gates never install snapshots. G4 adds the step "build and install Rachana at the pinned tag" to Drishti's build guides (Linux and Windows), and Drishti's build fails with a clear message naming that step when the pinned version is missing. The alternatives (a file repository inside Drishti, GitHub Packages) stay open if machines multiply.

### LT: live tables (design, 2026-11-21)

The owner approved live tables ("design and do it", 2026-11-21): Drishti's live result set engine becomes a Rachana feature.
The placement verdict, the design and the decisions **LT-1 to LT-19** with their recommendations are in
[LIVE_TABLES.md](DESIGN.md#live-tables-design) (sections 0 and 13); the lead decides them under the owner's delegation.

### Live tables (lead, under the owner's delegation, 2026-11-21)

The placement of the live-table engine is decided: **(a) Rachana, as the JVM module `rachana-live`**, never in Core or the JavaScript bundles; rights only through the host's `Authoriser` (no authoriser: writes refused); not a G4 dependency. **LT-1 to LT-19 are accepted as recommended**, including LT-8 (Drishti's write-back service stays in Drishti and is bound as the `ChangeSink`). LT-16 (the snapshot adapter lives in the host) is consistent with Drishti Lake's design (drishti `wip/i7b-products` docs/plans/DRISHTI_LAKE_DESIGN.md, host-supplied rows). Build: L1–L6 (read side) in G2, L7–L10 in G3; Drishti switches over in i8.

### L-12 amended (owner, 2026-11-21)

`drishti-rachana-libraries` is **public for the time being** (owner: "it is ok"), under the same proprietary licence as this repository (L-11 unchanged: all libraries proprietary). Like this repository, it carries no Drishti-internal data, only synthetic examples.

### AI kit and MCP moved to the AI release (owner, 2026-11-21)

The Rachana AI kit (prompt kit, validate-and-fix loop, evaluation harness) and the MCP tools (the sajhamcpserver tool pack) leave i7b G2 and are built in Drishti's GenAI release (i10 after the renumbering). The Studio's "describe a screen" command bar stays in the Studio design as a placeholder until then.

### Desktop hosts deferred (owner, 2026-11-21)

The desktop hosts (Electron, Java Swing over JCEF or JavaFX WebView, .NET WebView2) leave i7b and are **parked with no release assigned** (owner, final). The in-process bridge transport of the Data Protocol stays (built in G2), so a host can be added later without protocol changes. If revived, the first candidate is Swing over JavaFX WebView (jars only, no executable), after a painting spike.

### L-12 superseded: the libraries live inside Rachana (owner, 2026-11-21)

The owner does not want a separate library dependency: the fragment libraries (`core`, `finance`, `genomics`, `economy`, `geo`, `ops`, `ai`) move **into this repository** under `libraries/<name>/`, organised by domain, built and released **with Rachana on one version train** (each library keeps its own major in its manifest for `use: lib/frag@N`; the `.rlib` packaging and signing stay, for libraries that other teams write). Drishti then has one dependency (the Rachana pin). The `drishti-rachana-libraries` repository is moved here with its history and then **archived** (read-only); deleting it waits for the GitHub cleanup release. L-11 stands: the libraries are proprietary, under this repository's licence. The public synthetic test library stays where it is.

### S1-OPEN: private state of fragments and outputs wait for a writer (G2 lane A4, 2026-11-21; for the lead)

**Question.** Build S1 `state:` (screen state, a fragment's private state, `outputs:`) now, before I1 (the effect list)?

**Why it is blocked.** SUTRA_EXPANSION S1 says "only an action writes it", and LIBRARIES 6.2/6.3 say the same for a
fragment's private state and for a state handed over by name. Core has one effect today (`set: <control>` from an event,
L-21 rule 4); no action can write a state value. Built alone, `state:` would be a constant equal to its `default` (a
`let:` does that already, S2/X41, built). Four points are also not specified precisely enough to build without guessing:
(a) how a host hands state values back on the next bind (a `StateValues` beside `ControlValues`? the S4 view-state
document?); (b) the ViewModel shape at which contract (`state[] {id, value, url, reads}`, refuse or ignore-safe for an
older painter); (c) the namespacing of a fragment's private state (`<as>.<name>` as ids, `state.<as>.<name>` for outputs)
and whether a use without `as:` may hold state at all; (d) `url: true` and `remember: user` before S4 is specified.

**Recommendation.** Build S1 together with I1 in one lane, in this order: (1) I1's effect `set: { state: <name>, to:
<expr> }` (and `set: <control>` as today); (2) S1 `state:` with types, defaults, the 40-entry cap, `state.<id>` reads in
EL (a new `Expr.State`, parsed only at the level that has the key); (3) the bind request carries `StateValues` exactly
as `ControlValues` (the host keeps them per reader; `remember: user` is the host's), the ViewModel 1.2 lists
`state[] {id, value, url, reads}` (`reads`: panels to rebuild, as for controls, from the `state.` reads the compiler
already knows); (4) fragments: private state ids `<as>.<name>` (a stateful fragment requires `as:`, `RCH-2115` otherwise),
`outputs: {name: type}` readable by the user as `state.<as>.<name>`, never writable (`RCH-2117`); (5) S4's view-state
document after that, with `url: true`. Estimate: S1 5 bd + I1's state effect 3 bd + fragments' state/outputs 3 bd.

**S1-OPEN decided (lead, under the owner's delegation, 2026-11-21):** the recommendation is accepted: S1 is built together with I1 in one lane, in the order (1)–(5) above; (a) `StateValues` beside `ControlValues`; (b) ViewModel 1.2 `state[] {id, value, url, reads}`, an older painter ignores it safely; (c) private ids `<as>.<name>`, a stateful fragment requires `as:`; (d) `url: true` and `remember: user` wait for S4's view-state document.

### Level declaration (UP-0)

| # | Decision | Why |
|---|---|---|
| UP-0 | The `rachana:` key declares the level: `MAJOR.MINOR`, the minor optional; `rachana: 1` is 1.0 until levels 1.0 and 1.1 retire, then the newest; one switch, `Grammar.MAJOR_ONLY`. No project file, flag or attribute sets a level. A fragment's `rachana:` key is its level, exactly as a Sutra's (SPEC 2.1 rule 4); a fragment without the key is 1.0. Nothing rewrites a file to a later level: levels are read as written, and a host chooses the newest level it reads. | One way per job; every Sutra means the same on every host |
