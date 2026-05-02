package com.snippetmanager.ui;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.Set;
import java.awt.Insets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlightTextPane extends JTextPane {
    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "false",
            "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
            "int", "interface", "long", "native", "new", "null", "package", "private", "protected",
            "public", "record", "return", "sealed", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true", "try", "var", "void",
            "volatile", "while", "yield", "function", "let", "def", "from", "as", "await"
    );

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[A-Za-z_][A-Za-z0-9_]*\\b");

    private static final int HIGHLIGHT_DELAY_MS = 120;

    private final Style normalStyle;
    private final Style keywordStyle;
    private final Timer highlightTimer;
    private boolean applyingHighlight;
    private String placeholder = "// write your code here...";

    public SyntaxHighlightTextPane() {
        normalStyle = addStyle("normal", null);
        StyleConstants.setForeground(normalStyle, new Color(225, 228, 232));

        keywordStyle = addStyle("keyword", null);
        StyleConstants.setForeground(keywordStyle, new Color(122, 177, 255));
        StyleConstants.setBold(keywordStyle, true);

        setBackground(new Color(17, 24, 39));
        setCaretColor(new Color(240, 240, 240));
        setForeground(new Color(225, 228, 232));

        // Set tab size to 4 spaces
        getStyledDocument().putProperty("tabSize", 4);

        highlightTimer = new Timer(HIGHLIGHT_DELAY_MS, e -> refreshHighlightingNow());
        highlightTimer.setRepeats(false);

        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleHighlightRefresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleHighlightRefresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Ignore style-only updates to avoid recursive highlight churn.
            }
        });

        // Clear placeholder behavior on focus
        this.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (getText() == null || getText().isEmpty()) {
                    // ensure caret visible
                    setCaretPosition(0);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // nothing special
            }
        });
    }

    private void scheduleHighlightRefresh() {
        if (applyingHighlight) {
            return;
        }
        if (highlightTimer.isRunning()) {
            highlightTimer.restart();
        } else {
            highlightTimer.start();
        }
    }

    public void refreshHighlighting() {
        SwingUtilities.invokeLater(() -> {
            if (highlightTimer.isRunning()) {
                highlightTimer.stop();
            }
            refreshHighlightingNow();
        });
    }

    private void refreshHighlightingNow() {
        StyledDocument document = getStyledDocument();
        String content;
        try {
            content = document.getText(0, document.getLength());
        } catch (BadLocationException e) {
            return;
        }

        applyingHighlight = true;
        try {
            document.setCharacterAttributes(0, content.length(), normalStyle, true);

            Matcher matcher = WORD_PATTERN.matcher(content);
            while (matcher.find()) {
                String token = matcher.group();
                if (KEYWORDS.contains(token)) {
                    document.setCharacterAttributes(
                            matcher.start(),
                            matcher.end() - matcher.start(),
                            keywordStyle,
                            true
                    );
                }
            }
        } finally {
            applyingHighlight = false;
        }
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        scheduleHighlightRefresh();
    }

    public void setPlainTextWithoutStyles(String text) {
        StyledDocument document = getStyledDocument();
        try {
            document.remove(0, document.getLength());
            document.insertString(0, text == null ? "" : text, normalStyle);
        } catch (BadLocationException e) {
            throw new IllegalStateException("Unable to update editor text.", e);
        }
        scheduleHighlightRefresh();
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if ((getText() == null || getText().isEmpty()) && !isFocusOwner() && placeholder != null && !placeholder.isBlank()) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(125, 135, 150));
                g2.setFont(getFont().deriveFont(java.awt.Font.ITALIC));
                Insets ins = getInsets();
                int x = ins.left + 4;
                int y = ins.top + g2.getFontMetrics().getAscent() + 2;
                g2.drawString(placeholder, x, y);
            } finally {
                g2.dispose();
            }
        }
    }
}
