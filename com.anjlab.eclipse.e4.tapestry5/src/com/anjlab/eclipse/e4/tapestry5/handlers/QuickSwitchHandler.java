package com.anjlab.eclipse.e4.tapestry5.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.EditorReference;
import org.eclipse.ui.internal.PartPane;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
@SuppressWarnings("restriction")
public class QuickSwitchHandler extends AbstractHandler
{
    /**
     * The constructor.
     */
    public QuickSwitchHandler()
    {
    }

    /**
     * the command has been executed, so extract extract the needed information
     * from the application context.
     */
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        
        TapestryContext context = Activator.getDefault().getTapestryContext(window);
        
        if (context == null || context.isEmpty())
        {
            return null;
        }
        
        TapestryContextInformationControl informationControl =
                 new TapestryContextInformationControl(window, context);
        informationControl.setLocation(getLocation(window, informationControl.computeSizeHint()));
        //  TODO When inside module class try to map current method to service instrumenter
        informationControl.setInput(TapestryUtils.getTapestryFileFromPage(window.getActivePage()));
        informationControl.open();
        informationControl.setFocus();
        
        return null;
    }

    private Point getLocation(IWorkbenchWindow window, Point size)
    {
        Point centerPoint = null;
        
        IWorkbenchPartReference partReference = window.getActivePage().getActivePartReference();
        
        if (partReference instanceof EditorReference)
        {
            PartPane pane = ((EditorReference) partReference).getPane();
            Control control = pane.getControl();
            Rectangle partBounds = control.getBounds();
            
            centerPoint = new Point(partBounds.x, partBounds.y);
            
            alignPoint(centerPoint, control.getParent(), true, true);
            
            centerPoint.x += partBounds.width / 2;
            centerPoint.y += partBounds.height / 2;
        }
        
        if (centerPoint == null)
        {
            Monitor mon = window.getShell().getMonitor();
            
            Rectangle bounds = mon.getClientArea();
            
            Point screenCenter = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
            
            centerPoint = screenCenter;
        }
        
        centerPoint.x -= size.x / 2;
        centerPoint.y -= size.y / 2;
        
        return centerPoint;
    }

    private Composite alignPoint(Point centerPoint, Composite parent, boolean left, boolean top)
    {
        while (parent != null)
        {
            if (left)
            {
                centerPoint.x += parent.getBounds().x;
            }
            
            if (top)
            {
                centerPoint.y += parent.getBounds().y;
            }
            
            parent = parent.getParent();
        }
        return parent;
    }
}
