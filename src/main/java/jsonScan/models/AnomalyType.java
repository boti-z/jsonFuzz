package jsonScan.models;

/**
 * Types of anomalies detected during fuzzing
 */
public enum AnomalyType {
    STATUS_CODE_CHANGE("Status code differs from baseline"),
    ERROR_RESPONSE("Error message in response"),
    LENGTH_VARIANCE("Significant response length difference"),
    TIMEOUT("Request timed out"),
    CONNECTION_ERROR("Connection or network error"),
    CONTENT_TYPE_CHANGE("Content-Type header changed"),
    NO_ANOMALY("No anomaly detected");

    private final String description;

    AnomalyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
