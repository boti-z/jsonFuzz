package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.TestCategory;
import jsonScan.utils.JsonParser;

import java.util.*;

/**
 * Generates number-based test permutations including:
 * - Large floats and scientific notation
 * - Integer boundaries (2^53 ± 1)
 * - Negative boundaries
 * - Edge cases (Infinity, very large exponents)
 */
public class NumberTestGenerator implements TestGenerator {
    private int permutationCounter = 0;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private static final String[] TEST_NUMBERS = {
            "3.141592653589793238462643383279",
            "1.0e4096",
            "6e23",
            "1.602e-19",
            "123.456e7",
            "9007199254740990",
            "9007199254740993",
            "-9007199254740990",
            "-9007199254740993"
    };

    private static final String[] TEST_DESCRIPTIONS = {
            "High precision float",
            "Very large exponent (1.0e4096)",
            "Scientific notation (6e23)",
            "Negative exponent (1.602e-19)",
            "Decimal with exponent",
            "Integer boundary (2^53-1)",
            "Beyond integer boundary (2^53+1)",
            "Negative integer boundary",
            "Negative beyond boundary"
    };

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, jsonScan.models.PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();
        List<JsonParser.KeyValuePair> numberValues = JsonParser.findAllNumbers(jsonElement, "");

        for (JsonParser.KeyValuePair kvp : numberValues) {
            Number numberValue = (Number) kvp.getValue();
            String path = kvp.getPath();
            String displayKey = kvp.getKey();

            for (int i = 0; i < TEST_NUMBERS.length; i++) {
                permutations.add(createPermutation(originalJson, jsonElement, path,
                        numberValue, TEST_NUMBERS[i], TEST_DESCRIPTIONS[i], displayKey));
            }
        }

        return permutations;
    }

    private JsonPermutation createPermutation(String originalJson, JsonElement jsonElement,
                                             String path, Number oldValue, String newValueStr,
                                             String description, String displayKey) {
        String mutated = replaceNumberAtPath(jsonElement, path, oldValue, newValueStr);
        return new JsonPermutation(permutationCounter++, originalJson, mutated,
                getCategory(), description + " (path: " + path + ")");
    }

    private String replaceNumberAtPath(JsonElement element, String targetPath, Number oldValue, String newValueStr) {
        JsonElement modified = deepCopy(element);
        replaceAtPathRecursive(modified, targetPath, oldValue, newValueStr, "");

        String result = GSON.toJson(modified);
        result = result.replace("\"" + newValueStr + "\"", newValueStr);
        return result;
    }

    private boolean replaceAtPathRecursive(JsonElement element, String targetPath, Number oldValue,
                                          String newValue, String currentPath) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
                String key = entry.getKey();
                String newPath = currentPath.isEmpty() ? key : currentPath + "." + key;
                JsonElement value = entry.getValue();

                if (newPath.equals(targetPath) && value.isJsonPrimitive() &&
                    value.getAsJsonPrimitive().isNumber()) {
                    Number currentValue = value.getAsJsonPrimitive().getAsNumber();
                    if (numbersEqual(currentValue, oldValue)) {
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
                    value.getAsJsonPrimitive().isNumber()) {
                    Number currentValue = value.getAsJsonPrimitive().getAsNumber();
                    if (numbersEqual(currentValue, oldValue)) {
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

    private boolean numbersEqual(Number n1, Number n2) {
        if (n1.getClass().equals(n2.getClass())) {
            return n1.equals(n2);
        }

        double d1 = n1.doubleValue();
        double d2 = n2.doubleValue();

        if (Double.isNaN(d1) && Double.isNaN(d2)) {
            return true;
        }
        if (Double.isInfinite(d1) && Double.isInfinite(d2)) {
            return d1 == d2;
        }

        return Math.abs(d1 - d2) < 0.0000001;
    }

    private JsonElement deepCopy(JsonElement element) {
        return GSON.fromJson(GSON.toJson(element), JsonElement.class);
    }

    @Override
    public TestCategory getCategory() {
        return TestCategory.NUMBER_EDGE_CASES;
    }
}
