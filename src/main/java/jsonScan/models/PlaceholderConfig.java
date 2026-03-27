package jsonScan.models;

import java.util.Random;

/**
 * Configuration for placeholder values used in fuzzing tests
 */
public class PlaceholderConfig {
    private static final Random RANDOM = new Random();

    private String stringPlaceholder;
    private String numberPlaceholder;
    private String booleanPlaceholder;

    // Default placeholders
    private static final String DEFAULT_STRING = "FZZ123";
    private static final String DEFAULT_NUMBER = "999999";
    private static final String DEFAULT_BOOLEAN = "true";

    public PlaceholderConfig() {
        this.stringPlaceholder = DEFAULT_STRING;
        this.numberPlaceholder = DEFAULT_NUMBER;
        this.booleanPlaceholder = DEFAULT_BOOLEAN;
    }

    public PlaceholderConfig(String stringPlaceholder, String numberPlaceholder, String booleanPlaceholder) {
        setStringPlaceholder(stringPlaceholder);
        setNumberPlaceholder(numberPlaceholder);
        setBooleanPlaceholder(booleanPlaceholder);
    }

    public String getStringPlaceholder() {
        return stringPlaceholder;
    }

    public void setStringPlaceholder(String placeholder) {
        this.stringPlaceholder = placeholder != null && !placeholder.trim().isEmpty()
            ? placeholder.trim()
            : DEFAULT_STRING;
    }

    public String getNumberPlaceholder() {
        return numberPlaceholder;
    }

    public void setNumberPlaceholder(String placeholder) {
        this.numberPlaceholder = placeholder != null && !placeholder.trim().isEmpty()
            ? placeholder.trim()
            : DEFAULT_NUMBER;
    }

    public String getBooleanPlaceholder() {
        return booleanPlaceholder;
    }

    public void setBooleanPlaceholder(String placeholder) {
        this.booleanPlaceholder = placeholder != null && !placeholder.trim().isEmpty()
            ? placeholder.trim()
            : DEFAULT_BOOLEAN;
    }

    /**
     * Generate random string placeholder (max 6 characters)
     */
    public static String generateRandomString() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            result.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return result.toString();
    }

    /**
     * Generate random number placeholder (max 6 digits)
     */
    public static String generateRandomNumber() {
        return String.valueOf(RANDOM.nextInt(999999) + 1);
    }

    /**
     * Generate random boolean placeholder
     */
    public static String generateRandomBoolean() {
        return RANDOM.nextBoolean() ? "true" : "false";
    }

    /**
     * Reset to defaults
     */
    public void resetToDefaults() {
        this.stringPlaceholder = DEFAULT_STRING;
        this.numberPlaceholder = DEFAULT_NUMBER;
        this.booleanPlaceholder = DEFAULT_BOOLEAN;
    }

    /**
     * Randomize all placeholders
     */
    public void randomizeAll() {
        this.stringPlaceholder = generateRandomString();
        this.numberPlaceholder = generateRandomNumber();
        this.booleanPlaceholder = generateRandomBoolean();
    }
}
