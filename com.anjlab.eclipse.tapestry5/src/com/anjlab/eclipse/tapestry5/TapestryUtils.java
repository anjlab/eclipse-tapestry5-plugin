package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class TapestryUtils
{
    public static IFile findComplementFile(IFile forFile)
    {
        List<IFile> files = findTapestryFiles(forFile, true, new FileNameBuilder()
        {
            @Override
            public String getFileName(String fileName, String fileExtension)
            {
                String complementExtension = "tml".equals(fileExtension) ? "java" : "java".equals(fileExtension) ? "tml" : null;
                
                if (complementExtension == null)
                {
                    throw new IllegalArgumentException();
                }
                
                return fileName.substring(0, fileName.lastIndexOf(fileExtension)) + complementExtension;
            }
        });
        
        return !files.isEmpty() ? files.get(0) : null;
    }
    
    public static interface FileNameBuilder
    {
        String getFileName(String fileName, String fileExtension);
    }
    
    public static List<IFile> findTapestryFiles(IFile forFile, boolean findFirst, FileNameBuilder fileNameBuilder)
    {
        try
        {
            String complementFileName = null;
            
            //  Check if the file is in the web application context
            
            boolean fromWebapp = false;
            
            IProject project = forFile.getProject();
            
            IContainer webapp = findWebapp(project);
            
            if (isTemplateFile(forFile))
            {
                if (forFile.getParent().equals(webapp))
                {
                    String relativeFileName = getRelativeFileName(forFile, webapp);
                    
                    fromWebapp = relativeFileName != null;
                    
                    if (fromWebapp)
                    {
                        complementFileName = fileNameBuilder.getFileName(relativeFileName, forFile.getFileExtension());
                    }
                }
            }
            
            List<IFile> resources = new ArrayList<IFile>();
            
            if (!fromWebapp)
            {
                complementFileName = fileNameBuilder.getFileName(forFile.getName(), forFile.getFileExtension());
                
                //  Try searching in the same folder first
                
                resources = findMembers(forFile.getParent(), complementFileName);
                
                if (findFirst && !resources.isEmpty())
                {
                    return resources;
                }
            }
            
            //  Look in the source folders
            
            if (!forFile.getProject().hasNature(JavaCore.NATURE_ID))
            {
                Activator.getDefault().logError("'" + forFile.getProject() + "' is not a Java project");
                
                return Collections.emptyList();
            }
            
            IJavaProject javaProject = JavaCore.create(forFile.getProject());
            
            IContainer container = null;
            
            if (!fromWebapp)
            {
                IContainer adaptedProject = (IContainer) javaProject.getCorrespondingResource().getAdapter(IContainer.class);
                
                IResource adaptedFile = adaptedProject.findMember(forFile.getProjectRelativePath());
                
                container = adaptedFile.getParent();
                
                while (container != null && !isSourceFolder(container))
                {
                    container = container.getParent();
                }
                
                if (container == null)
                {
                    Activator.getDefault().logWarning("Unable to find source folder for file: " + forFile.getFullPath());
                    
                    return Collections.emptyList();
                }
                
                //  Get the file name relative to source folder
                String relativeFileName = getRelativeFileName(forFile, container);
                
                complementFileName = fileNameBuilder.getFileName(relativeFileName, forFile.getFileExtension());
            }
            
            for (IPackageFragmentRoot root : javaProject.getAllPackageFragmentRoots())
            {
                if (!isSourceFolder(root))
                {
                    continue;
                }
                
                IContainer resourceContainer = (IContainer) root.getCorrespondingResource().getAdapter(IContainer.class);
                
                if (container != null && resourceContainer.getFullPath().equals(container.getFullPath()))
                {
                    continue;
                }
                
                List<IFile> resources2 = findMembers(resourceContainer, complementFileName);
                
                if (findFirst && !resources2.isEmpty())
                {
                    return resources2;
                }
                
                resources.addAll(resources2);
            }
            
            //  Look for TML files in web application context
            //  https://github.com/anjlab/eclipse-tapestry5-plugin/issues/2
            
            if (complementFileName.endsWith(".tml"))
            {
                if (webapp != null)
                {
                    IResource file = webapp.findMember(complementFileName);
                    
                    if (file instanceof IFile)
                    {
                        resources.add((IFile) file);
                    }
                }
            }
            
            return resources;
        }
        catch (CoreException e)
        {
            Activator.getDefault().logError("Error finding complement file", e);
            
            return Collections.emptyList();
        }
    }

    private static String getRelativeFileName(IFile file, IContainer ancestor)
    {
        return file.getProjectRelativePath().toPortableString().substring(
                ancestor.getProjectRelativePath().toPortableString().length());
    }

    private static List<IFile> findMembers(IContainer container, String path)
    {
        List<IFile> resources = new ArrayList<IFile>();
        
        if (path.contains("*"))
        {
            //  Find files by mask
            int slashIndex = path.lastIndexOf("/");
            
            String parentPath = slashIndex < 0 ? "/" : path.substring(0, slashIndex);
            String mask = slashIndex < 0 ? path : path.substring(slashIndex + 1);
            
            Pattern pattern = Pattern.compile(mask);
            
            IResource resource = container.findMember(parentPath);
            
            if (resource instanceof IFolder)
            {
                try
                {
                    IResource[] members = ((IFolder) resource).members();
                    
                    for (IResource member : members)
                    {
                        if (pattern.matcher(member.getName()).matches())
                        {
                            resources.add((IFile) member);
                        }
                    }
                }
                catch (CoreException e)
                {
                    Activator.getDefault().logError("Error finding files by mask", e);
                }
            }
        }
        else
        {
            //  Exact match
            IResource resource = container.findMember(path);
            if (resource != null && resource instanceof IFile)
            {
                resources.add((IFile) resource);
            }
        }
        return resources;
    }

    private static boolean isSourceFolder(IContainer container) throws JavaModelException
    {
        return isSourceFolder((IJavaElement) container.getAdapter(IJavaElement.class));
    }

    private static boolean isSourceFolder(IJavaElement javaElement) throws JavaModelException
    {
        return javaElement != null
            && (javaElement instanceof IPackageFragmentRoot)
            && (((IPackageFragmentRoot) javaElement).getKind() == IPackageFragmentRoot.K_SOURCE);
    }

    public static boolean isTemplateFile(IFile file)
    {
        return "tml".equals(file.getFileExtension());
    }

    public static boolean isJavaFile(IFile file)
    {
        return "java".equals(file.getFileExtension());
    }

    public static boolean isPropertiesFile(IFile file)
    {
        return "properties".equals(file.getFileExtension());
    }
    
    public static TapestryContext createTapestryContext(IFile forFile)
    {
        return new TapestryContext(forFile);
    }

    public static AssetResolver createAssetResolver(String bindingPrefix, String assetPath)
    {
        if ("default".equals(bindingPrefix))
        {
            return new DefaultAssetResolver();
        }
        else if ("context".equals(bindingPrefix))
        {
            return new ContextAssetResolver();
        }
        return null;
    }

    public static IContainer findWebapp(IProject project)
    {
        //  TODO Support non-default locations?
        
        return (IContainer) project.findMember("src/main/webapp");
    }
}
