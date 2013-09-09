package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewSite;

import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class ViewContentProvider implements ITreeContentProvider
{
    private TreeParent invisibleRoot;
    private IViewSite viewSite;
    private IFile file;
    private TapestryContext context;

    public ViewContentProvider(IViewSite viewSite, IFile file, TapestryContext context)
    {
        this.viewSite = viewSite;
        this.file = file;
    }
    
    public ViewContentProvider(IViewSite viewSite, IFile file)
    {
        this(viewSite, file, null);
    }

    private void initialize()
    {
        invisibleRoot = new TreeParent("", new Object());
        
        if (file != null)
        {
            context = TapestryUtils.createTapestryContext(file);
            
            for (IFile relatedFile : context.getFiles())
            {
                invisibleRoot.addChild(new TreeObject(relatedFile.getName(), relatedFile));
            }
        }
    }

    public TapestryContext getContext()
    {
        return context;
    }
    
    public Object[] getElements(Object parent)
    {
        if (parent.equals(viewSite))
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
        return getElements(viewSite).length > 0;
    }
    
    public void inputChanged(Viewer v, Object oldInput, Object newInput)
    {
    }

    public void dispose()
    {
    }

}