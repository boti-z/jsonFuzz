package jsonScan.core;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import jsonScan.models.FuzzResult;
import jsonScan.models.JsonPermutation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Orchestrates the fuzzing process with background threading and progress tracking
 */
public class FuzzingOrchestrator {
    private final MontoyaApi api;
    private final ResponseAnalyzer analyzer;
    private final ExecutorService executorService;
    private volatile boolean cancelled;
    private static final int THREAD_POOL_SIZE = 10;
    private static final int REQUEST_TIMEOUT_MS = 10000; // 10 seconds

    public FuzzingOrchestrator(MontoyaApi api) {
        this.api = api;
        this.analyzer = new ResponseAnalyzer();
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.cancelled = false;
    }

    /**
     * Execute fuzzing in background with progress callback
     */
    public void fuzzAsync(HttpRequest baseRequest,
                         List<JsonPermutation> permutations,
                         Consumer<FuzzingProgress> progressCallback,
                         Consumer<FuzzingResult> completionCallback) {

        CompletableFuture.runAsync(() -> {
            try {
                FuzzingResult result = fuzz(baseRequest, permutations, progressCallback);
                completionCallback.accept(result);
            } catch (Exception e) {
                api.logging().logToError("Fuzzing error: " + e.getMessage());
                e.printStackTrace();
                completionCallback.accept(new FuzzingResult(new ArrayList<>(), null, e));
            }
        }, executorService);
    }

    /**
     * Execute fuzzing synchronously
     */
    public FuzzingResult fuzz(HttpRequest baseRequest,
                            List<JsonPermutation> permutations,
                            Consumer<FuzzingProgress> progressCallback) {

        List<FuzzResult> results = new ArrayList<>();
        cancelled = false;

        // Send baseline request first
        api.logging().logToOutput("Sending baseline request...");
        FuzzResult baseline = sendRequest(baseRequest, null);
        if (baseline == null || baseline.getResponse() == null) {
            return new FuzzingResult(results, null,
                    new Exception("Failed to get baseline response"));
        }

        int total = permutations.size();
        int completed = 0;

        // Send permuted requests
        for (JsonPermutation permutation : permutations) {
            if (cancelled) {
                api.logging().logToOutput("Fuzzing cancelled by user");
                break;
            }

            try {
                // Create modified request
                HttpRequest modifiedRequest = baseRequest.withBody(permutation.getMutatedJson());

                // Send request
                FuzzResult result = sendRequest(modifiedRequest, permutation);

                if (result != null) {
                    // Analyze against baseline
                    analyzer.analyzeResult(result, baseline);
                    results.add(result);

                    // Log interesting findings
                    if (result.hasAnomalies()) {
                        api.logging().logToOutput(String.format(
                                "[ANOMALY] %s - Status: %d, Anomalies: %s",
                                permutation.getDescription(),
                                result.getStatusCode(),
                                result.getAnomalies()
                        ));
                    }
                }

                completed++;
                if (progressCallback != null) {
                    progressCallback.accept(new FuzzingProgress(completed, total, permutation));
                }

            } catch (Exception e) {
                api.logging().logToError("Error processing permutation: " + e.getMessage());
            }
        }

        return new FuzzingResult(results, baseline, null);
    }

    /**
     * Send a single HTTP request
     */
    private FuzzResult sendRequest(HttpRequest request, JsonPermutation permutation) {
        long startTime = System.currentTimeMillis();

        try {
            HttpResponse response = api.http().sendRequest(request).response();
            long responseTime = System.currentTimeMillis() - startTime;

            return new FuzzResult(permutation, request, response, responseTime);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            String errorMsg = e.getMessage();

            if (responseTime >= REQUEST_TIMEOUT_MS) {
                errorMsg = "Request timeout";
            }

            return new FuzzResult(permutation, request, null, responseTime, errorMsg);
        }
    }

    /**
     * Cancel ongoing fuzzing operation
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Progress information
     */
    public static class FuzzingProgress {
        private final int completed;
        private final int total;
        private final JsonPermutation currentPermutation;

        public FuzzingProgress(int completed, int total, JsonPermutation currentPermutation) {
            this.completed = completed;
            this.total = total;
            this.currentPermutation = currentPermutation;
        }

        public int getCompleted() {
            return completed;
        }

        public int getTotal() {
            return total;
        }

        public int getPercentage() {
            return total > 0 ? (completed * 100 / total) : 0;
        }

        public JsonPermutation getCurrentPermutation() {
            return currentPermutation;
        }
    }

    /**
     * Final fuzzing result
     */
    public static class FuzzingResult {
        private final List<FuzzResult> results;
        private final FuzzResult baseline;
        private final Exception error;

        public FuzzingResult(List<FuzzResult> results, FuzzResult baseline, Exception error) {
            this.results = results;
            this.baseline = baseline;
            this.error = error;
        }

        public List<FuzzResult> getResults() {
            return results;
        }

        public FuzzResult getBaseline() {
            return baseline;
        }

        public boolean hasError() {
            return error != null;
        }

        public Exception getError() {
            return error;
        }

        public List<FuzzResult> getAnomalousResults() {
            List<FuzzResult> anomalous = new ArrayList<>();
            for (FuzzResult result : results) {
                if (result.hasAnomalies()) {
                    anomalous.add(result);
                }
            }
            return anomalous;
        }
    }
}
