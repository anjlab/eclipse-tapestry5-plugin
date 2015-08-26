package com.anjlab.eclipse.tapestry5.watchdog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.TapestryUtils;
import com.anjlab.eclipse.tapestry5.watchdog.WebXmlReader.WebXml;

public class WebXmlWatchdog extends AbstractWatchdog
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
                
                Map<IProject, IFile> webXmlFiles = new HashMap<IProject, IFile>();
                
                for (IFile changedFile : changedFiles)
                {
                    if (!changedFile.getName().equals("web.xml"))
                    {
                        continue;
                    }
                    
                    IFile webXml = null;
                    if (webXmlFiles.containsKey(changedFile.getProject()))
                    {
                        webXml = webXmlFiles.get(changedFile.getProject());
                    }
                    else
                    {
                        webXml = TapestryUtils.findWebXml(changedFile.getProject());
                        webXmlFiles.put(changedFile.getProject(), webXml);
                    }
                    
                    if (webXml == null)
                    {
                        //  This project doesn't have web.xml file
                        continue;
                    }
                    
                    if (ObjectUtils.equals(changedFile, webXml))
                    {
                        notifyWebXmlChanged(changedFile);
                    }
                }
            }
        };
        
        ResourcesPlugin.getWorkspace().addResourceChangeListener(postChangeListener, IResourceChangeEvent.POST_CHANGE);
    }

    private void notifyWebXmlChanged(IFile webXmlFile)
    {
        WebXml webXml = WebXmlReader.readWebXml(webXmlFile);
        
        for (IWebXmlListener listener : listeners.find(IWebXmlListener.class, null, true))
        {
            listener.webXmlChanged(webXmlFile, webXml);
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
