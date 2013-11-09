package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;

public class TapestryModule
{
    private TapestryProject project;
    private IType type;
    private ModuleReference reference;
    
    public static interface ModuleReference
    {
        String getLabel();
    }
    
    public TapestryModule(TapestryProject project, IType type, ModuleReference reference)
    {
        this.project = project;
        this.type = type;
        this.reference = reference;
    }
    
    public TapestryProject getProject()
    {
        return project;
    }
    
    public ModuleReference getReference()
    {
        return reference;
    }
    
    public String getName()
    {
        return type.getElementName();
    }
    
    public List<TapestryModule> subModules()
    {
        List<TapestryModule> subModules = new ArrayList<TapestryModule>();
        
        try
        {
            for (IAnnotation annotation : type.getAnnotations())
            {
                if (TapestryUtils.isTapestrySubModuleAnnotation(annotation))
                {
                    for (IMemberValuePair pair : annotation.getMemberValuePairs())
                    {
                        Object[] classes = (Object[]) pair.getValue();
                        for (Object className : classes)
                        {
                            IType subModule = EclipseUtils.findTypeDeclaration(
                                    type.getJavaProject().getProject(), (String) className);
                            
                            if (subModule != null)
                            {
                                subModules.add(new TapestryModule(project, subModule, new ModuleReference()
                                {
                                    @Override
                                    public String getLabel()
                                    {
                                        return "via @SubModule of " + getName();
                                    }
                                }));
                            }
                        }
                    }
                }
            }
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error getting submodules for " + getName(), e);
        }
        
        return subModules;
    }

    public List<LibraryMapping> libraryMappings() throws IllegalStateException, JavaModelException
    {
        final List<LibraryMapping> mappings = new ArrayList<LibraryMapping>();
        
        IClassFile classFile = type.getClassFile();
        
        String source = classFile != null
                      ? classFile.getSource()
                      : type.getCompilationUnit().getSource();
        
        CompilationUnit compilationUnit = EclipseUtils.parse(source);
        
        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(MethodInvocation node)
            {
                if ("add".equals(node.getName().toString())
                        || "override".equals(node.getName().toString()))
                {
                    if (node.arguments().size() == 1)
                    {
                        Object arg = node.arguments().get(0);
                        
                        if (arg instanceof ClassInstanceCreation)
                        {
                            ClassInstanceCreation creation = (ClassInstanceCreation) arg;
                            
                            Type creationType = creation.getType();
                            
                            if (creationType.isSimpleType())
                            {
                                Name name = ((SimpleType) creationType).getName();
                                
                                if (name.isSimpleName())
                                {
                                    if ("LibraryMapping".equals(((SimpleName) name).getIdentifier()))
                                    {
                                        String prefix = evalExpression(project.getProject(), creation.arguments().get(0));
                                        String pkg = "".equals(prefix)
                                                   ? TapestryUtils.getAppPackage(project.getProject())
                                                   : evalExpression(project.getProject(), creation.arguments().get(1));
                                        
                                        mappings.add(new LibraryMapping(prefix, pkg));
                                    }
                                }
                            }
                        }
                    }
                }
                return super.visit(node);
            }

            private String evalExpression(IProject project, Object expr)
            {
                if (expr instanceof StringLiteral)
                {
                    return ((StringLiteral) expr).getLiteralValue();
                }
                
                if (expr instanceof Name)
                {
                    IField field = EclipseUtils.findFieldDeclaration(project, ((Name) expr));
                    
                    if (field != null)
                    {
                        try
                        {
                            return (String) field.getConstant();
                        }
                        catch (JavaModelException e)
                        {
                            //  Ignore
                        }
                    }
                }
                
                return "<" + expr.toString() + ">";
            }
        });
        
        return mappings;
    }
}
