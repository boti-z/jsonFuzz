package jsonScan.models;

/**
 * Enum representing different categories of JSON fuzzing tests
 */
public enum TestCategory {
    STRING_VARIATIONS("String Variations", "Unicode encoding, null bytes, type confusion"),
    NUMBER_EDGE_CASES("Number Edge Cases", "Large floats, scientific notation, integer boundaries"),
    DUPLICATE_KEYS("Duplicate Keys", "Key duplication with various techniques"),
    UNICODE_ATTACKS("Unicode Attacks", "BOM, surrogates, character truncation"),
    COMMENT_INJECTION("Comment Injection", "Single and multi-line comment insertion"),
    WHITESPACE_VARIATIONS("Whitespace Variations", "Tabs, newlines, excessive whitespace"),
    DEEP_NESTING("Deep Nesting", "Test parser depth limits (⚠️ DoS Risk)"),
    ARRAY_OBJECT_DUPLICATION("Array/Object Duplication", "Duplicate arrays and objects"),
    TYPE_CONFUSION("Type Confusion", "Test type conversion vulnerabilities across parsers"),
    SPECIAL_NUMBERS("Special Numbers", "Infinity, negative zero, hexadecimal, octal, binary"),
    UNICODE_NORMALIZATION("Unicode Normalization", "NFC/NFD forms, homograph attacks, zero-width chars"),
    LARGE_PAYLOADS("Large Payloads", "Very long strings to test parser limits (⚠️ DoS Risk)");

    private final String displayName;
    private final String description;

    TestCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
