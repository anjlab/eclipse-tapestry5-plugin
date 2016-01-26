package com.anjlab.eclipse.tapestry5;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLInputFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.anjlab.eclipse.tapestry5.watchdog.EclipseClasspathWatchdog;
import com.anjlab.eclipse.tapestry5.watchdog.ITapestryContextListener;
import com.anjlab.eclipse.tapestry5.watchdog.IWebXmlListener;
import com.anjlab.eclipse.tapestry5.watchdog.TapestryContextWatchdog;
import com.anjlab.eclipse.tapestry5.watchdog.TapestryProjectWatchdog;
import com.anjlab.eclipse.tapestry5.watchdog.WebXmlReader;
import com.anjlab.eclipse.tapestry5.watchdog.WebXmlReader.WebXml;
import com.anjlab.eclipse.tapestry5.watchdog.WebXmlWatchdog;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements IWebXmlListener
{

    // The plug-in ID
    public static final String PLUGIN_ID = "com.anjlab.eclipse.tapestry5"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    private TapestryContextWatchdog tapestryContextWatchdog;
    private WebXmlWatchdog webXmlWatchdog;
    private TapestryProjectWatchdog tapestryProjectWatchdog;
    private TapestryModuleFactory tapestryModuleFactory;
    private EclipseClasspathWatchdog eclipseClasspathWatchdog;
    private TapestryContextFactory tapestryContextFactory;
    
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
        
        webXmlWatchdog = new WebXmlWatchdog();
        webXmlWatchdog.addListener(null, this);
        webXmlWatchdog.start();
        
        // Tapestry project watchdog should start before tapestry context one,
        // this is in order to receive resource change notifications first,
        // because tapestry context references to tapestry modules that could be
        // outdated
        tapestryProjectWatchdog = new TapestryProjectWatchdog();
        tapestryProjectWatchdog.start();

        tapestryContextFactory = new TapestryContextFactory();

        tapestryModuleFactory = new TapestryModuleFactory();

        eclipseClasspathWatchdog = new EclipseClasspathWatchdog();
        eclipseClasspathWatchdog.addListener(null, tapestryModuleFactory);
        eclipseClasspathWatchdog.start();

        tapestryContextWatchdog = new TapestryContextWatchdog();
        tapestryContextWatchdog.start();
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
        tapestryProjectWatchdog.stop();
        tapestryProjectWatchdog = null;
        
        webXmlWatchdog.stop();
        webXmlWatchdog.removeListener(null, this);
        webXmlWatchdog = null;
        
        tapestryContextWatchdog.stop();
        tapestryContextWatchdog = null;
        
        eclipseClasspathWatchdog.stop();
        eclipseClasspathWatchdog.removeListener(null, tapestryModuleFactory);
        eclipseClasspathWatchdog = null;
        
        tapestryModuleFactory = null;
        
        tapestryContextFactory = null;
        
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
        tapestryContextWatchdog.addListener(window, listener);
    }

    public void removeTapestryContextListener(IWorkbenchWindow window, ITapestryContextListener listener)
    {
        tapestryContextWatchdog.removeListener(window, listener);
    }
    
    private static final String WEB_XML = "web.xml";
    
    @Override
    public void webXmlChanged(IFile file, WebXml webXml)
    {
        //  Replace webXml in cache with new instance
        Activator.getDefault().getCache(file.getProject()).put(WEB_XML, webXml);
    }
    
    public WebXml getWebXml(IProject project)
    {
        Map<String, Object> projectCache = Activator.getDefault().getCache(project);
        
        WebXml webXml = (WebXml) projectCache.get(WEB_XML);
        
        if (webXml == null || webXml.isEmpty())
        {
            webXml = WebXmlReader.readWebXml(TapestryUtils.findWebXml(project));
            
            projectCache.put(WEB_XML, webXml);
        }
        
        return webXml;
    }
    
    public TapestryProject getTapestryProject(IWorkbenchWindow window)
    {
        return tapestryProjectWatchdog.getTapestryProject(window);
    }

    public void addTapestryProjectListener(IWorkbenchWindow window, ITapestryContextListener listener)
    {
        tapestryProjectWatchdog.addListener(window, listener);
    }

    public void removeTapestryProjectListener(IWorkbenchWindow window, ITapestryContextListener listener)
    {
        tapestryProjectWatchdog.removeListener(window, listener);
    }

    public void forceProjectRefresh(IWorkbenchWindow window)
    {
        tapestryProjectWatchdog.forceProjectRefresh(window);
    }
    
    public TapestryModuleFactory getTapestryModuleFactory()
    {
        return tapestryModuleFactory;
    }

    public TapestryContextFactory getTapestryContextFactory()
    {
        return tapestryContextFactory;
    }

    private static Image tapestryLogoIcon;
    
    public static Image getTapestryLogoIcon()
    {
        return tapestryLogoIcon != null
             ? tapestryLogoIcon
             : (tapestryLogoIcon = getImageDescriptor("icons/tapestry-logo.png").createImage());
    }
}
