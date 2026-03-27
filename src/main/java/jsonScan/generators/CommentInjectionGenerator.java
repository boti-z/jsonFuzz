package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.TestCategory;
import jsonScan.utils.JsonParser;

import java.util.*;

/**
 * Generates comment injection test permutations:
 * - Single-line comments (//)
 * - Multi-line comments (/* *\/)
 * - Comments between key-value pairs
 * - Comment truncation attacks
 */
public class CommentInjectionGenerator implements TestGenerator {
    private int permutationCounter = 0;

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, jsonScan.models.PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();

        // Test 1: Add single-line comment at the beginning
        String mutated1 = "// Comment at start\n" + originalJson;
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated1,
                getCategory(), "Single-line comment at start"));

        // Test 2: Add multi-line comment at the beginning
        String mutated2 = "/* Multi-line comment */\n" + originalJson;
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated2,
                getCategory(), "Multi-line comment at start"));

        // Test 3: Inject comment between first key-value pair
        int firstCommaIndex = originalJson.indexOf(',');
        if (firstCommaIndex > 0) {
            String mutated3 = originalJson.substring(0, firstCommaIndex) +
                    " // Injected comment\n " +
                    originalJson.substring(firstCommaIndex);
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated3,
                    getCategory(), "Single-line comment between properties"));

            String mutated4 = originalJson.substring(0, firstCommaIndex) +
                    " /* Injected comment */ " +
                    originalJson.substring(firstCommaIndex);
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated4,
                    getCategory(), "Multi-line comment between properties"));
        }

        // Test 5: Comment after colon
        int firstColonIndex = originalJson.indexOf(':');
        if (firstColonIndex > 0) {
            String mutated5 = originalJson.substring(0, firstColonIndex + 1) +
                    " // Comment after colon\n " +
                    originalJson.substring(firstColonIndex + 1);
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated5,
                    getCategory(), "Comment after colon"));
        }

        // Test 6: Comment inside string value (should break parsing or be ignored)
        List<JsonParser.KeyValuePair> stringValues = JsonParser.findAllStrings(jsonElement, "");
        if (!stringValues.isEmpty()) {
            JsonParser.KeyValuePair first = stringValues.get(0);
            String stringValue = (String) first.getValue();
            String mutated6 = originalJson.replace("\"" + stringValue + "\"",
                    "\"" + stringValue + "// comment" + "\"");
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated6,
                    getCategory(), "Comment-like text in string value"));
        }

        // Test 7: Nested comments
        String mutated7 = "/* Outer /* Inner */ comment */" + originalJson;
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated7,
                getCategory(), "Nested multi-line comments"));

        return permutations;
    }

    @Override
    public TestCategory getCategory() {
        return TestCategory.COMMENT_INJECTION;
    }
}
