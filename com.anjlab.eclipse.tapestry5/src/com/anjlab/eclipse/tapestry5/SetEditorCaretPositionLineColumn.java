package com.anjlab.eclipse.tapestry5;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.anjlab.eclipse.tapestry5.EclipseUtils.EditorCallback;

public class SetEditorCaretPositionLineColumn implements EditorCallback
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
    public void editorOpened(IEditorPart editorPart)
    {
        if (line < 0)
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
            ITextEditor textEditor = (ITextEditor) editorPart;
            
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
}