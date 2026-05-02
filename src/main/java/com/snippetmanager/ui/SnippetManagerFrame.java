package com.snippetmanager.ui;

import com.snippetmanager.service.SnippetService;
import com.snippetmanager.ui.dashboard.SnippetDashboardFrame;

/**
 * Backward-compatible wrapper for the old frame name.
 */
public class SnippetManagerFrame extends SnippetDashboardFrame {
    public SnippetManagerFrame(SnippetService service) {
        super(service);
    }
}
