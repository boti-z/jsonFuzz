import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import jsonScan.core.JsonFuzzer;
import jsonScan.ui.JsonScanContextMenu;
import jsonScan.ui.JsonScanSettings;
import jsonScan.ui.JsonScanTab;

/**
 * jsonScan - JSON Fuzzer for Burp Suite
 *
 * A comprehensive JSON fuzzer designed to find discrepancies between different JSON parsers.
 * Tests for duplicate keys, Unicode variations, number edge cases, comment injection,
 * whitespace handling, and more.
 *
 * @author Based on Python prototype by zharti
 */
public class Extension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        // Set extension name
        api.extension().setName("jsonScan");

        // Log initialization
        api.logging().logToOutput("========================================");
        api.logging().logToOutput("jsonScan - JSON Fuzzer Extension");
        api.logging().logToOutput("========================================");
        api.logging().logToOutput("Initializing extension...");

        try {
            // Initialize core fuzzer
            JsonFuzzer fuzzer = new JsonFuzzer();
            api.logging().logToOutput("✓ Core fuzzer initialized");

            // Initialize UI components
            JsonScanSettings settings = new JsonScanSettings(api);
            settings.register();
            api.logging().logToOutput("✓ Settings panel registered");

            JsonScanTab resultsTab = new JsonScanTab(api);
            resultsTab.register();
            api.logging().logToOutput("✓ Results tab registered");

            // Initialize context menu
            JsonScanContextMenu contextMenu = new JsonScanContextMenu(api, fuzzer, settings, resultsTab);
            api.userInterface().registerContextMenuItemsProvider(contextMenu);
            api.logging().logToOutput("✓ Context menu registered");

            // Log available test categories
            api.logging().logToOutput("");
            api.logging().logToOutput("Available test categories:");
            for (var category : jsonScan.models.TestCategory.values()) {
                api.logging().logToOutput("  • " + category.getDisplayName() + ": " + category.getDescription());
            }

            api.logging().logToOutput("");
            api.logging().logToOutput("========================================");
            api.logging().logToOutput("jsonScan loaded successfully!");
            api.logging().logToOutput("========================================");
            api.logging().logToOutput("Usage:");
            api.logging().logToOutput("1. Configure test categories in 'jsonScan Settings' tab");
            api.logging().logToOutput("2. Right-click on any HTTP request with JSON body");
            api.logging().logToOutput("3. Select 'jsonScan: Fuzz!' from context menu");
            api.logging().logToOutput("4. View results in 'jsonScan Results' tab");
            api.logging().logToOutput("========================================");

        } catch (Exception e) {
            api.logging().logToError("Failed to initialize jsonScan: " + e.getMessage());
            e.printStackTrace();
        }
    }
}