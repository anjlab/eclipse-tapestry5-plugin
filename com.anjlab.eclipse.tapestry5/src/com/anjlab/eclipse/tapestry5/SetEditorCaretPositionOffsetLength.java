package com.anjlab.eclipse.tapestry5;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.anjlab.eclipse.tapestry5.EclipseUtils.EditorCallback;

public class SetEditorCaretPositionOffsetLength implements EditorCallback
{
    private final int offset;
    private final int length;
    
    /**
     * 
     * @param offset 0-based offset
     * @param length
     */
    public SetEditorCaretPositionOffsetLength(int offset, int length)
    {
        this.offset = offset;
        this.length = length;
    }
    
    @Override
    public void editorOpened(IEditorPart editorPart)
    {
        if (offset < 0)
        {
            return;
        }
        
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
            ((ITextEditor) editorPart).selectAndReveal(offset, length);
        }
    }
}