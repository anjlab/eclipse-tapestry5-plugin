package com.anjlab.eclipse.e4.tapestry5.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MExpression;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;

import com.anjlab.eclipse.tapestry5.TapestryContext;

public class MElementContainerImpl implements MElementContainer<MStackElementImpl>
{
    private List<MStackElementImpl> children;
    
    private Map<String, IFile> lookupCache;
    
    public MElementContainerImpl(TapestryContext context)
    {
        children = new ArrayList<MStackElementImpl>();
        lookupCache = new HashMap<String, IFile>();
        
        for (IFile file : context.getFiles())
        {
            children.add(new MStackElementImpl(file));
            lookupCache.put(file.toString(), file);
        }
    }

    @Override
    public List<MStackElementImpl> getChildren()
    {
        return Collections.unmodifiableList(children);
    }
    
    public IFile lookupFile(String str)
    {
        return lookupCache.get(str);
    }

    @Override
    public MStackElementImpl getSelectedElement()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAccessibilityPhrase()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getContainerData()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MPlaceholder getCurSharedRef()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLocalizedAccessibilityPhrase()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MElementContainer<MUIElement> getParent()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getRenderer()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MExpression getVisibleWhen()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getWidget()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isOnTop()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isToBeRendered()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isVisible()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setAccessibilityPhrase(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setContainerData(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setCurSharedRef(MPlaceholder arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setOnTop(boolean arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setParent(
            MElementContainer<MUIElement> arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setRenderer(Object arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setToBeRendered(boolean arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setVisible(boolean arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setVisibleWhen(MExpression arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setWidget(Object arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getContributorURI()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getElementId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getPersistedState()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getTags()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getTransientData()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setContributorURI(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setElementId(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSelectedElement(
            MStackElementImpl arg0)
    {
        // TODO Auto-generated method stub
        
    }
}