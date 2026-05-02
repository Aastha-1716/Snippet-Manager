package com.snippetmanager.ui.dashboard;

import com.snippetmanager.model.Snippet;
import com.snippetmanager.service.SnippetService;
import com.snippetmanager.ui.SyntaxHighlightTextPane;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class SnippetDashboardFrame extends JFrame {
    private static final int FILTER_REFRESH_DELAY_MS = 180;
    private static final DateTimeFormatter LIST_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Color APP_BG_TOP = new Color(234, 243, 255);
    private static final Color APP_BG_BOTTOM = new Color(220, 231, 246);
    private static final Color SURFACE_BG = new Color(244, 248, 252);
    private static final Color CARD_BORDER = new Color(180, 200, 220);
    private static final Color INPUT_BG = new Color(250, 252, 255);
    private static final Color TEXT_PRIMARY = new Color(18, 30, 48);
    private static final Color TEXT_MUTED = new Color(110, 125, 145);
    private static final Color ACCENT = new Color(26, 105, 217);
    private static final Color ACCENT_SOFT = new Color(230, 240, 255);
    private static final Color BUTTON_NEUTRAL = new Color(236, 242, 251);
    private static final Color DARK_PANEL = new Color(16, 22, 34);

    private static final Font BASE_FONT = new Font("Segoe UI Variable", Font.PLAIN, 13);
    private static final Font BASE_FONT_BOLD = new Font("Segoe UI Variable", Font.BOLD, 13);
    private static final Font TITLE_FONT = new Font("Segoe UI Variable", Font.BOLD, 28);
    private static final Font SUBTITLE_FONT = new Font("Segoe UI Variable", Font.PLAIN, 14);
    private static final Font CODE_FONT = new Font("JetBrains Mono", Font.PLAIN, 14);

    private static final float CODE_FONT_SIZE = 12f;
    private static final Font KPI_VALUE_FONT = new Font("Segoe UI Variable", Font.BOLD, 20);
    private static final Font KPI_LABEL_FONT = new Font("Segoe UI Variable", Font.PLAIN, 12);

    private static final List<String> DEFAULT_LANGUAGES = List.of(
            "All",
            "Java",
            "JavaScript",
            "TypeScript",
            "Python",
            "C++",
            "Go",
            "Rust",
            "SQL",
            "Bash",
            "Text"
    );

    private final SnippetService service;

    private final DefaultListModel<Snippet> snippetListModel = new DefaultListModel<>();
    private final JList<Snippet> snippetList = new JList<>(snippetListModel);

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> languageFilter = new JComboBox<>(new DefaultComboBoxModel<>(DEFAULT_LANGUAGES.toArray(String[]::new)));
    private final JTextField tagFilter = new JTextField();

    private final JTextField titleField = new JTextField();
    private final JComboBox<String> languageField = new JComboBox<>(new DefaultComboBoxModel<>(DEFAULT_LANGUAGES.stream().filter(v -> !"All".equals(v)).toArray(String[]::new)));
    private final JTextField tagsField = new JTextField();
    private final SyntaxHighlightTextPane codeEditor = new SyntaxHighlightTextPane();
    private final Timer filterRefreshTimer;

    private final JLabel totalCountValue = new JLabel("0");
    private final JLabel languageCountValue = new JLabel("0");
    private final JLabel tagCountValue = new JLabel("0");
    private final JLabel selectedCountValue = new JLabel("0");
    private final JLabel selectedMetaValue = new JLabel("Select a snippet to inspect its details.");

    private Snippet currentSnippet;

    public SnippetDashboardFrame(SnippetService service) {
        this.service = service;
        this.filterRefreshTimer = new Timer(FILTER_REFRESH_DELAY_MS, e -> refreshSnippetList());
        this.filterRefreshTimer.setRepeats(false);

        applyLookAndFeel();
        setTitle("Snippet Studio");
        setSize(1360, 860);
        setMinimumSize(new Dimension(1080, 720));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUi();
        refreshSnippetList();
        createNewSnippet();
    }

    private void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void initUi() {
        JPanel root = new JPanel(new BorderLayout(16, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setPaint(new GradientPaint(0, 0, APP_BG_TOP, 0, getHeight(), APP_BG_BOTTOM));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = buildHeader();
        JPanel metrics = buildMetricsRow();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLibraryPane(), buildEditorPane());
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.32);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(metrics, BorderLayout.NORTH);
        center.add(splitPane, BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(buildFooterBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);

        JPanel copy = new JPanel(new GridLayout(3, 1, 0, 3));
        copy.setOpaque(false);

        JLabel title = new JLabel("Snippet Studio");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("A clean DSA dashboard for saving, searching, and exporting reusable algorithms");
        subtitle.setFont(SUBTITLE_FONT);
        subtitle.setForeground(TEXT_MUTED);

        JPanel badgeRow = new JPanel();
        badgeRow.setOpaque(false);
        badgeRow.add(createBadge("Railway MySQL", ACCENT_SOFT, TEXT_PRIMARY));
        badgeRow.add(createBadge("LeetCode-style editor", new Color(226, 247, 235), new Color(32, 126, 83)));

        copy.add(title);
        copy.add(subtitle);
        copy.add(badgeRow);
        header.add(copy, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildMetricsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setOpaque(false);
        row.add(createMetricCard("Total snippets", totalCountValue, "All stored algorithms"));
        row.add(createMetricCard("Languages", languageCountValue, "Distinct language filters"));
        row.add(createMetricCard("Tags", tagCountValue, "Total unique tags"));
        row.add(createMetricCard("Selected", selectedCountValue, "Focused item in editor"));
        return row;
    }

    private JPanel createMetricCard(String label, JLabel valueLabel, String detail) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(SURFACE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        valueLabel.setFont(KPI_VALUE_FONT);
        valueLabel.setForeground(TEXT_PRIMARY);

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(KPI_LABEL_FONT);
        labelComp.setForeground(TEXT_MUTED);

        JLabel detailComp = new JLabel(detail);
        detailComp.setFont(new Font("Segoe UI Variable", Font.PLAIN, 11));
        detailComp.setForeground(TEXT_MUTED);

        card.add(valueLabel, BorderLayout.NORTH);
        card.add(labelComp, BorderLayout.CENTER);
        card.add(detailComp, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildLibraryPane() {
        JPanel pane = new JPanel(new BorderLayout(12, 12));
        pane.setBackground(SURFACE_BG);
        pane.setBorder(createCardBorder("Algorithm Library"));

        JPanel filters = new JPanel(new GridLayout(6, 1, 6, 4));
        filters.setOpaque(false);

        filters.add(styleLabel("Search"));
        styleInput(searchField);
        filters.add(searchField);

        filters.add(styleLabel("Language"));
        styleInput(languageFilter);
        filters.add(languageFilter);

        filters.add(styleLabel("Tag contains"));
        styleInput(tagFilter);
        filters.add(tagFilter);

        snippetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snippetList.setBackground(new Color(244, 248, 252));
        snippetList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        snippetList.setFont(BASE_FONT);
        snippetList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> buildSnippetRow(value, isSelected));

        JScrollPane listScrollPane = new JScrollPane(snippetList);
        listScrollPane.setBorder(BorderFactory.createLineBorder(CARD_BORDER));

        pane.add(filters, BorderLayout.NORTH);
        pane.add(listScrollPane, BorderLayout.CENTER);

        attachFilterListeners();
        snippetList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Snippet selected = snippetList.getSelectedValue();
                if (selected != null) {
                    loadSnippetIntoEditor(selected);
                }
            }
        });
        return pane;
    }

    private JPanel buildEditorPane() {
        JPanel pane = new JPanel(new BorderLayout(12, 12));
        pane.setBackground(SURFACE_BG);
        pane.setBorder(createCardBorder("Snippet Editor"));

        JPanel compactFields = new JPanel(new GridLayout(1, 6, 8, 0));
        compactFields.setOpaque(false);
        compactFields.add(styleLabel("Title"));
        styleInput(titleField);
        compactFields.add(titleField);
        compactFields.add(styleLabel("Language"));
        languageField.setEditable(true);
        styleInput(languageField);
        compactFields.add(languageField);
        compactFields.add(styleLabel("Tags"));
        styleInput(tagsField);
        compactFields.add(tagsField);

        JPanel editorCard = new JPanel(new BorderLayout(0, 0));
        editorCard.setBackground(DARK_PANEL);
        editorCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(33, 43, 61)),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JPanel editorTopBar = new JPanel(new BorderLayout());
        editorTopBar.setBackground(new Color(10, 16, 28));
        editorTopBar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel codeLabel = new JLabel("Code");
        codeLabel.setFont(new Font("Segoe UI Variable", Font.BOLD, 14));
        codeLabel.setForeground(new Color(240, 244, 255));

        JLabel editorMeta = new JLabel("Monaco-like view");
        editorMeta.setFont(new Font("Segoe UI Variable", Font.PLAIN, 11));
        editorMeta.setForeground(new Color(145, 160, 180));
        editorTopBar.add(codeLabel, BorderLayout.WEST);
        editorTopBar.add(editorMeta, BorderLayout.EAST);

        codeEditor.setFont(CODE_FONT.deriveFont(CODE_FONT_SIZE));
        codeEditor.setBackground(new Color(7, 14, 26));
        codeEditor.setForeground(new Color(232, 235, 240));
        codeEditor.setMargin(new Insets(10, 12, 10, 12));
        JScrollPane editorScroll = new JScrollPane(codeEditor);
        editorScroll.setBorder(BorderFactory.createEmptyBorder());
        editorScroll.getViewport().setBackground(new Color(7, 14, 26));

        editorCard.add(editorTopBar, BorderLayout.NORTH);
        editorCard.add(editorScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 5, 8, 8));
        actions.setOpaque(false);
        JButton newButton = new JButton("New");
        JButton saveButton = new JButton("Save");
        JButton deleteButton = new JButton("Delete");
        JButton exportOneButton = new JButton("Export Selected");
        JButton exportFilteredButton = new JButton("Export Filtered");

        styleButton(newButton, false);
        styleButton(saveButton, true);
        styleButton(deleteButton, false);
        styleButton(exportOneButton, false);
        styleButton(exportFilteredButton, false);

        actions.add(newButton);
        actions.add(saveButton);
        actions.add(deleteButton);
        actions.add(exportOneButton);
        actions.add(exportFilteredButton);

        newButton.addActionListener(this::onNewClicked);
        saveButton.addActionListener(this::onSaveClicked);
        deleteButton.addActionListener(this::onDeleteClicked);
        exportOneButton.addActionListener(this::onExportSelectedClicked);
        exportFilteredButton.addActionListener(this::onExportFilteredClicked);

        pane.add(compactFields, BorderLayout.NORTH);
        pane.add(editorCard, BorderLayout.CENTER);
        pane.add(actions, BorderLayout.SOUTH);
        return pane;
    }

    private JPanel buildFooterBar() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 2, 0, 2));

        JLabel hint = new JLabel("Tip: use the left panel to filter DSA algorithms by language or tag, then edit and export from the right.");
        hint.setFont(new Font("Segoe UI Variable", Font.PLAIN, 12));
        hint.setForeground(TEXT_MUTED);
        footer.add(hint, BorderLayout.WEST);
        return footer;
    }

    private Border createCardBorder(String title) {
        TitledBorder titledBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(CARD_BORDER), title);
        titledBorder.setTitleFont(BASE_FONT_BOLD);
        titledBorder.setTitleColor(TEXT_MUTED);
        return BorderFactory.createCompoundBorder(titledBorder, BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private JLabel styleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BASE_FONT_BOLD);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    private JPanel createBadge(String text, Color background, Color foreground) {
        JPanel badge = new JPanel(new BorderLayout());
        badge.setBackground(background);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker()),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Variable", Font.BOLD, 11));
        label.setForeground(foreground);
        badge.add(label, BorderLayout.CENTER);
        return badge;
    }

    private JPanel buildSnippetRow(Snippet value, boolean isSelected) {
        JPanel row = new JPanel(new BorderLayout(0, 3));
        row.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (value != null) {
            String title = value.getTitle().isBlank() ? "Untitled Snippet" : value.getTitle();
            String tags = value.tagsAsCsv().isBlank() ? "No tags" : value.tagsAsCsv();
            String meta = value.getLanguage() + "  •  " + tags + "  •  " + value.getUpdatedAt().format(LIST_DATE_FORMAT);

            JLabel titleLine = new JLabel(title);
            titleLine.setFont(BASE_FONT_BOLD);

            JLabel metaLine = new JLabel(meta);
            metaLine.setFont(new Font("Segoe UI Variable", Font.PLAIN, 11));

            row.add(titleLine, BorderLayout.NORTH);
            row.add(metaLine, BorderLayout.SOUTH);

            if (isSelected) {
                row.setBackground(new Color(221, 233, 249));
                titleLine.setForeground(new Color(13, 51, 102));
                metaLine.setForeground(new Color(41, 78, 128));
            } else {
                row.setBackground(Color.WHITE);
                titleLine.setForeground(TEXT_PRIMARY);
                metaLine.setForeground(TEXT_MUTED);
            }
        }
        return row;
    }

    private void styleInput(Component component) {
        if (component instanceof JTextField textField) {
            textField.setFont(BASE_FONT);
            textField.setForeground(TEXT_PRIMARY);
            textField.setBackground(INPUT_BG);
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(CARD_BORDER),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));
        } else if (component instanceof JComboBox<?> comboBox) {
            comboBox.setFont(BASE_FONT);
            comboBox.setForeground(TEXT_PRIMARY);
            comboBox.setBackground(INPUT_BG);
            comboBox.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
        }
    }

    private void styleButton(JButton button, boolean primary) {
        button.setFont(BASE_FONT_BOLD);
        button.setFocusPainted(false);
        button.setMargin(new Insets(8, 10, 8, 10));
        button.setBorder(BorderFactory.createLineBorder(primary ? ACCENT : CARD_BORDER));
        // Ensure background paints (some LAFs ignore background unless opaque/filled)
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(primary ? ACCENT : BUTTON_NEUTRAL);
        button.setForeground(primary ? Color.BLACK : TEXT_PRIMARY);
    }

    private void attachFilterListeners() {
        DocumentListener searchListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleSnippetListRefresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleSnippetListRefresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleSnippetListRefresh();
            }
        };

        searchField.getDocument().addDocumentListener(searchListener);
        tagFilter.getDocument().addDocumentListener(searchListener);
        languageFilter.addActionListener(e -> scheduleSnippetListRefresh());
    }

    private void scheduleSnippetListRefresh() {
        if (filterRefreshTimer.isRunning()) {
            filterRefreshTimer.restart();
        } else {
            filterRefreshTimer.start();
        }
    }

    private void refreshSnippetList() {
        String query = searchField.getText();
        String selectedLanguage = String.valueOf(languageFilter.getSelectedItem());
        String tag = tagFilter.getText();

        List<Snippet> snippets;
        try {
            snippets = service.search(query, selectedLanguage, tag);
        } catch (Exception ex) {
            // Show a clear message and continue with an empty list so UI can start
            showMessage("Unable to connect to database: " + ex.getMessage());
            snippets = new ArrayList<>();
        }

        snippetListModel.clear();
        for (Snippet snippet : snippets) {
            snippetListModel.addElement(snippet);
        }
        updateMetrics(snippets);
    }

    private void updateMetrics(List<Snippet> snippets) {
        totalCountValue.setText(String.valueOf(snippets.size()));
        languageCountValue.setText(String.valueOf(snippets.stream().map(Snippet::getLanguage).filter(v -> !v.isBlank()).distinct().count()));
        tagCountValue.setText(String.valueOf(snippets.stream().flatMap(s -> s.getTags().stream()).filter(v -> !v.isBlank()).distinct().count()));
        selectedCountValue.setText(currentSnippet == null ? "0" : "1");
    }

    private void createNewSnippet() {
        currentSnippet = new Snippet();
        titleField.setText("");
        languageField.setSelectedItem("Text");
        tagsField.setText("");
        codeEditor.setPlainTextWithoutStyles("");
        snippetList.clearSelection();
        selectedMetaValue.setText("Create a new snippet or choose one from the library.");
        selectedCountValue.setText("0");
    }

    private void loadSnippetIntoEditor(Snippet snippet) {
        currentSnippet = service.getById(snippet.getId()).orElse(snippet);
        titleField.setText(currentSnippet.getTitle());
        languageField.setSelectedItem(currentSnippet.getLanguage());
        tagsField.setText(currentSnippet.tagsAsCsv());
        codeEditor.setPlainTextWithoutStyles(currentSnippet.getCode());
        selectedMetaValue.setText(currentSnippet.getLanguage() + " • " + currentSnippet.tagsAsCsv() + " • updated " + currentSnippet.getUpdatedAt().format(LIST_DATE_FORMAT));
        selectedCountValue.setText("1");
    }

    private void onNewClicked(ActionEvent event) {
        createNewSnippet();
    }

    private void onSaveClicked(ActionEvent event) {
        String title = titleField.getText().trim();
        String language = String.valueOf(languageField.getSelectedItem()).trim();
        String code = codeEditor.getText();

        if (title.isBlank()) {
            showMessage("Title is required.");
            return;
        }
        if (code.isBlank()) {
            showMessage("Code content is required.");
            return;
        }

        currentSnippet.setTitle(title);
        currentSnippet.setLanguage(language.isBlank() ? "Text" : language);
        currentSnippet.setTags(parseTags(tagsField.getText()));
        currentSnippet.setCode(code);

        service.save(currentSnippet);
        refreshSnippetList();
        reselectCurrentSnippet();
        showMessage("Snippet saved successfully.");
    }

    private void reselectCurrentSnippet() {
        UUID id = currentSnippet.getId();
        for (int i = 0; i < snippetListModel.size(); i++) {
            if (snippetListModel.get(i).getId().equals(id)) {
                snippetList.setSelectedIndex(i);
                break;
            }
        }
    }

    private void onDeleteClicked(ActionEvent event) {
        Snippet selected = snippetList.getSelectedValue();
        if (selected == null) {
            showMessage("Select a snippet to delete.");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Delete snippet: " + selected.getTitle() + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            service.delete(selected.getId());
            refreshSnippetList();
            createNewSnippet();
            showMessage("Snippet deleted.");
        }
    }

    private void onExportSelectedClicked(ActionEvent event) {
        Snippet selected = snippetList.getSelectedValue();
        if (selected == null) {
            showMessage("Select a snippet to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Selected Snippet");
        chooser.setSelectedFile(new java.io.File(safeName(selected.getTitle()) + extensionFor(selected.getLanguage())));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path output = chooser.getSelectedFile().toPath();
        String content = buildSingleSnippetExport(selected);
        writeFile(output, content, "Snippet exported to " + output);
    }

    private void onExportFilteredClicked(ActionEvent event) {
        List<Snippet> snippets = service.search(
                searchField.getText(),
                String.valueOf(languageFilter.getSelectedItem()),
                tagFilter.getText()
        );

        if (snippets.isEmpty()) {
            showMessage("No snippets to export for current filters.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Filtered Snippets");
        chooser.setSelectedFile(new java.io.File("snippets-export.md"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String content = buildFilteredExport(snippets);
        Path output = chooser.getSelectedFile().toPath();
        writeFile(output, content, "Exported " + snippets.size() + " snippets to " + output);
    }

    private String buildSingleSnippetExport(Snippet snippet) {
        return "Title: " + snippet.getTitle() + System.lineSeparator()
                + "Language: " + snippet.getLanguage() + System.lineSeparator()
                + "Tags: " + snippet.tagsAsCsv() + System.lineSeparator()
                + "Updated: " + snippet.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + System.lineSeparator()
                + System.lineSeparator()
                + snippet.getCode()
                + System.lineSeparator();
    }

    private String buildFilteredExport(List<Snippet> snippets) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Snippet Export").append(System.lineSeparator()).append(System.lineSeparator());

        for (Snippet snippet : snippets) {
            builder.append("## ").append(snippet.getTitle()).append(System.lineSeparator());
            builder.append("- Language: ").append(snippet.getLanguage()).append(System.lineSeparator());
            builder.append("- Tags: ").append(snippet.tagsAsCsv()).append(System.lineSeparator());
            builder.append("- Updated: ")
                    .append(snippet.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());

            String lang = snippet.getLanguage().toLowerCase(Locale.ROOT);
            builder.append("```").append(lang.isBlank() ? "text" : lang).append(System.lineSeparator());
            builder.append(snippet.getCode()).append(System.lineSeparator());
            builder.append("```").append(System.lineSeparator()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private void writeFile(Path output, String content, String successMessage) {
        try {
            Files.writeString(output, content, StandardCharsets.UTF_8);
            showMessage(successMessage);
        } catch (IOException e) {
            showMessage("Export failed: " + e.getMessage());
        }
    }

    private String extensionFor(String language) {
        return switch (language.toLowerCase(Locale.ROOT)) {
            case "java" -> ".java";
            case "javascript" -> ".js";
            case "typescript" -> ".ts";
            case "python" -> ".py";
            case "sql" -> ".sql";
            case "bash" -> ".sh";
            case "go" -> ".go";
            case "rust" -> ".rs";
            case "c++" -> ".cpp";
            default -> ".txt";
        };
    }

    private String safeName(String input) {
        String cleaned = input == null ? "snippet" : input.trim();
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isBlank() ? "snippet" : cleaned;
    }

    private List<String> parseTags(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

}
