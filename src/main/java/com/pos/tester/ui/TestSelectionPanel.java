package com.pos.tester.ui;

import com.pos.tester.model.TestConfig;
import com.pos.tester.tests.TestRunner;
import com.pos.tester.uitests.UITestRunner;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;

public class TestSelectionPanel extends JPanel {
    private final Map<String, JCheckBox> apiCheckboxes  = new LinkedHashMap<>();
    private final Map<String, JCheckBox> uiCheckboxes   = new LinkedHashMap<>();
    private JLabel countLabel;
    private final TestConfig config;

    // Test counts per module
    private static final Map<String, Integer> API_COUNTS = Map.of(
            "Authentication", 7,
            "Products", 11,
            "Orders", 7,
            "Invoices", 7,
            "Discount Codes", 8,
            "Shipments", 10,
            "Users & Permissions", 11,
            "Reports & Analytics", 11,
            "Customers & Warehouse", 13
    );
    private static final Map<String, Integer> UI_COUNTS = Map.of(
            "Login Page", 7,
            "POS Interface", 11,
            "Products Page", 9,
            "Orders & Invoices", 13,
            "Shipping Page", 10,
            "Customers & Discounts", 14
    );

    public TestSelectionPanel(TestConfig config) {
        this.config = config;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Select Tests to Run");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        add(title, BorderLayout.NORTH);

        // Split: left=API, right=UI
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);
        split.setDividerSize(6);

        // ── Left: API Tests ───────────────────────────────────────────────────
        JPanel apiSection = new JPanel(new BorderLayout());
        apiSection.setBorder(new TitledBorder(
                BorderFactory.createLineBorder(new Color(0, 100, 200), 2),
                "API Tests (Backend - FastAPI)",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(0, 100, 200)));

        JPanel apiList = new JPanel();
        apiList.setLayout(new BoxLayout(apiList, BoxLayout.Y_AXIS));

        TestRunner apiRunner = new TestRunner(config);
        apiRunner.getModuleDescriptions().forEach((name, desc) -> {
            JCheckBox cb = new JCheckBox(name, true);
            cb.setFont(new Font("Segoe UI", Font.BOLD, 12));
            cb.addActionListener(e -> updateCount());
            apiCheckboxes.put(name, cb);
            int count = API_COUNTS.getOrDefault(name, 5);
            apiList.add(buildRow(cb, desc, count + " tests", new Color(0, 100, 200)));
            apiList.add(Box.createVerticalStrut(2));
        });

        JScrollPane apiScroll = new JScrollPane(apiList);
        apiScroll.setBorder(null);
        apiSection.add(apiScroll, BorderLayout.CENTER);
        apiSection.add(buildModuleButtons(apiCheckboxes, true), BorderLayout.SOUTH);

        // ── Right: UI Tests ───────────────────────────────────────────────────
        JPanel uiSection = new JPanel(new BorderLayout());
        uiSection.setBorder(new TitledBorder(
                BorderFactory.createLineBorder(new Color(150, 0, 150), 2),
                "UI Tests (Frontend - Selenium)",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(150, 0, 150)));

        JPanel uiList = new JPanel();
        uiList.setLayout(new BoxLayout(uiList, BoxLayout.Y_AXIS));

        UITestRunner uiRunner = new UITestRunner(config);
        uiRunner.getModuleDescriptions().forEach((name, desc) -> {
            JCheckBox cb = new JCheckBox(name, false); // UI tests OFF by default
            cb.setFont(new Font("Segoe UI", Font.BOLD, 12));
            cb.addActionListener(e -> updateCount());
            uiCheckboxes.put(name, cb);
            int count = UI_COUNTS.getOrDefault(name, 5);
            uiList.add(buildRow(cb, desc, count + " tests", new Color(150, 0, 150)));
            uiList.add(Box.createVerticalStrut(2));
        });

        // UI Test note
        JLabel uiNote = new JLabel(
                "<html><i>⚠ Requires browser + Flask frontend (port 5000)</i></html>");
        uiNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        uiNote.setForeground(new Color(180, 100, 0));
        uiNote.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JPanel uiListWrapper = new JPanel(new BorderLayout());
        uiListWrapper.add(uiNote, BorderLayout.NORTH);
        uiListWrapper.add(new JScrollPane(uiList), BorderLayout.CENTER);

        JScrollPane uiScroll = new JScrollPane(uiListWrapper);
        uiScroll.setBorder(null);
        uiSection.add(uiScroll, BorderLayout.CENTER);
        uiSection.add(buildModuleButtons(uiCheckboxes, false), BorderLayout.SOUTH);

        split.setLeftComponent(apiSection);
        split.setRightComponent(uiSection);
        add(split, BorderLayout.CENTER);

        // Bottom bar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        countLabel = new JLabel();
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        countLabel.setForeground(Color.DARK_GRAY);
        bottomPanel.add(countLabel);

        JButton selectAllBtn = new JButton("Select All (API+UI)");
        JButton clearAllBtn  = new JButton("Deselect All");
        JButton apiOnlyBtn   = new JButton("API Only");
        JButton uiOnlyBtn    = new JButton("UI Only");

        styleBtn(selectAllBtn, new Color(0, 130, 0), Color.WHITE);
        styleBtn(clearAllBtn,  new Color(150, 0, 0),   Color.WHITE);
        styleBtn(apiOnlyBtn,   new Color(0, 90, 180),  Color.WHITE);
        styleBtn(uiOnlyBtn,    new Color(130, 0, 130),  Color.WHITE);

        selectAllBtn.addActionListener(e -> { setAll(apiCheckboxes, true); setAll(uiCheckboxes, true); updateCount(); });
        clearAllBtn.addActionListener(e ->  { setAll(apiCheckboxes, false); setAll(uiCheckboxes, false); updateCount(); });
        apiOnlyBtn.addActionListener(e ->   { setAll(apiCheckboxes, true);  setAll(uiCheckboxes, false); updateCount(); });
        uiOnlyBtn.addActionListener(e ->    { setAll(apiCheckboxes, false); setAll(uiCheckboxes, true);  updateCount(); });

        bottomPanel.add(selectAllBtn);
        bottomPanel.add(clearAllBtn);
        bottomPanel.add(apiOnlyBtn);
        bottomPanel.add(uiOnlyBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        updateCount();
    }

    private JPanel buildRow(JCheckBox cb, String desc, String countText, Color accent) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 1, 0, accent),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JLabel descLabel = new JLabel(desc);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        descLabel.setForeground(Color.GRAY);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(cb, BorderLayout.NORTH);
        textPanel.add(descLabel, BorderLayout.CENTER);

        JLabel countLabel = new JLabel(countText);
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        countLabel.setForeground(accent);

        row.add(textPanel, BorderLayout.CENTER);
        row.add(countLabel, BorderLayout.EAST);
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                cb.setSelected(!cb.isSelected());
                for (var l : cb.getActionListeners()) l.actionPerformed(null);
            }
        });
        return row;
    }

    private JPanel buildModuleButtons(Map<String, JCheckBox> checkboxes, boolean isApi) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton all  = new JButton("All");
        JButton none = new JButton("None");
        if (isApi) {
            JButton crit = new JButton("Critical");
            crit.addActionListener(e -> selectCriticalApi(checkboxes));
            p.add(crit);
        }
        all.addActionListener(e -> { setAll(checkboxes, true);  updateCount(); });
        none.addActionListener(e -> { setAll(checkboxes, false); updateCount(); });
        p.add(all); p.add(none);
        return p;
    }

    private void selectCriticalApi(Map<String, JCheckBox> cbs) {
        Set<String> critical = Set.of("Authentication", "Products", "Orders", "Invoices", "Users & Permissions");
        cbs.forEach((name, cb) -> cb.setSelected(critical.contains(name)));
        updateCount();
    }

    private void setAll(Map<String, JCheckBox> cbs, boolean selected) {
        cbs.values().forEach(cb -> cb.setSelected(selected));
    }

    private void styleBtn(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg); btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
    }

    private void updateCount() {
        long api = apiCheckboxes.values().stream().filter(JCheckBox::isSelected).count();
        long ui  = uiCheckboxes.values().stream().filter(JCheckBox::isSelected).count();
        countLabel.setText("Selected: " + api + " API module(s)  +  " + ui + " UI module(s)");
    }

    /** Returns selected API module names */
    public Set<String> getSelectedApiModules() {
        Set<String> s = new LinkedHashSet<>();
        apiCheckboxes.forEach((name, cb) -> { if (cb.isSelected()) s.add(name); });
        return s;
    }

    /** Returns selected UI module names */
    public Set<String> getSelectedUiModules() {
        Set<String> s = new LinkedHashSet<>();
        uiCheckboxes.forEach((name, cb) -> { if (cb.isSelected()) s.add(name); });
        return s;
    }

    public boolean hasApiModulesSelected() {
        return apiCheckboxes.values().stream().anyMatch(JCheckBox::isSelected);
    }

    public boolean hasUiModulesSelected() {
        return uiCheckboxes.values().stream().anyMatch(JCheckBox::isSelected);
    }
}
