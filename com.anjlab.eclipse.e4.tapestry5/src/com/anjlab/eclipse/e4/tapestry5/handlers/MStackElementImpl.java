package com.anjlab.eclipse.e4.tapestry5.handlers;

import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.commands.MBindingContext;
import org.eclipse.e4.ui.model.application.commands.MHandler;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MExpression;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;

import com.anjlab.eclipse.tapestry5.TapestryFile;

public class MStackElementImpl implements MStackElement, MPart
{
    private TapestryFile file;
    
    public MStackElementImpl(TapestryFile file)
    {
        this.file = file;
    }
    
    public TapestryFile getFile()
    {
        return file;
    }
    
    @Override
    public String getIconURI()
    {
        return file.toString();
    }

    @Override
    public String getLocalizedLabel()
    {
        return file.getName();
    }

    @Override
    public String getLabel()
    {
        return file.getName();
    }

    @Override
    public boolean isDirty()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setDirty(boolean arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getLocalizedTooltip()
    {
        return null;
    }

    @Override
    public String getTooltip()
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
        return true;
    }

    @Override
    public boolean isVisible()
    {
        // TODO Auto-generated method stub
        return true;
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
    public void setParent(MElementContainer<MUIElement> arg0)
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
    public String getContributionURI()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getObject()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setContributionURI(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setObject(Object arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public IEclipseContext getContext()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getProperties()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getVariables()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setContext(IEclipseContext arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setIconURI(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLabel(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setTooltip(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<MHandler> getHandlers()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MBindingContext> getBindingContexts()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLocalizedDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MMenu> getMenus()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MToolBar getToolbar()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCloseable()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCloseable(boolean arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDescription(String arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setToolbar(MToolBar arg0)
    {
        // TODO Auto-generated method stub
        
    }

//    @Override -- Since Luna
    public void updateLocalization()
    {
        // TODO Auto-generated method stub
        
    }

}
