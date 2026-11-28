/*
 * Copyright (c) 2026 Ashutosh Sinha <ajsinha@gmail.com>. All rights reserved.
 * Drishti Rachana. Proprietary: see the LICENSE file at the root of this repository.
 */
package com.ash.rachana.parse;

import com.ash.rachana.SutraExpressions;
import com.ash.rachana.SutraProblem;
import com.ash.rachana.model.StateEntry;
import com.ash.rachana.model.SourceLocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ash.rachana.model.Ordered;

/**
 * The typed form of a fragment (SUTRA_EXPANSION C1, DESIGN.md (libraries) 4; language level 1.2): a fragment whose every {@code params:} entry is
 * a mapping with a {@code type}. A parameter is of one of three classes (DESIGN.md (libraries) 4.2):
 *
 * <ul>
 *   <li><b>value</b> ({@code text}, {@code number}, {@code integer}, {@code decimal}, {@code boolean}, {@code date}, {@code datetime},
 *       {@code duration}, {@code money}, {@code list<T>}, {@code record{...}}, any of them followed by {@code ?}): written at the use as an
 *       expression, compiled in the user's context, read inside the fragment as {@code arg.<name>}. A YAML number or {@code true}/{@code
 *       false} at the use is a literal and is checked against the type, {@code min} and {@code max}; a default is always a literal. A
 *       {@code text} parameter takes its text as written (a label, a title). {@code arg.x} as a whole value becomes the expression or
 *       the literal itself; inside a longer expression or template it becomes the expression in parentheses or the literal as
 *       Rachana-EL text.</li>
 *   <li><b>table</b> ({@code type: table} with {@code columns:}): an expression giving rows, read inside as {@code arg.<name>};
 *       {@code optional: true} lets a use leave it out, and a panel with {@code when-bound: <name>} is then left out.</li>
 *   <li><b>name</b> ({@code id}, {@code format}, {@code tone}, {@code icon}, {@code variant}, {@code area}, {@code palette}, {@code key},
 *       {@code enum} with {@code options:}, {@code columns}): a word fixed when the Sutra compiles, put in as a <b>checked token</b> where the
 *       fragment writes {@code %{name}}. Free text substitution is the text form's alone.</li>
 * </ul>
 *
 * {@code slots:} (with {@code accepts: panels}) are holes the use fills with its own panels ({@code { slot: footer }} in the fragment's
 * panels). Expansion stays a compile-time step: the panels a typed use brings carry no trace of the fragment. Stateless; thread-safe.
 */
public final class TypedFragment {

    /** A parameter, a slot or an event at a use that the fragment does not declare. */
    public static final String UNKNOWN = "RCH-2114";
    /** A required parameter that the use does not give. */
    public static final String MISSING = "RCH-2115";
    /** A parameter of the wrong type, or outside its constraints. */
    public static final String WRONG_TYPE = "RCH-2116";
    /** A fragment that reaches outside what its use hands it (a public library's fragment reads the screen). */
    public static final String SCOPE = "RCH-2117";
    /** Slot content of a kind the slot does not accept, or more of it than its {@code max}. */
    public static final String SLOT = "RCH-2118";
    /** A column named where the caller's mapping cannot reach it (a computed column text, a bare name, a text off its table). */
    public static final String COLUMN_TEXT = com.ash.rachana.el.FragmentColumns.CODE;
    /** The language level that brings the typed form. */
    public static final String SINCE = "1.2";

    /** The value types (DESIGN.md (libraries) 4.2; X8's type language without {@code fn}, which waits for lambdas, X13). */
    public static final Set<String> VALUE_TYPES = Ordered.set("text", "number", "integer", "decimal", "boolean", "date", "datetime", "duration", "money");
    /** The name types: checked tokens. */
    /**
     * The one parameter name with a meaning of its own: a fragment that declares {@code id} (type {@code id}, optional) lets a use name its first
     * panel exactly ({@code with: { id: holidays }} gives the panel {@code holidays}, not {@code <as>-table}), so a screen converted to the
     * fragment keeps the ids its function keys, events and links name. A use that does not give it is scoped as before.
     */
    private static final String PANEL_ID = "id";
    public static final Set<String> NAME_TYPES = Ordered.set("id", "format", "tone", "icon", "variant", "area", "palette", "key", "enum", "columns");
    /** The name types that name the user's state or selection (DESIGN.md (libraries) 6.3), written beside {@link #NAME_TYPES}. */
    static final Set<String> HANDED_TYPES = Ordered.set("state", "selection");
    /** The tone words a {@code tone} parameter may take (SPEC 2.6). */
    public static final Set<String> TONES = Ordered.set("pos", "neg", "ok", "warn", "bad", "link", "accent");
    /** What a slot may accept at this level. */
    public static final Set<String> SLOT_KINDS = Set.of("panels");
    /** The keys a typed fragment file may have at this level. */
    public static final Set<String> KEYS = Ordered.set("rachana", "fragment", "version", "description", "doc", "params", "slots", "emits", "panels",
            "budget", "examples", "state", "outputs");
    /** The most parameters and slots a fragment declares (DESIGN.md (libraries) 18). */
    public static final int MAX_PARAMS = 40;
    public static final int MAX_SLOTS = 8;

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9-]{0,39}");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");
    private static final Pattern FKEY = Pattern.compile("F([1-9]|1[0-2])");
    private static final Pattern MASKED = Pattern.compile("pXq\\{([a-z][a-z0-9-]*)}");
    private static final Pattern ARG = Pattern.compile("(?<![A-Za-z0-9_.$@])arg\\.([a-z][a-z0-9-]*)");
    private static final Pattern VALUE_TYPE = Pattern.compile("(list<)?([a-z]+)(>)?\\??|record\\{[^}]*}\\??");
    /** A list of records ({@code list<record{id: text, label: text}>}), written at the use as a YAML list of mappings or an expression. */
    private static final Pattern LIST_RECORD = Pattern.compile("list<(record\\{[^}]*})>\\??");
    /** A record type and its fields. */
    private static final Pattern RECORD = Pattern.compile("record\\{([^}]*)}\\??");
    private static final Pattern FIELD_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");
    /** A function type (EL_EXPANSION X8, X13): {@code fn(T, ...) -> U}. */
    private static final Pattern FN_TYPE = Pattern.compile("fn\\(([^()]*)\\)\\s*->\\s*(.+)");
    /** A lambda as a use writes it: its names, then its body. */
    private static final Pattern LAMBDA = Pattern.compile("\\s*lambda\\s+([A-Za-z_][A-Za-z0-9_]*(?:\\s*,\\s*[A-Za-z_][A-Za-z0-9_]*)*)\\s*:(.*)",
            Pattern.DOTALL);

    /** One declared parameter. {@code dflt} is the default node (null for none). */
    public record Param(String name, String kind, String type, boolean required, PNode dflt, String doc, Number min, Number max,
            List<String> options, List<String> columns, PNode at) {

        /** {@code value}, {@code table} or {@code name}. */
        public String kind() {
            return kind;
        }

        /** Whether a use may leave it out with nothing in its place (an optional table, a nullable value without default). */
        public boolean optional() {
            return !required && dflt == null;
        }
    }

    /** One declared slot. */
    public record Slot(String name, String accepts, int max, String doc) { }

    private final String name;
    private final String file;
    private final PNode root;
    private final Map<String, Param> params;
    private final Map<String, Slot> slots;
    private final List<SutraProblem> problems;
    /** Its private state (S1): name to the declaration, in order. */
    private final Map<String, PNode> privateState = new LinkedHashMap<>();
    /** Its outputs: the private state the screen may read as {@code state.<as>.<name>}, name to type. */
    private final Map<String, String> outputs = new LinkedHashMap<>();

    private TypedFragment(String name, String file, PNode root, Map<String, Param> params, Map<String, Slot> slots, List<SutraProblem> problems) {
        this.name = name;
        this.file = file;
        this.root = root;
        this.params = params;
        this.slots = slots;
        this.problems = problems;
    }

    /**
     * Whether a fragment file's root (read with {@code %{} masked) is in the typed form: it has {@code params:} and every entry is a
     * mapping with a {@code type}. A file with none of them typed is the text form; a mixture is reported by {@link #of}.
     */
    public static boolean isTyped(PNode root) {
        PNode ps = root == null ? null : root.map().get("params");
        if (ps == null || !ps.isMap() || ps.map().isEmpty()) {
            return false;
        }
        return ps.map().values().stream().anyMatch(v -> v.isMap() && v.map().containsKey("type"));
    }

    /** The typed fragment a block holds (its declarations read and checked; {@link #problems()} lists what is wrong). */
    public static TypedFragment of(SutraBlocks.Block b) {
        PNode root = SutraBlocks.parse(b.text().replace("%{", "pXq{"), b.file());
        return read(b.name(), b.file(), root);
    }

    static TypedFragment read(String name, String file, PNode root) {
        List<SutraProblem> out = new ArrayList<>();
        Map<String, Param> params = new LinkedHashMap<>();
        Map<String, Slot> slots = new LinkedHashMap<>();
        root.map().forEach((k, v) -> {
            if (!KEYS.contains(k)) {
                out.add(problem(SutraBlocks.BLOCK, "a typed fragment takes " + new java.util.TreeSet<>(KEYS) + ", not '" + k + "' at this level", file, v));
            }
        });
        PNode ps = root.map().get("params");
        for (Map.Entry<String, PNode> e : ps == null ? List.<Map.Entry<String, PNode>>of() : ps.map().entrySet()) {
            Param p = param(name, e.getKey(), e.getValue(), file, out);
            if (p != null) {
                params.put(p.name(), p);
            }
        }
        if (params.size() > MAX_PARAMS) {
            out.add(problem(SutraBlocks.BLOCK, "fragment " + name + " has " + params.size() + " parameters; at most " + MAX_PARAMS + " (make it two fragments)", file, ps));
        }
        PNode ss = root.map().get("slots");
        for (Map.Entry<String, PNode> e : ss == null ? List.<Map.Entry<String, PNode>>of() : ss.map().entrySet()) {
            Map<String, PNode> d = e.getValue().map();
            String accepts = text(d.get("accepts"));
            if (!NAME.matcher(e.getKey()).matches() || !SLOT_KINDS.contains(accepts)) {
                out.add(problem(SutraBlocks.BLOCK, "slot " + e.getKey() + " of fragment " + name + " accepts one of " + SLOT_KINDS + " at this level", file, e.getValue()));
                continue;
            }
            int max = d.get("max") != null && d.get("max").value() instanceof Long l ? l.intValue() : 8;
            if (blank(docLine(d.get("doc")))) {
                out.add(problem("RCH-2055", "fragment " + name + ": slot " + e.getKey() + " has a doc of one line", file, e.getValue()));
            }
            slots.put(e.getKey(), new Slot(e.getKey(), accepts, max, docLine(d.get("doc"))));
        }
        if (slots.size() > MAX_SLOTS) {
            out.add(problem(SutraBlocks.BLOCK, "fragment " + name + " has more than " + MAX_SLOTS + " slots", file, ss));
        }
        PNode em = root.map().get("emits");
        if (em != null && !em.isList()) {
            out.add(problem(SutraBlocks.BLOCK, "fragment " + name + ": emits is a list of { name, payload?, doc }", file, em));
        }
        for (PNode e : em == null || !em.isList() ? List.<PNode>of() : em.list()) {
            String en = e.isMap() ? text(e.map().get("name")) : null;
            if (en == null || !NAME.matcher(en).matches() || e.map().keySet().stream().anyMatch(k -> !EVENT_KEYS.contains(k))) {
                out.add(problem(SutraBlocks.BLOCK, "fragment " + name + ": an event is { name, payload?, doc } with a lower-case kebab name", file, e));
            } else if (blank(docLine(e.map().get("doc")))) {
                out.add(problem("RCH-2055", "fragment " + name + ": event " + en + " has a doc of one line", file, e));
            }
        }
        PNode doc = root.map().get("doc");
        if (doc == null || blank(text(doc.map().get("summary")))) {
            out.add(problem("RCH-2055", "fragment " + name + " has doc.summary: one line saying what it shows", file, doc == null ? root : doc));
        } else if (text(doc.map().get("summary")).length() > 120) {
            out.add(problem("RCH-2055", "fragment " + name + ": doc.summary is at most 120 characters", file, doc.map().get("summary")));
        }
        TypedFragment t = new TypedFragment(name, file, root, params, slots, out);
        t.readState();
        t.checkBody();
        return t;
    }

    /** The keys a parameter declaration takes (DESIGN.md (libraries) 4.2 and 5.1); any other is refused, never read as part of the interface. */
    static final Set<String> PARAM_KEYS = Ordered.set("type", "doc", "default", "required", "optional", "columns", "contract", "min", "max",
            "options", "pattern", "max-rows", "live", "key", "window", "dynamic", "of");

    /** The keys a column of a table parameter takes (DESIGN.md (libraries) 5.1). */
    static final Set<String> COLUMN_KEYS = Ordered.set("type", "doc", "optional");

    /**
     * The words for a key {@code what} does not take. A key with spaces is almost always the rest of an unquoted flow-mapping text cut at
     * a comma ({@code doc: The change, written beside it} is the doc "The change" and a key "written beside it"): the fix says so.
     */
    private static String unknownKey(String what, String key, Set<String> keys) {
        return what + " takes " + new java.util.TreeSet<>(keys) + ", not '" + key + "'" + (key.contains(" ")
                ? ": a text with a comma inside { ... } is cut there; quote it (doc: \"..., ...\")" : "");
    }

    private static Param param(String fragment, String name, PNode d, String file, List<SutraProblem> out) {
        if (!d.isMap() || !d.map().containsKey("type")) {
            out.add(problem(SutraBlocks.BLOCK, "fragment " + fragment + " mixes the typed and the text form: parameter " + name
                    + " is a mapping with a type in the typed form", file, d));
            return null;
        }
        if (!NAME.matcher(name).matches()) {
            out.add(problem(SutraBlocks.BLOCK, "parameter '" + name + "' of fragment " + fragment + " is a lower-case kebab name", file, d));
            return null;
        }
        Map<String, PNode> m = d.map();
        for (String k : m.keySet()) {
            if (!PARAM_KEYS.contains(k)) {
                out.add(problem(SutraBlocks.BLOCK, unknownKey("parameter " + name + " of fragment " + fragment, k, PARAM_KEYS), file, m.get(k) == null ? d : m.get(k)));
            }
        }
        if (m.get("columns") != null && m.get("columns").isMap()) {
            m.get("columns").map().forEach((c, cd) -> {
                if (cd != null && cd.isMap()) {
                    for (String k : cd.map().keySet()) {
                        if (!COLUMN_KEYS.contains(k)) {
                            out.add(problem(SutraBlocks.BLOCK, unknownKey("column " + c + " of parameter " + name + " of fragment " + fragment, k,
                                    COLUMN_KEYS), file, cd));
                        }
                    }
                }
            });
        }
        String type = text(m.get("type"));
        String kind = "table".equals(type) ? "table" : NAME_TYPES.contains(type) || HANDED_TYPES.contains(type) ? "name" : "value";
        if ("value".equals(kind) && !valueType(type)) {
            out.add(problem(SutraBlocks.BLOCK, "parameter " + name + " of fragment " + fragment + " has type '" + type + "': a value type ("
                    + new java.util.TreeSet<>(VALUE_TYPES) + ", list<T>, record{...}, T?, fn(T, ...) -> U with one name or more), a name type (" + new java.util.TreeSet<>(NAME_TYPES)
                    + ") or table", file, m.get("type")));
            return null;
        }
        if ("selection".equals(type)) {
            out.add(problem("RCH-2023", "parameter " + name + " of fragment " + fragment + ": a selection parameter comes with selections "
                    + "(SUTRA_EXPANSION I2); not at this level", file, m.get("type")));
            return null;
        }
        String of = text(m.get("of"));
        if ("state".equals(type) && m.get("of") != null && !StateEntry.TYPES.contains(of)) {
            out.add(problem(SutraBlocks.BLOCK, "parameter " + name + " of fragment " + fragment + ": of is the type of the state it names (one of "
                    + String.join(", ", StateEntry.TYPES) + ")", file, m.get("of")));
        }
        boolean optional = Boolean.TRUE.equals(m.get("optional") == null ? null : m.get("optional").value());
        boolean required = Boolean.TRUE.equals(m.get("required") == null ? null : m.get("required").value());
        PNode dflt = m.get("default");
        if (required && dflt != null) {
            out.add(problem(SutraBlocks.BLOCK, "parameter " + name + " of fragment " + fragment + " is required or has a default, not both", file, d));
        }
        if (!required && dflt == null && !optional && !type.endsWith("?")) {
            out.add(problem(SutraBlocks.BLOCK, "parameter " + name + " of fragment " + fragment + " says required: true, a default, or optional: true", file, d));
        }
        if (blank(docLine(m.get("doc")))) {
            out.add(problem("RCH-2055", "fragment " + fragment + ": parameter " + name + " has a doc of one line", file, d));
        }
        if (m.containsKey("dynamic") && (!"text".equals(type) && !"text?".equals(type) || !(m.get("dynamic").value() instanceof Boolean))) {
            out.add(problem(SutraBlocks.BLOCK, "parameter " + name + " of fragment " + fragment + ": dynamic: true is for a text parameter (the use"
                    + " may write a template)", file, m.get("dynamic")));
        }
        List<String> options = new ArrayList<>();
        if (m.get("options") != null) {
            m.get("options").list().forEach(o -> options.add(o.text()));
        }
        if ("enum".equals(type) && options.isEmpty()) {
            out.add(problem(SutraBlocks.BLOCK, "parameter " + name + " of fragment " + fragment + " is an enum: it lists its options", file, d));
        }
        List<String> columns = new ArrayList<>(m.get("columns") == null || !m.get("columns").isMap() ? List.of() : m.get("columns").map().keySet());
        String contract = text(m.get("contract"));
        if ("table".equals(kind) && columns.isEmpty() && contract == null) {
            out.add(problem(SutraBlocks.BLOCK, "table parameter " + name + " of fragment " + fragment + " names its columns (columns: { name: { type } }) "
                    + "or a contract (contract: <name>)", file, d));
        }
        if ("table".equals(kind) && !columns.isEmpty() && contract != null) {
            out.add(problem(SutraBlocks.BLOCK, "table parameter " + name + " of fragment " + fragment + " names its columns or a contract, not both", file, d));
        }
        Param p = new Param(name, kind, type, required, dflt, docLine(m.get("doc")), number(m.get("min")), number(m.get("max")), List.copyOf(options),
                List.copyOf(columns), d);
        if (dflt != null) {
            String wrong = literal(p, dflt, true);
            if (wrong != null) {
                out.add(problem(WRONG_TYPE, "the default of parameter " + name + " of fragment " + fragment + " " + wrong, file, dflt));
            }
        }
        return p;
    }

    private static boolean valueType(String type) {
        Matcher lr = type == null ? null : LIST_RECORD.matcher(type);
        if (lr != null && lr.matches()) {
            return valueType(lr.group(1));
        }
        Matcher rec = type == null ? null : RECORD.matcher(type);
        if (rec != null && rec.matches()) {
            Map<String, String> fields = recordFields(type);
            return fields != null && fields.values().stream().allMatch(t -> !t.startsWith("record") && !t.startsWith("list<record") && valueType(t));
        }
        Matcher fn = type == null ? null : FN_TYPE.matcher(type);
        if (fn != null && fn.matches()) {
            if (fn.group(1).isBlank()) {
                return false;                                   // a lambda has at least one name
            }
            for (String t : fn.group(1).split(",")) {
                if (!t.isBlank() && !"any".equals(t.strip()) && !valueType(t.strip())) {
                    return false;
                }
            }
            return "any".equals(fn.group(2).strip()) || valueType(fn.group(2).strip());
        }
        Matcher vm = type == null ? null : VALUE_TYPE.matcher(type);
        if (vm == null || !vm.matches()) {
            return false;
        }
        return type.startsWith("record{") || VALUE_TYPES.contains(vm.group(2)) && (vm.group(1) == null) == (vm.group(3) == null);
    }

    /**
     * The fields of a record type ({@code record{id: text, required: boolean?}}: name to type, in written order; {@code record{}} has
     * none, any field is taken), or null when a field is not written {@code name: type}.
     */
    static Map<String, String> recordFields(String type) {
        Matcher rec = RECORD.matcher(type);
        if (!rec.matches()) {
            return null;
        }
        Map<String, String> out = new LinkedHashMap<>();
        if (rec.group(1).isBlank()) {
            return out;
        }
        for (String f : rec.group(1).split(",")) {
            int colon = f.indexOf(':');
            String n = colon < 0 ? "" : f.substring(0, colon).strip();
            if (!FIELD_NAME.matcher(n).matches() || out.containsKey(n)) {
                return null;
            }
            out.put(n, f.substring(colon + 1).strip());
        }
        return out;
    }

    /**
     * What is wrong with a written list or record (YAML at the use, or a default) for a {@code list<T>} or {@code record{...}} type, or
     * null: a list's items each of {@code T}; a record's fields among the declared ones, each required one present and of its type; a
     * scalar of its type (a {@code text} takes a word or a number as written, a date or a duration its text).
     */
    static String structure(String type, PNode v) {
        String t = type.endsWith("?") ? type.substring(0, type.length() - 1) : type;
        if (t.startsWith("list<") && t.endsWith(">")) {
            if (!v.isList()) {
                return "is a list (" + type + "), not " + shown(v);
            }
            for (int i = 0; i < v.list().size(); i++) {
                String w = structure(t.substring(5, t.length() - 1), v.list().get(i));
                if (w != null) {
                    return "item " + (i + 1) + " " + w;
                }
            }
            return null;
        }
        if (t.startsWith("record{")) {
            if (!v.isMap()) {
                return "is a record (" + type + "), not " + shown(v);
            }
            Map<String, String> fields = recordFields(t);
            for (Map.Entry<String, PNode> e : v.map().entrySet()) {
                if (!fields.isEmpty() && !fields.containsKey(e.getKey())) {
                    return "has no field '" + e.getKey() + "' (it has " + String.join(", ", fields.keySet()) + ")";
                }
                String w = fields.isEmpty() || e.getValue().value() == null && fields.get(e.getKey()).endsWith("?") ? null
                        : structure(fields.get(e.getKey()), e.getValue());
                if (w != null) {
                    return "field " + e.getKey() + " " + w;
                }
            }
            for (Map.Entry<String, String> f : fields.entrySet()) {
                if (!f.getValue().endsWith("?") && !v.map().containsKey(f.getKey())) {
                    return "needs field " + f.getKey() + " (" + f.getValue() + ")";
                }
            }
            return null;
        }
        Object x = v.value();
        boolean ok = switch (t) {
            case "text" -> x instanceof String || x instanceof Number;
            case "number", "decimal", "money" -> x instanceof Number;
            case "integer" -> x instanceof Long;
            case "boolean" -> x instanceof Boolean;
            default -> x instanceof String;                       // date, datetime, duration: their text
        };
        return ok ? null : "is " + ("integer".equals(t) ? "a whole number" : "a " + t) + ", not " + shown(v);
    }

    /**
     * Its {@code state:} (private to each use, S1; DESIGN.md (libraries) 4.4) and {@code outputs:} ({@code {name: type}}, 7.2): each output one of
     * its private state, of the same type. Checked with the Sutra's rules for state (the codes of SPEC 2.8).
     */
    private void readState() {
        PNode st = root.map().get("state");
        if (st != null) {
            if (!st.isMap()) {
                problems.add(problem("RCH-2012", "fragment " + name + ": state is a mapping of name to { type, default }", file, st));
            } else {
                EffectsReader reader = new EffectsReader(file, problems);
                st.map().forEach((k, v) -> {
                    if (reader.entry(k, v, null, false, "state") != null) {
                        privateState.put(k, v);
                    }
                });
            }
        }
        PNode outs = root.map().get("outputs");
        if (outs == null) {
            return;
        }
        if (!outs.isMap()) {
            problems.add(problem(SutraBlocks.BLOCK, "fragment " + name + ": outputs is a mapping of a private state's name to its type", file, outs));
            return;
        }
        outs.map().forEach((k, v) -> {
            PNode d = privateState.get(k);
            String type = d == null || d.map().get("type") == null ? null : d.map().get("type").text();
            if (d == null) {
                problems.add(problem(SutraBlocks.BLOCK, "fragment " + name + ": output " + k + " is not one of its state ("
                        + String.join(", ", privateState.keySet()) + ")", file, v));
            } else if (!java.util.Objects.equals(type, v.text())) {
                problems.add(problem(SutraBlocks.BLOCK, "fragment " + name + ": output " + k + " is " + v.text() + ", its state is " + type, file, v));
            } else {
                outputs.put(k, type);
            }
        });
    }

    /** Its private state's names (S1), in order. */
    public java.util.Set<String> stateNames() {
        return java.util.Collections.unmodifiableSet(privateState.keySet());
    }

    /** Its outputs ({@code outputs:}), name to type, in order. */
    public Map<String, String> outputs() {
        return java.util.Collections.unmodifiableMap(outputs);
    }

    private static final Pattern STATE_READ = Pattern.compile("(?<![A-Za-z0-9_.$@])state\\.([A-Za-z][A-Za-z0-9_]*)(?![A-Za-z0-9_])");

    /**
     * One of its own panels with its private state scoped to the use: {@code state.<name>} read as {@code state.<as>.<name>}, and each
     * {@code set: { state: <name> }} of its {@code on:} writing {@code <as>.<name>}. A fragment writes only its own state (DESIGN.md (libraries) 6.2):
     * a set of any other is {@code RCH-2117}.
     */
    private PNode ownState(PNode n, String as, String sutraFile, List<SutraProblem> out) {
        if (n.isMap()) {
            Map<String, PNode> m = new LinkedHashMap<>();
            n.map().forEach((k, v) -> {
                if ("set".equals(k) && v.isMap() && v.map().get("state") != null && v.map().get("state").text() != null) {
                    String target = v.map().get("state").text();
                    if (handed(target)) {                                    // LIBRARIES 6.3: the user's state, handed over by name
                        m.put(k, v);
                        return;
                    }
                    if (!privateState.containsKey(target)) {
                        out.add(problem(SCOPE, "fragment " + name + " sets state '" + target + "', which is not its own: a fragment writes only its own "
                                + "state (" + String.join(", ", privateState.keySet()) + ")", sutraFile, v.map().get("state")));
                    }
                    Map<String, PNode> set = new LinkedHashMap<>();
                    v.map().forEach((sk, sv) -> set.put(sk, "state".equals(sk) ? new PNode(as + "." + target, sv.line(), sv.column()) : ownState(sv, as, sutraFile, out)));
                    m.put(k, new PNode(set, v.line(), v.column()));
                } else if (("toggle".equals(k) || "clear".equals(k)) && n.map().keySet().stream().allMatch(x -> x.equals(k) || "when".equals(x))
                        && v.text() != null && !v.isMap() && !v.isList()) {                         // level 1.2: toggle/clear: <name> of its own state
                    String target = v.text();
                    if (handed(target)) {
                        m.put(k, v);
                    } else if (!privateState.containsKey(target)) {
                        out.add(problem(SCOPE, "fragment " + name + " " + k + "s state '" + target + "', which is not its own: a fragment writes only its "
                                + "own state (" + String.join(", ", privateState.keySet()) + ")", sutraFile, v));
                        m.put(k, v);
                    } else {
                        m.put(k, new PNode(as + "." + target, v.line(), v.column()));
                    }
                } else {
                    m.put(k, ownState(v, as, sutraFile, out));
                }
            });
            return new PNode(m, n.line(), n.column());
        }
        if (n.isList()) {
            List<PNode> l = new ArrayList<>();
            n.list().forEach(v -> l.add(ownState(v, as, sutraFile, out)));
            return new PNode(l, n.line(), n.column());
        }
        if (!(n.value() instanceof String s) || !s.contains("state.")) {
            return n;
        }
        Matcher m = STATE_READ.matcher(s);
        StringBuilder sb = new StringBuilder();
        int copied = 0;
        while (m.find()) {
            sb.append(s, copied, m.start());
            sb.append(privateState.containsKey(m.group(1)) ? "state." + as + "." + m.group(1) : m.group());
            copied = m.end();
        }
        sb.append(s, copied, s.length());
        return new PNode(sb.toString(), n.line(), n.column());
    }

    /** Whether {@code target} is {@code %{p}} of a state parameter {@code p} (DESIGN.md (libraries) 6.3): the one screen state the use hands over. */
    private boolean handed(String target) {
        Matcher m = target == null ? null : MASKED.matcher(target);
        return m != null && m.matches() && params.get(m.group(1)) != null && "state".equals(params.get(m.group(1)).type());
    }

    /** The state parameters (DESIGN.md (libraries) 6.3), name to the type of the state each names ({@code of}; null for any). */
    Map<String, String> stateParams() {
        Map<String, String> out = new LinkedHashMap<>();
        params.values().stream().filter(p -> "state".equals(p.type())).forEach(p -> out.put(p.name(), text(p.at().map().get("of"))));
        return out;
    }

    /** A fragment with no private state sets none: any {@code set: { state }} in its panels reaches the screen ({@code RCH-2117}). */
    private void foreignWrites(PNode panel, String sutraFile, List<SutraProblem> out) {
        walk(panel, n -> {
            for (String w : List.of("toggle", "clear")) {
                if (n.isMap() && n.map().get(w) != null && n.map().get(w).text() != null && !n.map().get(w).isMap()
                        && n.map().keySet().stream().allMatch(k -> k.equals(w) || "when".equals(k)) && !handed(n.map().get(w).text())) {
                    out.add(problem(SCOPE, "fragment " + name + " " + w + "s state '" + n.map().get(w).text() + "', which is not its own: a fragment "
                            + "writes only its own state (declare it under state:)", sutraFile, n.map().get(w)));
                }
            }
            if (n.isMap() && n.map().get("set") != null && n.map().get("set").isMap() && n.map().get("set").map().get("state") != null
                    && !handed(n.map().get("set").map().get("state").text())) {
                out.add(problem(SCOPE, "fragment " + name + " sets state '" + n.map().get("set").map().get("state").text() + "', which is not its "
                        + "own: a fragment writes only its own state (declare it under state:)", sutraFile, n.map().get("set")));
            }
        });
    }

    /** Every {@code %{x}} names a name parameter, every {@code arg.x} a value or table parameter, every {@code slot:} a slot. */
    private void checkBody() {
        PNode panels = root.map().get("panels");
        Set<PNode> targets = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        walk(panels, n -> {
            if (n.isMap() && n.map().get("set") != null && n.map().get("set").isMap() && n.map().get("set").map().get("state") != null) {
                targets.add(n.map().get("set").map().get("state"));
            }
        });
        walk(panels, n -> {
            if (n.value() instanceof String s && !targets.contains(n)) {
                Matcher m = MASKED.matcher(s);
                while (m.find()) {
                    Param p = params.get(m.group(1));
                    if (p != null && "state".equals(p.type())) {
                        problems.add(problem(SutraBlocks.BLOCK, "fragment " + name + " writes %{" + p.name() + "} outside set: { state: %{" + p.name()
                                + "} }: a state parameter names the one state it may write; read a value as a value parameter", file, n));
                    }
                }
            }
        });
        if (panels == null || !panels.isList()) {
            return;
        }
        walk(panels, n -> {
            if (n.value() instanceof String s) {
                Matcher m = MASKED.matcher(s);
                while (m.find()) {
                    Param p = params.get(m.group(1));
                    if (p == null || !"name".equals(p.kind())) {
                        problems.add(problem(SutraBlocks.BLOCK, "fragment " + name + " writes %{" + m.group(1) + "}: in the typed form %{...} names a "
                                + "name-class parameter (" + new java.util.TreeSet<>(NAME_TYPES) + "); read a value as arg." + m.group(1), file, n));
                    }
                }
                Matcher a = ARG.matcher(s);
                while (a.find()) {
                    String arg = longest(a.group(1));
                    Param p = arg == null ? null : params.get(arg);
                    if (p == null || "name".equals(p.kind())) {
                        problems.add(problem(SutraBlocks.BLOCK, "fragment " + name + " reads arg." + a.group(1) + ", which is not a value or table "
                                + "parameter it declares (" + params.keySet() + ")", file, n));
                    }
                }
            } else if (n.isMap() && n.map().containsKey("slot") && n.map().get("slot").text() != null && !slots.containsKey(n.map().get("slot").text())) {
                problems.add(problem(SutraBlocks.BLOCK, "fragment " + name + " places slot " + n.map().get("slot").text() + ", which it does not declare",
                        file, n));
            }
        });
    }

    /** The declared parameter that {@code arg.<text>} names: the longest declared name the text starts with ({@code arg.warn-at - 1}). */
    private String longest(String text) {
        String best = null;
        for (String p : params.keySet()) {
            if (text.startsWith(p) && (text.length() == p.length() || text.charAt(p.length()) == '-') && (best == null || p.length() > best.length())) {
                best = p;
            }
        }
        return best;
    }

    /** Its name. */
    public String name() {
        return name;
    }

    /** Its parameters, in declared order. */
    public Map<String, Param> params() {
        return params;
    }

    /** Its slots, in declared order. */
    public Map<String, Slot> slots() {
        return slots;
    }

    /** What is wrong with its declarations and body (empty when it is sound). */
    public List<SutraProblem> problems() {
        return problems;
    }

    /**
     * Where it reads outside what a use hands it (DESIGN.md (libraries) 4.4, L-6): {@code $}, {@code ctl.}, {@code state.}, {@code sel.}, a
     * {@code source:}, {@code data:} or {@code url:}. A fragment of a public library must have none ({@code RCH-2117}).
     */
    public List<SutraProblem> outsideScope() {
        List<SutraProblem> out = new ArrayList<>();
        PNode panels = root.map().get("panels");
        Pattern screen = Pattern.compile("\\$(?!\\{)|(?<![A-Za-z0-9_.])(ctl|state|sel)\\.");
        walk(panels, n -> {
            if (n.value() instanceof String s && screen.matcher(ownReads(s)).find()) {
                out.add(problem(SCOPE, "fragment " + name + " reads the screen around it ('" + s + "'): a library fragment reads only its parameters "
                        + "(arg.), its tables and its slots", file, n));
            } else if (n.isMap()) {
                for (String k : List.of("source", "data", "url")) {
                    if (n.map().containsKey(k)) {
                        out.add(problem(SCOPE, "fragment " + name + " names its own " + k + ": a library fragment is handed its tables as parameters",
                                file, n.map().get(k)));
                    }
                }
            }
        });
        return out;
    }

    /** {@code s} without its reads of the fragment's own private state ({@code state.<name>}: its own, not the screen's). */
    private String ownReads(String s) {
        s = NestedUses.withoutReads(s, NestedUses.names(root.map().get("panels")));   // a nested use's outputs: checked once bound
        if (privateState.isEmpty() || !s.contains("state.")) {
            return s;
        }
        Matcher m = STATE_READ.matcher(s);
        StringBuilder sb = new StringBuilder();
        int copied = 0;
        while (m.find()) {
            sb.append(s, copied, m.start()).append(privateState.containsKey(m.group(1)) ? "" : m.group());
            copied = m.end();
        }
        return sb.append(s, copied, s.length()).toString();
    }

    /**
     * What a caller knows about its own tables (decision L-21, "fragments are functions"): the columns, each with its type, of the table an
     * expression gives ({@code $.positions}: the {@code data:} entry's declared {@code fields} or its written rows), or null when they are
     * not known at compile time; and the published contracts (DESIGN.md (libraries) 5.2) a fragment's table parameter may name.
     */
    public interface Caller {

        /** The columns (name to type: {@code text}, {@code number}, {@code integer}, {@code decimal}, {@code boolean}, {@code date}, ...) of the
         * table {@code expression} gives, or null when unknown. */
        Map<String, String> columns(String expression);

        /** The contract file (Data Protocol schema form) named {@code name} for {@code fragment}, or null. */
        default PNode contract(SutraBlocks.Block fragment, String name) {
            return null;
        }

        /** A caller that knows nothing: no columns, no contracts. */
        static Caller none() {
            return e -> null;
        }
    }

    /**
     * One use bound (decision L-21): the panels it brings, its public events ({@code <as>.<event>} to the panel that raises it, read by the
     * screen as {@code @<as>.<event>}) and advisories ({@code RCH-2120}: a column mapping the fragment never reads).
     */
    public record Bound(List<PNode> panels, Map<String, String> events, List<SutraProblem> advisories, List<StateEntry> state) {

        /** A use that brings no private state. */
        public Bound(List<PNode> panels, Map<String, String> events, List<SutraProblem> advisories) {
            this(panels, events, advisories, List.of());
        }
    }

    /**
     * A use of a fragment that holds private state (S1, DESIGN.md (libraries) 4.4, 7.2): the fragment, its private state names and the ones it
     * publishes as {@code outputs:} (the screen reads them as {@code state.<as>.<name>}, never writes them).
     */
    public record Stateful(String fragment, Set<String> names, Set<String> outputs) { }

    /** A column of a table parameter: its local name, its type (EL words), whether a caller may leave it out. */
    record Col(String name, String type, boolean optional) { }

    /** Advisory: a use maps a column the fragment never reads (a warning, never refused). */
    public static final String UNUSED_MAPPING = "RCH-2120";
    /** The keys of a declared event. */
    static final Set<String> EVENT_KEYS = Ordered.set("name", "payload", "doc");
    /** The keys of one effect in a use's {@code on:} at this level: set a control from the event's payload. */
    static final Set<String> EFFECT_KEYS = Ordered.set("set", "field");
    /** Panel options that hold a field name of the panel's rows (renamed by a column mapping). */
    static final Set<String> FIELD_OPTIONS = Ordered.set("x", "y", "label", "value", "size", "group", "open", "high", "low", "close", "volume", "date",
            "detail", "status", "by", "across", "node", "parent", "path", "rowKey", "series", "dataset-key");
    /** A caller's column name a mapping may give: letters, digits, {@code _} and {@code -} ({@code bid-size} is read as {@code @['bid-size']}). */
    private static final Pattern FIELD = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]*");

    /** As {@link #bind} with a caller that knows nothing; the panels alone. */
    public List<PNode> expand(PNode use, String sutraFile, List<SutraProblem> out) {
        Bound b = bind(null, use, sutraFile, out, Caller.none());
        return b == null ? null : b.panels();
    }

    /**
     * The panels one use brings, bound like a call (decision L-21): {@code with:} checked against the declarations ({@code RCH-2114},
     * {@code 2115}, {@code 2116}); value parameters' expressions and literals put in for {@code arg.x}; name parameters as tokens; a table
     * parameter's local columns renamed to the caller's ({@code { from, columns }}, identity when not mapped) in the panels that read it;
     * slots filled; {@code as:} scoping every id the fragment brings ({@code <as>-<id>}); {@code on:} wiring a declared event to an effect.
     * Problems go to {@code out}; null when the use cannot be expanded.
     *
     * @param block the fragment's block (for its library's contracts), or null
     */
    public Bound bind(SutraBlocks.Block block, PNode use, String sutraFile, List<SutraProblem> out, Caller caller) {
        int before = out.size();
        List<SutraProblem> advisories = new ArrayList<>();
        Map<String, PNode> with = use.map().get("with") == null ? Map.of() : use.map().get("with").map();
        for (Map.Entry<String, PNode> e : with.entrySet()) {
            if (!params.containsKey(e.getKey())) {
                out.add(problem(UNKNOWN, "fragment " + name + " has no parameter '" + e.getKey() + "' (it has " + String.join(", ", params.keySet()) + ")",
                        sutraFile, e.getValue()));
            }
        }
        PNode asNode = use.map().get("as");
        String as = asNode == null ? null : asNode.text();
        if (asNode != null && (as == null || !NAME.matcher(as).matches())) {
            out.add(problem(WRONG_TYPE, "as: names this use of fragment " + name + " with a lower-case kebab word, not " + shown(asNode), sutraFile, asNode));
            as = null;
        }
        if (!privateState.isEmpty() && as == null) {
            out.add(problem(MISSING, "fragment " + name + " holds private state (" + String.join(", ", privateState.keySet()) + "): a use of it is named "
                    + "with as: (its state is then <as>.<name>)", sutraFile, use));
        }
        Map<String, Map<String, Col>> locals = new LinkedHashMap<>();
        for (Param p : params.values()) {
            if ("table".equals(p.kind())) {
                Map<String, Col> cols = tableColumns(p, block, caller, sutraFile, out);
                if (cols != null) {
                    locals.put(p.name(), cols);
                }
            }
        }
        Map<String, String> exprs = new LinkedHashMap<>();
        Map<String, PNode> literals = new LinkedHashMap<>();
        Map<String, PNode> tokens = new LinkedHashMap<>();
        Map<String, Map<String, String>> renames = new LinkedHashMap<>();
        Map<String, PNode> mappedAt = new LinkedHashMap<>();
        Set<String> unbound = new LinkedHashSet<>();
        Map<String, String> templates = new LinkedHashMap<>();
        for (Param p : params.values()) {
            PNode v = with.get(p.name());
            boolean fromDefault = v == null;
            if (v == null) {
                v = p.dflt();
            }
            if (v == null) {
                if (p.required()) {
                    out.add(problem(MISSING, "fragment " + name + " needs parameter '" + p.name() + "' (" + p.type() + "): " + nz(p.doc()), sutraFile, use));
                }
                unbound.add(p.name());
                continue;
            }
            if ("table".equals(p.kind()) && v.isMap()) {
                String from = mapped(p, v, locals.get(p.name()), caller, sutraFile, out, renames);
                if (from != null) {
                    exprs.put(p.name(), from);
                    mappedAt.put(p.name(), v);
                }
                continue;
            }
            String wrong = literal(p, v, fromDefault);
            if (wrong != null) {
                out.add(problem(WRONG_TYPE, "parameter " + p.name() + " of fragment " + name + " " + wrong, sutraFile, v));
                continue;
            }
            switch (p.kind()) {
                case "name" -> tokens.put(p.name(), v);
                default -> {
                    if (isText(p) && dynamic(p) && !fromDefault && v.value() instanceof String s && s.contains("${")) {
                        List<String> parts = com.ash.rachana.el.FragmentColumns.templateParts(s);
                        if (java.util.stream.IntStream.range(0, parts.size()).anyMatch(i -> i % 2 == 1 && rowReads(parts.get(i)))) {
                            out.add(problem(WRONG_TYPE, "parameter " + p.name() + " of fragment " + name + " is a template read in your screen's"
                                    + " context: its parts read the screen, not @, ^ or #index (inside the fragment they are the fragment's rows,"
                                    + " not yours): '" + s + "'", sutraFile, v));
                            continue;
                        }
                        templates.put(p.name(), s);           // a dynamic text: the caller's template
                    }
                    if (v.value() instanceof String s && (!fromDefault || isFn(p)) && !isText(p)) {
                        exprs.put(p.name(), s);
                        if ("table".equals(p.kind())) {
                            fit(p, s, locals.get(p.name()), Map.of(), caller, sutraFile, v, out);
                            renames.put(p.name(), Map.of());
                        }
                    } else {
                        literals.put(p.name(), v);
                    }
                }
            }
        }
        Map<String, PNode> slotted = use.map().get("slots") == null ? Map.of() : use.map().get("slots").map();
        for (Map.Entry<String, PNode> e : slotted.entrySet()) {
            Slot s = slots.get(e.getKey());
            if (s == null) {
                out.add(problem(UNKNOWN, "fragment " + name + " has no slot '" + e.getKey() + "' (it has " + String.join(", ", slots.keySet()) + ")",
                        sutraFile, e.getValue()));
            } else if (!e.getValue().isList()) {
                out.add(problem(SLOT, "slot " + s.name() + " of fragment " + name + " accepts a list of panels", sutraFile, e.getValue()));
            } else if (e.getValue().list().size() > s.max()) {
                out.add(problem(SLOT, "slot " + s.name() + " of fragment " + name + " takes at most " + s.max() + (s.max() == 1 ? " panel" : " panels") + ", not " + e.getValue().list().size(),
                        sutraFile, e.getValue()));
            }
        }
        if (out.size() > before) {
            return null;
        }
        List<PNode> panels = new ArrayList<>();
        List<Boolean> own = new ArrayList<>();
        Map<String, Set<String>> read = new LinkedHashMap<>();
        for (PNode p : root.map().get("panels").list()) {
            if (p.isMap() && p.map().containsKey("slot") && p.map().size() == 1) {
                PNode filled = slotted.get(p.map().get("slot").text());
                if (filled != null) {
                    for (PNode f : filled.list()) {               // the user's own panels: their context, untouched
                        panels.add(f);
                        own.add(false);
                    }
                }
                continue;
            }
            PNode bound = p.map().get("when-bound");
            if (bound != null && unbound.contains(bound.text())) {
                continue;
            }
            if (bound != null) {
                Map<String, PNode> m = new LinkedHashMap<>(p.map());
                m.remove("when-bound");
                p = new PNode(m, p.line(), p.column());
            }
            if (!privateState.isEmpty() && as != null) {
                p = ownState(p, as, sutraFile, out);                    // S1: state.<name> and set: { state: <name> } become <as>.<name>
            } else {
                foreignWrites(p, sutraFile, out);
            }
            for (String hands : List.of(com.ash.rachana.model.Effects.KEY, com.ash.rachana.model.Effects.ACTIONS)) {   // LIBRARIES 6.2: a fragment hands nothing to the host itself
                if (p.map().get(hands) != null) {
                    walk(p.map().get(hands), n -> {
                        if (n.isMap() && n.map().get("host") != null && !n.map().get("host").isMap() && !n.map().get("host").isList()) {
                            out.add(problem(SCOPE, "fragment " + name + " runs the host action '" + n.map().get("host").text() + "': a fragment never "
                                    + "calls the host; it returns an event and the Sutra that uses it decides", sutraFile, n.map().get("host")));
                        }
                    });
                }
            }
            Columns cols = new Columns(locals, renames, read);
            PNode rowsOf = p.map().get("rows");             // the rows the panel reads: a table's own (renamed), fixed fields, or refused
            com.ash.rachana.el.FragmentColumns.Rows over = rowsOf == null || rowsOf.text() == null ? null
                    : com.ash.rachana.el.FragmentColumns.rowsOf(rowsOf.text(), cols);
            String table = over == null ? null : over.row(rowsOf.text().trim());
            p = columns(p, table, SutraExpressions.expressionOptions(text(p.map().get("kind"))), true, false, cols, out);
            PNode filled = fill(dropUnbound(p, unbound), exprs, literals, tokens, templates, sutraFile, out);
            filled = nestedSlots(filled, slotted);
            panels.add(filled);
            own.add(true);
        }
        renames.forEach((t, m) -> m.forEach((local, theirs) -> {
            if (!read.getOrDefault(t, Set.of()).contains(local)) {
                advisories.add(problem(UNUSED_MAPPING, "the use maps column " + local + " of " + t + " to " + theirs + ", and fragment " + name
                        + " never reads " + local, sutraFile, mappedAt.get(t)));
            }
        }));
        PNode ownId = params.containsKey(PANEL_ID) ? tokens.get(PANEL_ID) : null;   // the use keeps its panel's own id (see PANEL_ID)
        if (as != null || ownId != null) {
            panels = scoped(panels, own, as, ownId == null ? null : ownId.text());
        }
        if (as != null) {
            panels = NestedUses.prefixed(panels, own, as, NestedUses.names(root.map().get("panels")));   // a nested use is <as>-<its as>
        }
        String scope = as;
        Map<String, String> events = new LinkedHashMap<>();
        Map<String, Integer> raisedBy = new LinkedHashMap<>();
        PNode em = root.map().get("emits");
        for (PNode e : em == null ? List.<PNode>of() : em.list()) {
            String ev = text(e.map().get("name"));
            int at = -1;
            for (int i = 0; i < panels.size(); i++) {
                if (own.get(i) && emits(panels.get(i), ev)) {
                    at = at == -1 ? i : -2;
                }
            }
            if (at < 0) {
                out.add(problem(SutraBlocks.BLOCK, "fragment " + name + " declares event " + ev + ", which " + (at == -1 ? "none" : "more than one")
                        + " of its panels emits (emits: [{ name: " + ev + ", on: ... }] on one panel)", file, e));
                continue;
            }
            raisedBy.put(ev, at);
            List<String> carried = carried(panels.get(at), ev);
            List<String> missing = payload(ev).stream().filter(x -> !carried.contains(x)).toList();
            if (!missing.isEmpty()) {
                out.add(problem(SutraBlocks.BLOCK, "fragment " + name + " declares event " + ev + " carrying " + String.join(", ", missing)
                        + ", which the panel that emits it does not put in its payload", file, e));
                continue;
            }
            if (scope != null) {                                // the caller reads @<as>.<event>.<payload name>: the fragment's return value
                events.put(scope + "." + ev, panels.get(at).map().get("id").text() + "." + ev);
            }
        }
        PNode on = use.map().get("on");
        if (on != null) {
            if (!on.isMap()) {
                out.add(problem(WRONG_TYPE, "on: maps an event of fragment " + name + " to an effect ({ select: { set: <control>, field: <name> } })", sutraFile, on));
            } else {
                for (Map.Entry<String, PNode> e : on.map().entrySet()) {
                    Integer at = raisedBy.get(e.getKey());
                    if (at == null) {
                        out.add(problem(UNKNOWN, "fragment " + name + " has no event '" + e.getKey() + "' (it has " + String.join(", ", declaredEvents())
                                + ")", sutraFile, e.getValue()));
                        continue;
                    }
                    PNode wired = panels.get(at);
                    for (PNode one : e.getValue().isList() ? e.getValue().list() : List.of(e.getValue())) {   // I1: a list of effects, in order
                        if (wired == null) {
                            break;
                        }
                        wired = one.isMap() && (one.map().get("set") != null && one.map().get("set").isMap()
                                || one.map().get("set") == null && one.map().keySet().stream().anyMatch(com.ash.rachana.model.Effects.BUILT_EFFECTS::contains))
                                ? stateEffect(wired, e.getKey(), one, sutraFile, out)
                                : effect(wired, e.getKey(), one, sutraFile, out, locals, renames);
                    }
                    if (wired != null) {
                        panels.set(at, wired);
                    }
                }
            }
        }
        List<StateEntry> state = new ArrayList<>();                      // S1: the use's private state, <as>.<name>
        if (!privateState.isEmpty() && as != null) {
            for (Map.Entry<String, PNode> e : privateState.entrySet()) {
                StateEntry st = new EffectsReader(file, out).entry(e.getKey(), e.getValue(), as, outputs.containsKey(e.getKey()), "state");
                if (st != null) {
                    state.add(st);
                }
            }
        }
        return out.size() > before ? null : new Bound(panels, events, advisories, state);
    }

    /** The events it declares, in order. */
    public List<String> declaredEvents() {
        List<String> out = new ArrayList<>();
        PNode em = root.map().get("emits");
        for (PNode e : em == null || !em.isList() ? List.<PNode>of() : em.list()) {
            if (e.isMap() && text(e.map().get("name")) != null) {
                out.add(text(e.map().get("name")));
            }
        }
        return out;
    }

    /** The local columns of table parameter {@code p}: written in the fragment, or read from its named contract. Null when not found. */
    private Map<String, Col> tableColumns(Param p, SutraBlocks.Block block, Caller caller, String sutraFile, List<SutraProblem> out) {
        Map<String, Col> cols = new LinkedHashMap<>();
        PNode written = p.at().map().get("columns");
        if (written != null && written.isMap()) {
            written.map().forEach((c, d) -> cols.put(c, new Col(c, d.isMap() ? nz(text(d.map().get("type"))) : "", d.isMap()
                    && Boolean.TRUE.equals(d.map().get("optional") == null ? null : d.map().get("optional").value()))));
            return cols;
        }
        String contract = text(p.at().map().get("contract"));
        PNode c = contract == null || block == null ? null : caller.contract(block, contract);
        if (c == null || !c.isMap() || c.map().get("columns") == null || !c.map().get("columns").isList()) {
            out.add(problem(SutraBlocks.BLOCK, "table parameter " + p.name() + " of fragment " + name + " names contract " + contract
                    + ", which its library does not hold (contracts/" + contract + ".json)", file, p.at()));
            return null;
        }
        for (PNode col : c.map().get("columns").list()) {
            String n = text(col.map().get("name"));
            boolean optional = col.map().get("optional") != null && Boolean.TRUE.equals(col.map().get("optional").value());
            cols.put(n, new Col(n, elType(text(col.map().get("type"))), optional));
        }
        return cols;
    }

    /** A Data Protocol type word as the EL type word ({@code string} is {@code text}). */
    static String elType(String t) {
        return t == null ? "" : "string".equals(t) ? "text" : t;
    }

    /** A table given as {@code { from, columns }}: checked; the rename recorded; the expression giving the caller's rows returned. */
    private String mapped(Param p, PNode v, Map<String, Col> locals, Caller caller, String sutraFile, List<SutraProblem> out,
            Map<String, Map<String, String>> renames) {
        int before = out.size();
        Map<String, PNode> m = v.map();
        for (String k : m.keySet()) {
            if (!"from".equals(k) && !"columns".equals(k)) {
                out.add(problem(UNKNOWN, "table parameter " + p.name() + " of fragment " + name + " is given as { from, columns }, not '" + k + "'", sutraFile, m.get(k)));
            }
        }
        String from = m.get("from") == null ? null : m.get("from").text();
        if (from == null || from.isBlank()) {
            out.add(problem(MISSING, "table parameter " + p.name() + " of fragment " + name + " given as a mapping needs from: (the caller's table)", sutraFile, v));
        }
        Map<String, String> rename = new LinkedHashMap<>();
        PNode cs = m.get("columns");
        if (cs != null && !cs.isMap()) {
            out.add(problem(WRONG_TYPE, "columns of " + p.name() + " maps the fragment's column names to the caller's ({ pnl: PNL_USD })", sutraFile, cs));
        }
        for (Map.Entry<String, PNode> e : cs == null || !cs.isMap() ? List.<Map.Entry<String, PNode>>of() : cs.map().entrySet()) {
            String theirs = e.getValue().text();
            if (locals != null && !locals.containsKey(e.getKey())) {
                out.add(problem(UNKNOWN, "fragment " + name + ": table " + p.name() + " has no column '" + e.getKey() + "' (it has "
                        + String.join(", ", locals.keySet()) + "); the use maps it to " + theirs, sutraFile, e.getValue()));
            } else if (theirs == null || !FIELD.matcher(theirs).matches()) {
                out.add(problem(WRONG_TYPE, "column " + e.getKey() + " of " + p.name() + " is mapped to a column name of the caller's table, not "
                        + shown(e.getValue()), sutraFile, e.getValue()));
            } else {
                rename.put(e.getKey(), theirs);
            }
        }
        if (out.size() > before || locals == null) {
            return null;
        }
        fit(p, from, locals, rename, caller, sutraFile, v, out);
        renames.put(p.name(), rename);
        return out.size() > before ? null : from;
    }

    /**
     * The caller's table against the fragment's local columns, when the caller's columns are known: each required local column present
     * under its mapped (or its own) name, of a fitting type; a mapped caller column that the table does not have. Messages name both sides.
     */
    private void fit(Param p, String from, Map<String, Col> locals, Map<String, String> rename, Caller caller, String sutraFile, PNode at,
            List<SutraProblem> out) {
        Map<String, String> theirs = locals == null ? null : caller.columns(from.trim());
        if (theirs == null) {
            return;
        }
        for (Col c : locals.values()) {
            String n = rename.getOrDefault(c.name(), c.name());
            String t = theirs.get(n);
            if (t == null && rename.containsKey(c.name())) {
                out.add(problem(WRONG_TYPE, "column " + c.name() + " of " + p.name() + " (fragment " + name + ") is mapped to " + n + ", which "
                        + from.trim() + " does not have (it has " + String.join(", ", theirs.keySet()) + ")", sutraFile, at));
            } else if (t == null && !c.optional()) {
                out.add(problem(MISSING, "fragment " + name + ": table " + p.name() + " needs column " + c.name() + (c.type().isEmpty() ? "" : " (" + c.type() + ")")
                        + "; " + from.trim() + " has " + String.join(", ", theirs.keySet()) + " (map it: { from: " + from.trim() + ", columns: { "
                        + c.name() + ": <column> } })", sutraFile, at));
            } else if (t != null && !fits(c.type(), t)) {
                out.add(problem(WRONG_TYPE, "column " + c.name() + " of " + p.name() + " (fragment " + name + ") is " + c.type() + "; " + from.trim()
                        + "'s " + n + " is " + t, sutraFile, at));
            }
        }
    }

    /** Whether a caller's column type serves a local column type (the numbers serve each other; an undeclared type serves anything). */
    public static boolean fits(String local, String theirs) {
        String l = elType(local).replace("?", "");
        String t = elType(theirs).replace("?", "");
        Set<String> numbers = Ordered.set("number", "integer", "decimal", "money");
        return l.isEmpty() || t.isEmpty() || l.equals(t) || numbers.contains(l) && numbers.contains(t) || "datetime".equals(l) && "date".equals(t);
    }

    /** The fragment's tables as the column pass sees them (decision L-21, rule 3). */
    private final class Columns implements com.ash.rachana.el.FragmentColumns.Tables {
        private final Map<String, Map<String, Col>> locals;
        private final Map<String, Map<String, String>> renames;
        private final Map<String, Set<String>> read;

        Columns(Map<String, Map<String, Col>> locals, Map<String, Map<String, String>> renames, Map<String, Set<String>> read) {
            this.locals = locals;
            this.renames = renames;
            this.read = read;
        }

        @Override
        public String table(String afterArg) {
            String p = longest(afterArg);
            return p != null && locals.containsKey(p) ? p : null;
        }

        @Override
        public Set<String> columns(String table) {
            Map<String, Col> c = locals.get(table);
            return c == null ? null : c.keySet();
        }

        @Override
        public String rename(String table, String column) {
            return renames.getOrDefault(table, Map.of()).getOrDefault(column, column);
        }

        @Override
        public void read(String table, String column) {
            read.computeIfAbsent(table, k -> new LinkedHashSet<>()).add(column);
        }

        @Override
        public List<String> tableNames() {
            return List.copyOf(locals.keySet());
        }
    }

    /**
     * A panel with its local column names renamed to the caller's (decision L-21, rule 3): over the table {@code table} the panel reads, its
     * field-name options and {@code bind}s; in every expression and template, {@code @.<column>}, a lambda element's field and the text of
     * a Core column parameter over a table ({@link com.ash.rachana.el.FragmentColumns}). A column named where the mapping cannot reach it is
     * {@code RCH-2121}.
     */
    private PNode columns(PNode n, String table, Set<String> exprKeys, boolean top, boolean expr, Columns cols, List<SutraProblem> out) {
        if (n.isMap()) {
            Map<String, PNode> m = new LinkedHashMap<>();
            String mark = top ? com.ash.rachana.el.FragmentColumns.markRow(text(n.map().get("kind"))) : null;
            n.map().forEach((k, v) -> {
                boolean isExpr = top && exprKeys.contains(k) || "bind".equals(k) || "when".equals(k);
                if (mark != null && ("emits".equals(k) || com.ash.rachana.model.Effects.KEY.equals(k))) {
                    m.put(k, columns(v, mark, exprKeys, false, isExpr, cols, out));   // @ is the clicked cell or point here, never a row
                } else if (table != null && (top && FIELD_OPTIONS.contains(k) && (v.isScalar() || v.isList()) || "bind".equals(k) && v.isScalar())) {
                    m.put(k, field(v, table, isExpr, "bind".equals(k), cols, out));
                } else {
                    m.put(k, columns(v, table, exprKeys, false, isExpr, cols, out));
                }
            });
            return new PNode(m, n.line(), n.column());
        }
        if (n.isList()) {
            List<PNode> l = new ArrayList<>();
            n.list().forEach(v -> l.add(columns(v, table, exprKeys, false, expr, cols, out)));
            return new PNode(l, n.line(), n.column());
        }
        if (!(n.value() instanceof String s)) {
            return n;
        }
        List<String> wrong = new ArrayList<>();
        String done = s.contains("${") ? com.ash.rachana.el.FragmentColumns.template(s, table, cols, wrong)
                : com.ash.rachana.el.FragmentColumns.rename(s, table, expr, cols, wrong);
        for (String w : wrong) {
            out.add(problem(COLUMN_TEXT, "fragment " + name + " " + w, file, n));
        }
        if (done == null) {
            done = table == null ? s : atColumns(s, table, cols);
        }
        return done.equals(s) ? n : new PNode(done, n.line(), n.column());
    }

    /** {@code @.<column>} renamed in a text that is no expression (read as text, the rule as written before the pass). */
    private static String atColumns(String s, String table, Columns cols) {
        if (!s.contains("@.") || cols.columns(table) == null) {
            return s;
        }
        Matcher m = Pattern.compile("(?<![A-Za-z0-9_$@])@\\.([A-Za-z_][A-Za-z0-9_]*)").matcher(s);
        StringBuilder sb = new StringBuilder();
        int copied = 0;
        while (m.find()) {
            String c = m.group(1);
            if (cols.columns(table).contains(c)) {
                cols.read(table, c);
                c = cols.rename(table, c);
            }
            sb.append(s, copied, m.start()).append("@.").append(c);
            copied = m.end();
        }
        return sb.append(s, copied, s.length()).toString();
    }

    /** A field-name option or {@code bind}: a local column name (or a list of them) renamed; anything else read as an expression. */
    private PNode field(PNode v, String table, boolean expr, boolean bind, Columns cols, List<SutraProblem> out) {
        if (v.isList()) {
            List<PNode> l = new ArrayList<>();
            v.list().forEach(x -> l.add(field(x, table, expr, bind, cols, out)));
            return new PNode(l, v.line(), v.column());
        }
        if (com.ash.rachana.el.FragmentColumns.unfollowed(table)) {       // rows derived where the mapping cannot follow: never silent
            for (String t : com.ash.rachana.el.FragmentColumns.unfollowedTables(table)) {
                if (v.value() instanceof String s && cols.columns(t) != null && cols.columns(t).contains(s)) {
                    out.add(problem(COLUMN_TEXT, "fragment " + name + " names column " + s + " of table " + t + " over rows derived from it that the "
                            + "caller's mapping cannot follow ('" + table.substring(table.indexOf('?', 1) + 1) + "'): read the table's own rows"
                            + " (arg." + t + ", filtered or through sort, take, drop, unique), or reshape them to fixed names (map(arg." + t
                            + ", {label: @." + s + "}))", file, v));
                    return v;
                }
            }
        } else if (v.value() instanceof String s && cols.columns(table).contains(s)) {
            cols.read(table, s);
            String theirs = cols.rename(table, s);
            boolean plain = theirs.matches("[A-Za-z_][A-Za-z0-9_]*");
            if (!bind && !plain) {                                    // a field-name option takes a name (SPEC 2.3a): never a silent wrong read
                out.add(problem(COLUMN_TEXT, "fragment " + name + " names column " + s + " of table " + table + " in a field-name option, and the"
                        + " caller maps it to '" + theirs + "', which is not a name ([A-Za-z_][A-Za-z0-9_]*): give the rows renamed in place of the mapping"
                        + " (" + table + ": \"map(<the caller's table>, {" + s + ": @['" + theirs + "'], ...})\")", file, v));
                return v;
            }
            return new PNode(bind && !plain ? "@['" + theirs + "']" : theirs, v.line(), v.column());   // a bind is an expression: bid-size is a minus
        }
        return columns(v, table, Set.of(), false, expr, cols, out);
    }

    /** The fragment's own panels with ids scoped to the use ({@code <as>-<id>}) and their reads of each other ({@code @id}) moved with them. */
    private static List<PNode> scoped(List<PNode> panels, List<Boolean> own, String as, String exact) {
        Map<String, String> ids = new LinkedHashMap<>();
        for (int i = 0; i < panels.size(); i++) {
            PNode id = own.get(i) && panels.get(i).isMap() ? panels.get(i).map().get("id") : null;
            if (id != null && id.text() != null) {
                // the fragment's first panel takes the use's own id exactly when it gave one; every other panel is <as>-<id>
                ids.put(id.text(), exact != null && ids.isEmpty() ? exact : as == null ? id.text() : as + "-" + id.text());
            }
        }
        List<PNode> out = new ArrayList<>();
        for (int i = 0; i < panels.size(); i++) {
            PNode p = panels.get(i);
            if (!own.get(i) || !p.isMap()) {
                out.add(p);
                continue;
            }
            PNode r = refs(p, ids);
            Map<String, PNode> m = new LinkedHashMap<>(r.map());
            if (m.get("id") != null && ids.containsKey(m.get("id").text())) {
                m.put("id", new PNode(ids.get(m.get("id").text()), m.get("id").line(), m.get("id").column()));
            }
            out.add(new PNode(m, p.line(), p.column()));
        }
        return out;
    }

    private static PNode refs(PNode n, Map<String, String> ids) {
        if (n.isMap()) {
            Map<String, PNode> m = new LinkedHashMap<>();
            n.map().forEach((k, v) -> m.put(k, refs(v, ids)));
            return new PNode(m, n.line(), n.column());
        }
        if (n.isList()) {
            List<PNode> l = new ArrayList<>();
            n.list().forEach(v -> l.add(refs(v, ids)));
            return new PNode(l, n.line(), n.column());
        }
        if (!(n.value() instanceof String s) || s.indexOf('@') < 0) {
            return n;
        }
        Matcher m = Pattern.compile("(?<![A-Za-z0-9_$@.-])@([a-z][a-z0-9-]*)(?![a-z0-9-])").matcher(s);
        StringBuilder sb = new StringBuilder();
        int copied = 0;
        while (m.find()) {
            String to = ids.get(m.group(1));
            sb.append(s, copied, m.start()).append((to == null ? m.group() : "@" + to));
            copied = m.end();
        }
        sb.append(s, copied, s.length());
        return new PNode(sb.toString(), n.line(), n.column());
    }

    /** Whether a panel emits event {@code ev} ({@code emits: [{ name: ev, on: ... }]}). */
    private static boolean emits(PNode panel, String ev) {
        PNode e = panel.isMap() ? panel.map().get("emits") : null;
        if (e == null || !e.isList()) {
            return false;
        }
        return e.list().stream().anyMatch(x -> x.isMap() && ev.equals(text(x.map().get("name"))));
    }

    /**
     * An event wired at the use to an effect: at this level the one effect Core has, setting a control from the event's row
     * ({@code { set: <control>, field: <column> }}, the panel's {@code on-select}); the field is the fragment's local name, renamed with the
     * table's mapping. Host actions wait for the effect list (I1).
     */
    private PNode effect(PNode panel, String ev, PNode v, String sutraFile, List<SutraProblem> out, Map<String, Map<String, Col>> locals,
            Map<String, Map<String, String>> renames) {
        if (!v.isMap() || v.map().get("set") == null || v.map().keySet().stream().anyMatch(k -> !EFFECT_KEYS.contains(k))) {
            out.add(problem(WRONG_TYPE, "on." + ev + " of fragment " + name + " is { set: <control>, field: <name> } at this level (host actions come "
                    + "with the effect list)", sutraFile, v));
            return null;
        }
        Map<String, PNode> sel = new LinkedHashMap<>();
        sel.put("by", v.map().get("set"));
        PNode f = v.map().get("field");
        if (f != null) {
            String local = f.text();
            List<String> payload = payload(ev);
            if (!payload.isEmpty() && !payload.contains(local)) {
                out.add(problem(UNKNOWN, "event " + ev + " of fragment " + name + " carries no '" + local + "' (it carries " + String.join(", ", payload) + ")",
                        sutraFile, f));
                return null;
            }
            String expr = payloadExpr(panel, ev, local);           // the row field the payload value reads (already in the caller's names)
            Matcher plain = expr == null ? null : Pattern.compile("@\\.([A-Za-z_][A-Za-z0-9_]*)").matcher(expr.trim());
            if (expr != null && !plain.matches()) {
                out.add(problem(WRONG_TYPE, "on." + ev + ".field of fragment " + name + " names " + local + ", which is computed ('" + expr
                        + "'), not a column of the row: a control is set from a column", sutraFile, f));
                return null;
            }
            sel.put("field", new PNode(plain != null ? plain.group(1) : local, f.line(), f.column()));
        }
        Map<String, PNode> m = new LinkedHashMap<>(panel.map());
        m.put("on-select", new PNode(sel, v.line(), v.column()));
        return new PNode(m, panel.line(), panel.column());
    }

    private static final Pattern EVENT_READ = Pattern.compile("(?<![A-Za-z0-9_.$@])event\\.([A-Za-z_][A-Za-z0-9_-]*)");

    /**
     * An event wired at the use to an effect of the list ({@code set: { state: <id>, to: <expression> }}, {@code toggle}, {@code clear},
     * {@code notify}, {@code host}: the Sutra decides what the fragment's event does, DESIGN.md (libraries) 6.1, 6.2): its expressions read the event's
     * payload as {@code event.<name>}, put in as the payload's own expression over the clicked row; the effect joins the {@code on:} of the
     * panel that raises the event, under the trigger it emits on. The state is the screen's (a fragment's private state is never written
     * from outside, {@code RCH-2117}, checked with the Sutra).
     */
    private PNode stateEffect(PNode panel, String ev, PNode effect, String sutraFile, List<SutraProblem> out) {
        PNode set = effect.map().get("set");
        if (set != null && (set.map().get("state") == null || set.map().get("state").text() == null || set.map().get("to") == null
                || EffectsReader.expression(set.map().get("to")) == null)) {
            out.add(problem(WRONG_TYPE, "on." + ev + " of fragment " + name + ": a state effect is { set: { state: <id>, to: <expression> } }", sutraFile, effect));
            return null;
        }
        String trigger = null;
        PNode e = panel.map().get("emits");
        for (PNode x : e == null ? List.<PNode>of() : e.list()) {
            if (x.isMap() && ev.equals(text(x.map().get("name")))) {
                trigger = text(x.map().get("on"));
            }
        }
        int before = out.size();
        PNode one = eventReads(effect, panel, ev, sutraFile, out);     // event.<name> put in as the payload's own expression
        if (out.size() > before) {
            return null;
        }
        Map<String, PNode> on = new LinkedHashMap<>();
        PNode had = panel.map().get(com.ash.rachana.model.Effects.KEY);
        if (had != null && had.isMap()) {
            on.putAll(had.map());
        }
        List<PNode> list = new ArrayList<>(on.get(trigger) != null && on.get(trigger).isList() ? on.get(trigger).list() : List.of());
        list.add(one);
        on.put(trigger, new PNode(list, effect.line(), effect.column()));
        Map<String, PNode> m2 = new LinkedHashMap<>(panel.map());
        m2.put(com.ash.rachana.model.Effects.KEY, new PNode(on, effect.line(), effect.column()));
        return new PNode(m2, panel.line(), panel.column());
    }

    /** {@code n} with every {@code event.<name>} of its text put in as the payload's expression of event {@code ev} ({@code (expr)}). */
    private PNode eventReads(PNode n, PNode panel, String ev, String sutraFile, List<SutraProblem> out) {
        if (n.isMap()) {
            Map<String, PNode> m = new LinkedHashMap<>();
            n.map().forEach((k, v) -> m.put(k, eventReads(v, panel, ev, sutraFile, out)));
            return new PNode(m, n.line(), n.column());
        }
        if (n.isList()) {
            List<PNode> l = new ArrayList<>();
            n.list().forEach(v -> l.add(eventReads(v, panel, ev, sutraFile, out)));
            return new PNode(l, n.line(), n.column());
        }
        if (!(n.value() instanceof String to) || !to.contains("event.")) {
            return n;
        }
        Matcher m = EVENT_READ.matcher(to);
        StringBuilder sb = new StringBuilder();
        int copied = 0;
        while (m.find()) {
            String expr = payloadExpr(panel, ev, m.group(1));
            if (expr == null) {
                out.add(problem(UNKNOWN, "event " + ev + " of fragment " + name + " carries no '" + m.group(1) + "' (it carries "
                        + String.join(", ", payload(ev)) + ")", sutraFile, n));
                return n;
            }
            sb.append(to, copied, m.start()).append('(').append(expr).append(')');
            copied = m.end();
        }
        sb.append(to, copied, to.length());
        return new PNode(sb.toString(), n.line(), n.column());
    }

    /** The payload names the panel's event {@code ev} carries. */
    private static List<String> carried(PNode panel, String ev) {
        PNode e = panel.map().get("emits");
        for (PNode x : e == null ? List.<PNode>of() : e.list()) {
            if (x.isMap() && ev.equals(text(x.map().get("name"))) && x.map().get("payload") != null && x.map().get("payload").isMap()) {
                return new ArrayList<>(x.map().get("payload").map().keySet());
            }
        }
        return List.of();
    }

    /** The expression the panel's event {@code ev} puts in payload field {@code field}, or null. */
    private static String payloadExpr(PNode panel, String ev, String field) {
        PNode e = panel.map().get("emits");
        for (PNode x : e == null ? List.<PNode>of() : e.list()) {
            if (x.isMap() && ev.equals(text(x.map().get("name"))) && x.map().get("payload") != null && x.map().get("payload").isMap()) {
                PNode v = x.map().get("payload").map().get(field);
                return v == null ? null : v.text();
            }
        }
        return null;
    }

    /** The payload names event {@code ev} declares (empty when it declares none). */
    private List<String> payload(String ev) {
        PNode em = root.map().get("emits");
        for (PNode e : em == null ? List.<PNode>of() : em.list()) {
            if (ev.equals(text(e.map().get("name"))) && e.map().get("payload") != null && e.map().get("payload").isMap()) {
                return new ArrayList<>(e.map().get("payload").map().keySet());
            }
        }
        return List.of();
    }

    /**
     * Panel keys that stand for something only when written: a function key, a code, an empty text, a condition, a width, a row filter
     * ({@code key}, {@code code}, {@code empty}, {@code when}, {@code where}, {@code span}, {@code height}, {@code pivot}, {@code waits}). A fragment
     * that writes one as a whole {@code arg.x} or {@code %{x}} of an optional parameter the use leaves out has the key left out of the panel,
     * not set to null.
     */
    static final Set<String> DROPPED_WHEN_UNBOUND = Ordered.set("key", "code", "empty", "when", "where", "span", "height", "pivot", "waits");

    private PNode dropUnbound(PNode n, Set<String> unbound) {
        if (unbound.isEmpty()) {
            return n;
        }
        if (n.isMap()) {
            Map<String, PNode> m = new LinkedHashMap<>();
            n.map().forEach((k, v) -> {
                if (DROPPED_WHEN_UNBOUND.contains(k) && v.value() instanceof String s && unboundWhole(s, unbound)) {
                    return;
                }
                m.put(k, dropUnbound(v, unbound));
            });
            return new PNode(m, n.line(), n.column());
        }
        if (n.isList()) {
            List<PNode> l = new ArrayList<>();
            n.list().forEach(v -> l.add(dropUnbound(v, unbound)));
            return new PNode(l, n.line(), n.column());
        }
        return n;
    }

    /** Whether {@code s} is, whole, {@code arg.x} or {@code %{x}} (masked) of a parameter the use left unbound. */
    private static boolean unboundWhole(String s, Set<String> unbound) {
        Matcher m = MASKED.matcher(s);
        if (m.matches()) {
            return unbound.contains(m.group(1));
        }
        if (s.startsWith("arg.") && unbound.contains(s.substring(4))) {
            return true;
        }
        Matcher call = CALL.matcher(s);                                // arg.f(@): a call of a function parameter left out
        return call.matches() && unbound.contains(call.group(1));
    }

    private static final Pattern CALL = Pattern.compile("arg\\.([a-z][a-z0-9-]*)\\([^()]*\\)");

    /**
     * A slot placed inside a panel's own {@code panels:} list ({@code { slot: body }} in a tabs panel): the use's panels take its place there,
     * in the user's context (a panel inside a tab reads {@code @} as the tab's element), untouched.
     */
    private PNode nestedSlots(PNode n, Map<String, PNode> slotted) {
        if (!n.isMap() || n.map().get("panels") == null || !n.map().get("panels").isList()) {
            return n;
        }
        List<PNode> inner = new ArrayList<>();
        boolean changed = false;
        for (PNode q : n.map().get("panels").list()) {
            if (q.isMap() && q.map().size() == 1 && q.map().containsKey("slot")) {
                changed = true;
                PNode filled = slotted.get(q.map().get("slot").text());
                if (filled != null) {
                    inner.addAll(filled.list());
                }
            } else {
                inner.add(q);
            }
        }
        if (!changed) {
            return n;
        }
        Map<String, PNode> m = new LinkedHashMap<>(n.map());
        PNode old = n.map().get("panels");
        m.put("panels", new PNode(inner, old.line(), old.column()));
        return new PNode(m, n.line(), n.column());
    }

    private PNode fill(PNode n, Map<String, String> exprs, Map<String, PNode> literals, Map<String, PNode> tokens, Map<String, String> templates,
            String sutraFile, List<SutraProblem> out) {
        if (n.isMap()) {
            Map<String, PNode> m = new LinkedHashMap<>();
            n.map().forEach((k, v) -> m.put(k, fill(v, exprs, literals, tokens, templates, sutraFile, out)));
            return new PNode(m, n.line(), n.column());
        }
        if (n.isList()) {
            List<PNode> l = new ArrayList<>();
            n.list().forEach(v -> l.add(fill(v, exprs, literals, tokens, templates, sutraFile, out)));
            return new PNode(l, n.line(), n.column());
        }
        if (!(n.value() instanceof String s)) {
            return n;
        }
        Matcher whole = MASKED.matcher(s);
        if (whole.matches() && tokens.containsKey(whole.group(1))) {
            PNode t = tokens.get(whole.group(1));
            return new PNode(t.value(), n.line(), n.column());    // a whole-value token: a word, or a list of columns
        }
        String arg = s.startsWith("arg.") ? longest(s.substring(4)) : null;
        if (arg != null && s.length() == 4 + arg.length()) {          // a whole value: the expression or the literal itself
            if (exprs.containsKey(arg)) {
                return new PNode(exprs.get(arg), n.line(), n.column());
            }
            if (literals.containsKey(arg)) {
                return new PNode(literals.get(arg).value(), n.line(), n.column());
            }
        }
        StringBuilder sb = new StringBuilder();
        Matcher m = MASKED.matcher(s);
        int copied = 0;
        while (m.find()) {
            PNode t = tokens.get(m.group(1));
            sb.append(s, copied, m.start());
            if (t == null || !t.isScalar()) {
                out.add(problem(WRONG_TYPE, "fragment " + name + " puts %{" + m.group(1) + "} inside a text: a list parameter fills a whole value", sutraFile, n));
            } else {
                sb.append(t.text());
            }
            copied = m.end();
        }
        sb.append(s, copied, s.length());
        String text = sb.toString();
        String done = args(text, exprs, literals, templates);
        return new PNode(done, n.line(), n.column());
    }

    /**
     * {@code text} with every {@code arg.x} put in: a value's expression in parentheses or its literal as EL text; a function parameter
     * (X13) as the lambda itself where it is a whole argument ({@code map(arg.rows, arg.f)}), and where the fragment calls it
     * ({@code arg.f(x)}) as a {@code let}-bound name around the expression ({@code let(fn_f = lambda v: ..., ... fn_f(x) ...)}; in a
     * template, around each {@code ${...}} that calls it).
     */
    private String args(String text, Map<String, String> exprs, Map<String, PNode> literals, Map<String, String> templates) {
        if (text.contains("${") && callsFunction(text)) {
            return com.ash.rachana.el.FragmentColumns.parts(text, part -> args(part, exprs, literals, templates));
        }
        StringBuilder done = new StringBuilder();
        Map<String, String> called = new LinkedHashMap<>();
        Matcher a = ARG.matcher(text);
        int copied = 0;
        while (a.find()) {
            String p = longest(a.group(1));
            if (p == null) {
                continue;
            }
            done.append(text, copied, a.start());
            int end = a.start() + 4 + p.length();
            if (isFn(params.get(p)) && exprs.containsKey(p)) {
                int k = end;
                while (k < text.length() && text.charAt(k) == ' ') {
                    k++;
                }
                if (k < text.length() && text.charAt(k) == '(') {
                    done.append(fnName(p));
                    called.put(fnName(p), exprs.get(p).strip());
                } else {
                    done.append(exprs.get(p).strip());
                }
            } else {
                done.append(exprs.containsKey(p) ? "(" + exprs.get(p) + ")" : templates.containsKey(p) ? templateEl(templates.get(p))
                        : literals.containsKey(p) ? el(literals.get(p)) : "null");
            }
            copied = end;
        }
        done.append(text, copied, text.length());
        if (called.isEmpty()) {
            return done.toString();
        }
        StringBuilder let = new StringBuilder("let(");
        called.forEach((k, v) -> let.append(k).append(" = ").append(v).append(", "));
        return let.append(done).append(')').toString();
    }

    /** Whether {@code text} calls a function parameter ({@code arg.f(}). */
    private boolean callsFunction(String text) {
        Matcher a = ARG.matcher(text);
        while (a.find()) {
            String p = longest(a.group(1));
            int k = p == null ? -1 : a.start() + 4 + p.length();
            while (k >= 0 && k < text.length() && text.charAt(k) == ' ') {
                k++;
            }
            if (p != null && isFn(params.get(p)) && k < text.length() && text.charAt(k) == '(') {
                return true;
            }
        }
        return false;
    }

    /** Whether a parameter takes text: its value at a use is the text as written (a literal), not an expression. */
    private static boolean isFn(Param p) {
        return p != null && "value".equals(p.kind()) && FN_TYPE.matcher(p.type()).matches();
    }

    /** The count of names a function type takes ({@code fn(number, text) -> number}: 2). */
    private static int arity(String type) {
        Matcher m = FN_TYPE.matcher(type);
        if (!m.matches() || m.group(1).isBlank()) {
            return 0;
        }
        return m.group(1).split(",").length;
    }

    /**
     * What is wrong with a function argument, or null: a lambda written in the user's context (a text {@code lambda a, b: body}) with the
     * type's count of names, whose body reads its names and the user's screen but not {@code @}, {@code ^} or {@code #index} (inside the
     * fragment they would be the fragment's row, not the user's).
     */
    private static String function(Param p, PNode v) {
        int n = arity(p.type());
        String names = n == 1 ? "lambda x: ..." : "lambda " + String.join(", ", List.of("a", "b", "c", "d", "e", "f", "g", "h").subList(0, Math.min(n, 8)))
                + ": ...";
        if (!(v.value() instanceof String s)) {
            return "is a function (" + p.type() + "): write " + names + ", not " + shown(v);
        }
        Matcher m = LAMBDA.matcher(s);
        if (!m.matches()) {
            return "is a function (" + p.type() + "): write " + names + ", not '" + s + "'";
        }
        int got = m.group(1).split(",").length;
        if (got != n) {
            return "is a function of " + n + (n == 1 ? " name" : " names") + " (" + p.type() + "); this lambda has " + got;
        }
        if (rowReads(m.group(2))) {
            return "is a function: its body reads its names and the screen, not @, ^ or #index (inside the fragment they are the fragment's"
                    + " rows, not yours): '" + s + "'";
        }
        return null;
    }

    /** Whether an expression reads {@code @} (a row or element), {@code ^} or {@code #index} outside its texts; {@code @<panel>} is fine. */
    private static boolean rowReads(String s) {
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '^' || c == '#' || c == '@' && (i + 1 >= s.length() || !Character.isLetter(s.charAt(i + 1)))) {
                return true;
            }
        }
        return false;
    }

    /** The {@code let} name a function parameter is bound to where the fragment calls it ({@code arg.warn-at(x)}: {@code fn_warn_at}). */
    private static String fnName(String param) {
        return "fn_" + param.replace('-', '_');
    }

    /** Whether a {@code text} parameter is declared {@code dynamic: true}: the use may write a template in its own context. */
    private static boolean dynamic(Param p) {
        PNode d = p.at() == null || !p.at().isMap() ? null : p.at().map().get("dynamic");
        return d != null && Boolean.TRUE.equals(d.value());
    }

    /** A template as one Rachana-EL expression: {@code concat} of its texts and its parts, each part's text form in its place. */
    private static String templateEl(String template) {
        List<String> parts = com.ash.rachana.el.FragmentColumns.templateParts(template);
        List<String> args = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            if (i % 2 == 1) {
                args.add("(" + parts.get(i) + ")");
            } else if (!parts.get(i).isEmpty()) {
                args.add(quoted(parts.get(i)));
            }
        }
        return "concat(" + String.join(", ", args) + ")";
    }

    private static boolean isText(Param p) {
        return "text".equals(p.type()) || "text?".equals(p.type());
    }

    /** A text as a Rachana-EL text literal (a backslash makes the next character literal, SPEC 4.2). */
    private static String quoted(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    /** A literal as Rachana-EL text: a number, {@code true}/{@code false}, {@code null} or a quoted text. */
    private static String el(PNode v) {
        if (v.isList()) {                                     // a list or a record written out: its Rachana-EL literal (level 1.1)
            List<String> items = new ArrayList<>();
            v.list().forEach(x -> items.add(el(x)));
            return "[" + String.join(", ", items) + "]";
        }
        if (v.isMap()) {
            List<String> fields = new ArrayList<>();
            v.map().forEach((k, x) -> fields.add(quoted(k) + ": " + el(x)));
            return "{" + String.join(", ", fields) + "}";
        }
        Object x = v.value();
        if (x == null) {
            return "null";
        }
        if (x instanceof String s) {
            return quoted(s);
        }
        return x.toString();
    }

    /**
     * What is wrong with a value for this parameter, or null: a name must be a token of its type; a value parameter's YAML number or flag
     * must fit the type and the bounds (a text at the use is an expression and is checked where it is compiled); a default is a literal.
     */
    static String literal(Param p, PNode v, boolean isDefault) {
        Object x = v.value();
        String base = p.type().endsWith("?") ? p.type().substring(0, p.type().length() - 1) : p.type();
        switch (p.kind()) {
            case "name" -> {
                if ("columns".equals(base)) {
                    return v.isList() ? null : "is a list of columns";
                }
                if (!(x instanceof String s)) {
                    return "is a " + base + " name, not " + shown(v);
                }
                return switch (base) {
                    case "tone" -> TONES.contains(s) ? null : "is a tone (" + String.join(", ", new java.util.TreeSet<>(TONES)) + "), not '" + s + "'";
                    case "enum" -> p.options().contains(s) ? null : "is one of " + String.join(", ", p.options()) + ", not '" + s + "'";
                    case "state" -> EffectsReader.ID.matcher(s).matches() ? null : "is the name of a state of the screen, not '" + s + "'";
                    case "key" -> FKEY.matcher(s).matches() ? null : "is a function key (F1-F12), not '" + s + "'";
                    default -> TOKEN.matcher(s).matches() ? null : "is a " + base + " name (a word), not '" + s + "'";
                };
            }
            case "table" -> {
                if (v.isMap()) {
                    return isDefault ? "is a default: a table parameter's default is an expression giving rows" : null;
                }
                return x instanceof String || x == null && !isDefault ? null : "is an expression giving rows, not " + shown(v);
            }
            default -> {
                if (isFn(p)) {
                    return function(p, v);
                }
                if (v.isMap() || v.isList()) {                 // a list or a record written out: checked field by field
                    if (!base.startsWith("list<") && !base.startsWith("record{")) {
                        return "is a value: write it as an expression";
                    }
                    String w = structure(base, v);
                    if (w == null && v.isList() && p.max() != null && v.list().size() > p.max().longValue()) {
                        w = "has at most " + p.max() + " items, not " + v.list().size();
                    }
                    if (w == null && v.isList() && p.min() != null && v.list().size() < p.min().longValue()) {
                        w = "has at least " + p.min() + " items, not " + v.list().size();
                    }
                    return w;
                }
                if (x == null) {
                    return p.type().endsWith("?") || !p.required() ? null : "is required and may not be empty";
                }
                if (x instanceof String) {
                    return isDefault && !("text".equals(base) || base.startsWith("date") || "duration".equals(base))
                            ? "is a literal of type " + base + ", not a text" : null;
                }
                boolean numeric = "number".equals(base) || "decimal".equals(base) || "money".equals(base) || "integer".equals(base);
                if (x instanceof Boolean) {
                    return "boolean".equals(base) ? null : "is of type " + base + ", not a true/false";
                }
                if (!(x instanceof Number num)) {
                    return "is of type " + base;
                }
                if (!numeric) {
                    return "is of type " + base + ", not a number";
                }
                if ("integer".equals(base) && !(x instanceof Long)) {
                    return "is a whole number, not " + x;
                }
                java.math.BigDecimal d = decimal(num);
                if (p.min() != null && d.compareTo(decimal(p.min())) < 0) {
                    return "is at least " + p.min() + ", not " + x;
                }
                if (p.max() != null && d.compareTo(decimal(p.max())) > 0) {
                    return "is at most " + p.max() + ", not " + x;
                }
                return null;
            }
        }
    }

    private static java.math.BigDecimal decimal(Number n) {
        return n instanceof Long l ? java.math.BigDecimal.valueOf(l) : new java.math.BigDecimal(n.toString());
    }

    private static String shown(PNode v) {
        return v.isMap() ? "a mapping" : v.isList() ? "a list" : v.value() == null ? "empty" : "'" + v.value() + "'";
    }

    private static Number number(PNode n) {
        return n != null && n.value() instanceof Number x ? x : null;
    }

    private static void walk(PNode n, java.util.function.Consumer<PNode> f) {
        if (n == null) {
            return;
        }
        f.accept(n);
        if (n.isMap()) {
            n.map().values().forEach(v -> walk(v, f));
        } else if (n.isList()) {
            n.list().forEach(v -> walk(v, f));
        }
    }

    private static String text(PNode n) {
        return n == null ? null : n.text();
    }

    /** A part's doc (SPEC 2.9, {@code $defs/docOf}): one line, or a doc block whose summary is its line. */
    static String docLine(PNode n) {
        return n != null && n.isMap() ? text(n.map().get("summary")) : text(n);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    static SutraProblem problem(String code, String message, String file, PNode at) {
        return new SutraProblem(code, message, new SourceLocation(file, at == null ? 1 : at.line(), at == null ? 1 : at.column()));
    }
}
