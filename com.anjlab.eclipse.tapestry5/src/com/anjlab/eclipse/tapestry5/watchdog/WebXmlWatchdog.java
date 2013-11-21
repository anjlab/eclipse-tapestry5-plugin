package com.anjlab.eclipse.tapestry5.watchdog;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.core.resources.IContainer;
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
    
    public synchronized WebXml getWebXmlCache(IProject project)
    {
        Map<String, Object> cache = Activator.getDefault().getCache(project);
        
        WebXml webXmlCache = (WebXml) cache.get(WEB_XML);
        if (webXmlCache == null)
        {
            webXmlCache = readWebXml(project);
            
            cache.put(WEB_XML, webXmlCache);
        }
        return webXmlCache;
    }

    public static class WebXml
    {
        private Map<String, String> params = new HashMap<String, String>();
        private Map<String, String> filters = new HashMap<String, String>();
        
        public String getParamValue(String paramName)
        {
            return params.get(paramName);
        }
        
        public void clear()
        {
            params.clear();
            filters.clear();
        }

        public Set<String> getFilterNames()
        {
            return filters.keySet();
        }
        
        public String getFilterClassName(String filterName)
        {
            return filters.get(filterName);
        }
    }
    
    private static WebXml readWebXml(IProject project)
    {
        WebXml webXml = new WebXml();
        
        IContainer webapp = TapestryUtils.findWebapp(project);
        
        if (webapp == null)
        {
            return webXml;
        }
        
        IFile webXmlFile = (IFile) webapp.findMember("/WEB-INF/web.xml");
        
        if (webXmlFile == null)
        {
            return webXml;
        }
        
        XMLStreamReader reader = null;
        InputStream input = null;
        
        try
        {
            input = webXmlFile.getContents();
            
            reader = Activator.getDefault().getXMLInputFactory().createXMLStreamReader(input);
            
            while (nextStartElement(reader))
            {
                String[] tags = new String[] { "param-name", "param-value",
                                               "filter-name", "filter-class" };
                
                for (int i = 0; i < tags.length; i += 2)
                {
                    if (tags[i].equals(reader.getName().getLocalPart()))
                    {
                        String key = reader.getElementText();
                        
                        if (nextStartElement(reader))
                        {
                            if (tags[i+1].equals(reader.getName().getLocalPart()))
                            {
                                String value = reader.getElementText();
                                
                                if (tags[i].equals("param-name"))
                                {
                                    webXml.params.put(key, value);
                                }
                                else if (tags[i].equals("filter-name"))
                                {
                                    webXml.filters.put(key, value);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Activator.getDefault().logError("Error reading web.xml", e);
        }
        finally
        {
            if (reader != null)
            {
                try { reader.close(); } catch (Exception e) {}
            }
            if (input != null)
            {
                try { input.close(); } catch (Exception e) {}
            }
        }
        
        return webXml;
    }

    private static boolean nextStartElement(XMLStreamReader reader) throws XMLStreamException
    {
        while (reader.hasNext())
        {
            if (reader.next() == XMLStreamConstants.START_ELEMENT)
            {
                return true;
            }
        }
        return false;
    }

}
