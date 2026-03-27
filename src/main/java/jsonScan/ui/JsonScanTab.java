package jsonScan.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import jsonScan.models.FuzzResult;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main results tab for displaying fuzzing results
 */
public class JsonScanTab {
    private final MontoyaApi api;
    private final JPanel mainPanel;
    private final ResultsTableModel tableModel;
    private final JTable resultsTable;
    private final HttpRequestEditor requestEditor;
    private final HttpResponseEditor responseEditor;
    private final JLabel statusLabel;

    public JsonScanTab(MontoyaApi api) {
        this.api = api;
        this.tableModel = new ResultsTableModel();
        this.requestEditor = api.userInterface().createHttpRequestEditor();
        this.responseEditor = api.userInterface().createHttpResponseEditor();
        this.resultsTable = new JTable(tableModel);
        this.statusLabel = new JLabel("Ready");
        this.mainPanel = createMainPanel();
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Status bar at top
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Status: "));
        statusPanel.add(statusLabel);
        panel.add(statusPanel, BorderLayout.NORTH);

        // Split pane for table and editors
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.4);

        // Top: Results table
        JPanel tablePanel = createTablePanel();
        mainSplitPane.setTopComponent(tablePanel);

        // Bottom: Request/Response editors
        JSplitPane editorSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        editorSplitPane.setResizeWeight(0.5);
        editorSplitPane.setLeftComponent(createEditorPanel("Request", requestEditor.uiComponent()));
        editorSplitPane.setRightComponent(createEditorPanel("Response", responseEditor.uiComponent()));
        mainSplitPane.setBottomComponent(editorSplitPane);

        panel.add(mainSplitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Configure table
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displaySelectedResult();
            }
        });

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Control panel (bottom)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton clearButton = new JButton("Clear Results");
        clearButton.addActionListener(e -> clearResults());
        controlPanel.add(clearButton);

        JButton exportButton = new JButton("Export to CSV");
        exportButton.addActionListener(e -> exportResults());
        controlPanel.add(exportButton);

        panel.add(controlPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createEditorPanel(String title, Component editorComponent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(editorComponent, BorderLayout.CENTER);
        return panel;
    }

    private void displaySelectedResult() {
        int selectedRow = resultsTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = resultsTable.convertRowIndexToModel(selectedRow);
            FuzzResult result = tableModel.getResultAt(modelRow);

            if (result != null) {
                requestEditor.setRequest(result.getRequest());
                if (result.getResponse() != null) {
                    responseEditor.setResponse(result.getResponse());
                } else {
                    // Clear response editor if no response
                    responseEditor.setResponse(HttpResponse.httpResponse(""));
                }
            }
        }
    }

    /**
     * Add results to the table
     */
    public void addResults(List<FuzzResult> results) {
        SwingUtilities.invokeLater(() -> {
            tableModel.addResults(results);
            updateStatus(results.size() + " results loaded");
        });
    }

    /**
     * Clear all results
     */
    public void clearResults() {
        SwingUtilities.invokeLater(() -> {
            tableModel.clearResults();
            requestEditor.setRequest(null);
            responseEditor.setResponse(null);
            updateStatus("Results cleared");
        });
    }

    private void exportResults() {
        int resultCount = tableModel.getRowCount();
        JOptionPane.showMessageDialog(mainPanel,
                String.format("CSV export functionality coming soon!\n\nTotal results: %d", resultCount),
                "Export Results",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Update status message
     */
    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
        });
    }

    /**
     * Get the main panel component
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * Register this tab with Burp
     */
    public void register() {
        api.userInterface().registerSuiteTab("jsonScan Results", mainPanel);
    }

    private static class ResultsTableModel extends AbstractTableModel {
        private final List<FuzzResult> results;
        private final String[] columnNames = {"#", "ID", "Category", "Description", "Status", "Length", "Time (ms)"};

        public ResultsTableModel() {
            this.results = new ArrayList<>();
        }

        public void addResults(List<FuzzResult> newResults) {
            results.addAll(newResults);
            fireTableDataChanged();
        }

        public void clearResults() {
            results.clear();
            fireTableDataChanged();
        }

        public FuzzResult getResultAt(int row) {
            if (row >= 0 && row < results.size()) {
                return results.get(row);
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return results.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 1, 4, 5, 6 -> Integer.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            FuzzResult result = results.get(rowIndex);

            if (result.getPermutation() == null) {
                return switch (columnIndex) {
                    case 0 -> rowIndex + 1;
                    case 1 -> 0;
                    case 2 -> "BASELINE";
                    case 3 -> "Original request";
                    case 4 -> result.getStatusCode();
                    case 5 -> result.getResponseLength();
                    case 6 -> result.getResponseTimeMs();
                    default -> "";
                };
            }

            return switch (columnIndex) {
                case 0 -> rowIndex + 1;
                case 1 -> result.getPermutation().getPermutationId();
                case 2 -> result.getPermutation().getCategory().getDisplayName();
                case 3 -> result.getPermutation().getDescription();
                case 4 -> result.getStatusCode() > 0 ? result.getStatusCode() : -1;
                case 5 -> result.getResponseLength();
                case 6 -> result.getResponseTimeMs();
                default -> "";
            };
        }
    }
}
