package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class LocalTapestryModule extends TapestryModule
{

    public LocalTapestryModule(TapestryProject project,
            IType type, ModuleReference reference)
    {
        super(project, type, reference);
    }

    @Override
    public TapestryFile getModuleFile()
    {
        IResource resource;
        try
        {
            resource = getModuleClass().getUnderlyingResource();
            IFile file = (IFile) resource.getAdapter(IFile.class);
            return TapestryUtils.createTapestryContext(file).getInitialFile();
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error getting ModuleFile", e);
            return null;
        }
    }
    
    @Override
    public boolean isReadOnly()
    {
        return false;
    }
    
    @Override
    public TapestryFile findJavaFileCaseInsensitive(String path)
    {
        try
        {
            for (IPackageFragmentRoot root : getModuleClass().getJavaProject().getAllPackageFragmentRoots())
            {
                if (!EclipseUtils.isSourceFolder(root))
                {
                    continue;
                }
                
                IContainer container = (IContainer) root.getCorrespondingResource().getAdapter(IContainer.class);
                
                IFile javaFile = EclipseUtils.findFileCaseInsensitive(container, path);
                
                if (javaFile != null)
                {
                    return TapestryUtils.createTapestryContext(javaFile).getInitialFile();
                }
            }
        }
        catch (JavaModelException e)
        {
            //  Ignore
        }
        return null;
    }
}
