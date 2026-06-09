package com.pos.tester.ui;

import com.pos.tester.model.TestConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {
    private final TestConfig config;
    private final ConfigPanel configPanel;
    private final TestSelectionPanel selectionPanel;
    private final ResultsPanel resultsPanel;
    private final RunPanel runPanel;
    private final JTabbedPane tabbedPane;

    public MainFrame() {
        this.config = new TestConfig();

        setTitle("POS System Test Tool v2.0 — API + UI Selenium");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1150, 760);
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);
        setIconImage(createAppIcon());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int r = JOptionPane.showConfirmDialog(MainFrame.this,
                        "Exit POS Test Tool?", "Confirm Exit",
                        JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) System.exit(0);
            }
        });

        configPanel    = new ConfigPanel(config);
        selectionPanel = new TestSelectionPanel(config);
        resultsPanel   = new ResultsPanel();
        runPanel       = new RunPanel(config, selectionPanel, resultsPanel);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabbedPane.addTab("⚙  Configuration",  configPanel);
        tabbedPane.addTab("☑  Test Selection",  selectionPanel);
        tabbedPane.addTab("▶  Run Tests",        runPanel);
        tabbedPane.addTab("📊  Results",          resultsPanel);

        // Apply config when leaving config tab
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() != 0) configPanel.applyConfig(config);
        });

        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);
        add(tabbedPane,     BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(22, 40, 80));
        header.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("POS System Test Tool");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("API Testing (FastAPI)  +  UI Testing (Selenium WebDriver)");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(160, 190, 255));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(sub);

        JLabel ver = new JLabel("v2.0  |  Java 17  |  Selenium 4");
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ver.setForeground(new Color(130, 160, 220));

        header.add(textPanel, BorderLayout.WEST);
        header.add(ver, BorderLayout.EAST);
        return header;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                new EmptyBorder(3, 10, 3, 10)));

        JLabel left = new JLabel("API: " + config.getBaseUrl() +
                "  |  Frontend: " + config.getFrontendUrl());
        left.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        left.setForeground(Color.GRAY);

        JLabel right = new JLabel("Java " + System.getProperty("java.version") +
                " | " + System.getProperty("os.name"));
        right.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        right.setForeground(Color.GRAY);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private Image createAppIcon() {
        int sz = 32;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                sz, sz, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(22, 40, 80));
        g.fillRoundRect(0, 0, sz, sz, 8, 8);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        String t = "POS";
        g.drawString(t, (sz - fm.stringWidth(t)) / 2, (sz + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return img;
    }
}
