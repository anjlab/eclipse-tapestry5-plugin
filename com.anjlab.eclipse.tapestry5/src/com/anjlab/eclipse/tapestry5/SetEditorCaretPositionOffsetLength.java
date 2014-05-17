package com.anjlab.eclipse.tapestry5;

import org.eclipse.ui.texteditor.ITextEditor;

public class SetEditorCaretPositionOffsetLength extends TextEditorCallback
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
    public void editorOpened(ITextEditor textEditor)
    {
        if (offset < 0)
        {
            return;
        }
        
        textEditor.selectAndReveal(offset, length);
    }
}