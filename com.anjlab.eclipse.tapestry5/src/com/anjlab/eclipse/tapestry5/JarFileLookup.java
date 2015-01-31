package com.anjlab.eclipse.tapestry5;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class JarFileLookup implements FileLookup
{
    private IType relativeTo;

    public JarFileLookup(IType relativeTo)
    {
        this.relativeTo = relativeTo;
    }

    @Override
    public TapestryFile findClasspathFileCaseInsensitive(String path)
    {
        if (relativeTo == null)
        {
            return null;
        }
        
        IParent root = (IParent) relativeTo.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
        
        TapestryFile file = null;
        
        try
        {
            file = searchChildren(root, path);
            
            if (file == null)
            {
                file = searchNonJavaResources(root, path);
            }
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error performing search", e);
        }
        
        return file;
    }


    private TapestryFile searchNonJavaResources(IParent root, String path) throws JavaModelException
    {
        String[] segments = path.split("/");

        if (root instanceof IPackageFragmentRoot)
        {
            for (Object nonJava : ((IPackageFragmentRoot) root).getNonJavaResources())
            {
                if (nonJava instanceof IJarEntryResource)
                {
                    IJarEntryResource resource = (IJarEntryResource) nonJava;
                    
                    if (StringUtils.equals(segments[0], resource.getName()))
                    {
                        return searchRecursively(resource, path, segments, 1);
                    }
                }
            }
        }
        
        return null;
    }

    private TapestryFile searchRecursively(IJarEntryResource parent, String path, String[] segments, int segmentIndex)
    {
        if (pathEquals(parent, path))
        {
            return Activator.getDefault()
                    .getTapestryContextFactory()
                    .createTapestryContext(parent)
                    .getInitialFile();
        }
        
        if (segmentIndex >= segments.length)
        {
            return null;
        }
        
        if (parent.isFile())
        {
            return null;
        }
        
        for (IJarEntryResource child : parent.getChildren())
        {
            if (StringUtils.equals(segments[segmentIndex], child.getName()))
            {
                return searchRecursively(child, path, segments, segmentIndex + 1);
            }
        }
        
        return null;
    }

    private TapestryFile searchChildren(IParent root, String path) throws JavaModelException
    {
        int index = path.lastIndexOf('/');
        
        String[] segments = index < 0
                          ? new String[] { path }
                          : new String[] { path.substring(0, index).replace("/", "."), path.substring(index + 1) };
        
        for (IJavaElement child : root.getChildren())
        {
            if (child.getElementName().equalsIgnoreCase(segments[0]))
            {
                if (segments.length == 0)
                {
                    if (child instanceof IClassFile)
                    {
                        IClassFile classFile = (IClassFile) child;
                        
                        return Activator.getDefault()
                                .getTapestryContextFactory()
                                .createTapestryContext(classFile)
                                .getInitialFile();
                    }
                    
                    if (child instanceof IJarEntryResource)
                    {
                        IJarEntryResource jarEntry = (IJarEntryResource) child;
                        
                        return Activator.getDefault()
                                .getTapestryContextFactory()
                                .createTapestryContext(jarEntry)
                                .getInitialFile();
                    }
                }
                else
                {
                    IPackageFragment pkg = (IPackageFragment) child;
                    
                    for (IClassFile classFile : pkg.getClassFiles())
                    {
                        if (classFile.getElementName().equalsIgnoreCase(segments[1]))
                        {
                            return Activator.getDefault()
                                    .getTapestryContextFactory()
                                    .createTapestryContext(classFile)
                                    .getInitialFile();
                        }
                    }
                    
                    for (Object nonJava : pkg.getNonJavaResources())
                    {
                        if (nonJava instanceof IJarEntryResource)
                        {
                            IJarEntryResource jarEntry = ((IJarEntryResource) nonJava);
                            
                            if (pathEquals(jarEntry, path))
                            {
                                return Activator.getDefault()
                                        .getTapestryContextFactory()
                                        .createTapestryContext(jarEntry)
                                        .getInitialFile();
                            }
                        }
                    }
                    
                    break;
                }
            }
        }
        
        return null;
    }

    private boolean pathEquals(IJarEntryResource resource, String path)
    {
        //  Full path has leading slash
        return resource.getFullPath().toPortableString().equalsIgnoreCase(path.startsWith("/") ? path : "/" + path);
    }
}
