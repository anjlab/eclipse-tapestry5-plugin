package com.anjlab.eclipse.e4.tapestry5.handlers;

import java.util.Collection;

import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MInputPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;
import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.EclipseUtils;

@SuppressWarnings("deprecation") // Since Luna
public class EPartServiceImpl implements EPartService
{
    private IWorkbenchWindow window;
    
    public EPartServiceImpl(IWorkbenchWindow window)
    {
        this.window = window;
    }
    
    @Override
    public void activate(MPart part)
    {
        EclipseUtils.openFile(window, ((MStackElementImpl) part).getFile());
    }

    @Override
    public void activate(MPart arg0, boolean arg1)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addPartListener(IPartListener arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void bringToTop(MPart arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public MPart createPart(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MPlaceholder createSharedPart(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MPlaceholder createSharedPart(String arg0,
            boolean arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MPart findPart(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MPart getActivePart()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<MPart> getDirtyParts()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<MInputPart> getInputParts(
            String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<MPart> getParts()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void hidePart(MPart arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void hidePart(MPart arg0, boolean arg1)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isPartVisible(MPart arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removePartListener(IPartListener arg0)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void requestActivation()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean saveAll(boolean arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean savePart(MPart arg0, boolean arg1)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public MPart showPart(String arg0, PartState arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MPart showPart(MPart arg0, PartState arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void switchPerspective(MPerspective arg0)
    {
        // TODO Auto-generated method stub
        
    }
}