package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

@SuppressWarnings("restriction")
public class EclipseUtils
{
    public static final String ECLIPSE_INTEGRATION_FOR_TAPESTRY5 = "Eclipse Integration for Tapestry5";
    
    public static final String SOURCE_NOT_FOUND = "source not found";

    public static ISelection getProjectExplorerSelection(IWorkbenchWindow window)
    {
        return window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
    }

    public static interface EditorCallback
    {
        void editorOpened(IEditorPart editorPart);
    }
    
    public static void openFile(final IWorkbenchWindow window, final IFile file)
    {
        openFile(window, file, null);
    }
    
    public static void openFile(final IWorkbenchWindow window, TapestryFile file)
    {
        openFile(window, file, null);
    }
    
    public static void openFile(final IWorkbenchWindow window, TapestryFile file, final EditorCallback editorCallback)
    {
        if (file instanceof TapestryFileReference)
        {
            TapestryFileReference reference = (TapestryFileReference) file;
            try
            {
                file = reference.resolveFile(false);
            }
            catch (UnresolvableReferenceException e)
            {
                EclipseUtils.openError(window,
                        "Unable to resolve '" + reference.getReference() + "': "
                                + e.getLocalizedMessage());
                
                return;
            }
        }
        
        if (file instanceof LocalFile)
        {
            openFile(window, ((LocalFile) file).getFile(), editorCallback);
        }
        else if (file instanceof JarEntryFile)
        {
            openInEditor(((JarEntryFile) file).getJarEntry(), editorCallback);
        }
        else if (file instanceof ClassFile)
        {
            openInEditor(((ClassFile) file).getClassFile(), editorCallback);
        }
    }

    private static void openInEditor(Object inputElement, final EditorCallback editorCallback)
    {
        try
        {
            IEditorPart editorPart = EditorUtility.openInEditor(inputElement);
            
            if (editorCallback != null)
            {
                editorCallback.editorOpened(editorPart);
            }
        }
        catch (PartInitException e)
        {
            Activator.getDefault().logError("Unable to open editor", e);
        }
    }
    
    public static void openFile(final IWorkbenchWindow window, final IFile file, final EditorCallback editorCallback)
    {
        window.getShell().getDisplay().asyncExec(new Runnable()
        {
            public void run()
            {
                try
                {
                    IEditorPart editor = IDE.openEditor(window.getActivePage(), file, true);
                    
                    if (editorCallback != null)
                    {
                        editorCallback.editorOpened(editor);
                    }
                }
                catch (Exception e)
                {
                    Activator.getDefault().logError("Unable to open editor", e);
                    
                    openError(window, "Unable to open editor: " + e.getLocalizedMessage());
                }
            }
        });
    }

    public static void openError(final IWorkbenchWindow window, String message)
    {
        MessageDialog.openError(
                window.getShell(),
                ECLIPSE_INTEGRATION_FOR_TAPESTRY5,
                message);
    }

    public static void openInformation(final IWorkbenchWindow window, String message)
    {
        MessageDialog.openInformation(
                window.getShell(),
                ECLIPSE_INTEGRATION_FOR_TAPESTRY5,
                message);
    }

    public static <T> List<T> getAllAffectedResources(IResourceDelta delta, Class<T> clazz)
    {
        return getAllAffectedResources(delta, clazz, 0xFFFFFFFF);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> List<T> getAllAffectedResources(IResourceDelta delta, Class<T> clazz, int deltaKind)
    {
        List<T> files = new ArrayList<T>();
        
        for (IResourceDelta child : delta.getAffectedChildren())
        {
            IResource resource = child.getResource();
            
            if (resource != null && clazz.isAssignableFrom(resource.getClass()))
            {
                if ((child.getKind() & deltaKind) != 0)
                {
                    files.add((T) resource);
                }
            }
            else
            {
                files.addAll(getAllAffectedResources(child, clazz, deltaKind));
            }
        }
        return files;
    }

    public static boolean isSourceFolder(IContainer container) throws JavaModelException
    {
        return EclipseUtils.isSourceFolder((IJavaElement) container.getAdapter(IJavaElement.class));
    }

    public static boolean isSourceFolder(IJavaElement javaElement) throws JavaModelException
    {
        return javaElement != null
            && (javaElement instanceof IPackageFragmentRoot)
            && (((IPackageFragmentRoot) javaElement).getKind() == IPackageFragmentRoot.K_SOURCE);
    }

    public static IFile findFileCaseInsensitive(IContainer container, String componentPath)
    {
        String[] parts = (componentPath.startsWith("/") ? componentPath.substring(1) : componentPath).split("/");
        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            try
            {
                boolean found = false;
                
                for (IResource member : container.members())
                {
                    if (part.equalsIgnoreCase(member.getName()))
                    {
                        if (member instanceof IFile && i == parts.length - 1)
                        {
                            return (IFile) member;
                        }
                        
                        if (!(member instanceof IContainer))
                        {
                            return null;
                        }
                        
                        container = (IContainer) member;
                        
                        found = true;
                        
                        break;
                    }
                }
                
                if (!found)
                {
                    return null;
                }
            }
            catch (CoreException e)
            {
            }
        }
        return null;
    }

    public static IField findFieldDeclaration(IProject project, Name name)
    {
        SearchPattern pattern = SearchPattern.createPattern(name.getFullyQualifiedName(),
                IJavaSearchConstants.FIELD, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_FULL_MATCH);
        
        final List<SearchMatch> matches = searchJava(project, pattern);
        
        return exactMatchOrNull(matches, IField.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T exactMatchOrNull(final List<SearchMatch> matches, Class<T> clazz)
    {
        for (SearchMatch match : matches)
        {
            if (match.isExact() && clazz.isAssignableFrom(match.getElement().getClass()))
            {
                return (T) match.getElement();
            }
        }
        
        return null;
    }

    public static IType findTypeDeclaration(IProject project, String className)
    {
        SearchPattern pattern = SearchPattern.createPattern(className,
                IJavaSearchConstants.TYPE,
                IJavaSearchConstants.DECLARATIONS,
                SearchPattern.R_FULL_MATCH);
        
        final List<SearchMatch> matches = searchJava(project, pattern);
        
        return exactMatchOrNull(matches, IType.class);
    }

    private static List<SearchMatch> searchJava(IProject project,
            SearchPattern pattern)
    {
        IJavaSearchScope scope = SearchEngine.createJavaSearchScope(
                new IJavaElement[] { JavaCore.create(project) });
        
        final List<SearchMatch> matches = new ArrayList<SearchMatch>();
        
        SearchRequestor requestor = new SearchRequestor()
        {
            @Override
            public void acceptSearchMatch(SearchMatch match) throws CoreException
            {
                matches.add(match);
            }
        };
    
        SearchEngine searchEngine = new SearchEngine();
        
        try
        {
            searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
                                scope, requestor, null);
        }
        catch (CoreException e)
        {
            Activator.getDefault().logWarning("Error performing search", e);;
        }
        return matches;
    }

    public static CompilationUnit parse(ICompilationUnit unit)
    {
        String source;
        try
        {
            source = unit.getSource();
        }
        catch (JavaModelException e)
        {
            throw new IllegalStateException(SOURCE_NOT_FOUND, e);
        }
        
        return parse(source);
    }

    public static CompilationUnit parse(String source)
    {
        if (source == null)
        {
            throw new IllegalStateException(SOURCE_NOT_FOUND);
        }
        
        ASTParser parser = ASTParser.newParser(getParserLevel());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null);
    }

    private static int parserLevel = -1;

    @SuppressWarnings("deprecation")
    public static int getParserLevel()
    {
        if (parserLevel == -1)
        {
            try
            {
                int JLS8 = 8;
                // Try to use Java 8's AST.JLS8
                ASTParser.newParser(JLS8);
                parserLevel = JLS8;
            }
            catch (IllegalArgumentException e)
            {
                //  Fallback to Java 7
                parserLevel = AST.JLS4;
            }
        }

        return parserLevel;
    }

    public static IProject getProjectFromSelection(ISelection selection)
    {
        if (selection instanceof ITreeSelection)
        {
            Object firstElement = ((ITreeSelection) selection).getFirstElement();
            
            if (firstElement != null)
            {
                IResource resource = (IResource) Platform.getAdapterManager().getAdapter(
                        firstElement, IResource.class);
                
                if (resource != null)
                {
                    return resource.getProject();
                }
            }
        }
        
        return null;
    }

    public static String eval(Object value, int valueKind, AST ast, IProject project)
    {
        if (ast != null
                && (valueKind == IMemberValuePair.K_SIMPLE_NAME
                    || valueKind == IMemberValuePair.K_QUALIFIED_NAME))
        {
            Name name = ast.newName((String) value);
            return evalExpression(project, name);
        }
        return (String) value;
    }

    public static String evalExpression(IProject project, Object expr)
    {
        if (expr instanceof String)
        {
            return (String) expr;
        }
        
        if (expr instanceof StringLiteral)
        {
            return ((StringLiteral) expr).getLiteralValue();
        }
        
        if (expr instanceof Name)
        {
            IField field = findFieldDeclaration(project, ((Name) expr));
            
            if (field != null)
            {
                try
                {
                    //  XXX String literals sometimes returned in quotes as they present in source code,
                    //  for example:
                    //      String foo = "bar";
                    //  may be returned as "bar" (in quotes) instead of just bar (without quotes).
                    
                    return (String) field.getConstant();
                }
                catch (JavaModelException e)
                {
                    //  Ignore
                }
            }
        }
        
        return "<" + expr + ">";
    }

    public static void ensureFileIsOpenedInEditor(IWorkbenchWindow window, IFile file, EditorCallback editorCallback)
    {
        IEditorReference[] editors = window.getActivePage().getEditorReferences();
        
        for (IEditorReference editor : editors)
        {
            try
            {
                IEditorInput editorInput = editor.getEditorInput();
                
                if (editorInput instanceof IFileEditorInput)
                {
                    if (ObjectUtils.equals(((IFileEditorInput) editorInput).getFile(), file))
                    {
                        //  The file is opened in editor
                        
                        editorCallback.editorOpened(null);
                        
                        return;
                    }
                }
            }
            catch (PartInitException e)
            {
                //  Ignore
            }
        }
        
        openFile(window, file, editorCallback);
    }

    public static boolean isJavaProject(IProject project) throws CoreException
    {
        return project.hasNature(JavaCore.NATURE_ID);
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

    public static void openDeclaration(IJavaElement element, EditorCallback editorCallback)
    {
        if (element == null)
        {
            return;
        }
        
        try
        {
            IEditorPart editor = JavaUI.openInEditor(element);
            
            if (editorCallback != null)
            {
                editorCallback.editorOpened(editor);
            }
        }
        catch (Exception e)
        {
            Activator.getDefault().logError("Error opening " + element.getElementName(), e);
        }
    }

    public static String resolveTypeNameForMember(IType type, IMember member, String typeSignature)
            throws JavaModelException
    {
        String typeName = Signature.toString(typeSignature);
        
        if (member.isBinary())
        {
            return typeName;
        }
        
        return resolveTypeName(type, typeName);
    }

    public static String resolveTypeName(IType type, String typeName) throws JavaModelException
    {
        String[][] resolvedTypes = type.resolveType(typeName);
        
        if (resolvedTypes == null)
        {
            return typeName;
        }
        else
        {
            return resolvedTypes[0][0] + "." + resolvedTypes[0][1];
        }
    }

    public static String toClassName(IProject project, TypeLiteral typeLiteral)
    {
        Type type = typeLiteral.getType();
        
        Name name = null;
        
        if (type instanceof SimpleType)
        {
            name = ((SimpleType) type).getName();
        }
        else if (type instanceof QualifiedType)
        {
            name = ((QualifiedType) type).getName();
        }
        else
        {
            return null;
        }
        
        return name.isQualifiedName()
                 ? name.getFullyQualifiedName()
                 : tryResolveFQNameFromImports(project, typeLiteral.getRoot(), name.getFullyQualifiedName());
    }

    private static String tryResolveFQNameFromImports(IProject project, ASTNode root, String simpleName)
    {
        if (!(root instanceof CompilationUnit))
        {
            return simpleName;
        }

        CompilationUnit compilationUnit = (CompilationUnit) root;

        for (Object importObj : compilationUnit.imports())
        {
            ImportDeclaration importDecl = (ImportDeclaration) importObj;

            if (importDecl.getName().getFullyQualifiedName().endsWith("." + simpleName))
            {
                return importDecl.getName().getFullyQualifiedName();
            }
            else if (importDecl.isOnDemand())
            {
                String packageName = importDecl.getName().getFullyQualifiedName();

                String candidate = packageName + "." + simpleName;

                if (EclipseUtils.findTypeDeclaration(project, candidate) != null)
                {
                    return candidate;
                }
            }
        }

        //  Assume it's from the same package
        return compilationUnit.getPackage().getName().getFullyQualifiedName() + "." + simpleName;
    }

}
