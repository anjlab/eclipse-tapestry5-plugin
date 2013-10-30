package com.anjlab.eclipse.tapestry5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.ui.IWorkbenchWindow;

public class TapestryUtils
{
    private static final String TAPESTRY_APP_PACKAGE = "tapestry.app-package";

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
                if (fromWebapp = isInFolder(forFile, webapp))
                {
                    String relativeFileName = getRelativeFileName(forFile, webapp);
                    
                    complementFileName = fileNameBuilder.getFileName(
                            joinPath(getPagesPath(project), relativeFileName),
                            forFile.getFileExtension());
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
                
                while (container != null && !EclipseUtils.isSourceFolder(container))
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
                if (!EclipseUtils.isSourceFolder(root))
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
                    IResource file = webapp.findMember(
                            complementFileName.substring(getPagesPath(project).length()));
                    
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

    public synchronized static String getPagesPath(IProject project)
    {
        String appPackage = getAppPackage(project);
        
        return appPackage != null
             ? '/' + appPackage.replace('.', '/') + "/pages"
             : "";
    }

    public static String getComponentsPath(IProject project)
    {
        String appPackage = getAppPackage(project);
        
        return appPackage != null
             ? '/' + appPackage.replace('.', '/') + "/components"
             : "";
    }

    public static String joinPath(String part1, String part2)
    {
        return (part1 + '/' + part2).replaceAll("//", "/");
    }

    private static boolean isInFolder(IFile file, IContainer folder)
    {
        IContainer parent = file.getParent();
        while (parent != null)
        {
            if (parent.equals(folder))
            {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    public static String getRelativeFileName(IResource file, IContainer ancestor)
    {
        return file.getProjectRelativePath().toPortableString().substring(
                ancestor.getProjectRelativePath().toPortableString().length());
    }

    public static String getPagesPackage(IProject project)
    {
        return getTapestryPackage(project, "pages");
    }

    public static String getTapestryPackage(IProject project, String subpackage)
    {
        String appPackage = getAppPackage(project);
        return appPackage != null
             ? appPackage + "." + subpackage
             : null;
    }
    
    public static String getComponentsPackage(IProject project)
    {
        return getTapestryPackage(project, "components");
    }

    public static String getMixinsPackage(IProject project)
    {
        return getTapestryPackage(project, "mixins");
    }
    
    public static List<IFile> findMembers(IContainer container, String path)
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

    public static boolean isStyleSheetFile(IFile file)
    {
        return "css".equals(file.getFileExtension());
    }

    public static boolean isJavaScriptFile(IFile file)
    {
        return "js".equals(file.getFileExtension());
    }

    public static boolean isTemplateFile(IFile file)
    {
        return "tml".equals(file.getFileExtension());
    }

    public static boolean isTemplateFile(IPath path)
    {
        return "tml".equals(path.getFileExtension());
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
    
    public synchronized static String getAppPackage(IProject project)
    {
        IContainer webapp = findWebapp(project);
        
        if (webapp == null)
        {
            return null;
        }
        
        IFile webXml = (IFile) webapp.findMember("/WEB-INF/web.xml");
        
        if (webXml == null)
        {
            return null;
        }
        
        Map<String, Object> cache = Activator.getDefault().getWebXmlCache(project);
        
        String appPackage = (String) cache.get(TAPESTRY_APP_PACKAGE);
        
        if (appPackage != null)
        {
            return appPackage;
        }
        
        XMLStreamReader reader = null;
        InputStream input = null;
        
        try
        {
            input = webXml.getContents();
            
            reader = Activator.getDefault().getXMLInputFactory()
                    .createXMLStreamReader(input);
            
            while (nextStartElement(reader))
            {
                if ("param-name".equals(reader.getName().getLocalPart()))
                {
                    if (TAPESTRY_APP_PACKAGE.equals(reader.getElementText()))
                    {
                        if (nextStartElement(reader))
                        {
                            if ("param-value".equals(reader.getName().getLocalPart()))
                            {
                                appPackage = reader.getElementText();
                                
                                cache.put(TAPESTRY_APP_PACKAGE, appPackage);
                                
                                return appPackage;
                            }
                        }
                        break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            Activator.getDefault().logError("Error reading value of 'tapestry.app-package' from web.xml", e);
        }
        finally
        {
            if (reader != null)
            {
                try { reader.close(); } catch (Exception e) {}
            }
            if (input != null)
            {
                try { input.close(); } catch (Exception e) {}
            }
        }
        
        return null;
    }

    private static boolean nextStartElement(XMLStreamReader reader) throws XMLStreamException
    {
        while (reader.hasNext())
        {
            if (reader.next() == XMLStreamConstants.START_ELEMENT)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isTapestryImportAnnotation(IAnnotation annotation)
    {
        return isTapestryImportAnnotationName(annotation.getElementName());
    }

    private static boolean isTapestryImportAnnotationName(String name)
    {
        return "org.apache.tapestry5.annotations.Import".equals(name) || "Import".equals(name);
    }

    public static boolean isTapestryImportAnnotation(Annotation annotation)
    {
        return isTapestryImportAnnotationName(annotation.getTypeName().getFullyQualifiedName());
    }

    public static IFile getFileForTapestryContext(IWorkbenchWindow window)
    {
        IFile file = null;
        
        try
        {
            file = EclipseUtils.getFileFromSelection(window.getSelectionService().getSelection());
        }
        catch (JavaModelException e)
        {
            //  Ignore
        }
        
        if (file == null)
        {
            file = EclipseUtils.getFileFromPage(window.getActivePage());
        }
        
        if (file == null)
        {
            try
            {
                file = EclipseUtils.getFileFromSelection(
                        window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer"));
            }
            catch (JavaModelException e)
            {
                //  Ignore
            }
        }
        
        return file;
    }

    public static IResource getResourceForTapestryContext(IWorkbenchWindow window)
    {
        IResource resource = getFileForTapestryContext(window);
        
        if (resource == null)
        {
            try
            {
                resource = EclipseUtils.getResourceFromSelection(
                        window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer"));
            }
            catch (JavaModelException e)
            {
                //  Ignore
            }
        }
        
        return resource;
    }

    public static IContainer getRoot(IFile forFile)
    {
        if (forFile == null)
        {
            return null;
        }
        
        final IContainer webapp = findWebapp(forFile.getProject());
        
        RootDetector rootDetector = new RootDetector()
        {
            @Override
            public boolean isRoot(IContainer container)
            {
                try
                {
                    return EclipseUtils.isSourceFolder(container) || container.equals(webapp);
                }
                catch (JavaModelException e)
                {
                    return false;
                }
            }
        };
        
        return getRoot(forFile, rootDetector);
    }
    
    private static interface RootDetector
    {
        boolean isRoot(IContainer container);
    }
    
    private static IContainer getRoot(IFile forFile, RootDetector rootDetector)
    {
        if (forFile == null)
        {
            return null;
        }
        
        IContainer container = forFile.getParent();
        
        while (container != null && !rootDetector.isRoot(container))
        {
            container = container.getParent();
        }
        
        return container;
    }

    public static boolean isWebApp(IContainer root)
    {
        return root.equals(findWebapp(root.getProject()));
    }

    public static String pathToPackageName(String relativeFileName, boolean leadingDot)
    {
        return (!leadingDot && relativeFileName.startsWith("/")
                    ? relativeFileName.substring(1)
                    : relativeFileName)
                 .replaceAll("/", ".");
    }

    public static String readToEnd(InputStream stream)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try
        {
            StringBuilder builder = new StringBuilder();
            
            int ch;
            while ((ch = reader.read()) != -1)
            {
                builder.append((char) ch);
            }
            
            return builder.toString();
        }
        catch (IOException e)
        {
            Activator.getDefault().logError("Error reading stream", e);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                //  Ignore
            }
        }
        
        return null;
    }

}
