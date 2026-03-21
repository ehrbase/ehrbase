package org.ehrbase.schemagen;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/**
 * Converts openEHR archetype paths and WebTemplate node IDs to valid PostgreSQL identifiers.
 */
public final class ColumnNamer {

    private static final int PG_IDENTIFIER_MAX_LENGTH = 63;

    private static final Set<String> SQL_RESERVED = Set.of(
            "all", "analyse", "analyze", "and", "any", "array", "as", "asc", "asymmetric",
            "authorization", "between", "binary", "both", "case", "cast", "check", "collate",
            "column", "constraint", "create", "cross", "current", "default", "deferrable",
            "desc", "distinct", "do", "else", "end", "except", "false", "fetch", "for",
            "foreign", "from", "grant", "group", "having", "in", "initially", "inner",
            "insert", "intersect", "into", "is", "join", "lateral", "leading", "left",
            "like", "limit", "localtime", "localtimestamp", "natural", "new", "not", "null",
            "offset", "old", "on", "only", "or", "order", "outer", "overlaps", "placing",
            "primary", "references", "returning", "right", "select", "session_user", "similar",
            "some", "symmetric", "table", "tablesample", "then", "to", "trailing", "true",
            "union", "unique", "user", "using", "variadic", "verbose", "when", "where",
            "window", "with", "abort", "access", "action", "add", "admin", "after", "aggregate",
            "also", "alter", "always", "assertion", "assignment", "at", "attribute", "backward",
            "before", "begin", "by", "cache", "called", "cascade", "chain", "characteristics",
            "checkpoint", "class", "close", "cluster", "comment", "commit", "committed",
            "configuration", "connection", "constraints", "content", "continue", "conversion",
            "copy", "cost", "csv", "current_date", "current_role", "current_time",
            "current_timestamp", "current_user", "cursor", "cycle", "data", "database", "day",
            "deallocate", "declare", "defaults", "deferred", "definer", "delete", "delimiter",
            "delimiters", "disable", "discard", "domain", "double", "drop", "each", "enable",
            "encoding", "encrypted", "enum", "escape", "event", "execute", "exists",
            "explain", "extension", "external", "extract", "filter", "first", "float",
            "following", "force", "forward", "function", "functions", "generated", "global",
            "granted", "greatest", "handler", "header", "hold", "hour", "identity", "if",
            "immediate", "immutable", "implicit", "import", "include", "including",
            "increment", "index", "indexes", "inherit", "inherits", "inline", "input",
            "instead", "int", "integer", "interval", "invoker", "isolation", "key",
            "label", "language", "large", "last", "least", "level", "listen", "load",
            "local", "location", "lock", "logged", "mapping", "match", "materialized",
            "maxvalue", "method", "minute", "minvalue", "mode", "month", "move", "name",
            "names", "next", "no", "none", "nothing", "notify", "nowait", "nullif",
            "nulls", "object", "of", "off", "oids", "operator", "option", "options",
            "ordinality", "out", "over", "owned", "owner", "parallel", "parser", "partial",
            "partition", "passing", "password", "plans", "policy", "position", "preceding",
            "prepare", "prepared", "preserve", "prior", "privileges", "procedural",
            "procedure", "program", "publication", "quote", "range", "read", "reassign",
            "recheck", "recursive", "ref", "referencing", "refresh", "reindex", "relative",
            "release", "rename", "repeatable", "replace", "replica", "reset", "restart",
            "restrict", "return", "returns", "revoke", "role", "rollback", "routine",
            "row", "rows", "rule", "savepoint", "schema", "scroll", "search", "second",
            "security", "sequence", "sequences", "serializable", "server", "session",
            "set", "share", "show", "simple", "skip", "snapshot", "sql", "stable",
            "standalone", "start", "statement", "statistics", "stdin", "stdout", "storage",
            "strict", "strip", "subscription", "support", "sysid", "system", "tables",
            "temp", "template", "temporary", "text", "time", "timestamp", "transaction",
            "transform", "treat", "trigger", "trim", "truncate", "trusted", "type",
            "types", "unbounded", "uncommitted", "unencrypted", "unknown", "unlisten",
            "unlogged", "until", "update", "vacuum", "valid", "validate", "validator",
            "value", "values", "varying", "version", "view", "views", "volatile",
            "whitespace", "without", "work", "wrapper", "write", "xml", "xmlattributes",
            "xmlconcat", "xmlelement", "xmlexists", "xmlforest", "xmlnamespaces",
            "xmlparse", "xmlpi", "xmlroot", "xmlserialize", "xmltable", "year", "yes", "zone"
    );

    private ColumnNamer() {}

    /**
     * Converts a WebTemplate node ID to a valid PostgreSQL column name.
     */
    public static String toColumnName(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "unnamed";
        }

        String sanitized = nodeId.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceFirst("^_", "")
                .replaceFirst("_$", "");

        if (sanitized.isEmpty()) {
            sanitized = "col";
        }

        if (SQL_RESERVED.contains(sanitized)) {
            sanitized = sanitized + "_val";
        }

        if (sanitized.length() > PG_IDENTIFIER_MAX_LENGTH) {
            String hash = shortHash(sanitized);
            sanitized = sanitized.substring(0, PG_IDENTIFIER_MAX_LENGTH - hash.length() - 1) + "_" + hash;
        }

        return sanitized;
    }

    /**
     * Generates a PostgreSQL table name from RM type and template ID.
     * Format: {rm_prefix}_{template_concept}
     */
    public static String toTableName(String rmType, String templateId) {
        String prefix = switch (rmType) {
            case "OBSERVATION" -> "obs";
            case "EVALUATION" -> "eval";
            case "INSTRUCTION" -> "instr";
            case "ACTION" -> "act";
            case "ADMIN_ENTRY" -> "admin";
            case "COMPOSITION" -> "comp";
            default -> rmType.toLowerCase().substring(0, Math.min(4, rmType.length()));
        };

        String concept = templateId.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceFirst("^_", "")
                .replaceFirst("_$", "");

        String fullName = prefix + "_" + concept;

        if (fullName.length() > PG_IDENTIFIER_MAX_LENGTH) {
            String hash = shortHash(fullName);
            fullName = fullName.substring(0, PG_IDENTIFIER_MAX_LENGTH - hash.length() - 1) + "_" + hash;
        }

        return fullName;
    }

    private static String shortHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 4);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
