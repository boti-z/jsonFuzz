package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.PlaceholderConfig;
import jsonScan.models.TestCategory;
import jsonScan.utils.JsonParser;
import jsonScan.utils.RawJsonBuilder;

import java.util.*;

/**
 * Generates string-based test permutations including:
 * - Unicode encoding variations
 * - Null byte injection
 * - Unpaired surrogates
 * - Type confusion (wrapping in arrays/objects)
 */
public class StringTestGenerator implements TestGenerator {
    private int permutationCounter = 0;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();
        List<JsonParser.KeyValuePair> stringValues = JsonParser.findAllStrings(jsonElement, "");

        for (JsonParser.KeyValuePair kvp : stringValues) {
            String stringValue = (String) kvp.getValue();
            String path = kvp.getPath();

            permutations.add(createPermutation(originalJson, jsonElement, path, stringValue,
                    stringValue + "\\ud888", "Unpaired surrogate at end"));

            if (!stringValue.isEmpty()) {
                String lastChar = stringValue.substring(stringValue.length() - 1);
                String unicodeLastChar = RawJsonBuilder.unicodeEncode(lastChar.charAt(0));
                String mutated = stringValue.substring(0, stringValue.length() - 1) + unicodeLastChar;
                permutations.add(createPermutation(originalJson, jsonElement, path, stringValue,
                        mutated, "Unicode encode last character"));
            }

            permutations.add(createPermutation(originalJson, jsonElement, path, stringValue,
                    stringValue + "\\u0000ef", "Null byte injection"));

            permutations.add(createArrayWrapper(originalJson, jsonElement, path, stringValue,
                    "Wrap string in array"));

            permutations.add(createObjectWrapper(originalJson, jsonElement, path, stringValue,
                    "Wrap string in object"));

            permutations.add(createArrayWithExtra(originalJson, jsonElement, path, stringValue,
                    placeholderConfig.getStringPlaceholder(), "Array with extra value"));

            if (stringValue.length() > 2) {
                int midPoint = stringValue.length() / 2;
                String firstPart = stringValue.substring(0, midPoint);
                String secondPart = stringValue.substring(midPoint);
                String unicodedSecond = RawJsonBuilder.unicodeEncodeString(secondPart);
                permutations.add(createPermutation(originalJson, jsonElement, path, stringValue,
                        firstPart + unicodedSecond, "Partial Unicode encoding"));
            }

            permutations.add(createArrayWithBoolean(originalJson, jsonElement, path, stringValue, true,
                    "Array with boolean true"));

            permutations.add(createArrayWithBoolean(originalJson, jsonElement, path, stringValue, false,
                    "Array with boolean false"));

            permutations.add(createArrayWithNull(originalJson, jsonElement, path, stringValue,
                    "Array with null"));

            permutations.add(createPermutation(originalJson, jsonElement, path, stringValue,
                    "\\u005C" + stringValue, "Backslash as Unicode prefix"));

            permutations.add(createPermutation(originalJson, jsonElement, path, stringValue,
                    stringValue + "\\u00", "Incomplete Unicode escape"));

            permutations.add(createPermutation(originalJson, jsonElement, path, stringValue,
                    "\uFEFF" + stringValue, "BOM prefix"));

            permutations.add(createPermutation(originalJson, jsonElement, path, stringValue,
                    stringValue + "\"", "Stray quote at end"));

            permutations.add(createPermutation(originalJson, jsonElement, path, stringValue,
                    stringValue + "\\r", "Carriage return at end"));
        }

        return permutations;
    }

    private JsonPermutation createPermutation(String originalJson, JsonElement jsonElement,
                                             String path, String oldValue, String newValue, String description) {
        String mutated = replaceValueAtPath(jsonElement, path, oldValue, newValue);
        return new JsonPermutation(permutationCounter++, originalJson, mutated,
                getCategory(), description + " (path: " + path + ")");
    }

    private JsonPermutation createArrayWrapper(String originalJson, JsonElement jsonElement,
                                              String path, String value, String description) {
        String mutated = wrapInArray(jsonElement, path, value);
        return new JsonPermutation(permutationCounter++, originalJson, mutated,
                getCategory(), description + " (path: " + path + ")");
    }

    private JsonPermutation createObjectWrapper(String originalJson, JsonElement jsonElement,
                                               String path, String value, String description) {
        String mutated = wrapInObject(jsonElement, path, value);
        return new JsonPermutation(permutationCounter++, originalJson, mutated,
                getCategory(), description + " (path: " + path + ")");
    }

    private JsonPermutation createArrayWithExtra(String originalJson, JsonElement jsonElement,
                                                String path, String value, String extraValue, String description) {
        String mutated = wrapInArrayWithExtra(jsonElement, path, value, extraValue);
        return new JsonPermutation(permutationCounter++, originalJson, mutated,
                getCategory(), description + " (path: " + path + ")");
    }

    private String replaceValueAtPath(JsonElement element, String path, String oldValue, String newValue) {
        JsonElement modified = deepCopy(element);
        replaceAtPathRecursive(modified, path, oldValue, newValue, "");
        return GSON.toJson(modified);
    }

    private boolean replaceAtPathRecursive(JsonElement element, String targetPath, String oldValue,
                                           String newValue, String currentPath) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();

                if (newPath.equals(targetPath) && entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isString()
                        && entry.getValue().getAsString().equals(oldValue)) {
                    obj.addProperty(entry.getKey(), newValue);
                    return true;
                }

                if (replaceAtPathRecursive(entry.getValue(), targetPath, oldValue, newValue, newPath)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                JsonElement child = arr.get(i);

                if (newPath.equals(targetPath) && child.isJsonPrimitive()
                        && child.getAsJsonPrimitive().isString()
                        && child.getAsString().equals(oldValue)) {
                    arr.set(i, new JsonPrimitive(newValue));
                    return true;
                }

                if (replaceAtPathRecursive(child, targetPath, oldValue, newValue, newPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String wrapInArray(JsonElement element, String path, String value) {
        JsonElement modified = deepCopy(element);
        wrapInArrayAtPath(modified, path, value, "");
        return GSON.toJson(modified);
    }

    private boolean wrapInArrayAtPath(JsonElement element, String targetPath, String value, String currentPath) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();

                if (newPath.equals(targetPath) && entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isString()
                        && entry.getValue().getAsString().equals(value)) {
                    JsonArray arr = new JsonArray();
                    arr.add(value);
                    obj.add(entry.getKey(), arr);
                    return true;
                }

                if (wrapInArrayAtPath(entry.getValue(), targetPath, value, newPath)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                JsonElement child = arr.get(i);

                if (newPath.equals(targetPath) && child.isJsonPrimitive()
                        && child.getAsJsonPrimitive().isString()
                        && child.getAsString().equals(value)) {
                    JsonArray wrapper = new JsonArray();
                    wrapper.add(value);
                    arr.set(i, wrapper);
                    return true;
                }

                if (wrapInArrayAtPath(child, targetPath, value, newPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String wrapInObject(JsonElement element, String path, String value) {
        JsonElement modified = deepCopy(element);
        wrapInObjectAtPath(modified, path, value, "");
        return GSON.toJson(modified);
    }

    private boolean wrapInObjectAtPath(JsonElement element, String targetPath, String value, String currentPath) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();

                if (newPath.equals(targetPath) && entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isString()
                        && entry.getValue().getAsString().equals(value)) {
                    JsonObject nested = new JsonObject();
                    nested.addProperty(entry.getKey(), value);
                    obj.add(entry.getKey(), nested);
                    return true;
                }

                if (wrapInObjectAtPath(entry.getValue(), targetPath, value, newPath)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                JsonElement child = arr.get(i);

                if (newPath.equals(targetPath) && child.isJsonPrimitive()
                        && child.getAsJsonPrimitive().isString()
                        && child.getAsString().equals(value)) {
                    JsonObject wrapper = new JsonObject();
                    String key = extractKeyFromPath(newPath);
                    wrapper.addProperty(key, value);
                    arr.set(i, wrapper);
                    return true;
                }

                if (wrapInObjectAtPath(child, targetPath, value, newPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String wrapInArrayWithExtra(JsonElement element, String path, String value, String extraValue) {
        JsonElement modified = deepCopy(element);
        wrapInArrayWithExtraAtPath(modified, path, value, extraValue, "");
        return GSON.toJson(modified);
    }

    private boolean wrapInArrayWithExtraAtPath(JsonElement element, String targetPath, String value,
                                                String extraValue, String currentPath) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();

                if (newPath.equals(targetPath) && entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isString()
                        && entry.getValue().getAsString().equals(value)) {
                    JsonArray arr = new JsonArray();
                    arr.add(value);
                    arr.add(extraValue);
                    obj.add(entry.getKey(), arr);
                    return true;
                }

                if (wrapInArrayWithExtraAtPath(entry.getValue(), targetPath, value, extraValue, newPath)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                JsonElement child = arr.get(i);

                if (newPath.equals(targetPath) && child.isJsonPrimitive()
                        && child.getAsJsonPrimitive().isString()
                        && child.getAsString().equals(value)) {
                    JsonArray wrapper = new JsonArray();
                    wrapper.add(value);
                    wrapper.add(extraValue);
                    arr.set(i, wrapper);
                    return true;
                }

                if (wrapInArrayWithExtraAtPath(child, targetPath, value, extraValue, newPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private JsonPermutation createArrayWithBoolean(String originalJson, JsonElement jsonElement,
                                                   String path, String value, boolean boolValue, String description) {
        String mutated = wrapInArrayWithBoolean(jsonElement, path, value, boolValue);
        return new JsonPermutation(permutationCounter++, originalJson, mutated,
                getCategory(), description + " (path: " + path + ")");
    }

    private JsonPermutation createArrayWithNull(String originalJson, JsonElement jsonElement,
                                                String path, String value, String description) {
        String mutated = wrapInArrayWithNull(jsonElement, path, value);
        return new JsonPermutation(permutationCounter++, originalJson, mutated,
                getCategory(), description + " (path: " + path + ")");
    }

    private String wrapInArrayWithBoolean(JsonElement element, String path, String value, boolean boolValue) {
        JsonElement modified = deepCopy(element);
        wrapInArrayWithBooleanAtPath(modified, path, value, boolValue, "");
        return GSON.toJson(modified);
    }

    private boolean wrapInArrayWithBooleanAtPath(JsonElement element, String targetPath, String value,
                                                  boolean boolValue, String currentPath) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();

                if (newPath.equals(targetPath) && entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isString()
                        && entry.getValue().getAsString().equals(value)) {
                    JsonArray arr = new JsonArray();
                    arr.add(value);
                    arr.add(boolValue);
                    obj.add(entry.getKey(), arr);
                    return true;
                }

                if (wrapInArrayWithBooleanAtPath(entry.getValue(), targetPath, value, boolValue, newPath)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                JsonElement child = arr.get(i);

                if (newPath.equals(targetPath) && child.isJsonPrimitive()
                        && child.getAsJsonPrimitive().isString()
                        && child.getAsString().equals(value)) {
                    JsonArray wrapper = new JsonArray();
                    wrapper.add(value);
                    wrapper.add(boolValue);
                    arr.set(i, wrapper);
                    return true;
                }

                if (wrapInArrayWithBooleanAtPath(child, targetPath, value, boolValue, newPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String wrapInArrayWithNull(JsonElement element, String path, String value) {
        JsonElement modified = deepCopy(element);
        wrapInArrayWithNullAtPath(modified, path, value, "");
        return GSON.toJson(modified);
    }

    private boolean wrapInArrayWithNullAtPath(JsonElement element, String targetPath, String value, String currentPath) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();

                if (newPath.equals(targetPath) && entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isString()
                        && entry.getValue().getAsString().equals(value)) {
                    JsonArray arr = new JsonArray();
                    arr.add(value);
                    arr.add(JsonNull.INSTANCE);
                    obj.add(entry.getKey(), arr);
                    return true;
                }

                if (wrapInArrayWithNullAtPath(entry.getValue(), targetPath, value, newPath)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                JsonElement child = arr.get(i);

                if (newPath.equals(targetPath) && child.isJsonPrimitive()
                        && child.getAsJsonPrimitive().isString()
                        && child.getAsString().equals(value)) {
                    JsonArray wrapper = new JsonArray();
                    wrapper.add(value);
                    wrapper.add(JsonNull.INSTANCE);
                    arr.set(i, wrapper);
                    return true;
                }

                if (wrapInArrayWithNullAtPath(child, targetPath, value, newPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractKeyFromPath(String path) {
        String cleaned = path.replaceAll("\\[\\d+\\]", "");
        if (cleaned.contains(".")) {
            return cleaned.substring(cleaned.lastIndexOf(".") + 1);
        }
        return cleaned.isEmpty() ? "value" : cleaned;
    }

    private JsonElement deepCopy(JsonElement element) {
        return GSON.fromJson(GSON.toJson(element), JsonElement.class);
    }

    @Override
    public TestCategory getCategory() {
        return TestCategory.STRING_VARIATIONS;
    }
}
