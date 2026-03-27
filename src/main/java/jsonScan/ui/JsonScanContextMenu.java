package jsonScan.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import jsonScan.core.FuzzingOrchestrator;
import jsonScan.core.JsonFuzzer;
import jsonScan.models.JsonPermutation;
import jsonScan.utils.JsonParser;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Context menu provider for JSON fuzzing
 */
public class JsonScanContextMenu implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final JsonFuzzer fuzzer;
    private final JsonScanSettings settings;
    private final JsonScanTab resultsTab;
    private final FuzzingOrchestrator orchestrator;

    public JsonScanContextMenu(MontoyaApi api, JsonFuzzer fuzzer,
                              JsonScanSettings settings, JsonScanTab resultsTab) {
        this.api = api;
        this.fuzzer = fuzzer;
        this.settings = settings;
        this.resultsTab = resultsTab;
        this.orchestrator = new FuzzingOrchestrator(api);
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();

        // Extract request from context (make it final for lambda usage)
        final HttpRequest request = extractRequestFromContext(event);

        // If no valid request found, return empty menu
        if (request == null) {
            api.logging().logToOutput("[jsonScan] No valid request found in context");
            return menuItems;
        }

        // Check if request has JSON body
        if (!hasJsonBody(request)) {
            api.logging().logToOutput("[jsonScan] Request does not contain JSON body");
            return menuItems;
        }

        api.logging().logToOutput("[jsonScan] JSON request detected, showing context menu items");

        // Create "Fuzz!" menu item
        JMenuItem fuzzMenuItem = new JMenuItem("jsonScan: Fuzz!");
        fuzzMenuItem.addActionListener(e -> launchFuzzer(request));
        menuItems.add(fuzzMenuItem);

        // Create "Estimate Permutations" menu item
        JMenuItem estimateMenuItem = new JMenuItem("jsonScan: Estimate Permutations");
        estimateMenuItem.addActionListener(e -> showEstimate(request));
        menuItems.add(estimateMenuItem);

        return menuItems;
    }

    /**
     * Extract HttpRequest from context event
     */
    private HttpRequest extractRequestFromContext(ContextMenuEvent event) {
        // Try to get request from message editor context (e.g., Repeater)
        if (event.messageEditorRequestResponse().isPresent()) {
            MessageEditorHttpRequestResponse messageEditor = event.messageEditorRequestResponse().get();
            HttpRequestResponse requestResponse = messageEditor.requestResponse();
            if (requestResponse != null && requestResponse.request() != null) {
                api.logging().logToOutput("[jsonScan] Context menu triggered from message editor");
                return requestResponse.request();
            }
        }

        // If not from message editor, try selected requests (e.g., Proxy History, Target, Logger)
        if (!event.selectedRequestResponses().isEmpty()) {
            // Get the first selected request
            HttpRequestResponse selectedItem = event.selectedRequestResponses().get(0);
            if (selectedItem != null && selectedItem.request() != null) {
                api.logging().logToOutput("[jsonScan] Context menu triggered from request/response table (Proxy/Target/Logger)");
                return selectedItem.request();
            }
        }

        return null;
    }

    /**
     * Check if request has JSON content
     */
    private boolean hasJsonBody(HttpRequest request) {
        // Check if body is empty
        String body = request.bodyToString();
        if (body == null || body.trim().isEmpty()) {
            return false;
        }

        // Check Content-Type header (preferred but not required)
        boolean hasJsonContentType = request.headers().stream()
                .anyMatch(h -> h.name().equalsIgnoreCase("Content-Type") &&
                        h.value().toLowerCase().contains("application/json"));

        // If Content-Type says it's JSON, trust it
        if (hasJsonContentType) {
            return JsonParser.isValidJson(body);
        }

        // Even without Content-Type header, check if body looks like JSON
        // This helps with requests that don't have proper headers set
        String trimmed = body.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            // Looks like JSON, validate it
            return JsonParser.isValidJson(body);
        }

        return false;
    }

    /**
     * Launch the fuzzer
     */
    private void launchFuzzer(HttpRequest request) {
        // Show confirmation dialog
        String jsonBody = request.bodyToString();

        // Update fuzzer with current settings
        fuzzer.setEnabledCategories(settings.getEnabledCategories());
        fuzzer.setPlaceholderConfig(settings.getPlaceholderConfig());

        // Estimate permutations
        int estimatedCount = fuzzer.estimatePermutationCount(jsonBody);

        int response = JOptionPane.showConfirmDialog(
                null,
                String.format("This will generate and send approximately %d permutations.\n" +
                        "Continue with fuzzing?", estimatedCount),
                "jsonScan - Confirm Fuzzing",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (response != JOptionPane.YES_OPTION) {
            return;
        }

        // Clear previous results
        resultsTab.clearResults();
        resultsTab.updateStatus("Generating permutations...");

        // Generate permutations in background
        SwingWorker<List<JsonPermutation>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<JsonPermutation> doInBackground() {
                return fuzzer.generatePermutations(jsonBody);
            }

            @Override
            protected void done() {
                try {
                    List<JsonPermutation> permutations = get();
                    resultsTab.updateStatus(String.format("Generated %d permutations. Sending requests...",
                            permutations.size()));

                    // Start fuzzing
                    startFuzzing(request, permutations);

                } catch (Exception e) {
                    api.logging().logToError("Error generating permutations: " + e.getMessage());
                    resultsTab.updateStatus("Error: " + e.getMessage());
                    JOptionPane.showMessageDialog(null,
                            "Error generating permutations: " + e.getMessage(),
                            "jsonScan Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    /**
     * Start the fuzzing process
     */
    private void startFuzzing(HttpRequest request, List<JsonPermutation> permutations) {
        // Create progress dialog
        JDialog progressDialog = new JDialog((Frame) null, "jsonScan - Fuzzing in Progress", false);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel statusLabel = new JLabel("Sending requests...");
        JProgressBar progressBar = new JProgressBar(0, permutations.size());
        progressBar.setStringPainted(true);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            orchestrator.cancel();
            progressDialog.dispose();
        });

        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(cancelButton, BorderLayout.SOUTH);

        progressDialog.add(panel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setVisible(true);

        // Start fuzzing
        orchestrator.fuzzAsync(
                request,
                permutations,
                progress -> {
                    // Update progress
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress.getCompleted());
                        statusLabel.setText(String.format("Sent %d / %d requests (%d%%)",
                                progress.getCompleted(),
                                progress.getTotal(),
                                progress.getPercentage()));
                        resultsTab.updateStatus(String.format("Fuzzing: %d%%", progress.getPercentage()));
                    });
                },
                result -> {
                    // Fuzzing completed
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();

                        if (result.hasError()) {
                            resultsTab.updateStatus("Error: " + result.getError().getMessage());
                            JOptionPane.showMessageDialog(null,
                                    "Fuzzing error: " + result.getError().getMessage(),
                                    "jsonScan Error",
                                    JOptionPane.ERROR_MESSAGE);
                        } else {
                            // Add baseline to results
                            List<jsonScan.models.FuzzResult> allResults = new ArrayList<>();
                            allResults.add(result.getBaseline());
                            allResults.addAll(result.getResults());

                            resultsTab.addResults(allResults);

                            int anomalyCount = result.getAnomalousResults().size();
                            resultsTab.updateStatus(String.format("Completed: %d requests sent, %d anomalies found",
                                    result.getResults().size(), anomalyCount));

                            api.logging().logToOutput(String.format(
                                    "[jsonScan] Fuzzing completed: %d requests, %d anomalies",
                                    result.getResults().size(), anomalyCount));

                            // Show completion dialog
                            JOptionPane.showMessageDialog(null,
                                    String.format("Fuzzing completed!\n\n" +
                                            "Requests sent: %d\n" +
                                            "Anomalies found: %d\n\n" +
                                            "View results in the jsonScan Results tab.",
                                            result.getResults().size(), anomalyCount),
                                    "jsonScan - Completed",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                }
        );
    }

    /**
     * Show permutation estimate
     */
    private void showEstimate(HttpRequest request) {
        String jsonBody = request.bodyToString();
        fuzzer.setEnabledCategories(settings.getEnabledCategories());
        fuzzer.setPlaceholderConfig(settings.getPlaceholderConfig());
        int estimate = fuzzer.estimatePermutationCount(jsonBody);

        StringBuilder message = new StringBuilder();
        message.append(String.format("Estimated permutations: %d\n\n", estimate));
        message.append("Enabled categories:\n");

        for (var category : settings.getEnabledCategories()) {
            message.append("  • ").append(category.getDisplayName()).append("\n");
        }

        JOptionPane.showMessageDialog(null,
                message.toString(),
                "jsonScan - Permutation Estimate",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
