package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.PlaceholderConfig;
import jsonScan.models.TestCategory;
import jsonScan.utils.JsonParser;
import jsonScan.utils.RawJsonBuilder;

import java.util.*;

/**
 * Generates duplicate key test permutations:
 * - Simple key duplication (preserving original type)
 * - Duplicate with different values (using placeholders)
 * - Duplicate with Unicode variations
 * - For objects/arrays: duplicate entirely + variations with removed elements
 */
public class DuplicateKeyGenerator implements TestGenerator {
    private int permutationCounter = 0;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();

        // Process all unique keys
        Set<String> processedKeys = new HashSet<>();
        generateForElement(jsonElement, originalJson, "", processedKeys, permutations, placeholderConfig);

        return permutations;
    }

    private void generateForElement(JsonElement element, String originalJson, String path,
                                    Set<String> processedKeys, List<JsonPermutation> permutations,
                                    PlaceholderConfig placeholderConfig) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                // Only process each unique key once globally
                if (!processedKeys.contains(key)) {
                    processedKeys.add(key);
                    generateDuplicateKeyTests(originalJson, key, value, permutations, placeholderConfig);
                }

                // Recurse into nested structures
                String newPath = path.isEmpty() ? key : path + "." + key;
                generateForElement(value, originalJson, newPath, processedKeys, permutations, placeholderConfig);
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = path + "[" + i + "]";
                generateForElement(arr.get(i), originalJson, newPath, processedKeys, permutations, placeholderConfig);
            }
        }
    }

    private void generateDuplicateKeyTests(String originalJson, String key, JsonElement value,
                                          List<JsonPermutation> permutations, PlaceholderConfig placeholderConfig) {
        // Test 1: Duplicate AFTER with same value (test last-key precedence)
        String sameValueJson = injectDuplicateKey(originalJson, key, GSON.toJson(value));
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, sameValueJson,
                getCategory(), "Duplicate key AFTER with same value (key: " + key + ")"));

        // Test 2: Duplicate AFTER with different value (type-appropriate placeholder)
        String differentValueJson = injectDuplicateKey(originalJson, key, getPlaceholderForType(value, placeholderConfig));
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, differentValueJson,
                getCategory(), "Duplicate key AFTER with different value (key: " + key + ")"));

        // Test 3: Duplicate BEFORE with different value (test first-key precedence)
        String beforeDifferentJson = injectDuplicateKeyBefore(originalJson, key, getPlaceholderForType(value, placeholderConfig));
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, beforeDifferentJson,
                getCategory(), "Duplicate key BEFORE with different value (key: " + key + ")"));

        // Test 4: Duplicate BEFORE with same value
        String beforeSameJson = injectDuplicateKeyBefore(originalJson, key, GSON.toJson(value));
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, beforeSameJson,
                getCategory(), "Duplicate key BEFORE with same value (key: " + key + ")"));

        // Test 5: Duplicate with Unicode variation of key (AFTER)
        String unicodeKey = createUnicodeVariation(key);
        String unicodeJson = injectDuplicateKeyRaw(originalJson, "\"" + key + "\"",
                "\"" + unicodeKey + "\"", getPlaceholderForType(value, placeholderConfig));
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, unicodeJson,
                getCategory(), "Duplicate key AFTER with Unicode variation (key: " + key + ")"));

        // Test 6: Duplicate with Unicode variation of key (BEFORE)
        String unicodeBeforeJson = injectDuplicateKeyRawBefore(originalJson, "\"" + key + "\"",
                "\"" + unicodeKey + "\"", getPlaceholderForType(value, placeholderConfig));
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, unicodeBeforeJson,
                getCategory(), "Duplicate key BEFORE with Unicode variation (key: " + key + ")"));

        // Test 7: Duplicate with unpaired surrogate in key (AFTER)
        String surrogateJson = injectDuplicateKeyRaw(originalJson, "\"" + key + "\"",
                "\"" + key + "\\ud888\"", getPlaceholderForType(value, placeholderConfig));
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, surrogateJson,
                getCategory(), "Duplicate key AFTER with unpaired surrogate (key: " + key + ")"));

        // Test 8: Duplicate with unpaired surrogate in key (BEFORE)
        String surrogateBeforeJson = injectDuplicateKeyRawBefore(originalJson, "\"" + key + "\"",
                "\"" + key + "\\ud888\"", getPlaceholderForType(value, placeholderConfig));
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, surrogateBeforeJson,
                getCategory(), "Duplicate key BEFORE with unpaired surrogate (key: " + key + ")"));

        // Test 9: Duplicate with null byte in key (AFTER)
        String nullByteJson = injectDuplicateKeyRaw(originalJson, "\"" + key + "\"",
                "\"" + key + "\\u0000ef\"", getPlaceholderForType(value, placeholderConfig));
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, nullByteJson,
                getCategory(), "Duplicate key AFTER with null byte (key: " + key + ")"));

        // Additional tests for objects and arrays
        if (value.isJsonObject()) {
            generateObjectVariations(originalJson, key, value.getAsJsonObject(), permutations);
        } else if (value.isJsonArray()) {
            generateArrayVariations(originalJson, key, value.getAsJsonArray(), permutations);
        }
    }

    /**
     * Generate variations for object values - remove one property at a time
     */
    private void generateObjectVariations(String originalJson, String key, JsonObject obj,
                                          List<JsonPermutation> permutations) {
        if (obj.size() == 0) {
            return;
        }

        // Duplicate with each property removed
        for (String property : new ArrayList<>(obj.keySet())) {
            JsonObject modified = deepCopy(obj).getAsJsonObject();
            modified.remove(property);
            String modifiedJson = injectDuplicateKey(originalJson, key, GSON.toJson(modified));
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, modifiedJson,
                    getCategory(), "Duplicate object missing property '" + property + "' (key: " + key + ")"));
        }

        // Duplicate with empty object
        String emptyObjJson = injectDuplicateKey(originalJson, key, "{}");
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, emptyObjJson,
                getCategory(), "Duplicate with empty object (key: " + key + ")"));
    }

    /**
     * Generate variations for array values - remove one element at a time
     */
    private void generateArrayVariations(String originalJson, String key, JsonArray arr,
                                        List<JsonPermutation> permutations) {
        if (arr.size() == 0) {
            return;
        }

        // Duplicate with each element removed
        for (int i = 0; i < arr.size(); i++) {
            JsonArray modified = deepCopy(arr).getAsJsonArray();
            modified.remove(i);
            String modifiedJson = injectDuplicateKey(originalJson, key, GSON.toJson(modified));
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, modifiedJson,
                    getCategory(), "Duplicate array missing element at index " + i + " (key: " + key + ")"));
        }

        // Duplicate with empty array
        String emptyArrJson = injectDuplicateKey(originalJson, key, "[]");
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, emptyArrJson,
                getCategory(), "Duplicate with empty array (key: " + key + ")"));

        // Duplicate with first element only
        if (arr.size() > 1) {
            JsonArray firstOnly = new JsonArray();
            firstOnly.add(deepCopy(arr.get(0)));
            String firstOnlyJson = injectDuplicateKey(originalJson, key, GSON.toJson(firstOnly));
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, firstOnlyJson,
                    getCategory(), "Duplicate array with first element only (key: " + key + ")"));
        }

        // Duplicate with last element only
        if (arr.size() > 1) {
            JsonArray lastOnly = new JsonArray();
            lastOnly.add(deepCopy(arr.get(arr.size() - 1)));
            String lastOnlyJson = injectDuplicateKey(originalJson, key, GSON.toJson(lastOnly));
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, lastOnlyJson,
                    getCategory(), "Duplicate array with last element only (key: " + key + ")"));
        }
    }

    /**
     * Get type-appropriate placeholder value
     */
    private String getPlaceholderForType(JsonElement value, PlaceholderConfig config) {
        if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isString()) {
                return "\"" + config.getStringPlaceholder() + "\"";
            } else if (primitive.isNumber()) {
                return config.getNumberPlaceholder();
            } else if (primitive.isBoolean()) {
                return config.getBooleanPlaceholder();
            }
        } else if (value.isJsonObject()) {
            return "{}";
        } else if (value.isJsonArray()) {
            return "[]";
        } else if (value.isJsonNull()) {
            return "null";
        }
        return "\"" + config.getStringPlaceholder() + "\"";
    }

    /**
     * Create Unicode variation of key (encode last character)
     */
    private String createUnicodeVariation(String key) {
        if (key.isEmpty()) {
            return key;
        }
        char lastChar = key.charAt(key.length() - 1);
        String unicoded = RawJsonBuilder.unicodeEncode(lastChar);
        return key.substring(0, key.length() - 1) + unicoded;
    }

    /**
     * Inject duplicate key with value into JSON string
     */
    private String injectDuplicateKey(String originalJson, String key, String value) {
        String keyPattern = "\"" + key + "\"";
        int keyIndex = originalJson.indexOf(keyPattern);

        if (keyIndex == -1) {
            return originalJson;
        }

        // Find the enclosing object's closing brace
        int openBraceCount = 0;
        int startIndex = originalJson.lastIndexOf('{', keyIndex);
        int closeBraceIndex = -1;

        for (int i = startIndex; i < originalJson.length(); i++) {
            char c = originalJson.charAt(i);
            if (c == '{') {
                openBraceCount++;
            } else if (c == '}') {
                openBraceCount--;
                if (openBraceCount == 0) {
                    closeBraceIndex = i;
                    break;
                }
            }
        }

        if (closeBraceIndex == -1) {
            return originalJson;
        }

        String beforeClose = originalJson.substring(0, closeBraceIndex);
        String afterClose = originalJson.substring(closeBraceIndex);

        String insertion = ", \"" + key + "\": " + value;
        if (beforeClose.trim().endsWith("{")) {
            insertion = "\"" + key + "\": " + value;
        }

        return beforeClose + insertion + afterClose;
    }

    /**
     * Inject duplicate key with custom key string (for Unicode variations)
     */
    private String injectDuplicateKeyRaw(String originalJson, String originalKey,
                                        String duplicateKey, String value) {
        int keyIndex = originalJson.indexOf(originalKey);

        if (keyIndex == -1) {
            return originalJson;
        }

        int openBraceCount = 0;
        int startIndex = originalJson.lastIndexOf('{', keyIndex);
        int closeBraceIndex = -1;

        for (int i = startIndex; i < originalJson.length(); i++) {
            char c = originalJson.charAt(i);
            if (c == '{') {
                openBraceCount++;
            } else if (c == '}') {
                openBraceCount--;
                if (openBraceCount == 0) {
                    closeBraceIndex = i;
                    break;
                }
            }
        }

        if (closeBraceIndex == -1) {
            return originalJson;
        }

        String beforeClose = originalJson.substring(0, closeBraceIndex);
        String afterClose = originalJson.substring(closeBraceIndex);

        String insertion = ", " + duplicateKey + ": " + value;
        if (beforeClose.trim().endsWith("{")) {
            insertion = duplicateKey + ": " + value;
        }

        return beforeClose + insertion + afterClose;
    }

    /**
     * Inject duplicate key BEFORE the original key (test first-key precedence)
     */
    private String injectDuplicateKeyBefore(String originalJson, String key, String value) {
        String keyPattern = "\"" + key + "\"";
        int keyIndex = originalJson.indexOf(keyPattern);

        if (keyIndex == -1) {
            return originalJson;
        }

        int startIndex = originalJson.lastIndexOf('{', keyIndex);
        if (startIndex == -1) {
            return originalJson;
        }

        String beforeKey = originalJson.substring(0, startIndex + 1);
        String afterBrace = originalJson.substring(startIndex + 1);

        String insertion = "\"" + key + "\": " + value + ", ";

        String trimmedAfter = afterBrace.trim();
        if (trimmedAfter.isEmpty() || trimmedAfter.startsWith("}")) {
            insertion = "\"" + key + "\": " + value;
        }

        return beforeKey + insertion + afterBrace;
    }

    /**
     * Inject duplicate key BEFORE with custom key string (for Unicode variations)
     */
    private String injectDuplicateKeyRawBefore(String originalJson, String originalKey,
                                               String duplicateKey, String value) {
        int keyIndex = originalJson.indexOf(originalKey);

        if (keyIndex == -1) {
            return originalJson;
        }

        int startIndex = originalJson.lastIndexOf('{', keyIndex);
        if (startIndex == -1) {
            return originalJson;
        }

        String beforeKey = originalJson.substring(0, startIndex + 1);
        String afterBrace = originalJson.substring(startIndex + 1);

        String insertion = duplicateKey + ": " + value + ", ";

        String trimmedAfter = afterBrace.trim();
        if (trimmedAfter.isEmpty() || trimmedAfter.startsWith("}")) {
            insertion = duplicateKey + ": " + value;
        }

        return beforeKey + insertion + afterBrace;
    }

    private JsonElement deepCopy(JsonElement element) {
        return GSON.fromJson(GSON.toJson(element), JsonElement.class);
    }

    @Override
    public TestCategory getCategory() {
        return TestCategory.DUPLICATE_KEYS;
    }
}
