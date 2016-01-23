package com.anjlab.eclipse.tapestry5.templates;

import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.anjlab.eclipse.tapestry5.TapestryContext.FileNameBuilder;
import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class TapestryTemplates
{
    private static final FileNameBuilder defaultTemplates = new FileNameBuilder()
    {
        @Override
        public String getFileName(String fileName, String fileExtension)
        {
            return fileName + "." + fileExtension;
        }
    };

    private final TapestryProject tapestryProject;
    private final FileNameBuilder versionedTemplates;

    private TapestryTemplates(TapestryProject tapestryProject, FileNameBuilder versionedTemplates)
    {
        this.tapestryProject = tapestryProject;
        this.versionedTemplates = versionedTemplates;
    }

    public static TapestryTemplates get(final TapestryProject tapestryProject)
    {
        if (tapestryProject == null)
        {
            return new TapestryTemplates(tapestryProject, defaultTemplates);
        }

        return new TapestryTemplates(tapestryProject, new FileNameBuilder()
        {
            @Override
            public String getFileName(String fileName, String fileExtension)
            {
                return fileName + "-" + tapestryProject.getTapestryVersionMajorMinor() + "." + fileExtension;
            }
        });
    }

    public InputStream openTemplate(IPath containerFullPath, String fileName, String fileExtension)
    {
        if (tapestryProject == null)
        {
            return getFromPlugin(fileName, fileExtension);
        }
        
        IContainer configDir =
                (IContainer) tapestryProject.getProject()
                        .findMember(TapestryUtils.SRC_MAIN_ECLIPSE_TAPESTRY5);
        
        if (configDir == null)
        {
            return getFromPlugin(fileName, fileExtension);
        }
        
        if (containerFullPath != null)
        {
            IPath tapestryRelativePath = getTapestryRelativePath(containerFullPath);
            
            if (tapestryRelativePath != null)
            {
                //  Find closest template
                for (int i = 0; i < tapestryRelativePath.segmentCount(); i++)
                {
                    IContainer subfolder = (IContainer) configDir.findMember(
                            tapestryRelativePath.removeLastSegments(i));
                    
                    if (subfolder != null)
                    {
                        InputStream stream = getFromContainer(subfolder, fileName, fileExtension);
                        
                        if (stream != null)
                        {
                            return stream;
                        }
                    }
                }
            }
        }
        
        InputStream stream = getFromContainer(configDir, fileName, fileExtension);
        
        return stream != null
                ? stream
                : getFromPlugin(fileName, fileExtension);
    }

    private IPath getTapestryRelativePath(IPath containerFullPath)
    {
        if (isFromThisProject(containerFullPath))
        {
            //  containerFullPath may not exist yet in file system
            IContainer container = null;
            
            IPath projectRelativePath = containerFullPath.removeFirstSegments(
                    tapestryProject.getProject().getFullPath().segmentCount());
            
            //  Find at closest parent that do exists
            for (int i = 0; container == null && i < projectRelativePath.segmentCount(); i++)
            {
                container = (IContainer) tapestryProject.getProject()
                        .findMember(projectRelativePath.removeLastSegments(i));
            }
            
            if (container != null)
            {
                IContainer root = TapestryUtils.getRoot(container);
                
                if (root != null)
                {
                    //  Get path relative to source folder or to WebApp
                    IPath rootRelativePath = projectRelativePath
                            .removeFirstSegments(
                                    root.getProjectRelativePath().segmentCount());
                    
                    if (TapestryUtils.isWebApp(root))
                    {
                        return rootRelativePath;
                    }
                    else
                    {
                        //  Remove leading package name from path,
                        //  make path relative to the tapestry-root packages
                        
                        IPath tapestryRelativePath = getPathOfSubpackage(
                                rootRelativePath,
                                TapestryUtils.getPagesPackage(tapestryProject.getProject()));
                        
                        if (tapestryRelativePath == null)
                        {
                            tapestryRelativePath = getPathOfSubpackage(
                                    rootRelativePath,
                                    TapestryUtils.getComponentsPackage(tapestryProject.getProject()));
                        }
                        
                        if (tapestryRelativePath == null)
                        {
                            tapestryRelativePath = getPathOfSubpackage(
                                    rootRelativePath,
                                    TapestryUtils.getMixinsPackage(tapestryProject.getProject()));
                        }
                        
                        return tapestryRelativePath;
                    }
                }
            }
        }
        
        return null;
    }

    private boolean isFromThisProject(IPath containerFullPath)
    {
        return tapestryProject.getProject().getFullPath().isPrefixOf(containerFullPath);
    }

    private IPath getPathOfSubpackage(IPath relativePath, String parentPackage)
    {
        IPath packagePath = Path.fromPortableString(parentPackage.replaceAll("\\.", "/"));
        
        if (packagePath.isPrefixOf(relativePath))
        {
            return relativePath.removeFirstSegments(packagePath.segmentCount());
        }
        
        return null;
    }

    private InputStream getFromContainer(
            IContainer container, String fileName, String fileExtension)
    {
        IResource resource = container.findMember(
                versionedTemplates.getFileName(fileName, fileExtension));

        if (resource == null && versionedTemplates != defaultTemplates)
        {
            resource = container.findMember(
                    defaultTemplates.getFileName(fileName, fileExtension));
        }

        try
        {
            return (resource instanceof IFile)
                    ? ((IFile) resource).getContents()
                    : null;
        }
        catch (CoreException e)
        {
            Activator.getDefault().logError("Error getting content from '" + resource + "'", e);
            return null;
        }
    }

    private InputStream getFromPlugin(String fileName, String fileExtension)
    {
        InputStream stream = getClass().getResourceAsStream(
                versionedTemplates.getFileName(fileName, fileExtension));

        if (stream == null && versionedTemplates != defaultTemplates)
        {
            stream = getClass().getResourceAsStream(
                    defaultTemplates.getFileName(fileName, fileExtension));
        }

        return stream;
    }
}
