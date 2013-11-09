package com.anjlab.eclipse.tapestry5.hyperlink;

import java.io.File;
import java.util.List;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryContext;
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
        
        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        
        ITextFileBuffer fileBuffer = bufferManager.getTextFileBuffer(document);
        
        if (fileBuffer == null)
        {
            return null;
        }
        
        IPath fileLocation = fileBuffer.getLocation();
        
        if (fileLocation == null || !TapestryUtils.isTemplateFile(fileLocation))
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
            
            final int componentOffset = lineInfo.getOffset() + leftIndex + leftOffset;
            
            if (offsetInLine < leftIndex + leftOffset || offsetInLine >= rightIndex - rightOffset)
            {
                return null;
            }
            
            final String componentName = text.substring(leftOffset, text.length() - rightOffset);
            
            IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(fileLocation);
            
            if (resource == null || resource.getProject() == null)
            {
                return null;
            }
            
            IJavaProject javaProject = JavaCore.create(resource.getProject());
            
            try
            {
                for (IPackageFragmentRoot root : javaProject.getAllPackageFragmentRoots())
                {
                    if (!EclipseUtils.isSourceFolder(root))
                    {
                        continue;
                    }
                    
                    IContainer container = (IContainer) root.getCorrespondingResource().getAdapter(IContainer.class);
                    
                    String componentPath = getComponentJavaFileName(componentName, resource.getProject());
                    
                    IFile javaFile = EclipseUtils.findFileCaseInsensitive(container, componentPath);
                    
                    if (javaFile == null)
                    {
                       File parentFile = new File(componentPath).getParentFile();
                       
                       if (parentFile != null)
                       {
                           componentPath = getComponentJavaFileName(componentName + parentFile.getName(), resource.getProject());
                           
                           javaFile = EclipseUtils.findFileCaseInsensitive(container, componentPath);
                       }
                    }
                    
                    if (javaFile != null)
                    {
                        TapestryContext context = TapestryUtils.createTapestryContext(javaFile);
                        
                        final List<IFile> files = context.getFiles();
                        
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
                }
            }
            catch (JavaModelException e)
            {
                return null;
            }
        }
        
        return null;
    }

    private String getComponentJavaFileName(final String componentName, IProject project)
    {
        return TapestryUtils.joinPath(
                TapestryUtils.getComponentsPath(project),
                componentName.replace('.', '/') + ".java");
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
        
        if (rightIndex >= line.length())
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