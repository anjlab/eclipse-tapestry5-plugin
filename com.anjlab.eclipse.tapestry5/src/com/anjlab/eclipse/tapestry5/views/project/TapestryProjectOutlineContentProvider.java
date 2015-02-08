package com.anjlab.eclipse.tapestry5.views.project;

import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewSite;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceInstrumenter;
import com.anjlab.eclipse.tapestry5.TapestrySymbol;
import com.anjlab.eclipse.tapestry5.views.TreeObject;
import com.anjlab.eclipse.tapestry5.views.TreeParent;
import com.anjlab.eclipse.tapestry5.views.TreeParent.DataObject;

public class TapestryProjectOutlineContentProvider implements ITreeContentProvider
{
    private static final String CONTRIBUTORS_NODE_LABEL = "Contributors";
    private static final String ADVISORS_NODE_LABEL = "Advisors";
    private static final String DECORATORS_NODE_LABEL = "Decorators";
    private static final String SERVICES_NODE_LABEL = "Services";
    private static final String LIBRARY_MAPPINGS_NODE_LABEL = "Library Mappings";
    private static final String JAVA_SCRIPT_STACKS_NODE_LABEL = "JavaScript Stacks";
    
    private TreeParent invisibleRoot;
    private TapestryProject project;
    private TreeParent modulesRoot;
    
    public TapestryProjectOutlineContentProvider(TapestryProject project)
    {
        this.project = project;
    }
    
    public TapestryProject getProject()
    {
        return project;
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
            
            if (module.isSourceAvailable())
            {
                TreeParent mappingsRoot = newChildNode(moduleRoot, LIBRARY_MAPPINGS_NODE_LABEL, new DataObject("LibraryMappingsNode"));
                
                for (LibraryMapping libraryMapping : module.libraryMappings())
                {
                    String pathPrefix = libraryMapping.getPathPrefix();
                    mappingsRoot.addChild(new TreeObject("".equals(pathPrefix) ? "(default)" : pathPrefix, libraryMapping));
                }
                
                TreeParent stacksRoot = newChildNode(moduleRoot, JAVA_SCRIPT_STACKS_NODE_LABEL, new DataObject("JavaScriptStacksNode"));
                
                for (JavaScriptStack javaScriptStack : module.javaScriptStacks())
                {
                    stacksRoot.addChild(new TreeObject(javaScriptStack.getName(), javaScriptStack));
                }
                
                TreeParent servicesRoot = newChildNode(moduleRoot, SERVICES_NODE_LABEL, new DataObject("ServicesNode"));
                
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
                
                TreeParent decoratorsRoot = newChildNode(moduleRoot, DECORATORS_NODE_LABEL, new DataObject("DecoratorsNode"));
                
                for (ServiceInstrumenter decorator : module.decorators())
                {
                    String id = decorator.getId();
                    
                    if (id == null)
                    {
                        id = "<No-Id>";
                    }
                    
                    decoratorsRoot.addChild(new TreeObject(id, decorator));
                }
                
                TreeParent advisorsRoot = newChildNode(moduleRoot, ADVISORS_NODE_LABEL, new DataObject("AdvisorsNode"));
                
                for (ServiceInstrumenter advisor : module.advisors())
                {
                    String id = advisor.getId();
                    
                    if (id == null)
                    {
                        id = "<No-Id>";
                    }
                    
                    advisorsRoot.addChild(new TreeObject(id, advisor));
                }
                
                TreeParent contributorsRoot = newChildNode(moduleRoot, CONTRIBUTORS_NODE_LABEL, new DataObject("ContributorsNode"));
                
                for (ServiceInstrumenter contributor : module.contributors())
                {
                    String id = contributor.getId();
                    
                    if (id == null)
                    {
                        id = "<Unknown>";
                    }
                    
                    contributorsRoot.addChild(new TreeObject(id, contributor));
                }
            }
            else
            {
                newChildNode(moduleRoot, LIBRARY_MAPPINGS_NODE_LABEL, EclipseUtils.SOURCE_NOT_FOUND);
                newChildNode(moduleRoot, JAVA_SCRIPT_STACKS_NODE_LABEL, EclipseUtils.SOURCE_NOT_FOUND);
                newChildNode(moduleRoot, SERVICES_NODE_LABEL, EclipseUtils.SOURCE_NOT_FOUND);
                newChildNode(moduleRoot, DECORATORS_NODE_LABEL, EclipseUtils.SOURCE_NOT_FOUND);
                newChildNode(moduleRoot, ADVISORS_NODE_LABEL, EclipseUtils.SOURCE_NOT_FOUND);
                newChildNode(moduleRoot, CONTRIBUTORS_NODE_LABEL, EclipseUtils.SOURCE_NOT_FOUND);
            }
        }
    }
    
    private TreeParent newChildNode(TreeParent parent, String label, Object data)
    {
        TreeParent node = new TreeParent(label, data);
        parent.addChild(node);
        return node;
    }
    
    public Object[] getElements(Object parent)
    {
        if (parent instanceof IViewSite || parent == null)
        {
            if (invisibleRoot == null)
            {
                initialize();
            }
            return getChildren(invisibleRoot);
        }
        return getChildren(parent);
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
        return new Object[0];
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