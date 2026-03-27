package jsonScan.generators;

import com.google.gson.JsonElement;
import jsonScan.models.JsonPermutation;
import jsonScan.models.PlaceholderConfig;
import jsonScan.models.TestCategory;

import java.util.List;

/**
 * Base interface for all test generators
 */
public interface TestGenerator {
    /**
     * Generate permutations for the given JSON element
     * @param originalJson The original JSON string
     * @param jsonElement The parsed JSON element
     * @param placeholderConfig Configuration for placeholder values
     * @return List of generated permutations
     */
    List<JsonPermutation> generate(String originalJson, JsonElement jsonElement, PlaceholderConfig placeholderConfig);

    /**
     * Get the category this generator belongs to
     */
    TestCategory getCategory();
}
