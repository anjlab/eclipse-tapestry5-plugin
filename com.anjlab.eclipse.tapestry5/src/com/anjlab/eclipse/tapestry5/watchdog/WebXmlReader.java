package com.anjlab.eclipse.tapestry5.watchdog;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.core.resources.IFile;

import com.anjlab.eclipse.tapestry5.Activator;

public class WebXmlReader
{
    public static class WebXml
    {
        private Map<String, String> params = new HashMap<String, String>();
        private Map<String, String> filters = new HashMap<String, String>();
        
        public String getParamValue(String paramName)
        {
            return params.get(paramName);
        }
        
        public boolean isEmpty()
        {
            return params.isEmpty() && filters.isEmpty();
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

        public Set<String> getParamNames()
        {
            return params.keySet();
        }
    }
    
    public static WebXml readWebXml(IFile webXmlFile)
    {
        if (webXmlFile == null)
        {
            return new WebXml();
        }
        
        WebXml webXml = new WebXml();
        
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
