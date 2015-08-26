package com.anjlab.eclipse.tapestry5.views.context;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.ClassNameReference;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceInstrumenter;
import com.anjlab.eclipse.tapestry5.internal.ServiceImplMatcher;
import com.anjlab.eclipse.tapestry5.internal.ServiceIntfMatcher;
import com.anjlab.eclipse.tapestry5.views.IProjectProvider;
import com.anjlab.eclipse.tapestry5.views.TreeObject;
import com.anjlab.eclipse.tapestry5.views.TreeParent;
import com.anjlab.eclipse.tapestry5.views.TreeParent.DataObject;
import com.anjlab.eclipse.tapestry5.views.project.TapestryProjectOutlineContentProvider;

public class TapestryContextContentProvider implements ITreeContentProvider, IProjectProvider
{
    private static final Object[] NO_CHILDREN = new Object[0];

    private final IWorkbenchWindow window;
    private final TapestryContext context;

    private TreeParent invisibleRoot;

    public TapestryContextContentProvider(IWorkbenchWindow window, TapestryContext context)
    {
        this.window = window;
        this.context = context;
    }

    @Override
    public IProject getProject()
    {
        return context == null ? null : context.getProject();
    }
    
    private void initialize()
    {
        invisibleRoot = new TreeParent("", new Object());
        
        if (context != null && !context.isEmpty())
        {
            TapestryModule module = getTapestryModule(context);
            
            if (module != null)
            {
                TreeParent moduleRoot = new TreeParent(module.getName(), module);
                
                TapestryProjectOutlineContentProvider.buildModuleTree(module, moduleRoot);
                
                invisibleRoot.addChild(moduleRoot);
                
                return;
            }
            
            TapestryService serviceImpl = getTapestryServiceImpl(context);
            
            if (serviceImpl != null)
            {
                TreeParent serviceImplRoot = new TreeParent(
                        context.getInitialFile().getName(),
                        context.getInitialFile());
                
                buildServiceImplTree(serviceImpl, serviceImplRoot);
                
                invisibleRoot.addChild(serviceImplRoot);
                
                return;
            }
            
            TapestryService serviceIntf = getTapestryServiceIntf(context);
            
            if (serviceIntf != null)
            {
                TreeParent serviceIntfRoot = new TreeParent(
                        context.getInitialFile().getName(),
                        context.getInitialFile());
                
                buildServiceIntfTree(serviceIntf, serviceIntfRoot);
                
                invisibleRoot.addChild(serviceIntfRoot);
                
                return;
            }
            
            for (TapestryFile relatedFile : context.getFiles())
            {
                invisibleRoot.addChild(new TreeObject(relatedFile.getName(), relatedFile));
            }
        }
    }

    private void buildServiceImplTree(TapestryService service, TreeParent serviceRoot)
    {
        TapestryProject project = Activator.getDefault().getTapestryProject(window);
        
        if (project == null)
        {
            return;
        }
        
        //  TODO Data objects should only be equals for exactly same item across all tree views
        //  i.e. data for same-named TreeParents for different nodes must be different
        //  otherwise Eclipse will reuse those items
        TreeParent otherImplsRoot = new TreeParent("Other Implementations", new DataObject("Other Implementations"));
        
        ServiceIntfMatcher intfMatcher = new ServiceIntfMatcher(service.getDefinition().getIntfClass());
        
        for (TapestryModule module : project.modules())
        {
            for (TapestryService other : module.services())
            {
                if (service != other && intfMatcher.matches(other))
                {
                    otherImplsRoot.addChild(
                            new TreeObject(
                                    other.getDefinition().getId(),
                                    new ClassNameReference(
                                            getProject(),
                                            other.getDefinition().getImplClass())));
                }
            }
        }
        
        addIfHasChildren(serviceRoot, otherImplsRoot);
        
        TreeParent specificationRoot = buildSpecificationTree(service, serviceRoot);
        
        TapestryService serviceIntf = project.findService(
                new ServiceIntfMatcher(service.getDefinition().getIntfClass()));
        
        if (serviceIntf != null)
        {
            addInstrumenters(serviceRoot, project, serviceIntf);
        }
        else
        {
            addNotInstrumentingNotice(specificationRoot);
        }
    }

    private TreeParent buildSpecificationTree(TapestryService service, TreeParent serviceRoot)
    {
        ServiceDefinition definition = service.getDefinition();
        
        TreeParent specificationRoot = new TreeParent("Specification", new DataObject("Specification"));
        serviceRoot.addChild(specificationRoot);
        
        specificationRoot.addChild(new TreeObject("Defined in", service.getReference()));
        specificationRoot.addChild(new TreeObject("Id", ObjectUtils.defaultIfNull(definition.getId(), "<?>")));
        specificationRoot.addChild(new TreeObject("Intf",
                definition.getIntfClass() != null
                ? new ClassNameReference(getProject(), definition.getIntfClass())
                : "<?>"));
        specificationRoot.addChild(new TreeObject("Scope", ObjectUtils.defaultIfNull(definition.getScope(), "Singleton")));
        
        TreeParent markersRoot = new TreeParent("Markers", new DataObject("Markers"));
        
        for (String marker : definition.getMarkers())
        {
            markersRoot.addChild(new TreeObject(marker, new DataObject(marker)));
        }
        
        addIfHasChildren(specificationRoot, markersRoot);
        
        return specificationRoot;
    }

    private void addNotInstrumentingNotice(TreeParent specificationRoot)
    {
        String notInstumentingNotice = "Instrumentation & scope won't be applied";
        specificationRoot.addChild(
                new TreeObject(
                        "*",
                        notInstumentingNotice));
    }

    private void addInstrumenters(TreeParent serviceRoot, TapestryProject project,
            TapestryService serviceIntf)
    {
        TreeParent advisorsRoot = new TreeParent("Advisors", new DataObject("Advisors"));
        
        for (TapestryModule module : project.modules())
        {
            for (ServiceInstrumenter advisor : module.advisors())
            {
                if (advisor.getServiceMatcher().matches(serviceIntf))
                {
                    advisorsRoot.addChild(new TreeObject(StringUtils.defaultIfEmpty(advisor.getId(), "<?>"), advisor));
                }
            }
        }
        
        addIfHasChildren(serviceRoot, advisorsRoot);
        
        TreeParent decoratorsRoot = new TreeParent("Decorators", new DataObject("Decorators"));
        
        for (TapestryModule module : project.modules())
        {
            for (ServiceInstrumenter decorator : module.decorators())
            {
                if (decorator.getServiceMatcher().matches(serviceIntf))
                {
                    decoratorsRoot.addChild(new TreeObject(StringUtils.defaultIfEmpty(decorator.getId(), "<?>"), decorator));
                }
            }
        }
        
        addIfHasChildren(serviceRoot, decoratorsRoot);
        
        TreeParent contributorsRoot = new TreeParent("Contributors", new DataObject("Contributors"));
        
        for (TapestryModule module : project.modules())
        {
            for (ServiceInstrumenter contributor : module.contributors())
            {
                if (contributor.getServiceMatcher().matches(serviceIntf))
                {
                    contributorsRoot.addChild(new TreeObject(StringUtils.defaultIfEmpty(contributor.getId(), "<?>"), contributor));
                }
            }
        }
        
        addIfHasChildren(serviceRoot, contributorsRoot);
    }

    private void buildServiceIntfTree(TapestryService service, TreeParent serviceRoot)
    {
        TapestryProject project = Activator.getDefault().getTapestryProject(window);
        
        if (project == null)
        {
            return;
        }
        
        buildSpecificationTree(service, serviceRoot);
        
        TreeParent otherImplsRoot = new TreeParent("Implementations", new DataObject("Implementations"));
        
        ServiceIntfMatcher intfMatcher = new ServiceIntfMatcher(service.getDefinition().getIntfClass());
        
        for (TapestryModule module : project.modules())
        {
            for (TapestryService other : module.services())
            {
                if (intfMatcher.matches(other))
                {
                    otherImplsRoot.addChild(
                            new TreeObject(
                                    other.getDefinition().getId(),
                                    new ClassNameReference(
                                            getProject(),
                                            other.getDefinition().getImplClass())));
                }
            }
        }
        
        addIfHasChildren(serviceRoot, otherImplsRoot);
        
        addInstrumenters(serviceRoot, project, service);
    }
    
    private TapestryService getTapestryServiceImpl(TapestryContext context)
    {
        TapestryProject project = Activator.getDefault().getTapestryProject(window);
        
        if (project == null)
        {
            return null;
        }
        
        TapestryFile file = context.getInitialFile();
        
        if (!file.isJavaFile())
        {
            return null;
        }
        
        String className = file.getClassName();
        
        if (StringUtils.isEmpty(className))
        {
            return null;
        }
        
        return project.findService(new ServiceImplMatcher(className));
    }

    private TapestryService getTapestryServiceIntf(TapestryContext context)
    {
        TapestryProject project = Activator.getDefault().getTapestryProject(window);
        
        if (project == null)
        {
            return null;
        }
        
        TapestryFile file = context.getInitialFile();
        
        if (!file.isJavaFile())
        {
            return null;
        }
        
        String className = file.getClassName();
        
        if (StringUtils.isEmpty(className))
        {
            return null;
        }
        
        return project.findService(new ServiceIntfMatcher(className));
    }

    /**
     * 
     * @param context
     * @return
     *      if this context represents tapestry module then that module will be returned,
     *      otherwise null.
     */
    private TapestryModule getTapestryModule(TapestryContext context)
    {
        TapestryProject project = Activator.getDefault().getTapestryProject(window);
        
        if (project == null)
        {
            return null;
        }
        
        for (TapestryModule module : project.modules())
        {
            if (module.getModuleFile().equals(context.getInitialFile()))
            {
                return module;
            }
        }
        
        return null;
    }

    public TapestryContext getContext()
    {
        return context;
    }
    
    public Object[] getElements(Object parent)
    {
        if (parent instanceof TapestryContextView
                || parent instanceof IInformationControl)
        {
            if (invisibleRoot == null)
            {
                initialize();
            }
            return getChildren(invisibleRoot);
        }
        return NO_CHILDREN;
    }

    public Object getParent(Object child)
    {
        if (child instanceof TreeObject)
        {
            return ((TreeObject) child).getParent();
        }
        return null;
    }

    public Object[] getChildren(Object parent)
    {
        if (parent instanceof TreeParent)
        {
            return ((TreeParent) parent).getChildren();
        }
        return NO_CHILDREN;
    }

    public boolean hasChildren(Object parent)
    {
        if (parent instanceof TreeParent)
        {
            return ((TreeParent) parent).hasChildren();
        }
        return false;
    }
    
    public boolean hasElements()
    {
        return getElements(null).length > 0;
    }
    
    public void inputChanged(Viewer v, Object oldInput, Object newInput)
    {
    }

    public void dispose()
    {
    }

    private static void addIfHasChildren(TreeParent root, TreeParent child)
    {
        if (child.hasChildren())
        {
            root.addChild(child);
        }
    }
}