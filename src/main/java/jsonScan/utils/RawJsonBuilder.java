package jsonScan.utils;

import java.util.*;

/**
 * Manual JSON builder that supports non-standard features like duplicate keys,
 * arbitrary whitespace, and comment injection.
 */
public class RawJsonBuilder {
    private final StringBuilder json;
    private boolean isFirstEntry;
    private final Deque<ContainerType> containerStack;

    private enum ContainerType {
        OBJECT, ARRAY
    }

    public RawJsonBuilder() {
        this.json = new StringBuilder();
        this.containerStack = new ArrayDeque<>();
        this.isFirstEntry = true;
    }

    public RawJsonBuilder startObject() {
        addCommaIfNeeded();
        json.append("{");
        containerStack.push(ContainerType.OBJECT);
        isFirstEntry = true;
        return this;
    }

    public RawJsonBuilder endObject() {
        if (containerStack.isEmpty() || containerStack.peek() != ContainerType.OBJECT) {
            throw new IllegalStateException("No object to close");
        }
        json.append("}");
        containerStack.pop();
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder startArray() {
        addCommaIfNeeded();
        json.append("[");
        containerStack.push(ContainerType.ARRAY);
        isFirstEntry = true;
        return this;
    }

    public RawJsonBuilder endArray() {
        if (containerStack.isEmpty() || containerStack.peek() != ContainerType.ARRAY) {
            throw new IllegalStateException("No array to close");
        }
        json.append("]");
        containerStack.pop();
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder addKey(String key) {
        if (containerStack.isEmpty() || containerStack.peek() != ContainerType.OBJECT) {
            throw new IllegalStateException("Keys can only be added to objects");
        }
        addCommaIfNeeded();
        json.append("\"").append(escapeString(key)).append("\":");
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder addRawKey(String rawKey) {
        if (containerStack.isEmpty() || containerStack.peek() != ContainerType.OBJECT) {
            throw new IllegalStateException("Keys can only be added to objects");
        }
        addCommaIfNeeded();
        json.append("\"").append(rawKey).append("\":");
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder addStringValue(String value) {
        addCommaIfNeeded();
        json.append("\"").append(escapeString(value)).append("\"");
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder addRawStringValue(String rawValue) {
        addCommaIfNeeded();
        json.append("\"").append(rawValue).append("\"");
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder addNumberValue(Number value) {
        addCommaIfNeeded();
        json.append(value);
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder addRawNumberValue(String rawNumber) {
        addCommaIfNeeded();
        json.append(rawNumber);
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder addBooleanValue(boolean value) {
        addCommaIfNeeded();
        json.append(value);
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder addNullValue() {
        addCommaIfNeeded();
        json.append("null");
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder addRawJson(String rawJson) {
        addCommaIfNeeded();
        json.append(rawJson);
        isFirstEntry = false;
        return this;
    }

    public RawJsonBuilder addWhitespace(String whitespace) {
        json.append(whitespace);
        return this;
    }

    public RawJsonBuilder addComment(String comment, boolean multiline) {
        if (multiline) {
            json.append("/*").append(comment).append("*/");
        } else {
            json.append("//").append(comment).append("\n");
        }
        return this;
    }

    private void addCommaIfNeeded() {
        if (!isFirstEntry && !containerStack.isEmpty()) {
            json.append(",");
        }
    }

    private String escapeString(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    public String build() {
        if (!containerStack.isEmpty()) {
            throw new IllegalStateException("Unclosed containers: " + containerStack.size());
        }
        return json.toString();
    }

    @Override
    public String toString() {
        return json.toString();
    }

    // Helper method to encode a character as Unicode escape
    public static String unicodeEncode(char c) {
        return String.format("\\u%04X", (int) c);
    }

    // Helper method to encode entire string as Unicode
    public static String unicodeEncodeString(String str) {
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            result.append(unicodeEncode(c));
        }
        return result.toString();
    }
}
