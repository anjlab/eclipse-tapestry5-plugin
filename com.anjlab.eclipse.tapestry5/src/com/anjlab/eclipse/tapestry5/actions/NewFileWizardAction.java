package com.anjlab.eclipse.tapestry5.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.EclipseUtils.EditorCallback;
import com.anjlab.eclipse.tapestry5.LocalFile;
import com.anjlab.eclipse.tapestry5.SetEditorCaretPositionOffsetLength;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryUtils;
import com.anjlab.eclipse.tapestry5.templates.TapestryTemplates;

public class NewFileWizardAction extends Action
{
    private static final String CARET_SNIPPET = "$Caret$";
    
    private TapestryContext tapestryContext;
    private IProject project;
    
    private String folder;
    private String fileName;
    
    private Shell shell;
    private IWorkbenchWindow window;
    
    public NewFileWizardAction(IProject project, TapestryContext tapestryContext, Shell shell, IWorkbenchWindow window)
    {
        this.project = project;
        this.tapestryContext = tapestryContext;
        this.shell = shell;
        this.window = window;
    }
    
    public void run()
    {
        WizardDialog dialog = new WizardDialog(shell, new Wizard()
        {
            private WizardNewFileCreationPage fileCreationPage;
            
            private Stack<IFolder> preCreatedFolders = new Stack<IFolder>();
            
            @Override
            public boolean performCancel()
            {
                deletePreCreatedFolders(null);
                
                return super.performCancel();
            }

            private void deletePreCreatedFolders(IContainer existingFolder)
            {
                while (!preCreatedFolders.isEmpty())
                {
                    try
                    {
                        IFolder folder = preCreatedFolders.pop();
                        
                        if (isEmpty(folder))
                        {
                            folder.delete(false, null);
                        }
                    }
                    catch (CoreException e)
                    {
                        Activator.getDefault().logWarning("Unable to delete pre-created folder during cleanup", e);
                    }
                }
            }

            private boolean isEmpty(IFolder folder) throws CoreException
            {
                return folder.members().length == 0;
            }

            private void preCreateFolder(IFolder folder) throws CoreException
            {
                if (!folder.exists())
                {
                    IContainer parent = folder.getParent();
                    
                    if (parent instanceof IFolder)
                    {
                        preCreateFolder((IFolder) parent);
                    }
                    
                    folder.create(false, true, null);
                    
                    preCreatedFolders.add(folder);
                }
            }
            
            @Override
            public void addPages()
            {
                super.addPages();
                
                setWindowTitle("Creating file for Tapestry Context");
                
                List<Object> segments = new ArrayList<Object>();
                
                segments.add(project);
                
                if (folder != null)
                {
                    IFolder segment = project.getFolder(folder);
                    
                    if (!segment.exists())
                    {
                        try
                        {
                            preCreateFolder(segment);
                        }
                        catch (CoreException e)
                        {
                            Activator.getDefault().logWarning("Unable to pre-create folder for new file", e);
                        }
                    }
                    
                    segments.add(segment);
                }
                
                IStructuredSelection selection = new TreeSelection(new TreePath(segments.toArray()));
                
                fileCreationPage = new WizardNewFileCreationPage("", selection)
                {
                    @Override
                    protected InputStream getInitialContents()
                    {
                        caretPosition = -1;
                        
                        IPath newFile = Path.fromPortableString(fileCreationPage.getFileName());
                        
                        TapestryTemplates templates = TapestryTemplates.get(Activator.getDefault().getTapestryProject(window));
                        
                        InputStream stream = templates.openTemplate("snippet", newFile.getFileExtension());
                        
                        if (stream != null)
                        {
                            String content = TapestryUtils.readToEnd(stream);
                            
                            String contextName = TapestryUtils.getDefaultContextNameFromFileName(newFile.removeFileExtension().lastSegment());
                            
                            if (tapestryContext != null)
                            {
                                contextName = tapestryContext.getName();
                            }
                            
                            content = content.replace("$ContextName$", contextName);
                            
                            caretPosition = content.indexOf(CARET_SNIPPET);
                            
                            if (caretPosition != -1)
                            {
                                content = content.replace(CARET_SNIPPET, "");
                            }
                            
                            return new ByteArrayInputStream(content.getBytes());
                        }
                        
                        return super.getInitialContents();
                    }
                };
                fileCreationPage.setFileName(fileName);
                fileCreationPage.setTitle("New file");
                
                addPage(fileCreationPage);
            }
            
            private int caretPosition;
            
            @Override
            public boolean performFinish()
            {
                IFile file = fileCreationPage.createNewFile();
                
                deletePreCreatedFolders(file.getParent());
                
                try
                {
                    String resourceType = getImportResourceType(file);
                    
                    if (resourceType == null || tapestryContext == null)
                    {
                        return true;
                    }
                    
                    addImport(file, resourceType);
                    
                    return true;
                }
                finally
                {
                    EclipseUtils.openFile(window, file, new SetEditorCaretPositionOffsetLength(caretPosition, 0));
                }
            }

            private void addImport(final IFile file, final String resourceType)
            {
                final IFile javaFile = ((LocalFile) tapestryContext.getJavaFile()).getFile();
                
                EclipseUtils.ensureFileIsOpenedInEditor(window, javaFile, new EditorCallback()
                {
                    @Override
                    public void editorOpened(IEditorPart editorPart)
                    {
                        addImport(file, resourceType, javaFile);
                    }
                });
            }

            private void addImport(IFile file, String resourceType, IFile javaFile)
            {
                IJavaElement javaElement = JavaCore.create(javaFile);
                
                final ICompilationUnit compilationUnit = (ICompilationUnit) javaElement;
                
                CompilationUnit unit = (CompilationUnit) EclipseUtils.parse(compilationUnit, ASTParser.K_COMPILATION_UNIT);
                
                TapestryImportAnnotationContext rewriteContext = new TapestryImportAnnotationContext();
                
                findTapestryImportAnnotation(unit, rewriteContext);
                
                NormalAnnotation newNormalAnnotation = null;
                
                try
                {
                    ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
                    
                    boolean tapestryImportAnnotationInImports = isTapestryImportAnnotationInImports(compilationUnit);
                    
                    if (!tapestryImportAnnotationInImports && isSafeToImportTapestryAnnotationClass(compilationUnit))
                    {
                        ImportDeclaration importDeclaration = unit.getAST().newImportDeclaration();
                        importDeclaration.setName(unit.getAST().newName("org.apache.tapestry5.annotations.Import"));
                        
                        ListRewrite listRewrite = rewrite.getListRewrite(unit, CompilationUnit.IMPORTS_PROPERTY);
                        
                        listRewrite.insertLast(importDeclaration, null);
                        
                        tapestryImportAnnotationInImports = true;
                    }
                    
                    if (rewriteContext.annotation == null)
                    {
                        newNormalAnnotation = newAnnotation(unit.getAST(), tapestryImportAnnotationInImports);
                        
                        addImport(newNormalAnnotation, file, resourceType, rewrite);
                        
                        ListRewrite listRewrite = rewrite.getListRewrite(rewriteContext.typeNode, TypeDeclaration.MODIFIERS2_PROPERTY);
                        
                        listRewrite.insertFirst(newNormalAnnotation, null);
                    }
                    else if (rewriteContext.annotation instanceof MarkerAnnotation)
                    {
                        newNormalAnnotation = newAnnotation(unit.getAST(), tapestryImportAnnotationInImports);
                        
                        addImport(newNormalAnnotation, file, resourceType, rewrite);
                        
                        rewrite.replace(rewriteContext.annotation, newNormalAnnotation, null);
                    }
                    else if (rewriteContext.annotation instanceof NormalAnnotation)
                    {
                        newNormalAnnotation = newAnnotation(unit.getAST(), tapestryImportAnnotationInImports);
                        
                        addImport(rewriteContext.annotation, file, resourceType, rewrite);
                    }
                    
                    Document document = new Document(compilationUnit.getSource());
                    
                    rewrite.rewriteAST(document, null).apply(document);
                    
                    compilationUnit.getBuffer().setContents(document.get());
                }
                catch (Exception e)
                {
                    Activator.getDefault().logError("Error modifying compilation unit", e);
                }
            }

            private void addImport(Annotation annotation, IFile file, String resourceType, ASTRewrite rewrite)
            {
                AST ast = rewrite.getAST();
                
                ListRewrite listRewrite = rewrite.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);
                
                List<?> values = listRewrite.getOriginalList();
                
                MemberValuePair originalResources = null;
                
                for (Object object : values)
                {
                    if (object instanceof MemberValuePair)
                    {
                        MemberValuePair pair = (MemberValuePair) object;
                        
                        if (resourceType.equals(pair.getName().getIdentifier()))
                        {
                            originalResources = pair;
                            break;
                        }
                    }
                }
                
                MemberValuePair newResources = ast.newMemberValuePair();
                newResources.setName(ast.newSimpleName(resourceType));
                
                Expression expression = null;
                
                if (originalResources != null)
                {
                    if (originalResources.getValue() instanceof StringLiteral)
                    {
                        expression = ast.newArrayInitializer();
                        
                        @SuppressWarnings("unchecked")
                        List<Expression> imports = ((ArrayInitializer) expression).expressions();
                        
                        imports.add((Expression) ASTNode.copySubtree(ast, originalResources.getValue()));
                        imports.add(newFileImport(file, ast));
                    }
                    else if (originalResources.getValue() instanceof ArrayInitializer)
                    {
                        expression = (Expression) ASTNode.copySubtree(ast, originalResources.getValue());
                        
                        @SuppressWarnings("unchecked")
                        List<Expression> imports = ((ArrayInitializer) expression).expressions();
                        
                        imports.add(newFileImport(file, ast));
                    }
                }
                else
                {
                    expression = newFileImport(file, ast);
                }
                
                newResources.setValue(expression);
                
                if (originalResources != null)
                {
                    listRewrite.replace(originalResources, newResources, null);
                }
                else
                {
                    listRewrite.insertLast(newResources, null);
                }
            }

            private String getImportResourceType(IFile file)
            {
                String resourceType = TapestryUtils.isStyleSheetFile(file.getProjectRelativePath())
                                    ? "stylesheet"
                                    : TapestryUtils.isJavaScriptFile(file.getProjectRelativePath())
                                        ? "library"
                                        : null;
                return resourceType;
            }
            
            private Expression newFileImport(IFile file, AST ast)
            {
                StringLiteral literal = ast.newStringLiteral();
                
                String location;
                
                IContainer root = TapestryUtils.getRoot(file);
                
                if (TapestryUtils.isWebApp(root))
                {
                    location = "context:" + TapestryUtils.getRelativeFileName(file, root);
                }
                else
                {
                    assert tapestryContext != null;
                    
                    location = getRelativePath(tapestryContext.getPackageName(),
                                               TapestryUtils.pathToPackageName(
                                                       TapestryUtils.getRelativeFileName(file.getParent(), root), false))
                             + file.getName();
                }
                
                literal.setLiteralValue(location);
                return literal;
            }

            private String getRelativePath(String contextPackageName, String newFilePackageName)
            {
                String[] contextPackageParts = contextPackageName.split("\\.");
                String[] newFilePackageParts = newFilePackageName.split("\\.");
                
                int i = 0;
                while (i < newFilePackageParts.length
                    && i < contextPackageParts.length
                    && contextPackageParts[i].equals(newFilePackageParts[i]))
                {
                    i++;
                }
                
                if (i == newFilePackageParts.length)
                {
                    return "";
                }
                
                StringBuilder builder = new StringBuilder();
                for (int j = i; j < contextPackageParts.length; j++)
                {
                    builder.append("../");
                }
                for (int j = i; j < newFilePackageParts.length; j++)
                {
                    builder.append(newFilePackageParts[j]).append("/");
                }
                
                return builder.toString();
            }

            private boolean isSafeToImportTapestryAnnotationClass(final ICompilationUnit compilationUnit)
                    throws JavaModelException
            {
                for (IImportDeclaration importDeclaration : compilationUnit.getImports())
                {
                    if (importDeclaration.getElementName().endsWith(".Import"))
                    {
                        //  Some other class named as 'Import' present in imports
                        //  -- have to use FQN for the @Import annotation
                        return false;
                    }
                }
                return true;
            }

            private boolean isTapestryImportAnnotationInImports(final ICompilationUnit compilationUnit) throws JavaModelException
            {
                for (IImportDeclaration importDeclaration : compilationUnit.getImports())
                {
                    if ("org.apache.tapestry5.annotations.Import".equals(importDeclaration.getElementName()))
                    {
                        return true;
                    }
                }
                return false;
            }
            
            class TapestryImportAnnotationContext
            {
                private ASTNode typeNode;
                private Annotation annotation;
            }
            
            private void findTapestryImportAnnotation(CompilationUnit unit, final TapestryImportAnnotationContext annotationContext)
            {
                unit.accept(new ASTVisitor()
                {
                    @Override
                    public boolean visit(TypeDeclaration node)
                    {
                        if (annotationContext.typeNode == null)
                        {
                            annotationContext.typeNode = node;
                        }
                        return super.visit(node);
                    }
                    @Override
                    public boolean visit(MarkerAnnotation node)
                    {
                        if (TapestryUtils.isTapestryImportAnnotation(node))
                        {
                            annotationContext.annotation = node;
                        }
                        return super.visit(node);
                    }
                    @Override
                    public boolean visit(NormalAnnotation node)
                    {
                        if (TapestryUtils.isTapestryImportAnnotation(node))
                        {
                            annotationContext.annotation = node;
                        }
                        return super.visit(node);
                    }
                });
            }

            private NormalAnnotation newAnnotation(final AST ast, boolean useSimpleName)
            {
                NormalAnnotation newNormalAnnotation;
                newNormalAnnotation = ast.newNormalAnnotation();
                if (useSimpleName)
                {
                    newNormalAnnotation.setTypeName(ast.newSimpleName("Import"));
                }
                else
                {
                    newNormalAnnotation.setTypeName(ast.newQualifiedName(
                            ast.newName("org.apache.tapestry5.annotations"), ast.newSimpleName("Import")));
                }
                return newNormalAnnotation;
            }
        });
        
        dialog.open();
    }
    
    public String getFileName()
    {
        return fileName;
    }
    
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
    
    public String getFolder()
    {
        return folder;
    }
    
    public void setFolder(String folder)
    {
        this.folder = folder;
    }
}