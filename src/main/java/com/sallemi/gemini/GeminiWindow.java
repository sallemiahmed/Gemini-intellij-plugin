package com.sallemi.gemini;

import com.intellij.ui.jcef.JBCefBrowser;

import javax.swing.*;

public class GeminiWindow {
    private final JPanel panel;
    private final JBCefBrowser browser;

    public GeminiWindow() {
        panel = new JPanel();
        panel.setLayout(new java.awt.BorderLayout());

        String geminiUrl = "https://gemini.google.com/app";

        browser = new JBCefBrowser(geminiUrl);
        panel.add(browser.getComponent(), java.awt.BorderLayout.CENTER);
    }

    public JPanel getContent() {
        return panel;
    }
}
