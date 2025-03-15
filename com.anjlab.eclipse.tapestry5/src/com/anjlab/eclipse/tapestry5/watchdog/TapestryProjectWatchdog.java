package com.anjlab.eclipse.tapestry5.watchdog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class TapestryProjectWatchdog extends AbstractWatchdog
{
    private final class TapestryProjectAnalyzerJob extends Job
    {
        public static final String FAMILY_NAME = EclipseUtils.ECLIPSE_INTEGRATION_FOR_TAPESTRY5;
        
        private final IWorkbenchWindow window;
        private final IProject project;

        private TapestryProjectAnalyzerJob(IWorkbenchWindow window, IProject project)
        {
            super(EclipseUtils.ECLIPSE_INTEGRATION_FOR_TAPESTRY5);
            this.window = window;
            this.project = project;
            
            //  Don't show progress pop-up to the user's face
            setUser(false);
            //  Low priority
            setPriority(DECORATE);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            try
            {
                waitForOtherJobs(monitor);
            }
            catch (OperationCanceledException e)
            {
                return Status.CANCEL_STATUS;
            }
            catch (InterruptedException e)
            {
                return Status.CANCEL_STATUS;
            }
            
            //  TODO Check if there's any build errors in this project
            //  Analyzing broken project is extremely slow
            
            monitor.beginTask("Analyzing " + project.getName(), IProgressMonitor.UNKNOWN);
            monitor.worked(1);
            
            final TapestryProject newTapestryProject = new TapestryProject(project);
            
            newTapestryProject.initialize(monitor);
            
            if (monitor.isCanceled())
            {
                //  Don't propagate project changes if the job was canceled
                return Status.CANCEL_STATUS;
            }
            
            EclipseUtils.asyncExec(window.getShell(), new Runnable()
            {
                @Override
                public void run()
                {
                    currentProjects.put(window, newTapestryProject);
                    
                    notifyProjectChanged(window, newTapestryProject);
                }
            });
            
            return Status.OK_STATUS;
        }

        private void waitForOtherJobs(IProgressMonitor monitor) throws OperationCanceledException, InterruptedException
        {
            getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, monitor);
            getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, monitor);
            getJobManager().join(JavaUI.ID_PLUGIN, monitor);
        }

        @Override
        public boolean belongsTo(Object family)
        {
            return FAMILY_NAME.equals(family);
        }
    }

    private static void cancelOtherJobsOfThisKind(IWorkbenchWindow window)
    {
        Job[] jobs = Job.getJobManager().find(TapestryProjectAnalyzerJob.FAMILY_NAME);
        for (Job job : jobs)
        {
            if (job instanceof TapestryProjectAnalyzerJob)
            {
                if (((TapestryProjectAnalyzerJob) job).window == window)
                {
                    job.cancel();
                }
            }
        }
    }

    private WindowSelectionListener windowListener;
    
    private final Map<IWorkbenchWindow, TapestryProject> currentProjects;

    private IResourceChangeListener postChangeListener;

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
            private final ActiveEditorTracker activeEditorTracker = new ActiveEditorTracker();
            
            @Override
            public void selectionChanged(IWorkbenchPart part, ISelection selection)
            {
                IWorkbenchPage page = part.getSite().getPage();
                
                //  https://github.com/anjlab/eclipse-tapestry5-plugin/issues/18
                if (!activeEditorTracker.editorChanged(page))
                {
                    return;
                }
                
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
                                && TapestryUtils.isTapestryProject(project)))
                {
                    changeTapestryProject(window, project);
                }
            }
        })
        .addListener();
        
        postChangeListener = new IResourceChangeListener()
        {
            @Override
            public void resourceChanged(IResourceChangeEvent event)
            {
                Set<IProject> affectedProjects = new HashSet<IProject>();
                
                List<IFile> affectedFiles = EclipseUtils.getAllAffectedResources(event.getDelta(), IFile.class);
                
                //  XXX This event is triggered twice on clean & build
                
                filterOutFilesLocatedInOutputFolders(affectedFiles);
                
                if (affectedFiles.size() == 0)
                {
                    return;
                }

                if (affectedFiles.size() > 25)
                {
                    //  Probably build is going on. It's faster to re-analyze entire project,
                    //  because number of files we should check may be huge here
                    affectedFiles.clear();
                    
                    affectedProjects.addAll(EclipseUtils.getAllAffectedResources(event.getDelta(), IProject.class));
                }
                
                for (IFile affectedFile : affectedFiles)
                {
                    //  Some project files might have been changed:
                    //  
                    //  1) Classpath          => SubModules added/removed
                    //  2) web.xml            => AppModule may have been reconfigured as well as list of Development/QA/Production modules
                    //  3) Module's Java file => SubModules, LibraryMappings changed
                    //  4) Tapestry Context   => Removed/Added, Component Specification changed
                    
                    if (isEclipseProjectClasspathFile(affectedFile))
                    {
                        //  We don't want to construct tapestry project multiple times if there's another file from this project that is changed
                        //  Collect all affected eclipse projects first, then update corresponding tapestry projects if needed
                        affectedProjects.add(affectedFile.getProject());
                        continue;
                    }
                    
                    if (isWebXmlFile(affectedFile))
                    {
                        affectedProjects.add(affectedFile.getProject());
                        continue;
                    }
                    
                    if (affectedFile.getName().endsWith(".class"))
                    {
                        continue;
                    }
                    
                    TapestryContext context = Activator.getDefault()
                            .getTapestryContextFactory()
                            .createTapestryContext(affectedFile);
                    
                    for (Entry<IWorkbenchWindow, TapestryProject> entry : currentProjects.entrySet())
                    {
                        TapestryProject project = entry.getValue();
                        
                        for (TapestryModule module : project.modules())
                        {
                            if (module.isReadOnly() && !module.isTapestryCoreModule())
                            {
                                //  We don't expect that files from read only modules will be changed
                                continue;
                            }
                            
                            if (TapestryUtils.isModuleFile(affectedFile, module))
                            {
                                Activator.getDefault().getTapestryModuleFactory().localModuleChanged(affectedFile);
                                affectedProjects.add(affectedFile.getProject());
                                continue;
                            }
                            
                            for (LibraryMapping mapping : module.libraryMappings())
                            {
                                //  TODO Support pages and mixins
                                if (context.getPackageName() != null
                                        && context.getPackageName().startsWith(
                                        mapping.getRootPackage() + ".components"))
                                {
                                    TapestryModule targetModule = module;
                                    
                                    //  XXX DRY: see TapestryModule#findComponents
                                    if (mapping.getPathPrefix().isEmpty() && module.isTapestryCoreModule())
                                    {
                                        //  This package is from the AppModule
                                        for (TapestryModule m : module.getProject().modules())
                                        {
                                            if (m.isAppModule())
                                            {
                                                targetModule = m;
                                                break;
                                            }
                                        }
                                    }
                                    
                                    List<TapestryContext> components = targetModule.getComponents();
                                    
                                    boolean replaced = false;
                                    
                                    for (int i = 0; i < components.size(); i++)
                                    {
                                        TapestryContext component = components.get(i);
                                        if (StringUtils.equals(component.getPackageName() + "." + component.getName(), 
                                                context.getPackageName() + "." + context.getName()))
                                        {
                                            if (context.getInitialFile().exists())
                                            {
                                                //  Replace with new context
                                                components.set(i, context);
                                            }
                                            else
                                            {
                                                //  File deleted
                                                components.remove(i--);
                                            }
                                            replaced = true;
                                            break;
                                        }
                                    }
                                    
                                    if (!replaced)
                                    {
                                        //  New component
                                        components.add(context);
                                    }
                                }
                            }
                        }
                    }
                }
                
                //  Find out which tapestry projects should be re-analyzed
                
                //  TODO This should better be done after project build
                
                Set<IProject> updatedProjects = new HashSet<IProject>();
                
                for (Entry<IWorkbenchWindow, TapestryProject> entry : currentProjects.entrySet())
                {
                    TapestryProject tapestryProject = entry.getValue();
                    
                    for (IProject project : affectedProjects)
                    {
                        if (tapestryProject.contains(project))
                        {
                            if (!updatedProjects.contains(tapestryProject.getProject()))
                            {
                                if (hasProblems(tapestryProject.getProject()))
                                {
                                    // Java search is very slow on broken
                                    // projects, skip automatic refresh

                                    // TODO Update icon in Tapestry Project
                                    // Outline to indicate it's probably out of
                                    // sync
                                    continue;
                                }

                                updateProject(tapestryProject.getProject());

                                updatedProjects.add(tapestryProject.getProject());
                            }
                        }
                    }
                }
                
                //  Usually updatedProjects will be empty here, because web.xml,
                //  classpath and Tapestry modules are not changed very often,
                //  but contexts do. And it is not necessary to re-analyze entire tapestry project,
                //  simply updating this context in corresponding module should be enough
                
                //  XXX Project analysis is a background job which may be still running now,
                //  so we should wait for it before updating corresponding module
                //  Though if some tapestry project was re-analyzed because of affected changes, then
                //  it should have been also pick up all the changes of its contexts
                
                //  XXX How can we know that this context is from the project that is now under analysis?
                //  Some modules might have been changed (added/removed) -- Update modules before analysis?
            }

            private void filterOutFilesLocatedInOutputFolders(List<IFile> affectedFiles)
            {
                Map<IProject, List<IPath>> outputLocations = new HashMap<IProject, List<IPath>>();
                
                for (int i = 0; i < affectedFiles.size(); i++)
                {
                    IFile affectedFile = affectedFiles.get(i);
                    
                    IProject project = affectedFile.getProject();
                    
                    List<IPath> locations = outputLocations.get(project);
                    
                    if (locations == null)
                    {
                        locations = findOutputLocations(project);
                        
                        outputLocations.put(project, locations);
                    }
                    
                    for (IPath output : locations)
                    {
                        if (output.isPrefixOf(affectedFile.getFullPath()))
                        {
                            //  This file is from the output folder, ignore it
                            
                            //  XXX ArrayList isn't the best choice for mutable lists
                            affectedFiles.remove(i--);
                            break;
                        }
                    }
                }
            }

            private List<IPath> findOutputLocations(IProject project)
            {
                List<IPath> locations = new ArrayList<IPath>();
                
                try
                {
                    if (EclipseUtils.isJavaProject(project))
                    {
                        IJavaProject javaProject = JavaCore.create(project);
                        
                        if (javaProject.getOutputLocation() != null)
                        {
                            locations.add(javaProject.getOutputLocation());
                        }
                        
                        for (IPackageFragmentRoot root : javaProject.getPackageFragmentRoots())
                        {
                            if (root instanceof IClasspathEntry)
                            {
                                IPath outputLocation = ((IClasspathEntry) root).getOutputLocation();
                                
                                if (outputLocation != null)
                                {
                                    locations.add(outputLocation);
                                }
                            }
                        }
                    }
                }
                catch (CoreException e)
                {
                    //  Ignore
                }
                
                return locations;
            }

            private boolean isWebXmlFile(IFile affectedFile)
            {
                return affectedFile.getName().equals("web.xml")
                        && Objects.equals(affectedFile, TapestryUtils.findWebXml(affectedFile.getProject()));
            }

            private boolean isEclipseProjectClasspathFile(IFile affectedFile)
            {
                return affectedFile.getName().equals(".classpath")
                    && Objects.equals(affectedFile.getParent(), affectedFile.getProject());
            }
        };
        
        ResourcesPlugin.getWorkspace().addResourceChangeListener(postChangeListener, IResourceChangeEvent.POST_CHANGE);
    }
    
    private void updateProject(IProject affectedProject)
    {
        //  Check if there's any window that has this project as current
        for (Entry<IWorkbenchWindow, TapestryProject> entry : currentProjects.entrySet())
        {
            IWorkbenchWindow window = entry.getKey();
            TapestryProject project = entry.getValue();
            
            for (TapestryModule module : project.modules())
            {
                if (Objects.equals(affectedProject, module.getEclipseProject()))
                {
                    //  Let every window get its own copy of TapestryProject
                    changeTapestryProject(window, affectedProject);
                    break;
                }
            }
        }
    }
    
    private synchronized void changeTapestryProject(final IWorkbenchWindow window, final IProject project)
    {
        //  Other jobs may still be running, cancel them and don't even schedule new job until they completed
        
        cancelOtherJobsOfThisKind(window);
        
        try
        {
            Job.getJobManager().join(TapestryProjectAnalyzerJob.FAMILY_NAME, new NullProgressMonitor());
        }
        catch (OperationCanceledException e)
        {
            //  Ignore
        }
        catch (InterruptedException e)
        {
            //  Ignore
        }
        
        Job analyzeProject = new TapestryProjectAnalyzerJob(window, project);
        
        analyzeProject.schedule();
    }
    
    private void notifyProjectChanged(IWorkbenchWindow targetWindow, TapestryProject newTapestryProject)
    {
        for (ITapestryContextListener listener : listeners.find(ITapestryContextListener.class, targetWindow, true))
        {
            listener.projectChanged(targetWindow, newTapestryProject);
        }
    }

    @Override
    public void stop()
    {
        windowListener.removeListener();
        windowListener = null;
        
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(postChangeListener);
        postChangeListener = null;
        
        super.stop();
    }
    
    public TapestryProject getTapestryProject(IWorkbenchWindow window)
    {
        return currentProjects.get(window);
    }

    public void forceProjectRefresh(IWorkbenchWindow window)
    {
        TapestryProject tapestryProject = currentProjects.get(window);

        if (tapestryProject != null)
        {
            IProject project = tapestryProject.getProject();

            Activator.getDefault().getTapestryModuleFactory().clearCache(project);

            updateProject(project);
        }
    }

    private static boolean hasProblems(IProject project)
    {
        try
        {
            IMarker[] markers = project.findMarkers(
                    IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER,
                    true,
                    IResource.DEPTH_INFINITE);

            for (IMarker marker : markers)
            {
                Integer severityType = (Integer) marker.getAttribute(IMarker.SEVERITY);
                if (severityType.intValue() == IMarker.SEVERITY_ERROR)
                {
                    return true;
                }
            }

            return false;
        }
        catch (CoreException e)
        {
            Activator.getDefault().logError("Error determining project problems", e);

            return true;
        }
    }
}
