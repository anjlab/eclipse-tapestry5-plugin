package com.anjlab.eclipse.tapestry5.views.project;

import java.util.List;

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
import com.anjlab.eclipse.tapestry5.views.TreeObject;
import com.anjlab.eclipse.tapestry5.views.TreeParent;
import com.anjlab.eclipse.tapestry5.views.TreeParent.DataObject;

public class TapestryProjectOutlineContentProvider implements ITreeContentProvider
{
    private TreeParent invisibleRoot;
    private TapestryProject project;

    public TapestryProjectOutlineContentProvider(TapestryProject project)
    {
        this.project = project;
    }
    
    public TapestryProject getProject()
    {
        return project;
    }
    
    private void initialize()
    {
        invisibleRoot = new TreeParent("", new Object());
        
        if (project != null)
        {
            TreeParent modulesRoot = new TreeParent("Modules", new DataObject("ModulesRoot"));
            
            invisibleRoot.addChild(modulesRoot);
            
            for (TapestryModule module : project.modules())
            {
                TreeParent moduleRoot = new TreeParent(module.getName(), module);
                
                modulesRoot.addChild(moduleRoot);
                
                if (module.isSourceAvailable())
                {
                    List<LibraryMapping> libraryMappings = module.libraryMappings();
                    
                    TreeParent mappingsRoot = newLibraryMappingsNode(moduleRoot, new DataObject("LibraryMappingsNode"));
                    
                    for (LibraryMapping libraryMapping : libraryMappings)
                    {
                        String pathPrefix = libraryMapping.getPathPrefix();
                        mappingsRoot.addChild(new TreeObject("".equals(pathPrefix) ? "(default)" : pathPrefix, libraryMapping));
                    }
                    
                    List<JavaScriptStack> stacks = module.javaScriptStacks();
                    
                    TreeParent stacksRoot = newJavaScriptStacksNode(moduleRoot, new DataObject("JavaScriptStacksNode"));
                    
                    for (JavaScriptStack javaScriptStack : stacks)
                    {
                        stacksRoot.addChild(new TreeObject(javaScriptStack.getName(), javaScriptStack));
                    }
                    
                    List<TapestryService> services = module.services();
                    
                    TreeParent servicesRoot = newServicesNode(moduleRoot, new DataObject("ServicesNode"));
                    
                    for (TapestryService service : services)
                    {
                        ServiceDefinition definition = service.getDefinition();
                        
                        String serviceId = definition.getId();
                        
                        if (serviceId == null)
                        {
                            serviceId = "<Unknown>";
                        }
                        
                        servicesRoot.addChild(new TreeObject(serviceId, service));
                    }
                }
                else
                {
                    newLibraryMappingsNode(moduleRoot, EclipseUtils.SOURCE_NOT_FOUND);
                    newJavaScriptStacksNode(moduleRoot, EclipseUtils.SOURCE_NOT_FOUND);
                    newServicesNode(moduleRoot, EclipseUtils.SOURCE_NOT_FOUND);
                }
            }
        }
    }
    
    private TreeParent newJavaScriptStacksNode(TreeParent parent, Object data)
    {
        TreeParent node = new TreeParent("JavaScript Stacks", data);
        parent.addChild(node);
        return node;
    }
    
    private TreeParent newLibraryMappingsNode(TreeParent parent, Object data)
    {
        TreeParent node = new TreeParent("Library Mappings", data);
        parent.addChild(node);
        return node;
    }
    
    private TreeParent newServicesNode(TreeParent parent, Object data)
    {
        TreeParent node = new TreeParent("Services", data);
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