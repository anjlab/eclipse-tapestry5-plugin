package com.anjlab.eclipse.tapestry5;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLInputFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.anjlab.eclipse.tapestry5.watchdog.TapestryContextWatchdog;
import com.anjlab.eclipse.tapestry5.watchdog.TapestryProjectWatchdog;
import com.anjlab.eclipse.tapestry5.watchdog.WebXmlWatchdog;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin
{

    // The plug-in ID
    public static final String PLUGIN_ID = "com.anjlab.eclipse.tapestry5"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    private TapestryContextWatchdog tapestryContextWatchdog;
    private WebXmlWatchdog webXmlWatchdog;
    private TapestryProjectWatchdog tapestryProjectWatchdog;
    
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
        
        tapestryContextWatchdog = new TapestryContextWatchdog();
        tapestryContextWatchdog.start();
        
        webXmlWatchdog = new WebXmlWatchdog();
        webXmlWatchdog.start();
        
        tapestryProjectWatchdog = new TapestryProjectWatchdog();
        tapestryProjectWatchdog.start();
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
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    public void stop(BundleContext context) throws Exception
    {
        tapestryContextWatchdog.stop();
        tapestryContextWatchdog = null;
        
        webXmlWatchdog.stop();
        webXmlWatchdog = null;
        
        tapestryProjectWatchdog.stop();
        tapestryProjectWatchdog = null;
        
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

    public void logWarning(String message, Throwable t)
    {
        getLog().log(new Status(Status.WARNING, PLUGIN_ID, message, t));
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

    public TapestryContext getTapestryContext(IWorkbenchWindow window)
    {
        return tapestryContextWatchdog.getTapestryContext(window);
    }

    public void addTapestryContextListener(IWorkbenchWindow window, ITapestryContextListener listener)
    {
        tapestryContextWatchdog.addTapestryContextListener(window, listener);
    }

    public void removeTapestryContextListener(IWorkbenchWindow window, ITapestryContextListener listener)
    {
        tapestryContextWatchdog.removeTapestryContextListener(window, listener);
    }

    public String getWebXmlPropertyValue(IProject project, String propertyName)
    {
        return webXmlWatchdog.getWebXmlCache(project).get(propertyName);
    }

    public String getWebXmlPropertyName(IProject project, String propertyName)
    {
        Map<String, String> cache = webXmlWatchdog.getWebXmlCache(project);
        
        for (String key : cache.keySet())
        {
            String value = cache.get(key);
            
            if (propertyName.equals(value))
            {
                return key;
            }
        }
        
        return null;
    }
    
    public TapestryProject getTapestryProject(IWorkbenchWindow window)
    {
        return tapestryProjectWatchdog.getTapestryProject(window);
    }

    public void addTapestryProjectListener(IWorkbenchWindow window, ITapestryContextListener listener)
    {
        tapestryProjectWatchdog.addTapestryContextListener(window, listener);
    }

    public void removeTapestryProjectListener(IWorkbenchWindow window, ITapestryContextListener listener)
    {
        tapestryProjectWatchdog.removeTapestryContextListener(window, listener);
    }
}
