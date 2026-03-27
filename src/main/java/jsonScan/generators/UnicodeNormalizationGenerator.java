package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.PlaceholderConfig;
import jsonScan.models.TestCategory;
import jsonScan.utils.JsonParser;

import java.text.Normalizer;
import java.util.*;

public class UnicodeNormalizationGenerator implements TestGenerator {
    private int permutationCounter = 0;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();
        List<JsonParser.KeyValuePair> strings = JsonParser.findAllStrings(jsonElement, "");

        for (JsonParser.KeyValuePair kvp : strings) {
            String stringValue = (String) kvp.getValue();
            String path = kvp.getPath();

            permutations.add(createNFCVariation(originalJson, jsonElement, path, stringValue));
            permutations.add(createNFDVariation(originalJson, jsonElement, path, stringValue));
            permutations.add(createHomographVariation(originalJson, jsonElement, path, stringValue));
            permutations.add(createZeroWidthVariation(originalJson, jsonElement, path, stringValue));
            permutations.add(createRTLOverrideVariation(originalJson, jsonElement, path, stringValue));
        }

        return permutations;
    }

    private JsonPermutation createNFCVariation(String originalJson, JsonElement jsonElement,
                                               String path, String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
        JsonElement modified = deepCopy(jsonElement);
        replaceStringAtPath(modified, path, value, normalized, "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Unicode NFC normalization (path: " + path + ")");
    }

    private JsonPermutation createNFDVariation(String originalJson, JsonElement jsonElement,
                                               String path, String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        JsonElement modified = deepCopy(jsonElement);
        replaceStringAtPath(modified, path, value, normalized, "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Unicode NFD normalization (path: " + path + ")");
    }

    private JsonPermutation createHomographVariation(String originalJson, JsonElement jsonElement,
                                                     String path, String value) {
        String homograph = value
                .replace('a', '\u0430')
                .replace('e', '\u0435')
                .replace('o', '\u043E')
                .replace('p', '\u0440')
                .replace('c', '\u0441')
                .replace('x', '\u0445')
                .replace('y', '\u0443')
                .replace('A', '\u0410')
                .replace('B', '\u0412')
                .replace('C', '\u0421')
                .replace('E', '\u0415')
                .replace('H', '\u041D')
                .replace('K', '\u041A')
                .replace('M', '\u041C')
                .replace('O', '\u041E')
                .replace('P', '\u0420')
                .replace('T', '\u0422')
                .replace('X', '\u0425');

        JsonElement modified = deepCopy(jsonElement);
        replaceStringAtPath(modified, path, value, homograph, "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Homograph attack (Cyrillic lookalikes) (path: " + path + ")");
    }

    private JsonPermutation createZeroWidthVariation(String originalJson, JsonElement jsonElement,
                                                     String path, String value) {
        String withZeroWidth = value + "\u200B\u200C\u200D\uFEFF";
        JsonElement modified = deepCopy(jsonElement);
        replaceStringAtPath(modified, path, value, withZeroWidth, "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Zero-width characters injection (path: " + path + ")");
    }

    private JsonPermutation createRTLOverrideVariation(String originalJson, JsonElement jsonElement,
                                                       String path, String value) {
        String withRTL = "\u202E" + value + "\u202C";
        JsonElement modified = deepCopy(jsonElement);
        replaceStringAtPath(modified, path, value, withRTL, "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "Right-to-left override (path: " + path + ")");
    }

    private boolean replaceStringAtPath(JsonElement element, String targetPath, String oldValue,
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

                if (replaceStringAtPath(entry.getValue(), targetPath, oldValue, newValue, newPath)) {
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

                if (replaceStringAtPath(child, targetPath, oldValue, newValue, newPath)) {
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
        return TestCategory.UNICODE_NORMALIZATION;
    }
}
