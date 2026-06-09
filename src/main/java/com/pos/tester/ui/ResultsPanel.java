package com.pos.tester.ui;

import com.pos.tester.model.TestResult;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ResultsPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable resultsTable;
    private final JLabel passLabel, failLabel, skipLabel, totalLabel;
    private final JTextArea detailArea;
    private int passCount = 0, failCount = 0, skipCount = 0;

    private static final Color PASS_COLOR = new Color(39, 174, 96);
    private static final Color FAIL_COLOR = new Color(192, 57, 43);
    private static final Color SKIP_COLOR = new Color(127, 140, 141);
    private static final Color RUNNING_COLOR = new Color(41, 128, 185);

    public ResultsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Summary bar
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        summaryPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        totalLabel = makeSummaryLabel("Total: 0", Color.DARK_GRAY);
        passLabel = makeSummaryLabel("✓ Passed: 0", PASS_COLOR);
        failLabel = makeSummaryLabel("✗ Failed: 0", FAIL_COLOR);
        skipLabel = makeSummaryLabel("⊘ Skipped: 0", SKIP_COLOR);

        summaryPanel.add(totalLabel);
        summaryPanel.add(passLabel);
        summaryPanel.add(failLabel);
        summaryPanel.add(skipLabel);

        JButton exportBtn = new JButton("Export");
        exportBtn.addActionListener(e -> exportResults());
        JButton copyAllBtn = new JButton("Copy All");
        copyAllBtn.setToolTipText("Copy all test results to clipboard");
        copyAllBtn.addActionListener(e -> copyFiltered(null));
        JButton copyFailBtn = new JButton("Copy Failed");
        copyFailBtn.setForeground(FAIL_COLOR);
        copyFailBtn.setToolTipText("Copy only FAIL lines to clipboard");
        copyFailBtn.addActionListener(e -> copyFiltered("FAIL"));
        JButton copyPassBtn = new JButton("Copy Passed");
        copyPassBtn.setForeground(PASS_COLOR);
        copyPassBtn.setToolTipText("Copy only PASS lines to clipboard");
        copyPassBtn.addActionListener(e -> copyFiltered("PASS"));
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> clearResults());

        summaryPanel.add(Box.createHorizontalStrut(20));
        summaryPanel.add(exportBtn);
        summaryPanel.add(copyAllBtn);
        summaryPanel.add(copyFailBtn);
        summaryPanel.add(copyPassBtn);
        summaryPanel.add(clearBtn);

        add(summaryPanel, BorderLayout.NORTH);

        // Results table
        String[] columns = {"Time", "Status", "Module", "Test", "Message", "Duration"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(24);
        resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setAutoCreateRowSorter(true);

        // Column widths
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(250);
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(300);
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(70);

        // Custom renderer for status column
        resultsTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                if (!isSelected) {
                    c.setForeground(switch (status) {
                        case "PASS" -> PASS_COLOR;
                        case "FAIL" -> FAIL_COLOR;
                        case "SKIP" -> SKIP_COLOR;
                        default -> RUNNING_COLOR;
                    });
                }
                setHorizontalAlignment(CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                return c;
            }
        });

        // Row color renderer
        resultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    String status = table.getValueAt(row, 1) != null ? table.getValueAt(row, 1).toString() : "";
                    c.setBackground(switch (status) {
                        case "FAIL" -> new Color(255, 240, 240);
                        case "PASS" -> new Color(240, 255, 240);
                        case "SKIP" -> new Color(248, 248, 248);
                        default -> Color.WHITE;
                    });
                    c.setForeground(col == 1 ? switch (status) {
                        case "PASS" -> PASS_COLOR;
                        case "FAIL" -> FAIL_COLOR;
                        case "SKIP" -> SKIP_COLOR;
                        default -> RUNNING_COLOR;
                    } : Color.DARK_GRAY);
                }
                return c;
            }
        });

        // Show detail on row click
        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });

        // Detail area
        detailArea = new JTextArea(5, 40);
        detailArea.setEditable(false);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailArea.setBorder(BorderFactory.createTitledBorder("Detail / Error Message"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(resultsTable),
                new JScrollPane(detailArea));
        splitPane.setResizeWeight(0.75);
        splitPane.setDividerSize(5);

        add(splitPane, BorderLayout.CENTER);
    }

    private JLabel makeSummaryLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(color);
        return label;
    }

    public void addResult(TestResult result) {
        SwingUtilities.invokeLater(() -> {
            String statusText = result.getStatus().toString();
            tableModel.addRow(new Object[]{
                    result.getTimestamp(),
                    statusText,
                    result.getModuleName(),
                    result.getTestName(),
                    result.getMessage() != null ? result.getMessage() : "",
                    result.getDurationMs() + "ms"
            });
            // Scroll to last row
            resultsTable.scrollRectToVisible(
                    resultsTable.getCellRect(tableModel.getRowCount() - 1, 0, true));

            // Update counts
            switch (result.getStatus()) {
                case PASS -> passCount++;
                case FAIL -> failCount++;
                case SKIP -> skipCount++;
            }
            updateSummary();
        });
    }

    private void updateSummary() {
        int total = passCount + failCount + skipCount;
        totalLabel.setText("Total: " + total);
        passLabel.setText("✓ Passed: " + passCount);
        failLabel.setText("✗ Failed: " + failCount);
        skipLabel.setText("⊘ Skipped: " + skipCount);
    }

    private void showDetail() {
        int row = resultsTable.getSelectedRow();
        if (row < 0) return;
        int modelRow = resultsTable.convertRowIndexToModel(row);
        String module = tableModel.getValueAt(modelRow, 2).toString();
        String test = tableModel.getValueAt(modelRow, 3).toString();
        String status = tableModel.getValueAt(modelRow, 1).toString();
        String message = tableModel.getValueAt(modelRow, 4).toString();
        String duration = tableModel.getValueAt(modelRow, 5).toString();
        detailArea.setText(String.format(
                "Module:   %s%nTest:     %s%nStatus:   %s%nDuration: %s%n%nMessage:%n%s",
                module, test, status, duration, message));
    }

    public void clearResults() {
        tableModel.setRowCount(0);
        passCount = failCount = skipCount = 0;
        detailArea.setText("");
        updateSummary();
    }

    private void exportResults() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("pos_test_results_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
            pw.println("POS Test Results - " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pw.println("=".repeat(80));
            pw.printf("Total: %d  |  Passed: %d  |  Failed: %d  |  Skipped: %d%n",
                    passCount + failCount + skipCount, passCount, failCount, skipCount);
            pw.println("=".repeat(80));
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                pw.printf("[%s] %-8s | %-22s | %-45s | %s%n",
                        tableModel.getValueAt(i, 0),
                        tableModel.getValueAt(i, 1),
                        tableModel.getValueAt(i, 2),
                        tableModel.getValueAt(i, 3),
                        tableModel.getValueAt(i, 4));
            }
            JOptionPane.showMessageDialog(this, "Results exported to:\n" + fc.getSelectedFile().getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copyFiltered(String statusFilter) {
        StringBuilder sb = new StringBuilder();
        String label = statusFilter == null ? "All" : statusFilter.charAt(0) + statusFilter.substring(1).toLowerCase();
        sb.append("POS Test Results (").append(label).append(") - ")
          .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("Passed: ").append(passCount).append(" | Failed: ").append(failCount)
          .append(" | Skipped: ").append(skipCount).append("\n\n");
        int copied = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String status = tableModel.getValueAt(i, 1).toString();
            if (statusFilter != null && !statusFilter.equals(status)) continue;
            sb.append(String.format("[%s] %s | %s::%s - %s%n",
                    status,
                    tableModel.getValueAt(i, 0),
                    tableModel.getValueAt(i, 2),
                    tableModel.getValueAt(i, 3),
                    tableModel.getValueAt(i, 4)));
            copied++;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(sb.toString()), null);
        JOptionPane.showMessageDialog(this, copied + " " + label + " result(s) copied to clipboard!");
    }

    public int getPassCount() { return passCount; }
    public int getFailCount() { return failCount; }
    public int getSkipCount() { return skipCount; }
}
