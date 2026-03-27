package jsonScan.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.settings.SettingsPanel;
import jsonScan.models.PlaceholderConfig;
import jsonScan.models.TestCategory;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Settings panel for jsonScan extension
 */
public class JsonScanSettings {
    private final MontoyaApi api;
    private final Map<TestCategory, JCheckBox> categoryCheckboxes;
    private final JPanel mainPanel;
    private final JTextField stringPlaceholderField;
    private final JTextField numberPlaceholderField;
    private final JTextField booleanPlaceholderField;

    public JsonScanSettings(MontoyaApi api) {
        this.api = api;
        this.categoryCheckboxes = new EnumMap<>(TestCategory.class);
        this.stringPlaceholderField = new JTextField(15);
        this.numberPlaceholderField = new JTextField(15);
        this.booleanPlaceholderField = new JTextField(15);
        this.mainPanel = createSettingsPanel();
        loadDefaultPlaceholders();
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("jsonScan - JSON Fuzzer Configuration");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Description
        JTextArea descriptionArea = new JTextArea(
                "Select which test categories to enable for JSON fuzzing. " +
                "Each category tests for different JSON parser discrepancies."
        );
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setEditable(false);
        descriptionArea.setBackground(panel.getBackground());
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionArea.setMaximumSize(new Dimension(600, 50));
        panel.add(descriptionArea);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Test Categories Section
        JLabel categoriesLabel = new JLabel("Test Categories:");
        categoriesLabel.setFont(new Font(categoriesLabel.getFont().getName(), Font.BOLD, 14));
        categoriesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(categoriesLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add checkboxes for standard categories (excluding DoS risk tests)
        for (TestCategory category : TestCategory.values()) {
            if (category == TestCategory.LARGE_PAYLOADS || category == TestCategory.DEEP_NESTING) {
                continue;
            }

            JPanel categoryPanel = new JPanel();
            categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.Y_AXIS));
            categoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            categoryPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 0));

            JCheckBox checkbox = new JCheckBox(category.getDisplayName());
            checkbox.setSelected(true);
            checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel descLabel = new JLabel(category.getDescription());
            descLabel.setFont(new Font(descLabel.getFont().getName(), Font.ITALIC, 11));
            descLabel.setForeground(Color.GRAY);
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            descLabel.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));

            categoryPanel.add(checkbox);
            categoryPanel.add(descLabel);

            panel.add(categoryPanel);
            categoryCheckboxes.put(category, checkbox);
        }

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // DoS Risk Tests Section
        JPanel dosPanel = createDoSRiskPanel();
        dosPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(dosPanel);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Placeholder Configuration Section
        JLabel placeholderLabel = new JLabel("Placeholder Configuration:");
        placeholderLabel.setFont(new Font(placeholderLabel.getFont().getName(), Font.BOLD, 14));
        placeholderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(placeholderLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel placeholderPanel = createPlaceholderPanel();
        placeholderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(placeholderPanel);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Bulk actions
        JPanel bulkActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bulkActionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> setAllCategories(true));

        JButton deselectAllButton = new JButton("Deselect All");
        deselectAllButton.addActionListener(e -> setAllCategories(false));

        bulkActionsPanel.add(selectAllButton);
        bulkActionsPanel.add(deselectAllButton);
        panel.add(bulkActionsPanel);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createDoSRiskPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel warningLabel = new JLabel("⚠️ DoS Risk Tests (use with caution):");
        warningLabel.setFont(new Font(warningLabel.getFont().getName(), Font.PLAIN, 11));
        warningLabel.setForeground(Color.DARK_GRAY);
        warningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        warningLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 0));
        panel.add(warningLabel);

        TestCategory[] dosCategories = {TestCategory.LARGE_PAYLOADS, TestCategory.DEEP_NESTING};

        for (TestCategory category : dosCategories) {
            JPanel categoryPanel = new JPanel();
            categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.Y_AXIS));
            categoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            categoryPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 0));

            JCheckBox checkbox = new JCheckBox("⚠️ " + category.getDisplayName());
            checkbox.setSelected(true);
            checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel descLabel = new JLabel(category.getDescription());
            descLabel.setFont(new Font(descLabel.getFont().getName(), Font.ITALIC, 11));
            descLabel.setForeground(Color.GRAY);
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            descLabel.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));

            categoryPanel.add(checkbox);
            categoryPanel.add(descLabel);

            panel.add(categoryPanel);
            categoryCheckboxes.put(category, checkbox);
        }

        return panel;
    }

    private JPanel createPlaceholderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("String Placeholder:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(stringPlaceholderField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton randomizeStringBtn = new JButton("Randomize");
        randomizeStringBtn.addActionListener(e -> stringPlaceholderField.setText(PlaceholderConfig.generateRandomString()));
        panel.add(randomizeStringBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Number Placeholder:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(numberPlaceholderField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton randomizeNumberBtn = new JButton("Randomize");
        randomizeNumberBtn.addActionListener(e -> numberPlaceholderField.setText(PlaceholderConfig.generateRandomNumber()));
        panel.add(randomizeNumberBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Boolean Placeholder:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(booleanPlaceholderField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton randomizeBooleanBtn = new JButton("Randomize");
        randomizeBooleanBtn.addActionListener(e -> booleanPlaceholderField.setText(PlaceholderConfig.generateRandomBoolean()));
        panel.add(randomizeBooleanBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        JButton resetBtn = new JButton("Reset to Defaults");
        resetBtn.addActionListener(e -> loadDefaultPlaceholders());
        panel.add(resetBtn, gbc);

        return panel;
    }

    private void loadDefaultPlaceholders() {
        PlaceholderConfig defaults = new PlaceholderConfig();
        stringPlaceholderField.setText(defaults.getStringPlaceholder());
        numberPlaceholderField.setText(defaults.getNumberPlaceholder());
        booleanPlaceholderField.setText(defaults.getBooleanPlaceholder());
    }

    private void setAllCategories(boolean enabled) {
        for (JCheckBox checkbox : categoryCheckboxes.values()) {
            checkbox.setSelected(enabled);
        }
    }

    /**
     * Get the currently enabled categories
     */
    public Set<TestCategory> getEnabledCategories() {
        Set<TestCategory> enabled = EnumSet.noneOf(TestCategory.class);
        for (Map.Entry<TestCategory, JCheckBox> entry : categoryCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                enabled.add(entry.getKey());
            }
        }
        return enabled;
    }

    /**
     * Set enabled categories
     */
    public void setEnabledCategories(Set<TestCategory> categories) {
        for (Map.Entry<TestCategory, JCheckBox> entry : categoryCheckboxes.entrySet()) {
            entry.getValue().setSelected(categories.contains(entry.getKey()));
        }
    }

    /**
     * Get the current placeholder configuration from UI
     */
    public PlaceholderConfig getPlaceholderConfig() {
        String stringPlaceholder = stringPlaceholderField.getText().trim();
        String numberPlaceholder = numberPlaceholderField.getText().trim();
        String booleanPlaceholder = booleanPlaceholderField.getText().trim();

        if (stringPlaceholder.isEmpty()) stringPlaceholder = "FZZ123";
        if (numberPlaceholder.isEmpty()) numberPlaceholder = "999999";
        if (booleanPlaceholder.isEmpty()) booleanPlaceholder = "true";

        return new PlaceholderConfig(stringPlaceholder, numberPlaceholder, booleanPlaceholder);
    }

    /**
     * Set placeholder configuration in UI
     */
    public void setPlaceholderConfig(PlaceholderConfig config) {
        stringPlaceholderField.setText(config.getStringPlaceholder());
        numberPlaceholderField.setText(config.getNumberPlaceholder());
        booleanPlaceholderField.setText(config.getBooleanPlaceholder());
    }

    /**
     * Get the settings panel component
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * Register this settings panel with Burp
     */
    public void register() {
        UserInterface ui = api.userInterface();
        ui.registerSuiteTab("jsonScan Settings", mainPanel);
    }
}
