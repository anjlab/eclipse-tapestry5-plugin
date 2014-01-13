package com.anjlab.eclipse.tapestry5;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class JarTapestryModule extends TapestryModule
{

    public JarTapestryModule(TapestryProject project,
                             IType type,
                             ModuleReference reference)
    {
        super(project, type, reference);
    }

    @Override
    public TapestryFile getModuleFile()
    {
        IClassFile classFile = getModuleClass().getClassFile();
        TapestryContext context = TapestryUtils.createTapestryContext(classFile);
        return context.getInitialFile();
    }
    
    @Override
    public boolean isReadOnly()
    {
        return true;
    }
    
    @Override
    protected void enumJavaClassesRecursively(String rootPackage, ObjectCallback<Object> callback)
    {
        IParent root = (IParent) getModuleClass().getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
        
        try
        {
            IPackageFragment packageFragment = findPackage(rootPackage, root);
            
            if (packageFragment != null)
            {
                enumJavaClassesRecursively(packageFragment, callback);
            }
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error performing search", e);
        }
    }

    private IPackageFragment findPackage(String packageName, IParent container) throws JavaModelException
    {
        for (IJavaElement child : container.getChildren())
        {
            if (child instanceof IPackageFragment)
            {
                IPackageFragment packageFragment = (IPackageFragment) child;
                
                if (packageFragment.getElementName().equals(packageName))
                {
                    return packageFragment;
                }
            }
        }
        
        return null;
    }
    
    private void enumJavaClassesRecursively(IPackageFragment packageFragment, ObjectCallback<Object> callback) throws JavaModelException
    {
        for (IJavaElement child : packageFragment.getChildren())
        {
            if (child instanceof IClassFile)
            {
                callback.callback(child);
            }
            else if (child instanceof IPackageFragment)
            {
                enumJavaClassesRecursively((IPackageFragment) child, callback);
            }
        }
    }

    @Override
    public TapestryFile findJavaFileCaseInsensitive(String path)
    {
        int index = path.lastIndexOf('/');
        
        String[] segments = index < 0
                          ? new String[] { path }
                          : new String[] { path.substring(0, index).replace("/", "."), path.substring(index + 1) };
        
        IParent root = (IParent) getModuleClass().getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
        
        try
        {
            for (IJavaElement child : root.getChildren())
            {
                if (child.getElementName().equalsIgnoreCase(segments[0]))
                {
                    if (segments.length == 0)
                    {
                        IClassFile classFile = (IClassFile) child;
                        
                        return TapestryUtils.createTapestryContext(classFile).getInitialFile();
                    }
                    else
                    {
                        IPackageFragment pkg = (IPackageFragment) child;
                        
                        for (IClassFile classFile : pkg.getClassFiles())
                        {
                            if (classFile.getElementName().equalsIgnoreCase(segments[1]))
                            {
                                return TapestryUtils.createTapestryContext(classFile).getInitialFile();
                            }
                        }
                        
                        break;
                    }
                }
            }
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error performing search", e);
        }
        
        return null;
    }
}
