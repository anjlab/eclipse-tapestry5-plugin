package com.anjlab.eclipse.tapestry5.watchdog;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class TapestryContextWatchdog extends AbstractWatchdog
{
    private WindowSelectionListener windowListener;
    
    private IResourceChangeListener postBuildListener;
    
    private IResourceChangeListener postChangeListener;
    
    private final Map<IWorkbenchWindow, TapestryContext> currentContexts;
    
    public TapestryContextWatchdog()
    {
        currentContexts = new ConcurrentHashMap<IWorkbenchWindow, TapestryContext>();
    }
    
    private void notifyContextChanged(IWorkbenchWindow targetWindow, TapestryContext newContext)
    {
        for (ITapestryContextListener listener : listeners.find(ITapestryContextListener.class, targetWindow, true))
        {
            listener.contextChanged(targetWindow, newContext);
        }
    }
    
    private void notifySelectionChanged(IWorkbenchWindow targetWindow, TapestryFile selectedFile)
    {
        for (ITapestryContextListener listener : listeners.find(ITapestryContextListener.class, targetWindow, true))
        {
            listener.selectionChanged(targetWindow, selectedFile);
        }
    }
    
    private void changeContext(IWorkbenchWindow window, TapestryContext newContext)
    {
        if (newContext != currentContexts.get(window))
        {
            currentContexts.put(window, newContext);
            
            notifyContextChanged(window, newContext);
        }
    }
    
    @Override
    public void start()
    {
        super.start();
        
        postChangeListener = new IResourceChangeListener()
        {
            @Override
            public void resourceChanged(IResourceChangeEvent event)
            {
                for (IFile affectedFile : EclipseUtils.getAllAffectedResources(
                                            event.getDelta(), IFile.class))
                {
                    for (Entry<IWorkbenchWindow, TapestryContext> entry : currentContexts.entrySet())
                    {
                        IWorkbenchWindow window = entry.getKey();
                        TapestryContext context = entry.getValue();
                        
                        if (context.contains(affectedFile))
                        {
                            if (!affectedFile.exists())
                            {
                                context.remove(affectedFile);
                                
                                changeContext(window, context);
                            }
                            else
                            {
                                //  This file is already in context and keeps being in this context.
                                //  If the java file changed we should update context, because @Imports could changed.
                                //  Otherwise context should stay the same, because it may be not possible to restore context from non-java file.
                                
                                if (TapestryUtils.isJavaFile(affectedFile.getProjectRelativePath()))
                                {
                                    context = TapestryUtils.createTapestryContext(affectedFile);
                                    
                                    changeContext(window, context);
                                }
                            }
                        }
                    }
                }
            }
        };
        
        ResourcesPlugin.getWorkspace().addResourceChangeListener(postChangeListener, IResourceChangeEvent.POST_CHANGE);
        
        postBuildListener = new IResourceChangeListener()
        {
            @Override
            public void resourceChanged(IResourceChangeEvent event)
            {
                List<IProject> projects = EclipseUtils.getAllAffectedResources(event.getDelta(), IProject.class);
                
                for (IProject project : projects)
                {
                    TapestryContext.deleteMarkers(project);
                }
                
                for (TapestryContext context : currentContexts.values())
                {
                    context.validate();
                }
            }
        };
        
        ResourcesPlugin.getWorkspace().addResourceChangeListener(postBuildListener, IResourceChangeEvent.POST_BUILD);
        
        windowListener = new WindowSelectionListener(new ISelectionListener()
        {
            private final ActiveEditorTracker activeEditorTracker = new ActiveEditorTracker();
            
            @Override
            public void selectionChanged(IWorkbenchPart part, ISelection selection)
            {
                IWorkbenchWindow window = part.getSite().getWorkbenchWindow();
                
                //  https://github.com/anjlab/eclipse-tapestry5-plugin/issues/18
                if (!activeEditorTracker.editorChanged(window.getActivePage()))
                {
                    return;
                }
                
                TapestryFile selectedFile = TapestryUtils.getTapestryFileFromPage(window.getActivePage());
                
                if (selectedFile == null)
                {
                    return;
                }
                
                TapestryContext context = currentContexts.get(window);
                
                if (context == null || !context.contains(selectedFile))
                {
                    context = selectedFile.getContext();
                }
                
                //  In case if we clicked on some file and couldn't obtain tapestry context
                //  for the file (i.e. because it doesn't follow naming conventions, or this is not a tapestry file)
                //  we simply keep showing the previous context
                
                if (!context.isEmpty())
                {
                    changeContext(window, context);
                }
                
                if (selectedFile != null)
                {
                    notifySelectionChanged(window, selectedFile);
                }
            }
        })
        .addListener();
    }
    
    @Override
    public void stop()
    {
        windowListener.removeListener();
        windowListener = null;
        
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(postChangeListener);
        postChangeListener = null;
        
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(postBuildListener);
        postBuildListener = null;
        
        currentContexts.clear();;
        
        super.stop();
    }
    
    public TapestryContext getTapestryContext(IWorkbenchWindow window)
    {
        return currentContexts.get(window);
    }
}
