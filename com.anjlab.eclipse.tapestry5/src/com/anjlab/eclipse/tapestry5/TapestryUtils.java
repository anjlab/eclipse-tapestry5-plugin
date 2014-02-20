package com.anjlab.eclipse.tapestry5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class TapestryUtils
{
    private static final String TAPESTRY_APP_PACKAGE = "tapestry.app-package";

    public static String getPagesPath(IProject project)
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

    public static boolean isInFolder(IFile file, IContainer folder)
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

    public static String getRelativeFileName(IResource resource, IContainer ancestor)
    {
        return resource.getProjectRelativePath().toPortableString().substring(
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
    
    public static boolean isStyleSheetFile(IPath path)
    {
        return "css".equals(path.getFileExtension());
    }

    public static boolean isJavaScriptFile(IPath file)
    {
        return "js".equals(file.getFileExtension());
    }

    public static boolean isTemplateFile(IPath path)
    {
        return "tml".equals(path.getFileExtension());
    }

    public static boolean isClassFile(IPath path)
    {
        return "class".equals(path.getFileExtension());
    }

    
    public static boolean isJavaFile(IPath path)
    {
        return "java".equals(path.getFileExtension());
    }

    public static boolean isPropertiesFile(IPath path)
    {
        return "properties".equals(path.getFileExtension());
    }

    public static AssetResolver createAssetResolver(String bindingPrefix) throws AssetException
    {
        if ("classpath".equals(bindingPrefix))
        {
            return new ClasspathAssetResolver();
        }
        else if ("context".equals(bindingPrefix))
        {
            return new ContextAssetResolver();
        }
        
        throw new AssetException("Binding prefix '" + bindingPrefix + "' not supported");
    }

    public static IContainer findWebapp(IProject project)
    {
        //  TODO Support non-default locations?
        
        return (IContainer) project.findMember("src/main/webapp");
    }
    
    public static boolean isTapestryAppProject(IProject project)
    {
        return getAppPackage(project) != null;
    }
    
    public static String getAppPackage(IProject project)
    {
        return Activator.getDefault().getWebXml(project).getParamValue(TAPESTRY_APP_PACKAGE);
    }
    
    public static boolean isTapestrySubModuleAnnotation(IAnnotation annotation)
    {
        return "org.apache.tapestry5.ioc.annotations.SubModule".equals(annotation.getElementName())
            || "SubModule".equals(annotation.getElementName());
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

    public static TapestryContext createTapestryContext(TapestryFile forFile)
    {
        if (forFile instanceof LocalFile)
        {
            return new LocalTapestryContext(forFile);
        }
        
        return TapestryContext.emptyContext();
    }

    public static TapestryContext createTapestryContext(IFile file)
    {
        return new LocalTapestryContext(file);
    }

    public static TapestryContext createTapestryContext(IWorkbenchWindow window)
    {
        TapestryFile file = getTapestryFileFromSelection(window.getSelectionService().getSelection());
        
        if (file != null)
        {
            return file.getContext();
        }
        
        file = TapestryUtils.getTapestryFileFromPage(window.getActivePage());
        
        if (file != null)
        {
            return file.getContext();
        }
        
        file = getTapestryFileFromSelection(EclipseUtils.getProjectExplorerSelection(window));
        
        if (file != null)
        {
            return file.getContext();
        }
        
        return TapestryContext.emptyContext();
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

    public static String getSimpleName(String className)
    {
        String[] parts = className.split("\\.");
        return parts[parts.length - 1];
    }

    public static TapestryContext createTapestryContext(IJarEntryResource jarEntry)
    {
        return new JarTapestryContext(jarEntry);
    }

    public static TapestryContext createTapestryContext(IClassFile classFile)
    {
        return new JarTapestryContext(classFile);
    }

    public static TapestryFile getTapestryFileFromPage(IWorkbenchPage page)
    {
        if (page == null)
        {
            return null;
        }
        
        IEditorPart activeEditor = page.getActiveEditor();
        
        if (activeEditor == null)
        {
            return null;
        }
        
        IEditorInput editorInput = activeEditor.getEditorInput();
        
        if (editorInput instanceof IFileEditorInput)
        {
            IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
            
            return createTapestryContext(fileEditorInput.getFile()).getInitialFile();
        }
        
        if (editorInput instanceof IStorageEditorInput)
        {
            try
            {
                IStorage storage = ((IStorageEditorInput) editorInput).getStorage();
                
                if (storage instanceof IJarEntryResource)
                {
                    return createTapestryContext((IJarEntryResource) storage).getInitialFile();
                }
            }
            catch (CoreException e)
            {
                //  Ignore
                Activator.getDefault().logError("Error getting file from JAR", e);
            }
        }
        
        if (editorInput instanceof IClassFileEditorInput)
        {
            IClassFile classFile = ((IClassFileEditorInput) editorInput).getClassFile();
            
            if (classFile != null)
            {
                return createTapestryContext(classFile).getInitialFile();
            }
        }
        
        return null;
    }

    private static TapestryFile getTapestryFileFromSelectionElement(Object firstElement) throws JavaModelException
    {
        if (firstElement == null)
        {
            return null;
        }
        
        if (firstElement instanceof ICompilationUnit)
        {
            ICompilationUnit compilationUnit = ((ICompilationUnit) firstElement);
            
            TapestryFile file = TapestryUtils.getFileFromResource(compilationUnit.getCorrespondingResource());
            
            if (file != null)
            {
                return file;
            }
        }
        
        if (firstElement instanceof IClassFile)
        {
            return createTapestryContext((IClassFile) firstElement).getInitialFile();
        }
        
        if (firstElement instanceof IJarEntryResource)
        {
            return createTapestryContext((IJarEntryResource) firstElement).getInitialFile();
        }
        
        if (firstElement instanceof ITreeSelection)
        {
            ITreeSelection treeSelection = (ITreeSelection) firstElement;
            
            return getTapestryFileFromSelectionElement(treeSelection.getFirstElement());
        }
        
        IResource resource = (IResource) Platform.getAdapterManager().getAdapter(firstElement, IResource.class);
        
        if (resource != null)
        {
            TapestryFile file = TapestryUtils.getFileFromResource(resource);
            
            if (file != null)
            {
                return file;
            }
        }
        
        return null;
    }

    public static TapestryModule getTapestryModule(IWorkbenchWindow window, IProject project)
    {
        if (project != null)
        {
            TapestryProject tapestryProject = Activator.getDefault().getTapestryProject(window);
            
            if (tapestryProject != null)
            {
                for (TapestryModule module : tapestryProject.modules())
                {
                    if (project.equals(
                            module.getModuleClass().getJavaProject().getProject()))
                    {
                        return module;
                    }
                }
            }
        }
        
        return null;
    }

    public static TapestryFile getTapestryFileFromSelection(ISelection selection)
    {
        try
        {
            return selection instanceof IStructuredSelection
                    ? getTapestryFileFromSelectionElement(((IStructuredSelection) selection).getFirstElement())
                    : null;
        }
        catch (JavaModelException e)
        {
            //  Ignore
            return null;
        }
    }

    protected static TapestryFile getFileFromResource(IResource resource)
    {
        if (resource != null)
        {
            IFile file = (IFile) resource.getAdapter(IFile.class);
            
            if (file != null)
            {
                return createTapestryContext(file).getInitialFile();
            }
        }
        
        return null;
    }

    public static TapestryContext getTapestryContext(IWorkbenchWindow window, String componentName)
    {
        TapestryContext tapestryContext = Activator.getDefault().getTapestryContext(window);
        
        if (tapestryContext == null)
        {
            return null;
        }
        
        TapestryModule tapestryModule = getTapestryModule(window, tapestryContext.getProject());
        
        if (tapestryModule == null)
        {
            return null;
        }
        
        TapestryContext targetContext;
        try
        {
            targetContext = tapestryModule.getProject().findComponentContext(componentName);
        }
        catch (JavaModelException e)
        {
            return null;
        }
        
        return targetContext;
    }

    public static IWorkbenchWindow getWorkbenchWindow(Shell shell)
    {
        IWorkbenchWindow currentWindow = null;
        
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows())
        {
            if (shell == window.getShell())
            {
                currentWindow = window;
                break;
            }
        }
        return currentWindow;
    }

    public static TapestryFile findFileInSourceFolders(IJavaProject javaProject, String path)
    {
        try
        {
            for (IPackageFragmentRoot root : javaProject.getAllPackageFragmentRoots())
            {
                if (!EclipseUtils.isSourceFolder(root))
                {
                    continue;
                }
                
                IContainer container = (IContainer) root.getCorrespondingResource().getAdapter(IContainer.class);
                
                IFile file = EclipseUtils.findFileCaseInsensitive(container, path);
                
                if (file != null)
                {
                    return createTapestryContext(file).getInitialFile();
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
