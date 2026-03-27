package jsonScan.models;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the result of a single fuzzing attempt
 */
public class FuzzResult {
    private final JsonPermutation permutation;
    private final HttpRequest request;
    private final HttpResponse response;
    private final long responseTimeMs;
    private final List<AnomalyType> anomalies;
    private final String errorMessage;

    public FuzzResult(JsonPermutation permutation, HttpRequest request, HttpResponse response,
                     long responseTimeMs) {
        this(permutation, request, response, responseTimeMs, null);
    }

    public FuzzResult(JsonPermutation permutation, HttpRequest request, HttpResponse response,
                     long responseTimeMs, String errorMessage) {
        this.permutation = permutation;
        this.request = request;
        this.response = response;
        this.responseTimeMs = responseTimeMs;
        this.errorMessage = errorMessage;
        this.anomalies = new ArrayList<>();
    }

    public JsonPermutation getPermutation() {
        return permutation;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public List<AnomalyType> getAnomalies() {
        return anomalies;
    }

    public void addAnomaly(AnomalyType anomaly) {
        if (!anomalies.contains(anomaly)) {
            anomalies.add(anomaly);
        }
    }

    public boolean hasAnomalies() {
        return !anomalies.isEmpty() && !anomalies.contains(AnomalyType.NO_ANOMALY);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    public int getStatusCode() {
        return response != null ? response.statusCode() : -1;
    }

    public int getResponseLength() {
        return response != null ? response.body().length() : 0;
    }
}
