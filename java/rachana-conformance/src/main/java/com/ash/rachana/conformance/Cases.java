/*
 * Copyright (c) 2026 Ashutosh Sinha <ajsinha@gmail.com>. All rights reserved.
 * Drishti Rachana. Proprietary: see the LICENSE file at the root of this repository.
 */
package com.ash.rachana.conformance;

import com.ash.rachana.api.Node;
import com.ash.rachana.el.*;
import com.ash.rachana.format.Formats;
import com.ash.rachana.parse.Json;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * The conformance cases answered over Rachana's own compiler (conformance/CONFORMANCE.md section 5), one JSON line in, one out, with no I/O:
 * the JVM runtime program ({@link JvmRuntime}) and the JavaScript build (java/rachana-js, the same code compiled by TeaVM) both answer
 * through it, so the runtimes cannot drift apart in the harness. It is the reference bridge (tools/reference-bridge/RefBridge.java)
 * moved onto this repository's code with its logic unchanged (split step A10); numbers are printed through Core's canonical
 * JSON writer ({@code Json}) so that every runtime prints the same bytes (split steps A5, A15).
 */
public final class Cases {
    private Cases() { }

    static final String VERSION = "1.1.1";
    static Formats formats;
    static ElCompiler EL;
    static com.ash.rachana.Rachana rachana;
    static String profiles = "core";

    /** Builds the instance the cases run on: {@code profiles} as {@code core,drishti}; the format files as {@link #instance} reads them. */
    public static void init(String profileList, List<String> files) {
        profiles = profileList;
        rachana = instance(profileList, files);
        formats = rachana.formats();
        EL = rachana.compiler();
    }

    /** The runtime's greeting line (RUNNER 5) for a runtime name ({@code jvm}, {@code node}, {@code chromium}...). */
    public static String hello(String runtime) {
        return "{\"runtime\":" + str(runtime) + ",\"runtimeVersion\":\"rachana-" + VERSION + "\",\"language\":\"" + com.ash.rachana.profile.Grammar.LEVEL + "\",\"profiles\":" + jsonList(profiles)
                + ",\"kinds\":[\"expr\",\"expr-error\",\"check\",\"grammar\",\"format\",\"problem\",\"edit\",\"sutra\",\"patch\",\"fire\",\"plan\",\"lib\",\"live\"]}";
    }

    /** One case line answered: its results line (RUNNER 4), without the line end. */
    public static String answer(String line) {
        Map<String, Object> c = obj(Json.read(line));
        StringBuilder sb = new StringBuilder("{\"case\":").append(str(text(c.get("case"))));
        try {
            String kind = text(c.get("kind"));
            sb.append(",\"actual\":");
            switch (kind) {
                case "expr" -> expr(c, sb);
                case "expr-error" -> exprError(c, sb);
                case "check" -> checked(c, sb);
                case "grammar" -> grammar(c, sb);
                case "format" -> format(c, sb);
                case "problem" -> problem(c, sb);
                case "edit" -> edit(c, sb);
                case "sutra" -> sutra(c, sb);
                case "patch" -> patch(c, sb);
                case "fire" -> fire(c, sb);
                case "plan" -> plan(c, sb);
                case "lib" -> lib(c, sb);
                case "live" -> live(c, sb);
                default -> throw new IllegalStateException("kind not implemented: " + kind);
            }
        } catch (Throwable t) {
            sb.setLength(sb.indexOf(",\"actual\":") < 0 ? sb.length() : sb.indexOf(",\"actual\":"));
            sb.append(",\"error\":").append(str(t.getClass().getSimpleName() + ": " + t.getMessage()));
        }
        return sb.append('}').toString();
    }

    /** One kind's answer: writes the actual document of a case into {@code sb}. */
    public interface Kind {
        void run(Map<String, Object> c, StringBuilder sb);
    }

    /**
     * One request (a case's fields without its id) answered by one kind (the facade of the JavaScript build): the actual document,
     * or {@code {"error": "<type>: <message>"}} when the runtime failed.
     */
    public static String run(Kind kind, String json) {
        StringBuilder sb = new StringBuilder();
        try {
            kind.run(obj(Json.read(json)), sb);
            return sb.toString();
        } catch (Throwable t) {
            return "{\"error\":" + str(t.getClass().getSimpleName() + ": " + t.getMessage()) + "}";
        }
    }

    /**
     * The instance the cases run on (split step A6): the first file is Core's format table, the others the format names of the first
     * profile named after {@code core} ({@code drishti}: the profile object of the conformance suite, which registers no function and takes the entity additions, split step A7).
     */
    static com.ash.rachana.Rachana instance(String profiles, List<String> files) {
        List<String> names = new ArrayList<>(List.of(profiles.split(",")));
        names.removeIf(n -> n.isBlank() || n.equals("core"));
        com.ash.rachana.RachanaOptions options = com.ash.rachana.RachanaOptions.DEFAULTS
                .withFormats(null, files.isEmpty() ? List.of() : files.subList(0, names.isEmpty() ? files.size() : 1));
        List<com.ash.rachana.profile.Profile> ps = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            com.ash.rachana.profile.Profile.Builder b = i == 0 ? profile(names.get(i).trim(), null)
                    : com.ash.rachana.profile.Profile.named(names.get(i).trim());
            if (i == 0 && files.size() > 1) {
                b.formats(files.subList(1, files.size()).toArray(String[]::new));
            }
            ps.add(b.build());
        }
        return com.ash.rachana.Rachana.with(options, ps.toArray(com.ash.rachana.profile.Profile[]::new));
    }

    /**
     * The conformance suite's host profile ({@code drishti}): the entity additions (split step A7: match, domain, links, provenance,
     * kind sources, form.save), no function of its own; its format names are added by the caller. With {@code blocks}, that library is
     * its fragment resolver and the source of the sections modules contribute (split step A6); without, no block resolves. The customer
     * corpus test (rachana-compat, CG3) compiles the corpus through this same profile.
     */
    public static com.ash.rachana.profile.Profile.Builder profile(String name, com.ash.rachana.parse.BlockLibrary blocks) {
        com.ash.rachana.profile.Profile.Builder b = com.ash.rachana.profile.Profile.named(name);
        return blocks == null ? com.ash.rachana.profile.Entities.addTo(b) : com.ash.rachana.profile.Entities.addTo(b, List::of, blocks);
    }

    // ---- expr ----------------------------------------------------------------------------------------------------

    public static void expr(Map<String, Object> c, StringBuilder sb) {
        Map<String, Object> given = obj(c.get("given"));
        EvalContext ctx = context(given, at(c).formats());              // fmt(...) knows the case's level's names
        Object v;
        List<String> reads = new ArrayList<>();
        try {
            if (c.containsKey("template")) {
                Template t = at(c).compiler().template(text(c.get("template")));
                v = t.render(ctx);
                LinkedHashSet<String> s = new LinkedHashSet<>();
                t.paths(s::add);
                reads.addAll(s);
            } else {
                Expr e = at(c).compiler().compile(expanded(c, text(c.get("expr"))));
                v = e.eval(ctx);
                LinkedHashSet<String> s = new LinkedHashSet<>();
                e.paths(s::add);
                reads.addAll(s);
            }
        } catch (com.ash.rachana.RachanaException e) {
            sb.append("{\"error\":{\"code\":").append(str(code(e))).append(",\"message\":").append(str(words(e))).append("}}");
            return;
        }
        Object s = Values.simplify(v);
        sb.append("{\"value\":");
        value(s, sb);
        sb.append(",\"type\":").append(str(type(s)));
        sb.append(",\"text\":").append(str(Values.masked(s) ? "•••" : Values.text(s)));
        sb.append(",\"reads\":[");
        boolean first = true;
        for (String r : reads) { if (!first) { sb.append(','); } first = false; sb.append(str(r.startsWith("$") ? r : "$." + r)); }
        sb.append("]}");
    }

    public static void exprError(Map<String, Object> c, StringBuilder sb) {
        String src = c.containsKey("template") ? text(c.get("template")) : text(c.get("expr"));
        try {
            src = c.containsKey("template") ? src : expanded(c, src);
        } catch (ElException e) {
            int at = Math.max(0, e.position());
            sb.append("{\"problems\":[{\"code\":").append(str(code(e))).append(",\"severity\":\"error\",\"message\":").append(str(words(e)))
                    .append(",\"expr\":{\"source\":").append(str(src)).append(",\"start\":").append(at).append(",\"end\":").append(Math.min(src.length(), e.end() > at ? e.end() : at + 1)).append("}}]}");
            return;
        }
        Map<String, Object> limits = obj(obj(c.get("given")).get("limits"));
        int depth = whole(limits.get("depth"), 200), len = whole(limits.get("length"), 10_000);
        ElCompiler comp = new ElCompiler(1000, new ElLimits(depth, len), rachana.functions(), level(c));
        try {
            if (c.containsKey("template")) { comp.template(src); } else { comp.compile(src); }
            sb.append("{\"problems\":[]}");
        } catch (ElException e) {
            int at = Math.max(0, e.position());
            sb.append("{\"problems\":[{\"code\":").append(str(code(e))).append(",\"severity\":\"error\",\"message\":").append(str(words(e)))
                    .append(",\"expr\":{\"source\":").append(str(src)).append(",\"start\":").append(at).append(",\"end\":").append(Math.min(src.length(), e.end() > at ? e.end() : at + 1)).append("}}]}");
        }
    }

    /**
     * FORMAT 5.2, 5.3: the expression with a model's measure functions ({@code functions: { net: { params: [x], expr: "x - 1" } }})
     * expanded into it ({@link MeasureFunctions}); the expression itself when the case declares none. A problem is {@code RCH-2101} at its
     * place: the token in the expression's own text, or the call whose function is at fault (a body, a count of arguments, a cycle).
     */
    static String expanded(Map<String, Object> c, String src) {
        Map<String, Object> fns = obj(c.get("functions"));
        if (fns.isEmpty()) {
            return src;
        }
        Map<String, MeasureFunctions.Def> defs = new LinkedHashMap<>();
        fns.forEach((name, d) -> {
            List<String> params = new ArrayList<>();
            items(obj(d).get("params")).forEach(p -> params.add(text(p)));
            defs.put(name, new MeasureFunctions.Def(params, text(obj(d).get("expr"))));
        });
        try {
            return MeasureFunctions.expand(src, defs, rachana.functions());
        } catch (IllegalArgumentException x) {
            throw x.getCause() instanceof ElException e ? e : x;        // located: the token, or the call whose function is at fault
        }
    }

    /**
     * FORMAT 5.4: checked mode (SPEC 4.14) over the case's known types at the case's level. {@code types.record} names the fields of
     * {@code $}, {@code types.row} those of {@code @}; a nested map is an object whose fields are read by their dotted path. Scope
     * {@code data} is a row scope (no {@code ctl}, {@code @panel}, {@code $src}, {@code #index}); {@code screen} (or none) the screen's.
     * The answer: the checked type (null when an error refuses the expression) and every problem, in order, with its place in the text.
     */
    public static void checked(Map<String, Object> c, StringBuilder sb) {
        Map<String, com.ash.rachana.el.check.ElType> cols = new LinkedHashMap<>();
        Map<String, Object> types = obj(c.get("types"));
        fieldTypes("$", obj(types.get("record")), cols);
        fieldTypes("@", obj(types.get("row")), cols);
        com.ash.rachana.el.check.Fields fields = new com.ash.rachana.el.check.Fields() {
            @Override
            public Optional<com.ash.rachana.el.check.Fields.Field> field(List<String> parts) {
                String path = String.join(".", parts);
                com.ash.rachana.el.check.ElType t = cols.get(path);
                return t == null ? Optional.empty() : Optional.of(new com.ash.rachana.el.check.Fields.Field(path, t, false));
            }

            @Override
            public boolean alias(String name) {
                return false;
            }

            @Override
            public Optional<String> measure(String nameOrId) {
                return Optional.empty();
            }

            @Override
            public List<String> names() {
                return new ArrayList<>(cols.keySet());
            }
        };
        String src = text(c.get("expr"));
        com.ash.rachana.el.check.Checker.Checked r = CheckedMode.check(src, "data".equals(text(c.get("scope")))
                ? com.ash.rachana.el.check.Scope.ROW : null, fields, level(c));
        boolean refused = r.problems().stream().anyMatch(com.ash.rachana.el.check.Problem::error);
        sb.append("{\"type\":").append(refused || r.type() == null ? "null" : str(r.type().word())).append(",\"problems\":[");
        boolean f = true;
        for (com.ash.rachana.el.check.Problem p : r.problems()) {
            if (!f) { sb.append(','); } f = false;
            sb.append("{\"code\":").append(str(p.code())).append(",\"severity\":").append(str(p.level())).append(",\"message\":").append(str(p.message()));
            if (p.fix() != null) { sb.append(",\"fix\":").append(str(p.fix())); }
            sb.append(",\"expr\":{\"source\":").append(str(src)).append(",\"start\":").append(p.start()).append(",\"end\":").append(p.end()).append("}}");
        }
        sb.append("]}");
    }

    /** The known types of {@code root}'s fields by dotted path ({@code $.a}, {@code $.o.b} for a nested map). */
    private static void fieldTypes(String root, Map<String, Object> types, Map<String, com.ash.rachana.el.check.ElType> out) {
        types.forEach((k, t) -> {
            if (t instanceof Map<?, ?>) {
                out.put(root + "." + k, com.ash.rachana.el.check.ElType.ANY);
                fieldTypes(root + "." + k, obj(t), out);
            } else {
                out.put(root + "." + k, com.ash.rachana.el.check.ElType.ofColumn(text(t)));
            }
        });
    }

    /**
     * FORMAT 5.5: whether the case's level reads {@code text} by the rule {@code rule} ({@code expression}, the default, or
     * {@code template}). Read by Rachana's own compiler at that level, the one every Sutra is read with.
     */
    public static void grammar(Map<String, Object> c, StringBuilder sb) {
        boolean ok = true;
        try {
            ElCompiler comp = at(c).compiler();
            if ("template".equals(text(c.get("rule")))) { comp.template(text(c.get("text"))); } else { comp.compile(text(c.get("text"))); }
        } catch (com.ash.rachana.RachanaException e) {
            ok = false;
        }
        sb.append("{\"accepted\":").append(ok).append('}');
    }

    public static void format(Map<String, Object> c, StringBuilder sb) {
        Object v = c.containsKey("value") ? plain(c.get("value")) : new LinkedHashMap<String, Object>();
        Object node = v instanceof Node ? v : (v == null ? null : v);
        String text = c.containsKey("format") ? at(c).formats().format(text(c.get("format")), node) : Values.text(node);   // the case's level's names
        String tone = c.containsKey("tone") && !Values.masked(node) ? com.ash.rachana.format.Tones.resolve(text(c.get("tone")), v) : null;
        sb.append("{\"text\":").append(str(text)).append(",\"tone\":").append(tone == null ? "null" : str(tone)).append('}');
    }

    /** The language level a case runs at: the minor it was written for ({@code since}; SUTRA_EXPANSION 6.1 rule 2: a case pins its level). */
    static String level(Map<String, Object> c) {
        return com.ash.rachana.profile.Grammar.LEVEL;        // Rachana 1.x reads one level; a case's since is its history only
    }

    private static final Map<String, com.ash.rachana.Rachana> AT = new LinkedHashMap<>();

    /** The instance at the case's language level. */
    static com.ash.rachana.Rachana at(Map<String, Object> c) {
        String l = level(c);
        return rachana;
    }

    /**
     * The grammar a case reads with (FORMAT 5.7): the instance's at the case's level, and with {@code files}, those files in place of
     * the host's: fragment files ({@code <name>.v<N>.fragment.yaml}, {@code .block.yaml}) as its fragment resolver, the folders
     * {@code rachana_libs/<library>/} as its libraries and {@code rachana.lock} as their lock. A fragment file that does not read adds
     * its problems to {@code problems}.
     */
    static com.ash.rachana.profile.Grammar grammar(com.ash.rachana.profile.Grammar base, Map<String, Object> c, List<com.ash.rachana.SutraProblem> problems) {
        Map<String, Object> files = obj(c.get("files"));
        if (files.isEmpty()) {
            return base;
        }
        List<com.ash.rachana.parse.SutraBlocks.Block> blocks = new ArrayList<>();
        Map<String, Map<String, String>> libs = new TreeMap<>();
        com.ash.rachana.lib.LibraryLock lock = null;
        Map<String, String> contracts = new TreeMap<>();
        for (Map.Entry<String, Object> f : files.entrySet()) {
            String path = f.getKey(), body = text(f.getValue());
            if (path.startsWith("rachana_libs/")) {
                String rest = path.substring("rachana_libs/".length());
                int slash = rest.indexOf('/');
                libs.computeIfAbsent(rest.substring(0, slash), k -> new TreeMap<>()).put(rest.substring(slash + 1), body);
            } else if (path.startsWith("contracts/") && path.endsWith(".json")) {      // the host's contracts (DESIGN.md (libraries) 5.2)
                contracts.put(path.substring("contracts/".length(), path.length() - ".json".length()), body);
            } else if (path.equals(com.ash.rachana.lib.LibraryLock.FILE)) {
                lock = com.ash.rachana.lib.LibraryLock.read(body);
            } else if (path.endsWith(".fragment.yaml") || path.endsWith(".block.yaml")) {
                try {
                    blocks.add(com.ash.rachana.parse.SutraBlocks.read(body, path.substring(path.lastIndexOf('/') + 1), path));
                } catch (com.ash.rachana.SutraException e) {
                    problems.addAll(e.problems());
                }
            }
        }
        List<com.ash.rachana.lib.LibraryBundle> bundles = new ArrayList<>();
        libs.forEach((n, fs) -> bundles.add(com.ash.rachana.lib.LibraryBundle.ofText(n, fs, "folder:rachana_libs/" + n)));
        com.ash.rachana.lib.LibrarySet set = com.ash.rachana.lib.LibrarySet.of(com.ash.rachana.lib.LibraryResolver.of(bundles));
        return base.withFragments(com.ash.rachana.parse.FragmentResolver.of(blocks, contracts)).withLibraries(lock == null ? set : set.locked(lock));
    }

    /**
     * FORMAT 5.13 {@code lib}: the library whose files {@code files} gives (paths inside the library folder), named {@code library},
     * read and checked (DESIGN.md (libraries) 3, 12): {@code {ok, problems, version, sha384, fragments}}, the last three null when it is refused.
     */
    public static void lib(Map<String, Object> c, StringBuilder sb) {
        Map<String, String> files = new TreeMap<>();
        obj(c.get("files")).forEach((k, v) -> files.put(k, text(v)));
        var bundle = com.ash.rachana.lib.LibraryBundle.ofText(text(c.get("library")), files, "case");
        List<com.ash.rachana.SutraProblem> ps = new ArrayList<>();
        com.ash.rachana.lib.Library lib = null;
        try {
            lib = com.ash.rachana.lib.Library.load(bundle, com.ash.rachana.lib.Library.Trust.NONE);
        } catch (com.ash.rachana.SutraException e) {
            ps.addAll(e.problems());
        }
        sb.append("{\"ok\":").append(ps.isEmpty()).append(",\"problems\":");
        problems(c, ps, sb);
        sb.append(",\"version\":").append(lib == null ? "null" : str(lib.version())).append(",\"sha384\":").append(lib == null ? "null" : str(lib.sha384()))
                .append(",\"fragments\":");
        if (lib == null) {
            sb.append("null");
        } else {
            sb.append('[');
            boolean f = true;
            for (String n : lib.fragmentNames()) { if (!f) { sb.append(','); } f = false; sb.append(str(n)); }
            sb.append(']');
        }
        sb.append('}');
    }

    /**
     * FORMAT 5.14: a live table's declaration read and checked ({@code LiveTableSpec}, LIVE_TABLES section 2): {@code {ok, problems,
     * schema}}, the schema document null when it is refused. {@code profileKeys} names the profile keys the host reads; formats are
     * the instance's.
     */
    public static void live(Map<String, Object> c, StringBuilder sb) {
        List<com.ash.rachana.SutraProblem> ps = new ArrayList<>();
        com.ash.rachana.table.LiveTableSpec spec = null;
        java.util.Set<String> keys = new java.util.LinkedHashSet<>();
        if (c.get("profileKeys") instanceof List<?> l) {
            l.forEach(k -> keys.add(text(k)));
        }
        try {
            spec = com.ash.rachana.table.LiveTableSpec.read(text(c.get("declaration")), c.get("file") == null ? "case.live.yaml" : text(c.get("file")),
                    keys, formats::has);
        } catch (com.ash.rachana.SutraException e) {
            ps.addAll(e.problems());
        }
        sb.append("{\"ok\":").append(ps.isEmpty()).append(",\"problems\":");
        problems(c, ps, sb);
        sb.append(",\"schema\":").append(spec == null ? "null" : com.ash.rachana.parse.Json.write(spec.schema())).append('}');
    }

    static void problems(Map<String, Object> c, List<com.ash.rachana.SutraProblem> ps, StringBuilder sb) {
        problems(c, ps, null, sb);
    }

    static void problems(Map<String, Object> c, List<com.ash.rachana.SutraProblem> ps, String main, StringBuilder sb) {
        sb.append('[');
        boolean f = true;
        for (var p : ps) {
            if (!f) { sb.append(','); } f = false;
            problem(p, !"1.0".equals(level(c)), main, sb);
        }
        sb.append(']');
    }

    /**
     * One problem of a compile (SPEC 6.1): code, severity, message, and its place. At level 1.1 and later the place is the whole of
     * SPEC 6.1, {@code path} (the YAML node's JSON Pointer) and {@code range} with start and end (line, column, offset) whenever the
     * problem is in the text compiled; at level 1.0 it is the start line and column the level was recorded with (compat/levels/1.0,
     * which a later runtime replays byte for byte). A problem of another file (a fragment, a library) has its start only.
     */
    static void problem(com.ash.rachana.SutraProblem p, boolean full, StringBuilder sb) {
        problem(p, full, null, sb);
    }

    /** As above; a problem located in a file other than {@code main} (a fragment) carries that file's name in {@code file} (SPEC 6.1). */
    static void problem(com.ash.rachana.SutraProblem p, boolean full, String main, StringBuilder sb) {
        sb.append("{\"code\":").append(str(p.code())).append(",\"severity\":").append(str(p.severity())).append(",\"message\":").append(str(p.message()));
        if (full && main != null && p.location().file() != null && !p.location().file().equals(main)) {
            sb.append(",\"file\":").append(str(p.location().file()));
        }
        if (full && p.path() != null) {
            sb.append(",\"path\":").append(str(p.path()));
        }
        var r = full ? p.range() : null;
        sb.append(",\"range\":{\"start\":{\"line\":").append(r == null ? p.location().line() : r.line()).append(",\"column\":")
                .append(r == null ? p.location().column() : r.column());
        if (r != null) {
            sb.append(",\"offset\":").append(r.offset()).append("},\"end\":{\"line\":").append(r.endLine()).append(",\"column\":").append(r.endColumn())
                    .append(",\"offset\":").append(r.endOffset());
        }
        sb.append("}}}");
    }

    /** The Sutra is parsed, then its expressions are checked: the problems in the order found. */
    public static void problem(Map<String, Object> c, StringBuilder sb) {
        List<com.ash.rachana.SutraProblem> ps = new ArrayList<>();
        List<com.ash.rachana.SutraProblem> warnings = List.of();
        List<com.ash.rachana.model.UsedLibrary> libraries = List.of();
        String file = c.get("file") == null ? "case.sutra.yaml" : text(c.get("file"));
        try {
            boolean noProfile = obj(c.get("given")).get("profiles") instanceof List<?> l && l.isEmpty();   // FORMAT 5.7: a runtime with no profile loaded
            var g = grammar(noProfile ? com.ash.rachana.profile.Grammar.core().atLevel(level(c)) : at(c).grammar(), c, ps);
            var sutra = (g == at(c).grammar() ? at(c).parser() : new com.ash.rachana.parse.SutraParser(g)).parse(text(c.get("sutra")), file, "conformance");
            ps.addAll(new com.ash.rachana.SutraExpressions(new ElCompiler(10_000, ElLimits.DEFAULTS, rachana.functions(), g.level())).check(sutra));
            warnings = sutra.advisories();
            libraries = sutra.libraries();
        } catch (com.ash.rachana.SutraException e) {
            ps.addAll(e.problems());
        }
        sb.append("{\"ok\":").append(ps.isEmpty()).append(",\"problems\":[");
        boolean f = true;
        for (int i = 0; i < ps.size() + warnings.size(); i++) {           // errors, then advisories (severity warning; ok stays true)
            var p = i < ps.size() ? ps.get(i) : warnings.get(i - ps.size());
            if (!f) { sb.append(','); } f = false;
            problem(p, !"1.0".equals(level(c)), file, sb);
        }
        sb.append(']');
        if (!libraries.isEmpty()) {                                        // the compile result's library hashes (level 1.2)
            sb.append(",\"libraries\":[");
            for (int i = 0; i < libraries.size(); i++) {
                var l = libraries.get(i);
                sb.append(i == 0 ? "" : ",").append("{\"name\":").append(str(l.name())).append(",\"version\":").append(str(l.version()))
                        .append(",\"sha384\":").append(str(l.sha384())).append('}');
            }
            sb.append(']');
        }
        sb.append('}');
    }

    /**
     * FORMAT 5.11: one editing operation of the reference (design/ops), its name in kebab case in the case ({@code set-option} is the
     * reference's {@code setOption}), its other fields as the reference names them. Applied: the new text and no problems. Refused: the
     * text unchanged and the reference's problem, with its line when it has one (plan H2).
     */
    public static void edit(Map<String, Object> c, StringBuilder sb) {
        String sutra = text(c.get("sutra"));
        Map<String, Object> op = new LinkedHashMap<>(obj(c.get("operation")));
        StringBuilder name = new StringBuilder();
        boolean up = false;
        for (char ch : text(op.get("op")).toCharArray()) { if (ch == '-') { up = true; } else { name.append(up ? Character.toUpperCase(ch) : ch); up = false; } }
        op.put("op", name.toString());
        String text = sutra;
        StringBuilder ps = new StringBuilder();
        try {
            var r = new com.ash.rachana.design.ops.OpApplier(at(c).parser()).apply(sutra, com.ash.rachana.design.ops.Ops.parse((Object) List.of(op)));
            text = r.yaml();
            for (var p : r.problems()) { editProblem(ps, p.code(), p.message(), p.line()); }
        } catch (com.ash.rachana.design.ops.Ops.FormatException e) {
            editProblem(ps, "RCH-5020", e.getMessage(), 0);
        }
        sb.append("{\"text\":").append(str(text)).append(",\"problems\":[").append(ps).append("]}");
    }

    static void editProblem(StringBuilder ps, String code, String message, int line) {
        if (ps.length() > 0) { ps.append(','); }
        ps.append("{\"code\":").append(str(code)).append(",\"severity\":\"error\",\"message\":").append(str(message));
        if (line > 0) { ps.append(",\"range\":{\"start\":{\"line\":").append(line).append("}}"); }
        ps.append('}');
    }

    /**
     * FORMAT 5 {@code sutra}: the Sutra over {@code given.record} (the entity's data; {@code given.today} the business date,
     * {@code given.ctl} the control values as a request carries them) built by Core's view pipeline with no host hooks
     * ({@code Rachana.views}, split step A14), written by Core's canonical writer. A host's fields (reference, mnemonic, provenance,
     * timings) are not set, so not written.
     */
    public static void sutra(Map<String, Object> c, StringBuilder sb) {
        sb.append("{\"viewModel\":").append(com.ash.rachana.view.ViewJson.write(screen(c).view())).append('}');
    }

    /**
     * FORMAT 5.9 {@code patch}: the view built as for {@code sutra}; then each step's Data Protocol patch applied to its table of
     * {@code given.tables} (by key, PROTOCOL 8.1), the view built again, and the ViewModel patch frames between the two written with the
     * new view ({@code frames: null} when the screen's shape changed and the whole ViewModel is sent again).
     */
    public static void patch(Map<String, Object> c, StringBuilder sb) {
        var s = screen(c);
        sb.append("{\"viewModel\":").append(com.ash.rachana.view.ViewJson.write(s.view())).append(",\"steps\":[");
        boolean first = true;
        for (Object o : items(c.get("steps"))) {
            Map<String, Object> step = obj(o);
            if (s.rows(text(step.get("table"))) == null) {
                throw new IllegalArgumentException("no table '" + text(step.get("table")) + "' in given.tables");
            }
            List<com.ash.rachana.view.Frame> frames = s.patch(text(step.get("table")), obj(step.get("patch")));
            sb.append(first ? "" : ",").append("{\"frames\":").append(frames == null ? "null" : com.ash.rachana.view.ViewJson.write(frames))
                    .append(",\"viewModel\":").append(com.ash.rachana.view.ViewJson.write(s.last())).append('}');
            first = false;
        }
        sb.append("]}");
    }

    /**
     * FORMAT 5.15 {@code fire} (level 1.2): the view built as for {@code sutra}; then each step fires a trigger on a mark of a panel as a
     * painter does ({@code Fire}): the effects the mark carries run in order, the state they write is carried by the next bind request,
     * and the view is built again. Per step: the screen's state after it, the effects run, the refusals and the ViewModel.
     */
    public static void fire(Map<String, Object> c, StringBuilder sb) {
        var s = screen(c);
        var vm = s.view();
        sb.append("{\"viewModel\":").append(com.ash.rachana.view.ViewJson.write(vm)).append(",\"steps\":[");
        boolean first = true;
        for (Object o : items(c.get("steps"))) {
            Map<String, Object> step = obj(o);
            boolean change = step.containsKey("control");                   // a control's change: the control takes the value, its effects run
            Object picked = step.get("value") instanceof List<?> l ? l.stream().map(Cases::text).toList() : step.get("value") == null ? "" : text(step.get("value"));
            var r = change ? com.ash.rachana.control.Fire.change(vm, text(step.get("control")), picked)
                    : com.ash.rachana.control.Fire.fire(vm, s.sutra(), text(step.get("panel")), text(step.get("trigger")),
                    step.get("mark") == null ? 0 : Integer.parseInt(text(step.get("mark"))), step.get("values") == null ? null : obj(step.get("values")),
                    today(c));
            Map<String, Object> writes = new LinkedHashMap<>();
            r.effects().stream().filter(e -> List.of("set", "toggle", "clear").contains(text(e.get("do"))))
                    .forEach(e -> writes.put(text(e.get("state")), e.get("value")));          // clear: empty, so the default stands
            if (change && r.refusals().isEmpty()) {
                s.controls(Map.of(text(step.get("control")), picked));
            }
            if (!writes.isEmpty() || change && r.refusals().isEmpty()) {
                s.state(writes);
                vm = s.view();
            }
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("state", com.ash.rachana.control.Fire.state(vm));                // the state the new view holds
            doc.put("effects", r.effects());
            doc.put("problems", r.refusals().stream().map(p -> { Map<String, Object> m = new LinkedHashMap<>(p); m.put("severity", "error"); return m; }).toList());
            sb.append(first ? "" : ",").append(Json.write(doc));
            sb.setLength(sb.length() - 1);
            sb.append(",\"viewModel\":").append(com.ash.rachana.view.ViewJson.write(vm)).append('}');
            first = false;
        }
        sb.append("]}");
    }

    /** FORMAT 5.10 {@code plan}: what the runtime asks of each table ({@code given.schemas} by table name), the plan document of B1. */
    public static void plan(Map<String, Object> c, StringBuilder sb) {
        Map<String, Object> g = obj(c.get("given"));
        var sutra = parse(c);
        Map<String, com.ash.rachana.table.TableSchema> schemas = null;
        if (g.containsKey("schemas")) {
            schemas = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : obj(g.get("schemas")).entrySet()) {
                schemas.put(e.getKey(), com.ash.rachana.table.TableSchema.of(e.getValue()));
            }
        }
        sb.append("{\"plan\":").append(Json.write(com.ash.rachana.table.DataPlan.plan(sutra, schemas, at(c).compiler(), context(g), at(c).language()))).append('}');
    }

    static com.ash.rachana.model.Sutra parse(Map<String, Object> c) {
        String file = c.get("file") == null ? "case.sutra.yaml" : text(c.get("file"));
        boolean coreOnly = "core".equals(text(c.get("profile")));          // a Core case is read with Core's grammar alone
        List<com.ash.rachana.SutraProblem> unread = new ArrayList<>();
        var base = coreOnly ? com.ash.rachana.profile.Grammar.core().atLevel(level(c)) : at(c).grammar();
        var gr = grammar(base, c, unread);                                 // FORMAT 5.7: files in place of the host's fragments and libraries
        if (!unread.isEmpty()) {
            throw new com.ash.rachana.SutraException(unread);
        }
        return (coreOnly || gr != base ? new com.ash.rachana.parse.SutraParser(gr) : at(c).parser()).parse(text(c.get("sutra")), file, "conformance");
    }

    /**
     * One screen of a {@code sutra} or {@code patch} case (Core's {@code LiveScreen}): the Sutra over {@code given.record} (the entity's
     * data; {@code given.today} the business date, {@code given.ctl} the control values as a request carries them, {@code given.state} the state values) and the tables of
     * {@code given.tables} (by table name: a list of rows, or {@code {schema, rows}}), held as keyed rows so that a patch can change
     * them. A host's fields (reference, mnemonic, provenance, timings) are not set, so not written.
     */
    /** A case's business date: {@code given.today}, else 2026-01-15. */
    static LocalDate today(Map<String, Object> c) {
        Map<String, Object> g = obj(c.get("given"));
        return g.containsKey("today") ? LocalDate.parse(text(g.get("today"))) : LocalDate.of(2026, 1, 15);
    }

    @SuppressWarnings("unchecked")
    public static com.ash.rachana.table.LiveScreen screen(Map<String, Object> c) {
        Map<String, Object> g = obj(c.get("given"));
        LocalDate today = today(c);
        Map<String, Object> ctl = new LinkedHashMap<>();
        obj(g.get("ctl")).forEach((k, v) -> ctl.put(k, v instanceof List<?> l ? l.stream().map(Cases::text).toList() : text(v)));
        var s = new com.ash.rachana.table.LiveScreen(at(c), parse(c), today, g.containsKey("record") ? node(g.get("record")) : null, ctl);
        s.pivots(CasePivots.ENGINE);                                      // FORMAT 5.7: the runner's own small pivot engine
        if (g.containsKey("state")) {                                       // level 1.2, S1: the state values the bind request carries
            Map<String, Object> state = new LinkedHashMap<>();
            obj(g.get("state")).forEach((k, v) -> state.put(k, v instanceof List<?> l ? l.stream().map(Cases::text).toList() : v == null ? "" : text(v)));
            s.state(state);
        }
        for (Map.Entry<String, Object> e : obj(g.get("tables")).entrySet()) {
            Object t = e.getValue();
            var schema = t instanceof Map<?, ?> m && m.containsKey("schema") ? com.ash.rachana.table.TableSchema.of(m.get("schema")) : null;
            List<Map<String, Object>> rows = new ArrayList<>();
            items(t instanceof Map<?, ?> m ? m.get("rows") : t).forEach(r -> rows.add((Map<String, Object>) r));
            s.table(e.getKey(), schema, rows);
        }
        for (String t : s.tableNames()) {                                   // given.tables are the host's tables: any other is missing (SPEC 2.4)
            if (!obj(g.get("tables")).containsKey(t)) {
                s.missing(t);
            }
        }
        return s;
    }


    static String words(com.ash.rachana.RachanaException e) {
        String m = e.getMessage(), c = e.code() == null ? "" : e.code().code();
        return m.startsWith(c + " ") ? m.substring(c.length() + 1) : m;
    }

    static String code(com.ash.rachana.RachanaException e) {
        String c = e.code() == null ? "" : e.code().code();
        return c;
    }

    // ---- context -------------------------------------------------------------------------------------------------

    static EvalContext context(Map<String, Object> g) {
        return context(g, formats);
    }

    static EvalContext context(Map<String, Object> g, Formats names) {
        Node root = node(g.containsKey("record") ? g.get("record") : new LinkedHashMap<String, Object>());
        EvalContext c = EvalContext.of(root, names);
        if (g.containsKey("today")) { c = c.withToday(LocalDate.parse(text(g.get("today")))); }
        Map<String, Object> ctl = new LinkedHashMap<>();
        // a multi-valued control (multi-select, the choices of oneOf/within) reaches the evaluator as a document list, as the host's
        // ControlValues hands it over (H2: ControlsTest passes Node.of(List.of(...))); a scalar as itself
        if (g.containsKey("ctl")) { obj(g.get("ctl")).forEach((k, x) -> { Object v = plain(x); ctl.put(k, v instanceof List<?> ? Node.of(v) : v); }); c = c.withControls(ctl); }
        Map<String, Node> events = new LinkedHashMap<>();
        if (g.containsKey("events")) { obj(g.get("events")).forEach((k, x) -> events.put(k, node(x))); }
        Object src = g.containsKey("src") ? node(g.get("src")) : null;
        if (!events.isEmpty() || src != null) { c = c.withScope(new EvalContext.Scope(events, null, src)); }
        List<Object> chain = new ArrayList<>();
        if (g.containsKey("parents")) { List<Object> ps = new ArrayList<>(); items(g.get("parents")).forEach(p -> ps.add(node(p))); Collections.reverse(ps); chain.addAll(ps); }
        if (g.containsKey("row")) { chain.add(node(g.get("row"))); }
        if (!chain.isEmpty()) {
            EvalContext base = c;
            int idx = whole(g.get("index"), -1);
            c = new EvalContext(base.root(), base.row(), idx, base.formats(), base.ctl(), base.today(), base.scope(), base.locals()).withRows(chain);
        }
        return c;
    }

    /** How a case's documents become nodes: Rachana's own, or a host's ({@code JvmRuntime --host-nodes}, {@link HostNodes}). */
    static java.util.function.Function<Object, Node> documents = Node::of;

    static Node node(Object n) { return documents.apply(plain(n)); }

    /** JSON to Java values; the tags of FORMAT 4 become the reference's own values. */
    static Object plain(Object n) {
        if (n == null) { return null; }
        if (n instanceof Boolean || n instanceof String || n instanceof Long || n instanceof Double) { return n; }
        if (n instanceof Number x) { return x.longValue(); }
        if (n instanceof List<?> a) { List<Object> l = new ArrayList<>(); a.forEach(x -> l.add(plain(x))); return l; }
        Map<String, Object> o = obj(n);
        if (o.size() == 1 && o.containsKey("$masked")) { return Node.masked(); }
        if (o.size() == 1 && o.containsKey("$dec")) { return new BigDecimal(text(o.get("$dec"))); }
        if (o.size() == 1 && o.containsKey("$num")) { return switch (text(o.get("$num"))) { case "NaN" -> Double.NaN; case "Infinity" -> Double.POSITIVE_INFINITY; default -> Double.NEGATIVE_INFINITY; }; }
        Map<String, Object> m = new LinkedHashMap<>();
        o.forEach((k, x) -> m.put(k, plain(x)));
        return m;
    }

    // ---- reading a case (plain JSON values) ----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object v) { return v instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of(); }

    static List<?> items(Object v) { return v instanceof List<?> l ? l : v instanceof Map<?, ?> m ? new ArrayList<>(m.values()) : List.of(); }

    /** A value's text as the case wrote it: text as itself, a number or flag in its JSON form, a missing value empty. */
    static String text(Object v) { return v == null ? "" : v instanceof String s ? s : v instanceof Map || v instanceof List ? "" : String.valueOf(v); }

    static int whole(Object v, int otherwise) { return v instanceof Number n ? n.intValue() : v instanceof String s ? parseInt(s, otherwise) : otherwise; }

    static int parseInt(String s, int otherwise) { try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return otherwise; } }

    // ---- results -------------------------------------------------------------------------------------------------

    static String type(Object s) {
        if (s == null) { return "empty"; }
        if (Values.masked(s)) { return "masked"; }
        if (s instanceof Boolean) { return "boolean"; }
        if (s instanceof BigDecimal) { return "decimal"; }
        if (s instanceof Number) { return "number"; }
        if (s instanceof String) { return "text"; }
        if (s instanceof Link) { return "link"; }
        if (s instanceof Node d) { return d.kind() == Node.Kind.ARRAY ? "list" : "object"; }
        if (s instanceof Collection) { return "list"; }
        if (s instanceof Map) { return "object"; }
        return "text";
    }

    static void value(Object s, StringBuilder sb) {
        if (s == null) { sb.append("null"); }
        else if (Values.masked(s)) { sb.append("{\"$masked\":true}"); }
        else if (s instanceof Boolean b) { sb.append(b); }
        else if (s instanceof BigDecimal d) { decimal(d, sb); }
        else if (s instanceof Double d) { number(d, sb); }
        else if (s instanceof Number n) { sb.append(n.longValue()); }
        else if (s instanceof String t) { sb.append(str(t)); }
        else if (s instanceof Link l) { sb.append("{\"$link\":{\"id\":").append(str(l.id())).append(l.kind() == null ? "" : ",\"kind\":" + str(l.kind())).append(l.label() == null ? "" : ",\"label\":" + str(l.label())).append("}}"); }
        else if (s instanceof Node d) { value(Values.simplify(d.unwrap()), sb); }
        else if (s instanceof Map<?, ?> m) {
            sb.append('{'); boolean f = true;
            for (Map.Entry<?, ?> e : m.entrySet()) { if (!f) { sb.append(','); } f = false; sb.append(str(String.valueOf(e.getKey()))).append(':'); value(Values.simplify(e.getValue()), sb); }
            sb.append('}');
        } else if (s instanceof Collection<?> l) {
            sb.append('['); boolean f = true;
            for (Object e : l) { if (!f) { sb.append(','); } f = false; value(Values.simplify(e), sb); }
            sb.append(']');
        } else { sb.append(str(s.toString())); }
    }

    /** FORMAT 9, N1 to N3 and N5. */
    static void number(double d, StringBuilder sb) {
        if (Double.isNaN(d)) { sb.append("{\"$num\":\"NaN\"}"); }
        else if (Double.isInfinite(d)) { sb.append("{\"$num\":\"").append(d > 0 ? "Infinity" : "-Infinity").append("\"}"); }
        else if (d == Math.floor(d) && Math.abs(d) < 1e15) { sb.append((long) d); }
        else { sb.append(Json.write(d)); }
    }

    /** N4: plain digits, trailing zeros of the fraction removed; $dec when a double cannot hold it. */
    static void decimal(BigDecimal b, StringBuilder sb) {
        BigDecimal s = b.stripTrailingZeros();
        String plain = s.scale() < 0 ? s.setScale(0).toPlainString() : s.toPlainString();
        double d = b.doubleValue();
        if (!Double.isInfinite(d) && new BigDecimal(Json.write(d)).compareTo(b) == 0) { number(d, sb); }
        else { sb.append("{\"$dec\":").append(str(plain)).append('}'); }
    }

    static String hex4(char ch) {
        String h = Integer.toHexString(ch);
        return "0000".substring(h.length()) + h;
    }

    static String jsonList(String commaSeparated) {
        StringBuilder b = new StringBuilder("[");
        for (String p : commaSeparated.split(",")) { if (b.length() > 1) { b.append(','); } b.append(str(p.strip())); }
        return b.append(']').toString();
    }

    static String str(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\b' -> b.append("\\b");
                case '\t' -> b.append("\\t");
                case '\n' -> b.append("\\n");
                case '\f' -> b.append("\\f");
                case '\r' -> b.append("\\r");
                default -> {
                    boolean lone = Character.isSurrogate(ch) && !(Character.isHighSurrogate(ch) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1)))
                            && !(Character.isLowSurrogate(ch) && i > 0 && Character.isHighSurrogate(s.charAt(i - 1)));
                    if (ch < 0x20 || ch == 0x7f || lone) { b.append("\\u").append(hex4(ch)); } else { b.append(ch); }
                }
            }
        }
        return b.append('"').toString();
    }
}
