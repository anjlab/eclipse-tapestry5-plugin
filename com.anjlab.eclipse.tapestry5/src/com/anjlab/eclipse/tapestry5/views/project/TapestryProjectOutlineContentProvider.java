package com.anjlab.eclipse.tapestry5.views.project;

import java.util.List;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewSite;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.views.TreeObject;
import com.anjlab.eclipse.tapestry5.views.TreeParent;

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
            TreeParent modulesRoot = new TreeParent("Modules", new Object());
            
            invisibleRoot.addChild(modulesRoot);
            
            for (TapestryModule module : project.modules())
            {
                TreeParent moduleRoot = new TreeParent(module.getName(), module);
                
                modulesRoot.addChild(moduleRoot);
                
                if (module.isSourceAvailable())
                {
                    try
                    {
                        List<LibraryMapping> libraryMappings = module.libraryMappings();
                        
                        TreeParent mappingsRoot = newLibraryMappingNode(moduleRoot, new Object());
                        
                        for (LibraryMapping libraryMapping : libraryMappings)
                        {
                            String pathPrefix = libraryMapping.getPathPrefix();
                            mappingsRoot.addChild(new TreeObject("".equals(pathPrefix) ? "(default)" : pathPrefix, libraryMapping));
                        }
                    }
                    catch (JavaModelException e)
                    {
                        newLibraryMappingNode(moduleRoot, e);
                    }
                }
                else
                {
                    newLibraryMappingNode(moduleRoot, EclipseUtils.SOURCE_NOT_FOUND);
                }
            }
        }
    }

    private TreeParent newLibraryMappingNode(TreeParent parent, Object data)
    {
        TreeParent node = new TreeParent("Library Mappings", data);
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