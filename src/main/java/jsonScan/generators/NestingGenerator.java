package jsonScan.generators;

import com.google.gson.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.TestCategory;
import jsonScan.utils.RawJsonBuilder;

import java.util.*;

/**
 * Generates deep nesting test permutations:
 * - Deep object nesting (test parser limits)
 * - Deep array nesting
 * - Mixed nesting patterns
 */
public class NestingGenerator implements TestGenerator {
    private int permutationCounter = 0;
    private static final int[] DEPTH_LEVELS = {50, 100, 500, 1000};

    @Override
    public List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, jsonScan.models.PlaceholderConfig placeholderConfig) {
        List<JsonPermutation> permutations = new ArrayList<>();

        // Generate deeply nested objects at various depths
        for (int depth : DEPTH_LEVELS) {
            String deepObject = generateDeeplyNestedObject(depth);
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, deepObject,
                    getCategory(), "Deeply nested objects (depth: " + depth + ")"));
        }

        // Generate deeply nested arrays at various depths
        for (int depth : DEPTH_LEVELS) {
            String deepArray = generateDeeplyNestedArray(depth);
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, deepArray,
                    getCategory(), "Deeply nested arrays (depth: " + depth + ")"));
        }

        // Generate mixed nesting
        for (int depth : DEPTH_LEVELS) {
            String mixed = generateMixedNesting(depth);
            permutations.add(new JsonPermutation(permutationCounter++, originalJson, mixed,
                    getCategory(), "Mixed object/array nesting (depth: " + depth + ")"));
        }

        return permutations;
    }

    private String generateDeeplyNestedObject(int depth) {
        RawJsonBuilder builder = new RawJsonBuilder();
        builder.startObject();
        builder.addKey("level").addNumberValue(0);

        for (int i = 1; i < depth; i++) {
            builder.addKey("nested").startObject();
            builder.addKey("level").addNumberValue(i);
        }

        // Close all objects
        for (int i = 0; i < depth; i++) {
            builder.endObject();
        }

        return builder.build();
    }

    private String generateDeeplyNestedArray(int depth) {
        RawJsonBuilder builder = new RawJsonBuilder();
        builder.startArray();

        for (int i = 1; i < depth; i++) {
            builder.startArray();
        }

        builder.addStringValue("deep");

        // Close all arrays
        for (int i = 0; i < depth; i++) {
            builder.endArray();
        }

        return builder.build();
    }

    private String generateMixedNesting(int depth) {
        RawJsonBuilder builder = new RawJsonBuilder();

        for (int i = 0; i < depth; i++) {
            if (i % 2 == 0) {
                builder.startObject();
                builder.addKey("level" + i);
            } else {
                builder.startArray();
            }
        }

        builder.addStringValue("deep");

        // Close all containers
        for (int i = 0; i < depth; i++) {
            if ((depth - i - 1) % 2 == 0) {
                builder.endObject();
            } else {
                builder.endArray();
            }
        }

        return builder.build();
    }

    @Override
    public TestCategory getCategory() {
        return TestCategory.DEEP_NESTING;
    }
}
