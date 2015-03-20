package com.anjlab.eclipse.e4.tapestry5.handlers;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.eclipse.e4.ui.internal.workbench.renderers.swt.BasicPartList;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.IResourceUtilities;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer;
import org.eclipse.e4.ui.workbench.swt.util.ISWTResourceUtilities;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.EditorReference;
import org.eclipse.ui.internal.PartPane;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.views.ViewLabelProvider;

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
        
        IWorkbenchPartReference partReference = window.getActivePage().getActivePartReference();
        
        if (!(partReference instanceof EditorReference))
        {
            //  Make in only work in editors
            return null;
        }
        
        TapestryContext context = Activator.getDefault().getTapestryContext(window);
        
        if (context == null || context.isEmpty())
        {
            return null;
        }
        
        final BasicPartList editorList = createEditorList(context, window);
        
        if (editorList == null)
        {
            Activator.getDefault().logError("This feature is not available for your Eclipse runtime");
            return null;
        }
        
        editorList.setInput();
        
        Point size = editorList.computeSizeHint();
        editorList.setSize(size.x, size.y);
        
        Point centerPoint = getLocation(window, size);
        
        editorList.setLocation(centerPoint);

        editorList.setVisible(true);
        editorList.setFocus();
        editorList.getShell().addListener(SWT.Deactivate, new Listener() {
            public void handleEvent(org.eclipse.swt.widgets.Event event) {
                EclipseUtils.asyncExec(editorList.getShell(), new Runnable() {
                    public void run() {
                        editorList.dispose();
                    }
                });
            }
        });
        
        return null;
    }

    private BasicPartList createEditorList(TapestryContext context, IWorkbenchWindow window)
    {
        final MElementContainerImpl mElementContainerImpl = new MElementContainerImpl(context);
        
        final ViewLabelProvider labelProvider = new ViewLabelProvider();
        
        StackRenderer stackRenderer = new StackRenderer()
        {
            public CTabItem findItemForPart(MPart part)
            {
                return null;
            }
        };
        
        ISWTResourceUtilities resUtils = new ISWTResourceUtilities()
        {
            @Override
            public ImageDescriptor imageDescriptorFromURI(URI uri)
            {
                TapestryFile file = mElementContainerImpl.lookupFile(uri.toString());
                
                if (file == null)
                {
                    return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(uri.toString());
                }
                
                return labelProvider.getImageDescriptor(file);
            }
            
            // @Override -- Since Luna
            public Image adornImage(Image arg0, Image arg1)
            {
                // TODO Auto-generated method stub
                return null;
            }
        };
        
        List<Object> initArgs = new ArrayList<Object>(
                Arrays.<Object>asList(
                        window.getShell(),
                        SWT.ON_TOP, SWT.V_SCROLL | SWT.H_SCROLL,
                        new EPartServiceImpl(window),
                        mElementContainerImpl,
                        stackRenderer,
                        resUtils,
                        false));
        
        for (Constructor<?> constructor : BasicPartList.class.getConstructors())
        {
            List<Class<?>> paramTypes = new ArrayList<Class<?>>(
                    Arrays.<Class<?>>asList(
                        Shell.class,
                        int.class,
                        int.class,
                        EPartService.class,
                        MElementContainer.class,
                        StackRenderer.class,
                        ISWTResourceUtilities.class,
                        boolean.class));
            
            if (Arrays.equals(constructor.getParameterTypes(), paramTypes.toArray()))
            {
                return newInstance(initArgs, constructor);
            }
            
            int stackRendererIndex = paramTypes.indexOf(StackRenderer.class);
            
            Class<?> stackRendererType = paramTypes.remove(stackRendererIndex);
            initArgs.remove(stackRendererIndex);
            
            if (Arrays.equals(constructor.getParameterTypes(), paramTypes.toArray()))
            {
                return newInstance(initArgs, constructor);
            }
            
            //  Eclipse Luna 4.4.2
            
            paramTypes.add(stackRendererIndex, stackRendererType);
            initArgs.add(stackRendererIndex, stackRenderer);
            
            EclipseContext eclipseContext = new EclipseContext(null);
            eclipseContext.set(IResourceUtilities.class.getName(), resUtils);
            stackRenderer.init(eclipseContext);
            
            int resourceUtilitiesIndex = paramTypes.indexOf(ISWTResourceUtilities.class);
            
            paramTypes.remove(resourceUtilitiesIndex);
            initArgs.remove(resourceUtilitiesIndex);
            
            if (Arrays.equals(constructor.getParameterTypes(), paramTypes.toArray()))
            {
                return newInstance(initArgs, constructor);
            }
        }
        
        return null;
    }

    private BasicPartList newInstance(List<Object> initArgs, Constructor<?> constructor)
    {
        try
        {
            return (BasicPartList) constructor.newInstance(initArgs.toArray());
        }
        catch (Exception e)
        {
            return null;
        }
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
