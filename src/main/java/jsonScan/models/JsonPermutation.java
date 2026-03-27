package jsonScan.models;

/**
 * Represents a single JSON permutation generated for fuzzing
 */
public class JsonPermutation {
    private final String originalJson;
    private final String mutatedJson;
    private final TestCategory category;
    private final String description;
    private final int permutationId;

    public JsonPermutation(int permutationId, String originalJson, String mutatedJson,
                          TestCategory category, String description) {
        this.permutationId = permutationId;
        this.originalJson = originalJson;
        this.mutatedJson = mutatedJson;
        this.category = category;
        this.description = description;
    }

    public int getPermutationId() {
        return permutationId;
    }

    public String getOriginalJson() {
        return originalJson;
    }

    public String getMutatedJson() {
        return mutatedJson;
    }

    public TestCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", category.getDisplayName(), description);
    }
}
