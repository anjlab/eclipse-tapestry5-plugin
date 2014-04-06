package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
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
    protected void enumJavaClassesRecursively(IProgressMonitor monitor, String rootPackage, ObjectCallback<Object> callback)
    {
        try
        {
            for (IPackageFragmentRoot root : getModuleClass().getJavaProject().getPackageFragmentRoots())
            {
                if (!EclipseUtils.isSourceFolder(root))
                {
                    continue;
                }
                
                for (IJavaElement child : root.getChildren())
                {
                    if (monitor.isCanceled())
                    {
                        return;
                    }
                    
                    if (child instanceof IPackageFragment && child.getElementName().startsWith(rootPackage))
                    {
                        enumJavaClassesRecursively(monitor, (IPackageFragment) child, callback);
                    }
                }
            }
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error enumerating Java classes", e);
        }
    }

    private void enumJavaClassesRecursively(IProgressMonitor monitor, IPackageFragment packageFragment, ObjectCallback<Object> callback) throws JavaModelException
    {
        for (IJavaElement child : packageFragment.getChildren())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            
            IResource resource = child.getCorrespondingResource();
            
            if (resource != null && TapestryUtils.isJavaFile(resource.getProjectRelativePath()))
            {
                callback.callback(resource);
            }
        }
    }
    
    @Override
    public TapestryFile findClasspathFileCaseInsensitive(String path)
    {
        IJavaProject javaProject = getModuleClass().getJavaProject();
        
        return TapestryUtils.findFileInSourceFolders(javaProject, path);
    }
}
