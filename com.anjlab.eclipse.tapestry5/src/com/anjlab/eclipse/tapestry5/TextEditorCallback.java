package com.anjlab.eclipse.tapestry5;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.anjlab.eclipse.tapestry5.EclipseUtils.EditorCallback;

public abstract class TextEditorCallback implements EditorCallback
{

    @Override
    public final void editorOpened(IEditorPart editorPart)
    {
        if (editorPart instanceof MultiPageEditorPart)
        {
            Object selectedPage = ((MultiPageEditorPart) editorPart).getSelectedPage();
            
            if (selectedPage instanceof IEditorPart)
            {
                editorPart = (IEditorPart) selectedPage;
            }
        }
        
        if (editorPart instanceof ITextEditor)
        {
            editorOpened((ITextEditor) editorPart);
        }
    }

    public abstract void editorOpened(ITextEditor textEditor);
}
