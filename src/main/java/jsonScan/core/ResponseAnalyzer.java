package jsonScan.core;

import burp.api.montoya.http.message.responses.HttpResponse;
import jsonScan.models.AnomalyType;
import jsonScan.models.FuzzResult;

import java.util.*;

/**
 * Analyzes responses to detect anomalies and parser discrepancies
 */
public class ResponseAnalyzer {
    private static final double LENGTH_VARIANCE_THRESHOLD = 0.1; // 10% difference
    private static final int MIN_LENGTH_DIFF = 50; // Minimum byte difference to flag

    /**
     * Analyze a fuzz result against a baseline
     */
    public void analyzeResult(FuzzResult result, FuzzResult baseline) {
        if (result == null || baseline == null) {
            return;
        }

        // Check for errors
        if (result.hasError()) {
            if (result.getErrorMessage().toLowerCase().contains("timeout")) {
                result.addAnomaly(AnomalyType.TIMEOUT);
            } else {
                result.addAnomaly(AnomalyType.CONNECTION_ERROR);
            }
            return;
        }

        HttpResponse response = result.getResponse();
        HttpResponse baselineResponse = baseline.getResponse();

        if (response == null || baselineResponse == null) {
            result.addAnomaly(AnomalyType.CONNECTION_ERROR);
            return;
        }

        // Check status code
        if (response.statusCode() != baselineResponse.statusCode()) {
            result.addAnomaly(AnomalyType.STATUS_CODE_CHANGE);
        }

        // Check for error responses
        if (isErrorResponse(response)) {
            result.addAnomaly(AnomalyType.ERROR_RESPONSE);
        }

        // Check response length variance
        int baselineLength = baselineResponse.body().length();
        int responseLength = response.body().length();
        double lengthDiff = Math.abs(responseLength - baselineLength);

        if (lengthDiff > MIN_LENGTH_DIFF) {
            double variance = lengthDiff / Math.max(baselineLength, 1);
            if (variance > LENGTH_VARIANCE_THRESHOLD) {
                result.addAnomaly(AnomalyType.LENGTH_VARIANCE);
            }
        }

        // Check Content-Type change
        String baselineContentType = getContentType(baselineResponse);
        String responseContentType = getContentType(response);
        if (!baselineContentType.equals(responseContentType)) {
            result.addAnomaly(AnomalyType.CONTENT_TYPE_CHANGE);
        }

        // If no anomalies detected
        if (result.getAnomalies().isEmpty()) {
            result.addAnomaly(AnomalyType.NO_ANOMALY);
        }
    }

    /**
     * Analyze all results against baseline
     */
    public void analyzeAllResults(List<FuzzResult> results, FuzzResult baseline) {
        for (FuzzResult result : results) {
            analyzeResult(result, baseline);
        }
    }

    /**
     * Check if response contains error indicators
     */
    private boolean isErrorResponse(HttpResponse response) {
        if (response.statusCode() >= 400) {
            return true;
        }

        String bodyStr = response.bodyToString().toLowerCase();
        return bodyStr.contains("error") ||
               bodyStr.contains("exception") ||
               bodyStr.contains("failed") ||
               bodyStr.contains("invalid") ||
               bodyStr.contains("malformed");
    }

    /**
     * Get Content-Type header value
     */
    private String getContentType(HttpResponse response) {
        return response.headers().stream()
                .filter(h -> h.name().equalsIgnoreCase("Content-Type"))
                .findFirst()
                .map(h -> h.value().split(";")[0].trim())
                .orElse("");
    }

    /**
     * Group results by anomaly type
     */
    public Map<AnomalyType, List<FuzzResult>> groupByAnomaly(List<FuzzResult> results) {
        Map<AnomalyType, List<FuzzResult>> grouped = new EnumMap<>(AnomalyType.class);

        for (AnomalyType type : AnomalyType.values()) {
            grouped.put(type, new ArrayList<>());
        }

        for (FuzzResult result : results) {
            for (AnomalyType anomaly : result.getAnomalies()) {
                grouped.get(anomaly).add(result);
            }
        }

        return grouped;
    }

    /**
     * Calculate similarity score between two responses (0.0 to 1.0)
     */
    public double calculateSimilarity(HttpResponse response1, HttpResponse response2) {
        if (response1 == null || response2 == null) {
            return 0.0;
        }

        double score = 0.0;
        int checks = 0;

        // Status code match
        checks++;
        if (response1.statusCode() == response2.statusCode()) {
            score += 1.0;
        }

        // Length similarity
        checks++;
        int len1 = response1.body().length();
        int len2 = response2.body().length();
        if (len1 > 0 && len2 > 0) {
            double lengthRatio = (double) Math.min(len1, len2) / Math.max(len1, len2);
            score += lengthRatio;
        }

        // Content-Type match
        checks++;
        if (getContentType(response1).equals(getContentType(response2))) {
            score += 1.0;
        }

        return score / checks;
    }

    /**
     * Find the most interesting results (those with multiple anomalies)
     */
    public List<FuzzResult> findMostInteresting(List<FuzzResult> results, int limit) {
        List<FuzzResult> sorted = new ArrayList<>(results);
        sorted.sort((r1, r2) -> {
            // Sort by number of anomalies (descending)
            int anomalyDiff = r2.getAnomalies().size() - r1.getAnomalies().size();
            if (anomalyDiff != 0) {
                return anomalyDiff;
            }
            // Then by status code difference from 200
            return Math.abs(r2.getStatusCode() - 200) - Math.abs(r1.getStatusCode() - 200);
        });

        return sorted.subList(0, Math.min(limit, sorted.size()));
    }
}
