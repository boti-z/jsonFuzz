package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.PlaceholderConfig;
import jsonScan.models.TestCategory;
import jsonScan.utils.JsonParser;

import java.util.*;

public class SpecialNumberGenerator implements TestGenerator {
    private int permutationCounter = 0;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();
        List<JsonParser.KeyValuePair> numbers = JsonParser.findAllNumbers(jsonElement, "");

        for (JsonParser.KeyValuePair kvp : numbers) {
            String path = kvp.getPath();

            permutations.add(replaceWithSpecialNumber(originalJson, jsonElement, path, "Infinity", "Positive Infinity"));
            permutations.add(replaceWithSpecialNumber(originalJson, jsonElement, path, "-Infinity", "Negative Infinity"));
            permutations.add(replaceWithSpecialNumber(originalJson, jsonElement, path, "-0", "Negative Zero"));
            permutations.add(replaceWithSpecialNumber(originalJson, jsonElement, path, "0xFF", "Hexadecimal notation"));
            permutations.add(replaceWithSpecialNumber(originalJson, jsonElement, path, "0o777", "Octal notation"));
            permutations.add(replaceWithSpecialNumber(originalJson, jsonElement, path, "0b1010", "Binary notation"));
        }

        return permutations;
    }

    private JsonPermutation replaceWithSpecialNumber(String originalJson, JsonElement jsonElement,
                                                     String path, String specialValue, String description) {
        JsonElement modified = deepCopy(jsonElement);
        replaceAtPath(modified, path, new JsonPrimitive(specialValue), "");
        return new JsonPermutation(permutationCounter++, originalJson, GSON.toJson(modified),
                getCategory(), description + " (path: " + path + ")");
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
        return TestCategory.SPECIAL_NUMBERS;
    }
}
