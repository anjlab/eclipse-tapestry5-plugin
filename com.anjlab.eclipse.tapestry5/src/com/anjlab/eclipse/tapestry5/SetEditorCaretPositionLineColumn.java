package com.anjlab.eclipse.tapestry5;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.ITextEditor;

public class SetEditorCaretPositionLineColumn extends TextEditorCallback
{
    private final int line;
    private final int column;
    
    /**
     * 
     * @param line 0-based line number
     * @param column 0-based column number
     */
    public SetEditorCaretPositionLineColumn(int line, int column)
    {
        this.line = line;
        this.column = column;
    }
    
    @Override
    public void editorOpened(ITextEditor textEditor)
    {
        if (line < 0)
        {
            return;
        }
        
        IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
        
        int offset = 0;
        
        if (document != null)
        {
            try
            {
                IRegion lineInfo = document.getLineInformation(line - 1);
                
                if (lineInfo != null)
                {
                    offset = lineInfo.getOffset();
                    
                    //  Check to stay on the same line
                    if (column <= lineInfo.getLength())
                    {
                        offset += column;
                    }
                }
            }
            catch (BadLocationException e)
            {
                //  Line not found, open in 0 offset
            }
        }
        
        textEditor.selectAndReveal(offset, 0);
    }
}