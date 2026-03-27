package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.PlaceholderConfig;
import jsonScan.models.TestCategory;
import jsonScan.utils.JsonParser;

import java.util.*;

public class TypeConfusionGenerator implements TestGenerator {
    private int permutationCounter = 0;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();

        List<JsonParser.KeyValuePair> allPrimitives = new ArrayList<>();
        allPrimitives.addAll(JsonParser.findAllStrings(jsonElement, ""));
        allPrimitives.addAll(JsonParser.findAllNumbers(jsonElement, ""));
        allPrimitives.addAll(JsonParser.findAllBooleans(jsonElement, ""));

        for (JsonParser.KeyValuePair kvp : allPrimitives) {
            String path = kvp.getPath();
            Object value = kvp.getValue();

            permutations.add(replaceWithNaN(originalJson, jsonElement, path, value));
            permutations.add(replaceWithUnquotedNaN(originalJson, path, value));
            permutations.add(replaceWithNull(originalJson, jsonElement, path, value));
            permutations.add(replaceWithTrue(originalJson, jsonElement, path, value));
            permutations.add(replaceWithFalse(originalJson, jsonElement, path, value));

            if (value instanceof Number) {
                permutations.add(convertNumberToString(originalJson, jsonElement, path, (Number) value));
            } else if (value instanceof String) {
                String strValue = (String) value;
                if (isNumericString(strValue)) {
                    permutations.add(convertStringToNumber(originalJson, jsonElement, path, strValue));
                } else {
                    permutations.add(convertNonNumericStringToNumber(originalJson, jsonElement, path));
                }
            } else if (value instanceof Boolean) {
                permutations.add(convertBooleanToString(originalJson, jsonElement, path, (Boolean) value));
            }
        }

        return permutations;
    }

    private JsonPermutation replaceWithNaN(String originalJson, JsonElement jsonElement, String path, Object value) {
        JsonElement modified = deepCopy(jsonElement);
        replaceAtPath(modified, path, new JsonPrimitive("NaN"), "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Replace with NaN string (path: " + path + ")");
    }

    private JsonPermutation replaceWithUnquotedNaN(String originalJson, String path, Object value) {
        String mutated = replaceValueWithRawText(originalJson, path, value, "NaN");
        return new JsonPermutation(permutationCounter++, originalJson, mutated,
                getCategory(), "Replace with unquoted NaN (path: " + path + ")");
    }

    private String replaceValueWithRawText(String originalJson, String path, Object value, String rawText) {
        String searchPattern;

        if (value instanceof String) {
            searchPattern = "\"" + escapeJsonString((String) value) + "\"";
        } else if (value instanceof Number) {
            searchPattern = value.toString();
        } else if (value instanceof Boolean) {
            searchPattern = value.toString();
        } else {
            return originalJson;
        }

        String keyPattern = extractKeyFromPath(path);
        if (keyPattern != null && !keyPattern.isEmpty()) {
            String fullPattern = "\"" + keyPattern + "\"\\s*:\\s*" + searchPattern;
            String replacement = "\"" + keyPattern + "\": " + rawText;
            return originalJson.replaceFirst(fullPattern, replacement);
        }

        return originalJson;
    }

    private String extractKeyFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String cleaned = path.replaceAll("\\[\\d+\\]", "");
        if (cleaned.contains(".")) {
            return cleaned.substring(cleaned.lastIndexOf(".") + 1);
        }
        return cleaned;
    }

    private String escapeJsonString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private JsonPermutation replaceWithNull(String originalJson, JsonElement jsonElement, String path, Object value) {
        JsonElement modified = deepCopy(jsonElement);
        replaceAtPath(modified, path, JsonNull.INSTANCE, "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Replace with null (path: " + path + ")");
    }

    private JsonPermutation replaceWithTrue(String originalJson, JsonElement jsonElement, String path, Object value) {
        JsonElement modified = deepCopy(jsonElement);
        replaceAtPath(modified, path, new JsonPrimitive(true), "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Replace with true (path: " + path + ")");
    }

    private JsonPermutation replaceWithFalse(String originalJson, JsonElement jsonElement, String path, Object value) {
        JsonElement modified = deepCopy(jsonElement);
        replaceAtPath(modified, path, new JsonPrimitive(false), "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Replace with false (path: " + path + ")");
    }

    private JsonPermutation convertNumberToString(String originalJson, JsonElement jsonElement, String path, Number value) {
        JsonElement modified = deepCopy(jsonElement);
        replaceAtPath(modified, path, new JsonPrimitive(value.toString()), "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Convert number to string (path: " + path + ")");
    }

    private JsonPermutation convertStringToNumber(String originalJson, JsonElement jsonElement, String path, String value) {
        JsonElement modified = deepCopy(jsonElement);
        try {
            if (value.contains(".")) {
                replaceAtPath(modified, path, new JsonPrimitive(Double.parseDouble(value)), "");
            } else {
                replaceAtPath(modified, path, new JsonPrimitive(Long.parseLong(value)), "");
            }
        } catch (NumberFormatException e) {
            replaceAtPath(modified, path, new JsonPrimitive(0), "");
        }
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Convert numeric string to number (path: " + path + ")");
    }

    private JsonPermutation convertNonNumericStringToNumber(String originalJson, JsonElement jsonElement, String path) {
        JsonElement modified = deepCopy(jsonElement);
        replaceAtPath(modified, path, new JsonPrimitive(0), "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Convert non-numeric string to 0 (path: " + path + ")");
    }

    private JsonPermutation convertBooleanToString(String originalJson, JsonElement jsonElement, String path, Boolean value) {
        JsonElement modified = deepCopy(jsonElement);
        replaceAtPath(modified, path, new JsonPrimitive(value.toString()), "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Convert boolean to string (path: " + path + ")");
    }

    private boolean isNumericString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean replaceAtPath(JsonElement element, String targetPath, JsonElement newValue, String currentPath) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();

                if (newPath.equals(targetPath)) {
                    obj.add(entry.getKey(), newValue);
                    return true;
                }

                if (replaceAtPath(entry.getValue(), targetPath, newValue, newPath)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = currentPath + "[" + i + "]";

                if (newPath.equals(targetPath)) {
                    arr.set(i, newValue);
                    return true;
                }

                if (replaceAtPath(arr.get(i), targetPath, newValue, newPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private JsonElement deepCopy(JsonElement element) {
        return GSON.fromJson(GSON.toJson(element), JsonElement.class);
    }

    @Override
    public TestCategory getCategory() {
        return TestCategory.TYPE_CONFUSION;
    }
}
