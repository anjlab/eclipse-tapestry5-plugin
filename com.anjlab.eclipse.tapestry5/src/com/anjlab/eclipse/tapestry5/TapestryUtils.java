package com.anjlab.eclipse.tapestry5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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

    public static AssetResolver createAssetResolver(String bindingPrefix) throws UnresolvableReferenceException
    {
        if ("classpath".equals(bindingPrefix))
        {
            return new ClasspathAssetResolver2();
        }
        else if ("context".equals(bindingPrefix))
        {
            return new ContextAssetResolver();
        }
        
        throw new UnresolvableReferenceException("Binding prefix '" + bindingPrefix + "' not supported");
    }

    public static IContainer findWebapp(IProject project)
    {
        //  TODO Support non-default locations?
        
        return (IContainer) project.findMember("src/main/webapp");
    }
    
    public static IFile findWebXml(IProject project)
    {
        IContainer webapp = TapestryUtils.findWebapp(project);
        
        if (webapp == null)
        {
            return null;
        }
        
        return (IFile) webapp.findMember("/WEB-INF/web.xml");
    }
    
    public static boolean isTapestryAppProject(IProject project)
    {
        return getAppPackage(project) != null;
    }
    
    public static String getAppPackage(IProject project)
    {
        return Activator.getDefault().getWebXml(project).getParamValue(TAPESTRY_APP_PACKAGE);
    }
    
    public static boolean isTapestryJavaScriptStackInterface(String interfaceName)
    {
        return "JavaScriptStack".equals(interfaceName)
                || "org.apache.tapestry5.services.javascript.JavaScriptStack".equals(interfaceName);
    }

    public static String getSimpleName(String className)
    {
        int lastDot = className.lastIndexOf('.');
        
        if (lastDot != -1)
        {
            return className.substring(lastDot + 1);
        }
        
        return className;
    }

    public static boolean isAnnotationEquals(Annotation annotation, String typeName)
    {
        return isTypeNameEquals(annotation.getTypeName().getFullyQualifiedName(), typeName);
    }

    private static boolean isTypeNameEquals(String name, String fqName)
    {
        return name.equals(fqName) || name.equals(getSimpleName(fqName));
    }

    public static IAnnotation findAnnotation(IAnnotation[] annotations, String typeName)
    {
        for (IAnnotation annotation : annotations)
        {
            if (isTypeNameEquals(annotation.getElementName(), typeName))
            {
                return annotation;
            }
        }
        
        return null;
    }

    public static boolean isTapestryImportAnnotation(Annotation annotation)
    {
        String name = annotation.getTypeName().getFullyQualifiedName();
        return "org.apache.tapestry5.annotations.Import".equals(name) || "Import".equals(name);
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
            
            return Activator.getDefault()
                    .getTapestryContextFactory()
                    .createTapestryContext(fileEditorInput.getFile())
                    .getInitialFile();
        }
        
        if (editorInput instanceof IStorageEditorInput)
        {
            try
            {
                IStorage storage = ((IStorageEditorInput) editorInput).getStorage();
                
                if (storage instanceof IJarEntryResource)
                {
                    return Activator.getDefault()
                            .getTapestryContextFactory()
                            .createTapestryContext((IJarEntryResource) storage)
                            .getInitialFile();
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
                return Activator.getDefault()
                        .getTapestryContextFactory()
                        .createTapestryContext(classFile)
                        .getInitialFile();
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
            return Activator.getDefault()
                    .getTapestryContextFactory()
                    .createTapestryContext((IClassFile) firstElement)
                    .getInitialFile();
        }
        
        if (firstElement instanceof IJarEntryResource)
        {
            return Activator.getDefault()
                    .getTapestryContextFactory()
                    .createTapestryContext((IJarEntryResource) firstElement)
                    .getInitialFile();
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
                return Activator.getDefault()
                        .getTapestryContextFactory()
                        .createTapestryContext(file)
                        .getInitialFile();
            }
        }
        
        return null;
    }

    public static TapestryContextScope getTapestryContext(IWorkbenchWindow window, String componentName)
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
        
        if (targetContext == null)
        {
            return null;
        }
        
        return new TapestryContextScope(window, tapestryModule.getProject(), targetContext, null);
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
                    return Activator.getDefault()
                            .getTapestryContextFactory()
                            .createTapestryContext(file)
                            .getInitialFile();
                }
            }
        }
        catch (JavaModelException e)
        {
            //  Ignore
        }
        return null;
    }

    public static String getDefaultContextNameFromFileName(String fileNameWithoutExtension)
    {
        final String DEFAULT_CONTEXT_NAME = "Unknown";
        
        if (StringUtils.isEmpty(fileNameWithoutExtension))
        {
            return DEFAULT_CONTEXT_NAME;
        }
        
        StringBuilder builder = new StringBuilder(fileNameWithoutExtension.length());
        
        boolean changeCase = true;
        
        for (int i = 0; i < fileNameWithoutExtension.length(); i++)
        {
            char ch = fileNameWithoutExtension.charAt(i);
            
            if (Character.isDigit(ch) || Character.isLetter(ch))
            {
                if (changeCase)
                {
                    ch = Character.toUpperCase(ch);
                    
                    changeCase = Character.isDigit(ch);
                }
                
                builder.append(ch);
            }
            else
            {
                changeCase = true;
            }
        }
        
        return builder.length() == 0
             ? DEFAULT_CONTEXT_NAME
             : builder.toString();
    }

    public static JavaScriptStack findStack(IProject project, String stackName)
    {
        IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
        
        for (IWorkbenchWindow window : windows)
        {
            TapestryProject tapestryProject = Activator.getDefault().getTapestryProject(window);
            
            if (tapestryProject != null && tapestryProject.contains(project))
            {
                return tapestryProject.findStack(stackName);
            }
        }
        
        return null;
    }

    public static boolean isTapestryDefaultNamespace(String namespace)
    {
        return !StringUtils.isEmpty(namespace)
            && namespace.startsWith("http://tapestry.apache.org/schema/tapestry");
    }

    public static String findTapestryAttribute(Node node, String attributeName)
    {
        NamedNodeMap attributes = node.getAttributes();
        
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Node attribute = attributes.item(i);
            
            if (attribute.getNamespaceURI() != null && isTapestryDefaultNamespace(attribute.getNamespaceURI()))
            {
                if (attributeName.equals(attribute.getLocalName()))
                {
                    return attribute.getNodeValue();
                }
            }
        }
        return null;
    }

    public static String getComponentName(IWorkbenchWindow window, Node element)
    {
        //  if element is from Tapestry namespace -- we have a componentName
        String namespace = element.getNamespaceURI();
        
        if (namespace != null)
        {
            if (isTapestryDefaultNamespace(namespace))
            {
                return element.getLocalName();
            }
            else if (namespace.startsWith("tapestry-library:"))
            {
                return namespace.substring("tapestry-library:".length()) + "." + element.getLocalName();
            }
            else if (namespace.equals("tapestry:parameter"))
            {
                //  TODO Add support for tapestry parameters
                return null;
            }
        }
        
        String componentName = findTapestryAttribute(element, "type");
        
        if (componentName == null)
        {
            String componentId = findTapestryAttribute(element, "id");
            
            if (componentId != null)
            {
                //  Get type from field declaration in Java class
                
                TapestryContext tapestryContext = Activator.getDefault().getTapestryContext(window);
                
                if (tapestryContext == null)
                {
                    return null;
                }
                
                TapestryProject tapestryProject = Activator.getDefault().getTapestryProject(window);
                
                for (Component component : tapestryContext.getSpecification().getComponents())
                {
                    if (StringUtils.equals(componentId, component.getId()))
                    {
                        return getComponentName(tapestryProject, component);
                    }
                }
            }
        }
        return componentName;
    }

    public static String getComponentName(TapestryProject tapestryProject, Component component)
    {
        if (component.isJavaType())
        {
            //  Convert component type to component name
            for (TapestryModule tapestryModule : tapestryProject.modules())
            {
                for (LibraryMapping libraryMapping : tapestryModule.libraryMappings())
                {
                    if (component.getType().startsWith(libraryMapping.getRootPackage()))
                    {
                        return component.getType().substring(
                                (libraryMapping.getRootPackage() + ".components").length()
                                + ".".length());
                    }
                }
            }
            
            return null;
        }
        
        return component.getType();
    }

    public static boolean isTapestryComponentsNamespace(String namespace)
    {
        if (StringUtils.isEmpty(namespace))
        {
            return false;
        }
        
        return isTapestryDefaultNamespace(namespace)
            || namespace.startsWith("tapestry-library:");
    }

    public static final String ORG_APACHE_TAPESTRY5_ANNOTATIONS_PATH = "org.apache.tapestry5.annotations.Path";

    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_SUB_MODULE = "org.apache.tapestry5.ioc.annotations.SubModule";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_IMPORT_MODULE = "org.apache.tapestry5.ioc.annotations.ImportModule";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ADVISE = "org.apache.tapestry5.ioc.annotations.Advise";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_SERVICE_ID = "org.apache.tapestry5.ioc.annotations.ServiceId";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_CONTRIBUTE = "org.apache.tapestry5.ioc.annotations.Contribute";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_STARTUP = "org.apache.tapestry5.ioc.annotations.Startup";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_DECORATE = "org.apache.tapestry5.ioc.annotations.Decorate";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ORDER = "org.apache.tapestry5.ioc.annotations.Order";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_MATCH = "org.apache.tapestry5.ioc.annotations.Match";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_OPTIONAL = "org.apache.tapestry5.ioc.annotations.Optional";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_MARKER = "org.apache.tapestry5.ioc.annotations.Marker";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_SYMBOL = "org.apache.tapestry5.ioc.annotations.Symbol";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_INJECT = "org.apache.tapestry5.ioc.annotations.Inject";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_INJECT_SERVICE = "org.apache.tapestry5.ioc.annotations.InjectService";
    public static final String ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_LOCAL = "org.apache.tapestry5.ioc.annotations.Local";

    public static final String JAVAX_INJECT_NAMED = "javax.inject.Named";

    public static final String BUILD_METHOD_NAME_PREFIX = "build";
    public static final String CONTRIBUTE_METHOD_NAME_PREFIX = "contribute";
    public static final String ADVISE_METHOD_NAME_PREFIX = "advise";
    public static final String DECORATE_METHOD_NAME_PREFIX = "decorate";

    public static boolean isStartupMethod(IMethod method) throws JavaModelException
    {
        return method.getElementName().equals("startup")
                || findAnnotation(
                        method.getAnnotations(),
                        ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_STARTUP) != null;
    }

    public static boolean isContributorMethod(IMethod method) throws JavaModelException
    {
        return method.getElementName().startsWith(CONTRIBUTE_METHOD_NAME_PREFIX)
                || findAnnotation(
                        method.getAnnotations(),
                        ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_CONTRIBUTE) != null;
    }

    public static boolean isAdvisorMethod(IMethod method) throws JavaModelException
    {
        return method.getElementName().startsWith(ADVISE_METHOD_NAME_PREFIX)
                || findAnnotation(
                        method.getAnnotations(),
                        ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_ADVISE) != null;
    }

    public static boolean isDecoratorMethod(IMethod method) throws JavaModelException
    {
        return method.getElementName().startsWith(DECORATE_METHOD_NAME_PREFIX)
            || findAnnotation(
                    method.getAnnotations(),
                    ORG_APACHE_TAPESTRY5_IOC_ANNOTATIONS_DECORATE) != null;
    }

    public static boolean isServiceBuilderMethod(IMethod method)
    {
        return method.getElementName().startsWith(BUILD_METHOD_NAME_PREFIX);
    }

    public static TapestryProject getCurrentProject()
    {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        
        TapestryProject tapestryProject = Activator.getDefault().getTapestryProject(window);
        
        return tapestryProject;
    }

    public static String expandSymbols(String input)
    {
        TapestryProject project = getCurrentProject();
        
        if (project != null)
        {
            try
            {
                input = project.expandSymbols(input);
            }
            catch (RuntimeException e)
            {
                Activator.getDefault().logWarning(
                        "Error expanding asset '" + input + "'", e);
            }
        }
        return input;
    }

    public static boolean isModuleFile(IFile affectedFile, TapestryModule module)
    {
        return ObjectUtils.equals(module.getModuleFile().getPath(), affectedFile.getProjectRelativePath())
                && ObjectUtils.equals(module.getModuleFile().getProject(), affectedFile.getProject());
    }

}
