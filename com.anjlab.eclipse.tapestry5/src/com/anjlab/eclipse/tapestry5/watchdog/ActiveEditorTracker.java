package com.anjlab.eclipse.tapestry5.watchdog;

import java.util.Objects;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

public class ActiveEditorTracker
{
    private IEditorPart previousEditor;

    public boolean editorChanged(IWorkbenchPage page)
    {
        if (page == null)
        {
            previousEditor = null;

            return true;
        }

        if (Objects.equals(page.getActiveEditor(), previousEditor))
        {
            return false;
        }

        previousEditor = page.getActiveEditor();

        return true;
    }
}