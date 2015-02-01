package com.anjlab.eclipse.tapestry5;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class ClasspathAssetResolver2 extends ClasspathAssetResolver
{
    @Override
    public TapestryFile resolve(final String path, final TapestryFile relativeTo)
            throws UnresolvableReferenceException
    {
        try
        {
            return super.resolve(path, relativeTo);
        }
        catch (UnresolvableReferenceException e)
        {
            //  Tapestry 5.4
            try
            {
                IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                
                final TapestryProject tapestryProject = Activator.getDefault().getTapestryProject(window);
                
                if (tapestryProject == null)
                {
                    //  TODO Project could have been not yet initialized, so we should try to resolve
                    //  this file again when project becomes available
                    
                    throw createAssetException(path, null);
                }
                
                //  Find library mapping for the parentDir's packageName
                //  and lookup META-INF/assets/{path-prefix}/subDir/fileName
                
                FileLookup fileLookup = relativeTo.getContext().createLookup();
                
                String relativePath = fileLookup.findClasspathRelativePath(relativeTo);
                
                if (StringUtils.isEmpty(relativePath))
                {
                    throw createAssetException(path, null);
                }
                
                IPath basePath = new Path(relativePath);
                
                IPath parentDir = basePath.removeLastSegments(1);
                
                String packageName =
                        StringUtils
                            .removeStart(parentDir.toPortableString(), "/")
                            .replace('/', '.');
                
                LibraryMapping mapping = tapestryProject.findLibraryMapping(packageName);
                
                if (mapping == null)
                {
                    throw createAssetException(path, null);
                }
                
                IPath rootPath = new Path(mapping.getRootPackage().replace('.', '/'));
                
                IPath subDir = parentDir.makeRelativeTo(rootPath).addTrailingSeparator();
                
                if ("components".equals(subDir.segment(0))
                      || "pages".equals(subDir.segment(0)))
                {
                    //  Component & Page assets share the same virtual folder
                    subDir = subDir.removeFirstSegments(1);
                }
                
                String folderName = StringUtils.isEmpty(mapping.getPathPrefix())
                        ? ""
                        : mapping.getPathPrefix() + "/";
                
                return fileLookup
                        .findClasspathFileCaseInsensitive(
                                "META-INF/assets/"
                                        + folderName
                                        + (subDir.segmentCount() == 0 ? "" : subDir.toPortableString())
                                        + path);
            }
            catch (JavaModelException e2)
            {
                throw createAssetException(path, e2);
            }
        }
    }
}
