package es.nexus.neurbia.extension.fc.asset_filter;

import java.util.*;
import java.util.stream.*;

public class CatalogQueryBuilder {

    // --- CONFIG ---
    static final String NS = "https://w3id.org/nexus/v0.0.1/ns/";
    static final String DS = "https://w3id.org/dspace/v0.8/";
    public static class ListCondition {
        public List<Condition> conditions;
    }

    // --- MODEL ---
    public static class Condition {
        public String type;      // SimpleObject | ArrayObject | SimpleArray | NestedObject
        public String name;      // e.g. "pack.name", "tags", "customObject.customPProperty2"
        public String operator;  // eq | contains (accepts "contanis" too)
        public String value;     // single value
        public String source;    // asset, catalog
    }

    // --- PUBLIC API ---
    public static String buildSql(List<Condition> conditions) {
        // Normalize and clean
        for (Condition c : conditions) {
            if (c.operator != null && c.operator.equalsIgnoreCase("contanis")) c.operator = "contains";
            if (c.operator != null) c.operator = c.operator.toLowerCase(Locale.ROOT);
            if (c.type != null) c.type = c.type.trim();
        }

        StringBuilder from = new StringBuilder();
        StringBuilder where = new StringBuilder("WHERE 1=1");

        // Base FROM + datasets expansion
        from.append(
                "FROM edc_federated_catalog c\n" +
                        "CROSS JOIN LATERAL json_array_elements(c.catalog->'datasets') AS d\n"
        );

        // Track which lateral joins were already added
        Set<String> addedAliases = new HashSet<>();

        // Helpers
        class Joins {
            void addArrayObjectJoins(String arrayName, String fieldName) {
                String arrAlias = arrayName + "_obj";
                String fieldAlias = fieldName + "_obj";
                if (!addedAliases.contains(arrAlias)) {
                    from.append("CROSS JOIN LATERAL json_array_elements(d->'properties'->'")
                            .append(ns(arrayName)).append("') AS ").append(arrAlias).append("\n");
                    addedAliases.add(arrAlias);
                }
                if (!addedAliases.contains(fieldAlias)) {
                    from.append("CROSS JOIN LATERAL json_array_elements(")
                            .append(arrAlias).append("->'")
                            .append(ns(fieldName)).append("') AS ").append(fieldAlias).append("\n");
                    addedAliases.add(fieldAlias);
                }
            }
        }
        Joins joins = new Joins();

        // Build WHERE
        for (Condition c : conditions) {
            String type = c.type == null ? "" : c.type;
            String op   = c.operator == null ? "eq" : c.operator;
            String val  = c.value;

            switch (type) {
                case "ArrayObject": {
                    // Expect "array.field" (e.g., pack.name) with language-tagged @value
                    String[] parts = split2(c.name);
                    String arrayName = parts[0], fieldName = parts[1];
                    String fieldAlias = fieldName + "_obj";
                    joins.addArrayObjectJoins(arrayName, fieldName);

                    if ("eq".equals(op)) {
                        where.append("\nAND ").append(fieldAlias).append("->>'@value' = ").append(q(val));
                    } else if ("contains".equals(op)) {
                        where.append("\nAND ").append(fieldAlias).append("->>'@value' ILIKE '%'||")
                                .append(q(val)).append("||'%'");
                    } else {
                        throw new IllegalArgumentException("Unsupported operator for ArrayObject: " + op);
                    }
                    break;
                }

                case "SimpleArray": {
                    // Array of strings (e.g., tags). Use jsonb membership '?|' with ARRAY[...]
                    // Single value still works as ARRAY['value']
                    List<String> arr = Arrays.stream(val.split(",")).toList();
                    String arrayLiteral = "ARRAY[" + arr.stream().map(CatalogQueryBuilder::q).collect(Collectors.joining(",")) + "]";
                    where.append("AND  jsonb_exists_all(\n" +
                                    "      (d->'properties'->'")
                            .append(ns(c.name)).append("')::jsonb,").append(arrayLiteral).append(")");
                    break;
                }

                case "NestedObject": {
                    // One-level nested object whose leaf is language-tagged array: -> 'obj' -> 'field' ->0->>'@value'
                    String[] parts = split2(c.name);
                    String obj = parts[0], field = parts[1];
                    if ("eq".equals(op)) {
                        where.append("\nAND d->'properties'->'")
                                .append(ns(obj)).append("'->'")
                                .append(ns(field)).append("'->0->>'@value' = ").append(q(val));
                    } else if ("contains".equals(op)) {
                        where.append("\nAND d->'properties'->'")
                                .append(ns(obj)).append("'->'")
                                .append(ns(field)).append("'->0->>'@value' ILIKE '%'||")
                                .append(q(val)).append("||'%'");
                    } else {
                        throw new IllegalArgumentException("Unsupported operator for NestedObject: " + op);
                    }
                    break;
                }

                case "SimpleObject": {
                        if(c.source.equals("asset")) {
                            if ("eq".equals(op)) {
                                where.append("\nAND d->'properties'->>'")
                                        .append(ns(c.name)).append("' = ").append(q(val));
                            } else if ("contains".equals(op)) {
                                where.append("\nAND d->'properties'->>'")
                                        .append(ns(c.name)).append("' ILIKE '%")
                                        .append(val).append("%'");
                            } else {
                                throw new IllegalArgumentException("Unsupported operator for SimpleObject: " + op);
                            }

                        }
                        if(c.source.equals("catalog")){
                            if ("eq".equals(op)) {
                                where.append("\nAND c.catalog ->'properties'->>'")
                                        .append(ds(c.name)).append("' = ").append(q(val));
                            }else if ("contains".equals(op)) {
                                where.append("\nAAND c.catalog ->'properties'->>'")
                                        .append(ds(c.name)).append("' ILIKE '%'||")
                                        .append(q(val)).append("||'%'");
                            }
                            else{
                                throw new IllegalArgumentException("Unsupported operator for SimpleObject: " + op);
                            }
                        }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported condition type: " + type);
            }
        }

        // Final query
        return ""
                + "SELECT DISTINCT ON (d->>'id',c.catalog -> 'properties'->>'https://w3id.org/dspace/v0.8/participantId')\n"
                + "  d->>'id' AS id,\n"
                + "  d AS asset,\n"
                + "  c.catalog -> 'properties'->>'https://w3id.org/dspace/v0.8/participantId' as participantId,\n"
                + "  c.catalog -> 'properties'->>'https://w3id.org/edc/v0.0.1/ns/originator' as originatorULR\n"
                + from
                + where
                + ";\n";
    }

    // --- UTIL ---
    private static String ns(String key) { return NS + key; }

    private static String ds(String key) { return DS + key;}
    // Safely quote a SQL string literal
    private static String q(String s) {
        if (s == null) return "NULL";
        return "'" + s.replace("'", "''") + "'";
    }

    private static String[] split2(String s) {
        String[] parts = s.split("\\.", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Expected 'a.b' got: " + s);
        return parts;
    }


}
