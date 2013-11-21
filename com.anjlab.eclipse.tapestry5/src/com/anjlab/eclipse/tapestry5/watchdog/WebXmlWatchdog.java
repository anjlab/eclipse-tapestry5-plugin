package com.anjlab.eclipse.tapestry5.watchdog;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class WebXmlWatchdog extends AbstractWatchdog
{
    private static final String WEB_XML = "web.xml";

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
                    if (WEB_XML.equals(changedFile.getName()))
                    {
                        getWebXmlCache(changedFile.getProject()).clear();
                    }
                }
            }
        };
        
        ResourcesPlugin.getWorkspace().addResourceChangeListener(postChangeListener, IResourceChangeEvent.POST_CHANGE);
    }

    @Override
    public void stop()
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(postChangeListener);
        
        postChangeListener = null;
        
        super.stop();
    }
    
    @SuppressWarnings("unchecked")
    public synchronized Map<String, String> getWebXmlCache(IProject project)
    {
        Map<String, Object> cache = Activator.getDefault().getCache(project);
        
        Map<String, String> webXmlCache = (Map<String, String>) cache.get(WEB_XML);
        if (webXmlCache == null)
        {
            webXmlCache = new ConcurrentHashMap<String, String>();
            
            webXmlCache.putAll(TapestryUtils.readWebXml(project));
            
            cache.put(WEB_XML, webXmlCache);
        }
        return webXmlCache;
    }

}
