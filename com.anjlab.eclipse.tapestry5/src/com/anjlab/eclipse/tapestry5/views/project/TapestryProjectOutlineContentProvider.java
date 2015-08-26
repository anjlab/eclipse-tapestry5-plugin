package com.anjlab.eclipse.tapestry5.views.project;

import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceInstrumenter;
import com.anjlab.eclipse.tapestry5.TapestrySymbol;
import com.anjlab.eclipse.tapestry5.views.IProjectProvider;
import com.anjlab.eclipse.tapestry5.views.TreeObject;
import com.anjlab.eclipse.tapestry5.views.TreeParent;
import com.anjlab.eclipse.tapestry5.views.TreeParent.DataObject;

public class TapestryProjectOutlineContentProvider implements ITreeContentProvider, IProjectProvider
{
    private static final Object[] NO_CHILDREN = new Object[0];

    private static final String CONTRIBUTORS_NODE_LABEL = "Contributors";
    private static final String ADVISORS_NODE_LABEL = "Advisors";
    private static final String DECORATORS_NODE_LABEL = "Decorators";
    private static final String SERVICES_NODE_LABEL = "Services";
    private static final String LIBRARY_MAPPINGS_NODE_LABEL = "Library Mappings";
    private static final String JAVA_SCRIPT_STACKS_NODE_LABEL = "JavaScript Stacks";
    private static final String IMPORTED_MODULES_NODE_LABEL = "Imported Modules";
    
    private TreeParent invisibleRoot;
    private TapestryProject project;
    private TreeParent modulesRoot;
    
    public TapestryProjectOutlineContentProvider(TapestryProject project)
    {
        this.project = project;
    }
    
    @Override
    public IProject getProject()
    {
        return project == null ? null : project.getProject();
    }
    
    public TreeParent getModulesRoot()
    {
        return modulesRoot;
    }
    
    private void initialize()
    {
        invisibleRoot = new TreeParent("", new Object());
        
        if (project != null)
        {
            addModules();
            addSymbols();
        }
    }

    private void addSymbols()
    {
        TreeParent symbolsRoot = new TreeParent("Symbols", new DataObject("SymbolsRoot"));
        
        invisibleRoot.addChild(symbolsRoot);
        
        for (Entry<String, List<TapestrySymbol>> symbolValues : project.symbols().entrySet())
        {
            for (TapestrySymbol symbol : symbolValues.getValue())
            {
                symbolsRoot.addChild(new TreeObject(symbol.getName(), symbol));
            }
        }
    }

    private void addModules()
    {
        this.modulesRoot = new TreeParent("Modules", new DataObject("ModulesRoot"));
        
        invisibleRoot.addChild(modulesRoot);
        
        for (TapestryModule module : project.modules())
        {
            TreeParent moduleRoot = new TreeParent(module.getName(), module);
            
            modulesRoot.addChild(moduleRoot);
            
            buildModuleTree(module, moduleRoot);
        }
    }

    public static void buildModuleTree(TapestryModule module, TreeParent moduleRoot)
    {
        if (module.isSourceAvailable())
        {
            TreeParent mappingsRoot = new TreeParent(LIBRARY_MAPPINGS_NODE_LABEL, new DataObject("LibraryMappingsNode"));
            
            for (LibraryMapping libraryMapping : module.libraryMappings())
            {
                String pathPrefix = libraryMapping.getPathPrefix();
                mappingsRoot.addChild(new TreeObject("".equals(pathPrefix) ? "(default)" : pathPrefix, libraryMapping));
            }
            
            addIfHasChildren(moduleRoot, mappingsRoot);
            
            TreeParent stacksRoot = new TreeParent(JAVA_SCRIPT_STACKS_NODE_LABEL, new DataObject("JavaScriptStacksNode"));
            
            for (JavaScriptStack javaScriptStack : module.javaScriptStacks())
            {
                stacksRoot.addChild(new TreeObject(javaScriptStack.getName(), javaScriptStack));
            }
            
            addIfHasChildren(moduleRoot, stacksRoot);
            
            TreeParent servicesRoot = new TreeParent(SERVICES_NODE_LABEL, new DataObject("ServicesNode"));
            
            for (TapestryService service : module.services())
            {
                ServiceDefinition definition = service.getDefinition();
                
                String serviceId = definition.getId();
                
                if (serviceId == null)
                {
                    serviceId = "<Unknown>";
                }
                
                servicesRoot.addChild(new TreeObject(serviceId, service));
            }
            
            addIfHasChildren(moduleRoot, servicesRoot);
            
            TreeParent decoratorsRoot = new TreeParent(DECORATORS_NODE_LABEL, new DataObject("DecoratorsNode"));
            
            for (ServiceInstrumenter decorator : module.decorators())
            {
                String id = decorator.getId();
                
                if (id == null)
                {
                    id = "<No-Id>";
                }
                
                decoratorsRoot.addChild(new TreeObject(id, decorator));
            }
            
            addIfHasChildren(moduleRoot, decoratorsRoot);
            
            TreeParent advisorsRoot = new TreeParent(ADVISORS_NODE_LABEL, new DataObject("AdvisorsNode"));
            
            for (ServiceInstrumenter advisor : module.advisors())
            {
                String id = advisor.getId();
                
                if (id == null)
                {
                    id = "<No-Id>";
                }
                
                advisorsRoot.addChild(new TreeObject(id, advisor));
            }
            
            addIfHasChildren(moduleRoot, advisorsRoot);
            
            TreeParent contributorsRoot = new TreeParent(CONTRIBUTORS_NODE_LABEL, new DataObject("ContributorsNode"));
            
            for (ServiceInstrumenter contributor : module.contributors())
            {
                String id = contributor.getId();
                
                if (id == null)
                {
                    id = "<Unknown>";
                }
                
                contributorsRoot.addChild(new TreeObject(id, contributor));
            }
            
            addIfHasChildren(moduleRoot, contributorsRoot);
            
            TreeParent importedModulesRoot = new TreeParent(IMPORTED_MODULES_NODE_LABEL, new DataObject("ImportedModulesNode"));
            
            for (TapestryModule subModule : module.subModules())
            {
                importedModulesRoot.addChild(new TreeObject(subModule.getName(), subModule));
            }
            
            addIfHasChildren(moduleRoot, importedModulesRoot);
        }
        else
        {
            moduleRoot.addChild(new TreeParent("Source not available", EclipseUtils.SOURCE_NOT_FOUND));
        }
    }

    private static void addIfHasChildren(TreeParent root, TreeParent child)
    {
        if (child.hasChildren())
        {
            root.addChild(child);
        }
    }
    
    public Object[] getElements(Object parent)
    {
        if (parent instanceof TapestryProjectOutlineView
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
}