package com.anjlab.eclipse.tapestry5.watchdog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.ITapestryContextListener;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class TapestryProjectWatchdog extends AbstractTapestryWatchdog
{
    private WindowSelectionListener windowListener;
    
    private final Map<IWorkbenchWindow, TapestryProject> currentProjects;
    
    public TapestryProjectWatchdog()
    {
        currentProjects = new HashMap<IWorkbenchWindow, TapestryProject>();
    }
    
    @Override
    public void start()
    {
        super.start();
        
        windowListener = new WindowSelectionListener(new ISelectionListener()
        {
            @Override
            public void selectionChanged(IWorkbenchPart part, ISelection selection)
            {
                IWorkbenchPage page = part.getSite().getPage();
                
                TapestryFile tapestryFile = TapestryUtils.getTapestryFileFromPage(page);
                
                final IProject project = tapestryFile != null
                                       ? tapestryFile.getProject()
                                       : EclipseUtils.getProjectFromSelection(selection);
                
                if (project == null)
                {
                    return;
                }
                
                final IWorkbenchWindow window = part.getSite().getWorkbenchWindow();
                
                TapestryProject tapestryProject = currentProjects.get(window);
                
                if (tapestryProject == null
                        || (!tapestryProject.contains(project)
                                && TapestryUtils.isTapestryAppProject(project)))
                {
                    final TapestryProject newTapestryProject = new TapestryProject(project);
                    
                    Job analyzeProject = new Job(EclipseUtils.ECLIPSE_INTEGRATION_FOR_TAPESTRY5)
                    {
                        @Override
                        protected IStatus run(IProgressMonitor monitor)
                        {
                            monitor.beginTask("Analyzing " + project.getName(), IProgressMonitor.UNKNOWN);
                            monitor.worked(1);
                            
                            newTapestryProject.initialize(monitor);
                            
                            window.getShell().getDisplay().syncExec(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    changeProject(window, newTapestryProject);
                                }
                            });
                            
                            return Status.OK_STATUS;
                        }
                    };
                    
                    //  Don't show progress pop-up to the user's face
                    analyzeProject.setUser(false);
                    analyzeProject.schedule();
                }
            }
        })
        .addListener();
    }
    
    private void changeProject(IWorkbenchWindow window, TapestryProject newTapestryProject)
    {
        if (newTapestryProject != currentProjects.get(window))
        {
            currentProjects.put(window, newTapestryProject);
            
            notifyProjectChanged(window, newTapestryProject);
        }
    }
    
    private void notifyProjectChanged(IWorkbenchWindow targetWindow, TapestryProject newTapestryProject)
    {
        notifyProjectChanged(tapestryContextListeners.get(targetWindow), targetWindow, newTapestryProject);
        notifyProjectChanged(tapestryContextListeners.get(NULL_WINDOW), targetWindow, newTapestryProject);
    }
    
    private void notifyProjectChanged(List<ITapestryContextListener> listeners,
            IWorkbenchWindow targetWindow, TapestryProject newTapestryProject)
    {
        if (listeners != null)
        {
            for (ITapestryContextListener listener : listeners)
            {
                listener.projectChanged(targetWindow, newTapestryProject);
            }
        }
    }

    @Override
    public void stop()
    {
        windowListener.removeListener();
        windowListener = null;
        
        super.stop();
    }
    
    public TapestryProject getTapestryProject(IWorkbenchWindow window)
    {
        return currentProjects.get(window);
    }
}
