package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class LocalFileLookup implements FileLookup
{
    private final IType relativeTo;

    public LocalFileLookup(IType relativeTo)
    {
        this.relativeTo = relativeTo;
    }

    @Override
    public String findClasspathRelativePath(TapestryFile file) throws JavaModelException
    {
        IJavaProject javaProject = JavaCore.create(file.getProject());
        
        IContainer adaptedProject = (IContainer) javaProject.getCorrespondingResource().getAdapter(IContainer.class);
        
        IResource adaptedFile = adaptedProject.findMember(file.getPath());
        
        if (adaptedFile == null)
        {
            return null;
        }
        
        IContainer container = adaptedFile.getParent();
        
        while (container != null && !EclipseUtils.isSourceFolder(container))
        {
            container = container.getParent();
        }
        
        if (container != null)
        {
            //  Get the file name relative to source folder
            return TapestryUtils.getRelativeFileName(((LocalFile) file).getFile(), container);
        }
        
        return null;
    }

    @Override
    public TapestryFile findClasspathFileCaseInsensitive(String path)
    {
        if (relativeTo == null)
        {
            return null;
        }
        
        IJavaProject javaProject = relativeTo.getJavaProject();
        
        return TapestryUtils.findFileInSourceFolders(javaProject, path);
    }

}
