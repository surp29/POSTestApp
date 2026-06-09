package com.pos.tester.ui;

import com.pos.tester.model.TestConfig;
import com.pos.tester.uitests.UITestRunner;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.Map;

public class ConfigPanel extends JPanel {
    // ── API fields ────────────────────────────────────────────────────────────
    private final JTextField      urlField;
    private final JTextField      usernameField;
    private final JPasswordField  passwordField;
    private final JSpinner        timeoutSpinner;
    private final JTextField      prefixField;
    private final JCheckBox       stopOnFailCheck;
    private final JCheckBox       verboseCheck;
    private final JSpinner        retriesSpinner;

    // ── UI / Selenium fields ──────────────────────────────────────────────────
    private final JTextField      frontendUrlField;
    private final JComboBox<TestConfig.BrowserType> browserCombo;
    private final JCheckBox       headlessCheck;
    private final JCheckBox       screenshotCheck;
    private final JTextField      screenshotDirField;
    private final JSpinner        uiWaitSpinner;
    private final JTextField      customBinField;
    private final JLabel          browserStatusLabel;

    public ConfigPanel(TestConfig config) {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // ════════════════════════════════════════════════════════════════
        //   Tab 1 – API Tests (Backend)
        // ════════════════════════════════════════════════════════════════
        JPanel apiTab = new JPanel(new GridBagLayout());
        apiTab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 6, 5, 6);
        g.anchor = GridBagConstraints.WEST;

        JPanel serverPanel = new JPanel(new GridBagLayout());
        serverPanel.setBorder(new TitledBorder("Backend Server (FastAPI – Port 5001)"));
        GridBagConstraints sg = gbc();

        urlField       = new JTextField(config.getBaseUrl(), 32);
        usernameField  = new JTextField(config.getAdminUsername(), 20);
        passwordField  = new JPasswordField(config.getAdminPassword(), 20);
        addLF(serverPanel, sg, 0, "API Base URL:",    urlField,      "http://localhost:5001");
        addLF(serverPanel, sg, 1, "Admin Username:",  usernameField, null);
        addLF(serverPanel, sg, 2, "Admin Password:",  passwordField, null);

        JPanel optPanel = new JPanel(new GridBagLayout());
        optPanel.setBorder(new TitledBorder("API Test Options"));
        GridBagConstraints og = gbc();
        timeoutSpinner  = new JSpinner(new SpinnerNumberModel(config.getTimeoutSeconds(),  5, 300, 5));
        retriesSpinner  = new JSpinner(new SpinnerNumberModel(config.getConnectionRetries(), 1, 10, 1));
        prefixField     = new JTextField(config.getTestDataPrefix(), 15);
        stopOnFailCheck = new JCheckBox("Stop on first failure", config.isStopOnFirstFailure());
        verboseCheck    = new JCheckBox("Verbose logging",        config.isVerboseLogging());
        addLF(optPanel, og, 0, "Request Timeout (sec):", timeoutSpinner, null);
        addLF(optPanel, og, 1, "Connection Retries:",    retriesSpinner, null);
        addLF(optPanel, og, 2, "Test Data Prefix:",      prefixField,    "Prefix for auto-created records");
        og.gridx=0; og.gridy=3; og.gridwidth=2; optPanel.add(stopOnFailCheck, og);
        og.gridy=4;                               optPanel.add(verboseCheck,   og);

        g.gridx=0; g.gridy=0; g.fill=GridBagConstraints.HORIZONTAL; g.weightx=1;
        apiTab.add(serverPanel, g);
        g.gridy=1; apiTab.add(optPanel, g);
        g.gridy=2; g.fill=GridBagConstraints.BOTH; g.weighty=1;
        apiTab.add(makeApiInfoPanel(), g);

        // ════════════════════════════════════════════════════════════════
        //   Tab 2 – UI Tests (Selenium)
        // ════════════════════════════════════════════════════════════════
        JPanel uiTab = new JPanel(new GridBagLayout());
        uiTab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints ug = gbc();

        // ── Frontend server ───────────────────────────────────────────
        JPanel fePanel = new JPanel(new GridBagLayout());
        fePanel.setBorder(new TitledBorder("Frontend Server (Flask – Port 5000)"));
        GridBagConstraints fg = gbc();
        frontendUrlField = new JTextField(config.getFrontendUrl(), 32);
        addLF(fePanel, fg, 0, "Frontend URL:", frontendUrlField, "http://localhost:5000");

        // ── Browser settings ──────────────────────────────────────────
        JPanel bsPanel = new JPanel(new GridBagLayout());
        bsPanel.setBorder(new TitledBorder("Browser Settings"));
        GridBagConstraints bg = gbc();

        // Detect installed browsers
        Map<TestConfig.BrowserType, String> detected = UITestRunner.detectInstalledBrowsers();
        browserCombo = new JComboBox<>(TestConfig.BrowserType.values());
        // Default to first detected browser
        if (!detected.isEmpty()) {
            browserCombo.setSelectedItem(detected.keySet().iterator().next());
        } else {
            browserCombo.setSelectedItem(config.getBrowserType());
        }

        // Status label showing detected browsers
        browserStatusLabel = new JLabel(buildBrowserStatusHtml(detected));
        browserStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        browserStatusLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        headlessCheck     = new JCheckBox("Headless mode (no browser window)", config.isHeadless());
        screenshotCheck   = new JCheckBox("Screenshot on test failure",         config.isScreenshotOnFail());
        uiWaitSpinner     = new JSpinner(new SpinnerNumberModel(config.getUiWaitSeconds(), 3, 60, 1));
        screenshotDirField = new JTextField(config.getScreenshotDir(), 30);

        // Custom binary path
        customBinField = new JTextField(config.getCustomBinaryPath(), 30);
        customBinField.setToolTipText("Leave blank to auto-detect browser path");
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> browseForBinary());

        // Refresh detect button
        JButton detectBtn = new JButton("Re-detect Browsers");
        detectBtn.addActionListener(e -> refreshBrowserStatus());

        addLF(bsPanel, bg, 0, "Browser:",          browserCombo,  null);
        bg.gridx=0; bg.gridy=1; bg.gridwidth=2; bg.fill=GridBagConstraints.HORIZONTAL;
        bsPanel.add(browserStatusLabel, bg);
        bg.gridy=2; bg.gridwidth=1; bg.fill=GridBagConstraints.NONE; bg.weightx=0;
        bsPanel.add(detectBtn, bg);
        bg.gridy=3; bg.gridwidth=2; bg.fill=GridBagConstraints.HORIZONTAL;
        bsPanel.add(headlessCheck, bg);
        bg.gridy=4; bsPanel.add(screenshotCheck, bg);
        addLF(bsPanel, bg, 5, "UI Wait (sec):", uiWaitSpinner, null);
        addLF(bsPanel, bg, 6, "Screenshot Dir:", screenshotDirField, null);

        // Custom binary row
        bg.gridy=7; bg.gridx=0; bg.gridwidth=1; bg.fill=GridBagConstraints.NONE;
        bsPanel.add(new JLabel("Custom .exe (optional):"), bg);
        bg.gridx=1; bg.fill=GridBagConstraints.HORIZONTAL; bg.weightx=1;
        bsPanel.add(customBinField, bg);
        bg.gridx=2; bg.fill=GridBagConstraints.NONE; bg.weightx=0;
        bsPanel.add(browseBtn, bg);

        ug.gridx=0; ug.gridy=0; ug.fill=GridBagConstraints.HORIZONTAL; ug.weightx=1;
        uiTab.add(fePanel, ug);
        ug.gridy=1; uiTab.add(bsPanel, ug);
        ug.gridy=2; ug.fill=GridBagConstraints.BOTH; ug.weighty=1;
        uiTab.add(makeUiInfoPanel(), ug);

        tabs.addTab("⚙  API Tests (Backend)",   apiTab);
        tabs.addTab("🌐  UI Tests (Selenium)",   uiTab);
        add(tabs, BorderLayout.CENTER);

        // Apply button
        JButton applyBtn = new JButton("Apply Settings");
        applyBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        applyBtn.setBackground(new Color(0, 120, 215));
        applyBtn.setForeground(Color.WHITE);
        applyBtn.addActionListener(e -> applyConfigWithFeedback(config));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(applyBtn);
        add(south, BorderLayout.SOUTH);
    }

    // ─── Browser detection helpers ────────────────────────────────────────────

    private String buildBrowserStatusHtml(Map<TestConfig.BrowserType, String> detected) {
        if (detected.isEmpty()) {
            return "<html><font color='red'>⚠ No browser detected automatically. Set custom path below.</font></html>";
        }
        StringBuilder sb = new StringBuilder("<html>");
        for (Map.Entry<TestConfig.BrowserType, String> e : detected.entrySet()) {
            sb.append("<font color='green'>✓ ")
              .append(e.getKey().toString())
              .append("</font> — <font color='gray'>")
              .append(e.getValue())
              .append("</font><br>");
        }
        sb.append("</html>");
        return sb.toString();
    }

    private void refreshBrowserStatus() {
        Map<TestConfig.BrowserType, String> detected = UITestRunner.detectInstalledBrowsers();
        browserStatusLabel.setText(buildBrowserStatusHtml(detected));
        if (!detected.isEmpty()) {
            browserCombo.setSelectedItem(detected.keySet().iterator().next());
        }
        JOptionPane.showMessageDialog(this,
                detected.isEmpty()
                    ? "No browsers detected. Please install Microsoft Edge or Opera One,\nor set a custom binary path below."
                    : "Detected: " + detected.size() + " browser(s):\n" +
                      detected.entrySet().stream()
                          .map(e -> "  " + e.getKey() + " → " + e.getValue())
                          .reduce("", (a, b) -> a + "\n" + b),
                "Browser Detection",
                detected.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    private void browseForBinary() {
        JFileChooser fc = new JFileChooser("C:\\Program Files");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Executable (*.exe)", "exe"));
        fc.setDialogTitle("Select Browser Executable");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            customBinField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    // ─── Info panels ──────────────────────────────────────────────────────────

    private JPanel makeApiInfoPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder("Quick Start – API Tests"));
        JTextArea t = new JTextArea("""
                1. Set API Base URL  →  http://localhost:5001
                2. Enter Admin username & password
                3. Go to "Test Selection" → choose API modules
                4. Go to "Run Tests" → click ▶ Run API Tests

                The tester calls FastAPI endpoints directly (no browser needed).
                """);
        t.setEditable(false); t.setBackground(getBackground());
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        p.add(new JScrollPane(t));
        return p;
    }

    private JPanel makeUiInfoPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder("How UI Tests Work"));
        JTextArea t = new JTextArea("""
                UI tests use Selenium WebDriver to control a real browser.

                Steps:
                  1. App downloads the correct browser driver automatically
                  2. Opens browser → navigates to /login
                  3. Logs in as admin
                  4. Goes to each page → clicks buttons, fills forms, verifies results
                  5. Takes screenshot on failure (saved to Screenshot Dir)
                  6. Closes browser and shows results

                Supported browsers:
                  • Microsoft Edge 149  — pre-installed on Windows 11 ✓
                  • Opera One 132       — install from opera.com, detected automatically

                EdgeDriver / OperaDriver are downloaded automatically on first run.
                Both Flask (port 5000) AND FastAPI (port 5001) must be running.
                """);
        t.setEditable(false); t.setBackground(getBackground());
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        p.add(new JScrollPane(t));
        return p;
    }

    // ─── Apply config ─────────────────────────────────────────────────────────

    public void applyConfig(TestConfig config) {
        config.setBaseUrl(urlField.getText());
        config.setAdminUsername(usernameField.getText());
        config.setAdminPassword(new String(passwordField.getPassword()));
        config.setTimeoutSeconds((Integer) timeoutSpinner.getValue());
        config.setConnectionRetries((Integer) retriesSpinner.getValue());
        config.setTestDataPrefix(prefixField.getText());
        config.setStopOnFirstFailure(stopOnFailCheck.isSelected());
        config.setVerboseLogging(verboseCheck.isSelected());

        config.setFrontendUrl(frontendUrlField.getText());
        config.setBrowserType((TestConfig.BrowserType) browserCombo.getSelectedItem());
        config.setHeadless(headlessCheck.isSelected());
        config.setScreenshotOnFail(screenshotCheck.isSelected());
        config.setScreenshotDir(screenshotDirField.getText());
        config.setUiWaitSeconds((Integer) uiWaitSpinner.getValue());
        config.setCustomBinaryPath(customBinField.getText());
    }

    public void applyConfigWithFeedback(TestConfig config) {
        applyConfig(config);
        JOptionPane.showMessageDialog(this, "Settings applied!", "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    // ─── Layout helpers ───────────────────────────────────────────────────────

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private void addLF(JPanel panel, GridBagConstraints gbc, int row,
                        String label, JComponent field, String tip) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        if (tip != null) field.setToolTipText(tip);
        panel.add(field, gbc);
        gbc.weightx = 0;
    }
}
