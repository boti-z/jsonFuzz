package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.TestCategory;
import jsonScan.utils.JsonParser;

import java.util.*;

/**
 * Generates Unicode-specific test permutations:
 * - BOM insertion
 * - Surrogate pairs
 * - Character truncation vectors
 * - Stray backslashes and quotes
 * - Normalization attacks
 */
public class UnicodeTestGenerator implements TestGenerator {
    private int permutationCounter = 0;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, jsonScan.models.PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();
        List<JsonParser.KeyValuePair> stringValues = JsonParser.findAllStrings(jsonElement, "");

        for (JsonParser.KeyValuePair kvp : stringValues) {
            String stringValue = (String) kvp.getValue();
            String path = kvp.getPath();

            String mutated1 = replaceStringAtPath(jsonElement, path, stringValue, stringValue + "\\ud800");
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated1,
                    getCategory(), "Unpaired surrogate \\ud800 (path: " + path + ")"));

            String mutated2 = replaceStringAtPath(jsonElement, path, stringValue, stringValue + "\\ud888");
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated2,
                    getCategory(), "Unpaired surrogate \\ud888 (path: " + path + ")"));

            String mutated3 = replaceStringAtPath(jsonElement, path, stringValue, stringValue + "\\\\");
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated3,
                    getCategory(), "Stray backslash (path: " + path + ")"));

            String mutated4 = replaceStringAtPath(jsonElement, path, stringValue, "\\uFEFF" + stringValue);
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated4,
                    getCategory(), "BOM at start (path: " + path + ")"));

            String mutated5 = replaceStringAtPath(jsonElement, path, stringValue, stringValue + "\\u00");
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated5,
                    getCategory(), "Incomplete Unicode escape (path: " + path + ")"));

            String mutated6 = replaceStringAtPath(jsonElement, path, stringValue, stringValue + "\\u005C");
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated6,
                    getCategory(), "Backslash as Unicode (path: " + path + ")"));

            String mutated7 = replaceStringAtPath(jsonElement, path, stringValue, stringValue + "\\u0000ef");
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated7,
                    getCategory(), "Null byte (path: " + path + ")"));
        }

        return permutations;
    }

    private String replaceStringAtPath(JsonElement element, String targetPath, String oldValue, String newValue) {
        JsonElement modified = deepCopy(element);
        replaceAtPathRecursive(modified, targetPath, oldValue, newValue, "");
        return GSON.toJson(modified);
    }

    private boolean replaceAtPathRecursive(JsonElement element, String targetPath, String oldValue,
                                          String newValue, String currentPath) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
                String key = entry.getKey();
                String newPath = currentPath.isEmpty() ? key : currentPath + "." + key;
                JsonElement value = entry.getValue();

                if (newPath.equals(targetPath) && value.isJsonPrimitive() &&
                    value.getAsJsonPrimitive().isString()) {
                    if (value.getAsString().equals(oldValue)) {
                        obj.addProperty(key, newValue);
                        return true;
                    }
                }

                if (replaceAtPathRecursive(value, targetPath, oldValue, newValue, newPath)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = currentPath + "[" + i + "]";
                JsonElement value = arr.get(i);

                if (newPath.equals(targetPath) && value.isJsonPrimitive() &&
                    value.getAsJsonPrimitive().isString()) {
                    if (value.getAsString().equals(oldValue)) {
                        arr.set(i, new JsonPrimitive(newValue));
                        return true;
                    }
                }

                if (replaceAtPathRecursive(value, targetPath, oldValue, newValue, newPath)) {
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
        return TestCategory.UNICODE_ATTACKS;
    }
}
