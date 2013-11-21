package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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

public abstract class TapestryModule
{
    private TapestryProject project;
    private IType moduleClass;
    private ModuleReference reference;
    
    private boolean sourceAvailable;
    private boolean appModule;
    
    public static interface ModuleReference
    {
        String getLabel();
    }
    
    public TapestryModule(TapestryProject project, IType moduleClass, ModuleReference reference)
    {
        this.project = project;
        this.moduleClass = moduleClass;
        this.reference = reference;
    }
    
    public static TapestryModule createTapestryModule(TapestryProject project, IType moduleClass, ModuleReference reference)
    {
        if (moduleClass.getResource() != null)
        {
            return new LocalTapestryModule(project, moduleClass, reference);
        }
        return new JarTapestryModule(project, moduleClass, reference);
    }
    
    public TapestryProject getProject()
    {
        return project;
    }
    
    public IProject getEclipseProject()
    {
        return moduleClass.getJavaProject().getProject();
    }
    
    public ModuleReference getReference()
    {
        return reference;
    }
    
    public IType getModuleClass()
    {
        return moduleClass;
    }
    
    public String getName()
    {
        return moduleClass.getElementName();
    }
    
    public void initialize(IProgressMonitor monitor)
    {
        monitor.subTask("Analyzing " + moduleClass.getFullyQualifiedName() + "...");
        
        findSubModules(monitor);
        
        findLibraryMappings(monitor);
    }
    
    private List<TapestryModule> subModules;
    
    public List<TapestryModule> subModules()
    {
        if (subModules == null)
        {
            findSubModules(new NullProgressMonitor());
        }
        
        return subModules;
    }

    private synchronized void findSubModules(IProgressMonitor monitor)
    {
        if (subModules != null)
        {
            return;
        }
        
        subModules = new ArrayList<TapestryModule>();
        
        try
        {
            for (IAnnotation annotation : moduleClass.getAnnotations())
            {
                if (TapestryUtils.isTapestrySubModuleAnnotation(annotation))
                {
                    for (IMemberValuePair pair : annotation.getMemberValuePairs())
                    {
                        Object[] classes = (Object[]) pair.getValue();
                        for (Object className : classes)
                        {
                            IType subModuleClass = EclipseUtils.findTypeDeclaration(
                                    moduleClass.getJavaProject().getProject(), (String) className);
                            
                            if (subModuleClass != null)
                            {
                                subModules.add(createTapestryModule(project, subModuleClass, new ModuleReference()
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
    }
    
    private List<LibraryMapping> libraryMappings;

    public List<LibraryMapping> libraryMappings() throws JavaModelException
    {
        if (libraryMappings == null)
        {
            findLibraryMappings(new NullProgressMonitor());
        }
        return libraryMappings;
    }

    private synchronized void findLibraryMappings(IProgressMonitor monitor)
    {
        if (libraryMappings != null)
        {
            return;
        }
        
        libraryMappings = new ArrayList<LibraryMapping>();
        
        IClassFile classFile = moduleClass.getClassFile();
        
        String source = null;
        
        try
        {
            source = classFile != null
                          ? classFile.getSource()
                          : moduleClass.getCompilationUnit().getSource();
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error getting source file", e);
        }
        
        this.sourceAvailable = source != null;
        
        if (!sourceAvailable)
        {
            return;
        }
        
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
                                        
                                        libraryMappings.add(new LibraryMapping(prefix, pkg));
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
    }

    public abstract TapestryFile getModuleFile();

    public abstract boolean isReadOnly();
    
    public boolean isSourceAvailable()
    {
        return sourceAvailable;
    }

    public void setAppModule(boolean appModule)
    {
        this.appModule = appModule;
    }
    
    public boolean isAppModule()
    {
        return appModule;
    }

    public List<LibraryMapping> libraryMappings(String libraryPrefix) throws JavaModelException
    {
        List<LibraryMapping> result = new ArrayList<LibraryMapping>();
        for (LibraryMapping mapping : libraryMappings())
        {
            if (mapping.getPathPrefix().equals(libraryPrefix))
            {
                result.add(mapping);
            }
        }
        return result;
    }

    public abstract TapestryFile findJavaFileCaseInsensitive(String path);

}
