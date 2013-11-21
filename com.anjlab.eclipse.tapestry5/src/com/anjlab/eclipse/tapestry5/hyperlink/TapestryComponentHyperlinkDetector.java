package com.anjlab.eclipse.tapestry5.hyperlink;

import java.util.List;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class TapestryComponentHyperlinkDetector extends AbstractHyperlinkDetector
{

    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
            IRegion region, boolean canShowMultipleHyperlinks)
    {
        if (region == null || textViewer == null)
        {
            return null;
        }

        IDocument document = textViewer.getDocument();

        int offset = region.getOffset();

        if (document == null)
        {
            return null;
        }
        
        if (!isTapestryTemplate(document))
        {
            return null;
        }
        
        TapestryContext tapestryContext = null;
        IWorkbenchWindow currentWindow = null;
        
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows())
        {
            if (textViewer.getTextWidget().getShell() == window.getShell())
            {
                currentWindow = window;
                tapestryContext = Activator.getDefault().getTapestryContext(window);
                break;
            }
        }
        
        if (tapestryContext == null)
        {
            return null;
        }
        
        IRegion lineInfo;
        String line;
        try
        {
            lineInfo = document.getLineInformationOfOffset(offset);
            line = document.get(lineInfo.getOffset(), lineInfo.getLength());
        }
        catch (BadLocationException ex)
        {
            return null;
        }
        
        int offsetInLine = offset - lineInfo.getOffset();
        
        if (offsetInLine >= line.length())
        {
            return null;
        }
        
        char ch = line.charAt(offsetInLine);
        
        if (!isValidCharForComponentReference(ch))
        {
            return null;
        }
        
        int leftIndex = offsetInLine;
        int rightIndex = offsetInLine;
        
        while (leftIndex > 0 && isValidCharForComponentReference(line.charAt(leftIndex - 1)))
        {
            leftIndex--;
        }
        
        while (rightIndex + 1 < line.length() && isValidCharForComponentReference(line.charAt(rightIndex + 1)))
        {
            rightIndex++;
        }
        
        String text = line.substring(leftIndex, rightIndex + 1);
        
        if (checkPreconditions(line, leftIndex, rightIndex))
        {
            int leftOffset = 0;
            int rightOffset = 0;
            
            //  <t:alerts/>
            //           ^
            if (text.endsWith("/"))
            {
                rightOffset = 1;
            }
            
            final int componentOffset = lineInfo.getOffset() + leftIndex + leftOffset;
            
            if (offsetInLine < leftIndex + leftOffset || offsetInLine >= rightIndex - rightOffset)
            {
                return null;
            }
            
            final String componentName = text.substring(leftOffset, text.length() - rightOffset);
            
            TapestryModule tapestryModule = TapestryUtils.getTapestryModule(currentWindow, tapestryContext.getProject());
            
            if (tapestryModule == null)
            {
                return null;
            }
            
            TapestryContext targetContext;
            try
            {
                targetContext = tapestryModule.getProject().findComponentContext(componentName);
            }
            catch (JavaModelException e)
            {
                return null;
            }
            
            if (targetContext == null)
            {
                return null;
            }
            
            final List<TapestryFile> files = targetContext.getFiles();
            
            IHyperlink[] links = new IHyperlink[files.size()];
            
            for (int i = 0; i < files.size(); i++)
            {
                final int index = i;
                
                links[index] = new IHyperlink()
                {
                    @Override
                    public void open()
                    {
                        EclipseUtils.openFile(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), files.get(index));
                    }
                    
                    @Override
                    public String getTypeLabel()
                    {
                        return files.get(index).getName();
                    }
                    
                    @Override
                    public String getHyperlinkText()
                    {
                        return files.get(index).getName();
                    }
                    
                    @Override
                    public IRegion getHyperlinkRegion()
                    {
                        return new Region(componentOffset, componentName.length());
                    }
                };
            }
            
            return links;
        }
        
        return null;
    }

    protected boolean isTapestryTemplate(IDocument document)
    {
        //  If we take current TapestryContext we couldn't know if current file is a TML file
        //  This is probably OK, because the overhead shouldn't be that big in this case.
        //  Though we can check first char of current document to see if it is '<', then we probably in TML file
        
        try
        {
            return (document.getChar(0) == '<');
        }
        catch (BadLocationException e)
        {
            //  Ignore
        }
        return false;
    }

    private boolean checkPreconditions(String line, int leftIndex, int rightIndex)
    {
        if (leftIndex <= 0)
        {
            return false;
        }
        
        char leftChar = line.charAt(leftIndex - 1);
        
        //  <t:component
        //    ^
        if (leftChar == ':')
        {
            return true;
        }
        
        //  <component xmlns="http://tapestry.apache.org/schema/tapestry_5_3.xsd"
        //  ^
        if (leftChar == '<')
        {
            return true;
        }
        
        if (rightIndex + 1 >= line.length())
        {
            return false;
        }
        
        char rightChar = line.charAt(rightIndex + 1);
        
        //  <div t:type='component'
        //              ^         ^
        if ((leftChar == '"' && rightChar == '"') || (leftChar == '\'' && rightChar == '\''))
        {
            return true;
        }
        
        return false;
    }

    private boolean isValidCharForComponentReference(char ch)
    {
        return !Character.isSpaceChar(ch)
            && ch != '<' && ch != '>' && ch != ':' && ch != '\'' && ch != '"';
    }

}