package jsonScan.utils;

import com.google.gson.*;
import java.util.*;

/**
 * Utility class for parsing and analyzing JSON structures
 */
public class JsonParser {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    /**
     * Parse JSON string to JsonElement
     */
    public static JsonElement parse(String jsonString) {
        try {
            return gson.fromJson(jsonString, JsonElement.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    /**
     * Check if string is valid JSON
     */
    public static boolean isValidJson(String jsonString) {
        return parse(jsonString) != null;
    }

    /**
     * Find all string values in a JSON structure
     */
    public static List<KeyValuePair> findAllStrings(JsonElement element, String path) {
        List<KeyValuePair> results = new ArrayList<>();
        findStringsRecursive(element, path, results);
        return results;
    }

    private static void findStringsRecursive(JsonElement element, String path, List<KeyValuePair> results) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            results.add(new KeyValuePair(path, element.getAsString()));
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String newPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                findStringsRecursive(entry.getValue(), newPath, results);
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = path + "[" + i + "]";
                findStringsRecursive(arr.get(i), newPath, results);
            }
        }
    }

    /**
     * Find all number values in a JSON structure
     */
    public static List<KeyValuePair> findAllNumbers(JsonElement element, String path) {
        List<KeyValuePair> results = new ArrayList<>();
        findNumbersRecursive(element, path, results);
        return results;
    }

    private static void findNumbersRecursive(JsonElement element, String path, List<KeyValuePair> results) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            results.add(new KeyValuePair(path, element.getAsJsonPrimitive().getAsNumber()));
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String newPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                findNumbersRecursive(entry.getValue(), newPath, results);
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = path + "[" + i + "]";
                findNumbersRecursive(arr.get(i), newPath, results);
            }
        }
    }

    /**
     * Find all boolean values in a JSON structure
     */
    public static List<KeyValuePair> findAllBooleans(JsonElement element, String path) {
        List<KeyValuePair> results = new ArrayList<>();
        findBooleansRecursive(element, path, results);
        return results;
    }

    private static void findBooleansRecursive(JsonElement element, String path, List<KeyValuePair> results) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            results.add(new KeyValuePair(path, element.getAsJsonPrimitive().getAsBoolean()));
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String newPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                findBooleansRecursive(entry.getValue(), newPath, results);
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = path + "[" + i + "]";
                findBooleansRecursive(arr.get(i), newPath, results);
            }
        }
    }

    /**
     * Find all object keys in a JSON structure
     */
    public static List<String> findAllKeys(JsonElement element) {
        List<String> keys = new ArrayList<>();
        findKeysRecursive(element, keys);
        return keys;
    }

    private static void findKeysRecursive(JsonElement element, List<String> keys) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            keys.addAll(obj.keySet());
            for (JsonElement child : obj.entrySet().stream().map(Map.Entry::getValue).toArray(JsonElement[]::new)) {
                findKeysRecursive(child, keys);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                findKeysRecursive(child, keys);
            }
        }
    }

    /**
     * Get nesting depth of JSON structure
     */
    public static int getDepth(JsonElement element) {
        return getDepthRecursive(element, 0);
    }

    private static int getDepthRecursive(JsonElement element, int currentDepth) {
        if (element.isJsonObject()) {
            int maxDepth = currentDepth;
            for (JsonElement child : element.getAsJsonObject().entrySet().stream()
                    .map(Map.Entry::getValue).toArray(JsonElement[]::new)) {
                maxDepth = Math.max(maxDepth, getDepthRecursive(child, currentDepth + 1));
            }
            return maxDepth;
        } else if (element.isJsonArray()) {
            int maxDepth = currentDepth;
            for (JsonElement child : element.getAsJsonArray()) {
                maxDepth = Math.max(maxDepth, getDepthRecursive(child, currentDepth + 1));
            }
            return maxDepth;
        }
        return currentDepth;
    }

    /**
     * Pretty print JSON
     */
    public static String prettyPrint(String jsonString) {
        JsonElement element = parse(jsonString);
        if (element == null) {
            return jsonString;
        }
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        return prettyGson.toJson(element);
    }

    /**
     * Helper class to store key-value pairs with path
     */
    public static class KeyValuePair {
        private final String path;
        private final Object value;

        public KeyValuePair(String path, Object value) {
            this.path = path;
            this.value = value;
        }

        public String getPath() {
            return path;
        }

        public Object getValue() {
            return value;
        }

        public String getKey() {
            if (path.contains(".")) {
                return path.substring(path.lastIndexOf(".") + 1);
            }
            return path.replace("[", "").replace("]", "");
        }
    }
}
