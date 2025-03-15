package com.anjlab.eclipse.tapestry5.watchdog;

import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;

import com.anjlab.eclipse.tapestry5.EclipseUtils;

public class EclipseClasspathWatchdog extends AbstractWatchdog
{
    private IResourceChangeListener postChangeListener;

    @Override
    public void start()
    {
        super.start();
        
        postChangeListener = new IResourceChangeListener()
        {
            @Override
            public void resourceChanged(IResourceChangeEvent event)
            {
                List<IFile> changedFiles = EclipseUtils.getAllAffectedResources(
                        event.getDelta(), IFile.class, IResourceDelta.CHANGED);
                
                for (IFile changedFile : changedFiles)
                {
                    if (isEclipseProjectClasspathFile(changedFile))
                    {
                        notifyEclipseClasspathChanged(changedFile);
                    }
                }
            }
        };
        
        ResourcesPlugin.getWorkspace().addResourceChangeListener(postChangeListener, IResourceChangeEvent.POST_CHANGE);
    }

    private boolean isEclipseProjectClasspathFile(IFile file)
    {
        return file.getName().equals(".classpath")
            && Objects.equals(file.getParent(), file.getProject());
    }

    private void notifyEclipseClasspathChanged(IFile file)
    {
        for (IEclipseClasspathListener listener : listeners.find(IEclipseClasspathListener.class, null, true))
        {
            listener.classpathChanged(file);
        }
    }
    
    @Override
    public void stop()
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(postChangeListener);
        
        postChangeListener = null;
        
        super.stop();
    }
}
