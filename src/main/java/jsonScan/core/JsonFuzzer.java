package jsonScan.core;

import com.google.gson.JsonElement;
import jsonScan.generators.*;
import jsonScan.models.JsonPermutation;
import jsonScan.models.PlaceholderConfig;
import jsonScan.models.TestCategory;
import jsonScan.utils.JsonParser;

import java.util.*;

/**
 * Main JSON fuzzer orchestrator that coordinates all test generators
 */
public class JsonFuzzer {
    private final Map<TestCategory, TestGenerator> generators;
    private final Set<TestCategory> enabledCategories;
    private final PlaceholderConfig placeholderConfig;

    public JsonFuzzer() {
        this.generators = new HashMap<>();
        this.enabledCategories = EnumSet.allOf(TestCategory.class);
        this.placeholderConfig = new PlaceholderConfig();

        // Register all generators
        registerGenerator(new StringTestGenerator());
        registerGenerator(new NumberTestGenerator());
        registerGenerator(new DuplicateKeyGenerator());
        registerGenerator(new UnicodeTestGenerator());
        registerGenerator(new CommentInjectionGenerator());
        registerGenerator(new WhitespaceGenerator());
        registerGenerator(new NestingGenerator());
        registerGenerator(new ArrayObjectDuplicationGenerator());
        registerGenerator(new TypeConfusionGenerator());
        registerGenerator(new SpecialNumberGenerator());
        registerGenerator(new UnicodeNormalizationGenerator());
        registerGenerator(new LargePayloadGenerator());
    }

    /**
     * Get placeholder configuration
     */
    public PlaceholderConfig getPlaceholderConfig() {
        return placeholderConfig;
    }

    /**
     * Update placeholder configuration
     */
    public void setPlaceholderConfig(PlaceholderConfig config) {
        this.placeholderConfig.setStringPlaceholder(config.getStringPlaceholder());
        this.placeholderConfig.setNumberPlaceholder(config.getNumberPlaceholder());
        this.placeholderConfig.setBooleanPlaceholder(config.getBooleanPlaceholder());
    }

    private void registerGenerator(TestGenerator generator) {
        generators.put(generator.getCategory(), generator);
    }

    /**
     * Enable a specific test category
     */
    public void enableCategory(TestCategory category) {
        enabledCategories.add(category);
    }

    /**
     * Disable a specific test category
     */
    public void disableCategory(TestCategory category) {
        enabledCategories.remove(category);
    }

    /**
     * Check if a category is enabled
     */
    public boolean isCategoryEnabled(TestCategory category) {
        return enabledCategories.contains(category);
    }

    /**
     * Set all enabled categories at once
     */
    public void setEnabledCategories(Set<TestCategory> categories) {
        enabledCategories.clear();
        enabledCategories.addAll(categories);
    }

    /**
     * Get all enabled categories
     */
    public Set<TestCategory> getEnabledCategories() {
        return new HashSet<>(enabledCategories);
    }

    /**
     * Generate all permutations for the given JSON
     */
    public List<JsonPermutation> generatePermutations(String jsonString) {
        List<JsonPermutation> allPermutations = new ArrayList<>();

        // Validate JSON
        if (!JsonParser.isValidJson(jsonString)) {
            return allPermutations; // Return empty list for invalid JSON
        }

        JsonElement jsonElement = JsonParser.parse(jsonString);
        if (jsonElement == null) {
            return allPermutations;
        }

        // Generate permutations for each enabled category
        for (TestCategory category : enabledCategories) {
            TestGenerator generator = generators.get(category);
            if (generator != null) {
                try {
                    List<JsonPermutation> categoryPermutations = generator.generate(jsonString, jsonElement, placeholderConfig);
                    allPermutations.addAll(categoryPermutations);
                } catch (Exception e) {
                    // Log error but continue with other generators
                    System.err.println("Error in generator " + category + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return allPermutations;
    }

    /**
     * Get estimated permutation count without generating them
     */
    public int estimatePermutationCount(String jsonString) {
        if (!JsonParser.isValidJson(jsonString)) {
            return 0;
        }

        JsonElement jsonElement = JsonParser.parse(jsonString);
        if (jsonElement == null) {
            return 0;
        }

        int estimate = 0;
        int stringCount = JsonParser.findAllStrings(jsonElement, "").size();
        int numberCount = JsonParser.findAllNumbers(jsonElement, "").size();
        int booleanCount = JsonParser.findAllBooleans(jsonElement, "").size();
        int keyCount = JsonParser.findAllKeys(jsonElement).size();
        int primitiveCount = stringCount + numberCount + booleanCount;

        if (enabledCategories.contains(TestCategory.STRING_VARIATIONS)) {
            estimate += stringCount * 16;
        }
        if (enabledCategories.contains(TestCategory.NUMBER_EDGE_CASES)) {
            estimate += numberCount * 9;
        }
        if (enabledCategories.contains(TestCategory.DUPLICATE_KEYS)) {
            estimate += keyCount * 9;
        }
        if (enabledCategories.contains(TestCategory.UNICODE_ATTACKS)) {
            estimate += stringCount * 7;
        }
        if (enabledCategories.contains(TestCategory.COMMENT_INJECTION)) {
            estimate += 7;
        }
        if (enabledCategories.contains(TestCategory.WHITESPACE_VARIATIONS)) {
            estimate += 7;
        }
        if (enabledCategories.contains(TestCategory.DEEP_NESTING)) {
            estimate += 12;
        }
        if (enabledCategories.contains(TestCategory.ARRAY_OBJECT_DUPLICATION)) {
            estimate += (stringCount + numberCount) * 2;
        }
        if (enabledCategories.contains(TestCategory.TYPE_CONFUSION)) {
            estimate += primitiveCount * 7;
        }
        if (enabledCategories.contains(TestCategory.SPECIAL_NUMBERS)) {
            estimate += numberCount * 6;
        }
        if (enabledCategories.contains(TestCategory.UNICODE_NORMALIZATION)) {
            estimate += stringCount * 5;
        }
        if (enabledCategories.contains(TestCategory.LARGE_PAYLOADS)) {
            estimate += stringCount * 2;
        }

        return estimate;
    }
}
