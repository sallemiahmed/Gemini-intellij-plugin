package com.sallemi.gemini;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public class GeminiToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        GeminiWindow window = new GeminiWindow();
        Content content = ContentFactory.getInstance().createContent(window.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
