package com.anjlab.eclipse.tapestry5.views.context;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.views.IProjectProvider;
import com.anjlab.eclipse.tapestry5.views.TreeObject;
import com.anjlab.eclipse.tapestry5.views.TreeParent;
import com.anjlab.eclipse.tapestry5.views.project.TapestryProjectOutlineContentProvider;

public class TapestryContextContentProvider implements ITreeContentProvider, IProjectProvider
{
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
                TapestryFile moduleFile = context.getInitialFile();
                
                TreeParent moduleRoot = new TreeParent(moduleFile.getName(), module);
                
                invisibleRoot.addChild(moduleRoot);
                
                TapestryProjectOutlineContentProvider.buildModuleTree(module, moduleRoot);
            }
            else
            {
                for (TapestryFile relatedFile : context.getFiles())
                {
                    invisibleRoot.addChild(new TreeObject(relatedFile.getName(), relatedFile));
                }
            }
        }
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