package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.TestCategory;

import java.util.*;

/**
 * Generates whitespace variation test permutations:
 * - Tab vs space variations
 * - Newline injection
 * - Mixed whitespace
 * - Excessive whitespace
 */
public class WhitespaceGenerator implements TestGenerator {
    private int permutationCounter = 0;

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, jsonScan.models.PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();

        // Test 1: Replace all spaces with tabs
        String mutated1 = originalJson.replace(" ", "\t");
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated1,
                getCategory(), "Replace spaces with tabs"));

        // Test 2: Add newlines after structural characters
        String mutated2 = originalJson
                .replace("{", "{\n")
                .replace("}", "\n}")
                .replace("[", "[\n")
                .replace("]", "\n]")
                .replace(",", ",\n");
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated2,
                getCategory(), "Newlines after structural characters"));

        // Test 3: Excessive whitespace
        String mutated3 = originalJson
                .replace(":", ":    ")
                .replace(",", ",    ")
                .replace("{", "{    ")
                .replace("}", "    }");
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated3,
                getCategory(), "Excessive whitespace"));

        // Test 4: Mixed whitespace (spaces, tabs, newlines)
        String mutated4 = originalJson
                .replace(" ", " \t")
                .replace(":", ":\n\t")
                .replace(",", ", \t\n");
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated4,
                getCategory(), "Mixed whitespace characters"));

        // Test 5: Carriage return + line feed
        String mutated5 = originalJson
                .replace(",", ",\r\n")
                .replace("{", "{\r\n")
                .replace("}", "\r\n}");
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated5,
                getCategory(), "Windows-style line endings (CRLF)"));

        // Test 6: Remove all whitespace (minified)
        String mutated6 = originalJson
                .replaceAll("\\s+", "");
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated6,
                getCategory(), "Remove all whitespace (minified)"));

        // Test 7: Unicode whitespace characters
        String mutated7 = originalJson
                .replace(" ", "\u00A0") // Non-breaking space
                .replace(",", ",\u2003"); // Em space
        permutations.add(new JsonPermutation(permutationCounter++, originalJson, mutated7,
                getCategory(), "Unicode whitespace characters"));

        return permutations;
    }

    @Override
    public TestCategory getCategory() {
        return TestCategory.WHITESPACE_VARIATIONS;
    }
}
