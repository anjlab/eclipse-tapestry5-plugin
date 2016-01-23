package com.anjlab.eclipse.tapestry5;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class JarTapestryModule extends TapestryModule
{

    public JarTapestryModule(TapestryProject project, IType type)
    {
        super(project, type);
    }

    @Override
    public TapestryFile getModuleFile()
    {
        IClassFile classFile = getModuleClass().getClassFile();
        
        TapestryContext context = Activator.getDefault()
                .getTapestryContextFactory()
                .createTapestryContext(classFile);
        
        return context.getInitialFile();
    }
    
    @Override
    public boolean isReadOnly()
    {
        return true;
    }
    
    @Override
    protected void enumJavaClassesRecursively(IProgressMonitor monitor, String rootPackage, ObjectCallback<Object, RuntimeException> callback)
    {
        IPackageFragmentRoot root = (IPackageFragmentRoot) getModuleClass()
                .getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
        
        try
        {
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
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error performing search", e);
        }
    }

    private void enumJavaClassesRecursively(IProgressMonitor monitor, IPackageFragment packageFragment, ObjectCallback<Object, RuntimeException> callback) throws JavaModelException
    {
        for (IJavaElement child : packageFragment.getChildren())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            
            if (child instanceof IClassFile)
            {
                callback.callback(child);
            }
            else if (child instanceof IPackageFragment)
            {
                enumJavaClassesRecursively(monitor, (IPackageFragment) child, callback);
            }
        }
    }

    @Override
    public TapestryFile findClasspathFileCaseInsensitive(String path)
    {
        return new JarFileLookup(getModuleClass())
            .findClasspathFileCaseInsensitive(path);
    }

}
