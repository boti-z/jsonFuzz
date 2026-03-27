package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.PlaceholderConfig;
import jsonScan.models.TestCategory;
import jsonScan.utils.JsonParser;

import java.util.*;

public class LargePayloadGenerator implements TestGenerator {
    private int permutationCounter = 0;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private static final int SIZE_10KB = 10 * 1024;
    private static final int SIZE_100KB = 100 * 1024;

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();
        List<JsonParser.KeyValuePair> strings = JsonParser.findAllStrings(jsonElement, "");

        for (JsonParser.KeyValuePair kvp : strings) {
            String path = kvp.getPath();

            permutations.add(create10KBPayload(originalJson, jsonElement, path, placeholderConfig.getStringPlaceholder()));
            permutations.add(create100KBPayload(originalJson, jsonElement, path, placeholderConfig.getStringPlaceholder()));
        }

        return permutations;
    }

    private JsonPermutation create10KBPayload(String originalJson, JsonElement jsonElement,
                                              String path, String baseString) {
        String largePayload = generateLargeString(SIZE_10KB, baseString);
        JsonElement modified = deepCopy(jsonElement);
        replaceStringAtPath(modified, path, largePayload, "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "10KB large payload (path: " + path + ")");
    }

    private JsonPermutation create100KBPayload(String originalJson, JsonElement jsonElement,
                                               String path, String baseString) {
        String largePayload = generateLargeString(SIZE_100KB, baseString);
        JsonElement modified = deepCopy(jsonElement);
        replaceStringAtPath(modified, path, largePayload, "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), "100KB large payload (path: " + path + ")");
    }

    private String generateLargeString(int targetSize, String baseString) {
        if (baseString == null || baseString.isEmpty()) {
            baseString = "A";
        }

        StringBuilder sb = new StringBuilder(targetSize);
        while (sb.length() < targetSize) {
            sb.append(baseString);
        }

        if (sb.length() > targetSize) {
            sb.setLength(targetSize);
        }

        return sb.toString();
    }

    private boolean replaceStringAtPath(JsonElement element, String targetPath, String newValue, String currentPath) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();

                if (newPath.equals(targetPath)) {
                    obj.addProperty(entry.getKey(), newValue);
                    return true;
                }

                if (replaceStringAtPath(entry.getValue(), targetPath, newValue, newPath)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                String newPath = currentPath + "[" + i + "]";

                if (newPath.equals(targetPath)) {
                    arr.set(i, new JsonPrimitive(newValue));
                    return true;
                }

                if (replaceStringAtPath(arr.get(i), targetPath, newValue, newPath)) {
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
        return TestCategory.LARGE_PAYLOADS;
    }
}
