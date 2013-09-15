package com.anjlab.eclipse.tapestry5;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLInputFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin
{

    private static final String WEB_XML = "web.xml";

    // The plug-in ID
    public static final String PLUGIN_ID = "com.anjlab.eclipse.tapestry5"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    private IResourceChangeListener postChangeListener;

    /**
     * The constructor
     */
    public Activator()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
     * )
     */
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;
        
        projectCache = new ConcurrentHashMap<String, Map<String,Object>>();
        
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

    private Map<String, Map<String, Object>> projectCache;
    
    public synchronized Map<String, Object> getCache(IProject project)
    {
        Map<String, Object> cache = projectCache.get(project.getName());
        if (cache == null)
        {
            cache = new ConcurrentHashMap<String, Object>();
            projectCache.put(project.getName(), cache);
        }
        return cache;
    }
    
    @SuppressWarnings("unchecked")
    public synchronized Map<String, Object> getWebXmlCache(IProject project)
    {
        Map<String, Object> cache = getCache(project);
        Map<String, Object> webXmlCache = (Map<String, Object>) cache.get(WEB_XML);
        if (webXmlCache == null)
        {
            webXmlCache = new ConcurrentHashMap<String, Object>();
            cache.put(WEB_XML, webXmlCache);
        }
        return webXmlCache;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    public void stop(BundleContext context) throws Exception
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(postChangeListener);
        
        projectCache = null;
        
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault()
    {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path
     * 
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path)
    {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public void logError(String message)
    {
        getLog().log(new Status(Status.ERROR, PLUGIN_ID, message));
    }

    public void logError(String message, Throwable t)
    {
        getLog().log(new Status(Status.ERROR, PLUGIN_ID, message, t));
    }

    public void logWarning(String message)
    {
        getLog().log(new Status(Status.WARNING, PLUGIN_ID, message));
    }
    
    private XMLInputFactory factory = null;
    
    public XMLInputFactory getXMLInputFactory()
    {
        if (factory == null)
        {
            factory = XMLInputFactory.newInstance();
        }
        return factory;
    }

}
