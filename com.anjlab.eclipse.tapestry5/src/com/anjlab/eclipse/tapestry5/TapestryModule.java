package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
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
import org.eclipse.jdt.core.dom.Type;

public abstract class TapestryModule
{
    private TapestryProject project;
    private IType moduleClass;
    private ModuleReference reference;
    
    private boolean sourceAvailable;
    private boolean appModule;
    private boolean tapestryCoreModule;
    
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
        
        findComponents(monitor);
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
                        Object[] classes = pair.getValue().getClass().isArray()
                                         ? (Object[]) pair.getValue()
                                         : new Object[] { pair.getValue() };
                        
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

    public List<LibraryMapping> libraryMappings()
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
                                        String prefix = EclipseUtils.evalExpression(project.getProject(), creation.arguments().get(0));
                                        String pkg = "".equals(prefix)
                                                   ? TapestryUtils.getAppPackage(project.getProject())
                                                   : EclipseUtils.evalExpression(project.getProject(), creation.arguments().get(1));
                                        
                                        libraryMappings.add(new LibraryMapping(prefix, pkg));
                                    }
                                }
                            }
                        }
                    }
                }
                return super.visit(node);
            }
        });
    }

    private List<TapestryContext> components;
    
    public List<TapestryContext> getComponents()
    {
        if (components == null)
        {
            findComponents(new NullProgressMonitor());
        }
        return components;
    }
    
    public interface ObjectCallback<T>
    {
        void callback(T obj);
    }
    
    private synchronized void findComponents(IProgressMonitor monitor)
    {
        if (components != null)
        {
            return;
        }
        
        components = new ArrayList<TapestryContext>();
        
        ObjectCallback<Object> componentClassFound = new ObjectCallback<Object>()
        {
            @Override
            public void callback(Object obj)
            {
                TapestryContext componentContext = null;
                
                if (obj instanceof IFile)
                {
                    componentContext = TapestryUtils.createTapestryContext((IFile) obj);
                }
                else if (obj instanceof IClassFile)
                {
                    IClassFile classFile = (IClassFile) obj;
                    
                    //  Ignore inner classes
                    if (!classFile.getElementName().contains("$"))
                    {
                        componentContext = TapestryUtils.createTapestryContext(classFile);
                    }
                }
                
                if (componentContext != null)
                {
                    components.add(componentContext);
                }
            }
        };
        
        for (LibraryMapping mapping : libraryMappings())
        {
            String componentsPackage = mapping.getRootPackage() + ".components";
            
            if (mapping.getPathPrefix().isEmpty() && isTapestryCoreModule())
            {
                //  This package is from the AppModule
                for (TapestryModule module : getProject().modules())
                {
                    if (module.isAppModule())
                    {
                        module.enumJavaClassesRecursively(componentsPackage, componentClassFound);
                        break;
                    }
                }
            }
            else
            {
                enumJavaClassesRecursively(componentsPackage, componentClassFound);
            }
        }
    }

    protected abstract void enumJavaClassesRecursively(String rootPackage, ObjectCallback<Object> callback);

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

    public void setTapestryCoreModule(boolean tapestryCoreModule)
    {
        this.tapestryCoreModule = tapestryCoreModule;
    }
    
    public boolean isTapestryCoreModule()
    {
        return tapestryCoreModule;
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

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof TapestryModule))
        {
            return false;
        }
        return this.moduleClass.equals(((TapestryModule) obj).moduleClass);
    }
}
