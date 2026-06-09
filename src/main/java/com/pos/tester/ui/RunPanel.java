package com.pos.tester.ui;

import com.pos.tester.model.TestConfig;
import com.pos.tester.model.TestResult;
import com.pos.tester.tests.TestRunner;
import com.pos.tester.uitests.UITestRunner;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.Set;

public class RunPanel extends JPanel {
    private final TestConfig config;
    private final TestSelectionPanel selectionPanel;
    private final ResultsPanel resultsPanel;

    private final JTextPane logPane;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JButton runApiBtn;
    private final JButton runUiBtn;
    private final JButton runAllBtn;
    private final JButton cancelButton;

    private Thread testThread;
    private volatile boolean running = false;
    private TestRunner apiRunner;
    private UITestRunner uiRunner;

    private static final Color LOG_PASS    = new Color(39,  174, 96);
    private static final Color LOG_FAIL    = new Color(192, 57,  43);
    private static final Color LOG_SKIP    = new Color(127, 140, 141);
    private static final Color LOG_INFO    = new Color(41,  128, 185);
    private static final Color LOG_UI      = new Color(150, 0,   150);

    public RunPanel(TestConfig config, TestSelectionPanel selectionPanel, ResultsPanel resultsPanel) {
        this.config = config;
        this.selectionPanel = selectionPanel;
        this.resultsPanel   = resultsPanel;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── Controls ─────────────────────────────────────────────────────────
        JPanel controlPanel = new JPanel(new BorderLayout(6, 6));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        runApiBtn = makeBtn("▶  Run API Tests", new Color(39, 174, 96), Color.WHITE, 170, 36);
        runUiBtn  = makeBtn("◉  Run UI Tests",  new Color(150, 0, 150),  Color.WHITE, 160, 36);
        runAllBtn = makeBtn("⚡  Run All",       new Color(30, 100, 200), Color.WHITE, 130, 36);
        cancelButton = makeBtn("■  Cancel",      new Color(192, 57, 43),  Color.WHITE, 110, 36);
        JButton clearLogBtn = new JButton("Clear Log");

        runApiBtn.addActionListener(e -> startTests(true, false));
        runUiBtn.addActionListener(e ->  startTests(false, true));
        runAllBtn.addActionListener(e -> startTests(true, true));
        cancelButton.addActionListener(e -> cancel());
        clearLogBtn.addActionListener(e -> clearLog());
        cancelButton.setEnabled(false);

        btnRow.add(runApiBtn); btnRow.add(runUiBtn); btnRow.add(runAllBtn);
        btnRow.add(cancelButton); btnRow.add(clearLogBtn);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        progressBar.setPreferredSize(new Dimension(0, 22));

        statusLabel = new JLabel("Ready. Select modules and click a Run button.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.DARK_GRAY);

        controlPanel.add(btnRow, BorderLayout.NORTH);
        controlPanel.add(progressBar, BorderLayout.CENTER);
        controlPanel.add(statusLabel, BorderLayout.SOUTH);
        add(controlPanel, BorderLayout.NORTH);

        // ── Log pane ─────────────────────────────────────────────────────────
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("Consolas", Font.PLAIN, 12));
        logPane.setBackground(new Color(28, 28, 28));
        logPane.setForeground(Color.WHITE);

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Execution Log"));
        logPanel.add(new JScrollPane(logPane), BorderLayout.CENTER);
        add(logPanel, BorderLayout.CENTER);

        // Legend
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 4));
        legendPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        addLegend(legendPanel, "✓ PASS", LOG_PASS);
        addLegend(legendPanel, "✗ FAIL", LOG_FAIL);
        addLegend(legendPanel, "⊘ SKIP", LOG_SKIP);
        addLegend(legendPanel, "[API] API Test", LOG_INFO);
        addLegend(legendPanel, "[UI] UI Selenium", LOG_UI);
        add(legendPanel, BorderLayout.SOUTH);

        logInfo("POS Test Tool ready.");
        logInfo("• Click 'Run API Tests' → tests the FastAPI backend (port 5001)");
        logInfo("• Click 'Run UI Tests' → runs Selenium browser tests on Flask frontend (port 5000)");
        logInfo("• Click 'Run All' → runs both API + UI tests sequentially");
    }

    private void startTests(boolean runApi, boolean runUi) {
        Set<String> apiModules = selectionPanel.getSelectedApiModules();
        Set<String> uiModules  = selectionPanel.getSelectedUiModules();

        if (runApi && apiModules.isEmpty() && !runUi) {
            warn("No API modules selected. Go to Test Selection tab.");
            return;
        }
        if (runUi && uiModules.isEmpty() && !runApi) {
            warn("No UI modules selected. Go to Test Selection tab.");
            return;
        }

        // Pre-flight: browser check for UI tests
        if (runUi && !uiModules.isEmpty()) {
            UITestRunner checker = new UITestRunner(config);
            String check = checker.checkBrowserAvailable();
            if (check != null) {
                if (check.startsWith("warn:")) {
                    // Auto-switched – just inform user
                    logColored("⚠ " + check.substring(5), new Color(230, 126, 34));
                } else {
                    // Fatal: no browser found
                    JOptionPane.showMessageDialog(this,
                            "Browser Not Found\n\n" + check,
                            "Cannot Start UI Tests", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        setRunning(true);
        resultsPanel.clearResults();
        clearLog();
        progressBar.setValue(0);
        progressBar.setString("0%");

        logDivider();
        if (runApi) logInfo("API Tests selected: " + apiModules.size() + " modules → " + apiModules);
        if (runUi)  logColored("UI Tests selected: " + uiModules.size() + " modules → " + uiModules, LOG_UI);
        logDivider();

        testThread = new Thread(() -> {
            try {
                // ── API Tests ─────────────────────────────────────────────────
                if (runApi && !apiModules.isEmpty()) {
                    updateStatus("Connecting to API server " + config.getBaseUrl() + "...");
                    apiRunner = new TestRunner(config);
                    apiRunner.setOnTestComplete(this::handleResult);
                    apiRunner.setOnStatusUpdate(msg -> SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("[API] " + msg);
                        logInfo("[API] " + msg);
                    }));
                    apiRunner.setOnProgressUpdate(p -> {
                        if (!runUi) SwingUtilities.invokeLater(() -> setProgress(p));
                    });
                    apiRunner.runSelected(apiModules);
                }

                // ── UI Tests ──────────────────────────────────────────────────
                if (running && runUi && !uiModules.isEmpty()) {
                    logDivider();
                    logColored("Starting UI (Selenium) tests...", LOG_UI);
                    updateStatus("Launching browser: " + config.getBrowserType() + "...");
                    uiRunner = new UITestRunner(config);
                    uiRunner.setOnTestComplete(this::handleResult);
                    uiRunner.setOnStatusUpdate(msg -> SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("[UI] " + msg);
                        logColored("[UI] " + msg, LOG_UI);
                    }));
                    uiRunner.setOnProgressUpdate(p -> SwingUtilities.invokeLater(() -> setProgress(p)));
                    uiRunner.runSelected(uiModules);
                }

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    logError("FATAL ERROR: " + ex.getMessage());
                    statusLabel.setText("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(RunPanel.this,
                            "Test run failed:\n\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    setRunning(false);
                    setProgress(100);
                    progressBar.setString("Complete");
                    logDivider();
                    logInfo(String.format("FINAL: %d passed | %d failed | %d skipped",
                            resultsPanel.getPassCount(),
                            resultsPanel.getFailCount(),
                            resultsPanel.getSkipCount()));
                });
            }
        });
        testThread.setDaemon(true);
        testThread.start();
    }

    private void handleResult(TestResult result) {
        SwingUtilities.invokeLater(() -> {
            resultsPanel.addResult(result);
            boolean isUi = result.getModuleName().startsWith("[UI]");
            Color base = isUi ? LOG_UI : LOG_INFO;
            String line = String.format("%s [%s] %s::%s - %s (%dms)",
                    result.getStatusIcon(), result.getStatus(),
                    result.getModuleName(), result.getTestName(),
                    result.getMessage(), result.getDurationMs());
            Color color = switch (result.getStatus()) {
                case PASS -> LOG_PASS;
                case FAIL -> LOG_FAIL;
                case SKIP -> LOG_SKIP;
                default   -> base;
            };
            logColored(line, color);
            if (result.getStatus() == TestResult.Status.FAIL && result.getDetails() != null) {
                logColored("  └─ " + result.getDetails(), LOG_FAIL);
            }
        });
    }

    private void cancel() {
        if (apiRunner != null) apiRunner.cancel();
        if (uiRunner  != null) uiRunner.cancel();
        if (testThread != null) testThread.interrupt();
        cancelButton.setEnabled(false);
        updateStatus("Cancelling...");
        logColored("⚠ Tests cancelled by user", new Color(230, 126, 34));
    }

    private void setRunning(boolean r) {
        running = r;
        runApiBtn.setEnabled(!r);
        runUiBtn.setEnabled(!r);
        runAllBtn.setEnabled(!r);
        cancelButton.setEnabled(r);
    }

    private void setProgress(int p) {
        progressBar.setValue(p);
        progressBar.setString(p + "%");
    }

    private void updateStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private void clearLog() {
        SwingUtilities.invokeLater(() -> logPane.setText(""));
    }

    private void logDivider() {
        logColored("─".repeat(70), Color.GRAY);
    }

    private void logInfo(String msg) {
        logColored(msg, new Color(200, 200, 200));
    }

    private void logError(String msg) {
        logColored(msg, LOG_FAIL);
    }

    private void logColored(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = logPane.getStyledDocument();
            Style style = doc.addStyle("c", null);
            StyleConstants.setForeground(style, color);
            try {
                doc.insertString(doc.getLength(), msg + "\n", style);
                logPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    private JButton makeBtn(String text, Color bg, Color fg, int w, int h) {
        JButton btn = new JButton(text);
        btn.setBackground(bg); btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(w, h));
        return btn;
    }

    private void addLegend(JPanel panel, String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(color);
        panel.add(lbl);
    }
}
