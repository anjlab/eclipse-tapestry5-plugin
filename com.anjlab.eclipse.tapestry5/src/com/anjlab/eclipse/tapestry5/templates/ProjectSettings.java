package com.anjlab.eclipse.tapestry5.templates;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.PlatformUI;

import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.Openable;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryService.ServiceDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;

//  TODO Add file markers with SEVERITY_INFO in Eclipse views
//  for config file if some supported options are missing,
//  provide a link to latest template:
//  link to GitHub or create unnamed editor and copy default template to it.
public class ProjectSettings implements Openable
{
    private static final Gson gson =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private IFile source;

    private boolean isDefault;

    public static class TapestryServiceSettings extends ServiceDefinition
    {
        // If not empty then other values, like intfClass/implClass,
        // may be treated as parameters for this auto-discovery rule.
        private String discovery;
        
        public String getDiscovery()
        {
            return discovery;
        }
        
        public TapestryServiceSettings clone()
        {
            TapestryServiceSettings copy = new TapestryServiceSettings();
            copy.copyFrom(this);
            copy.discovery = discovery;
            return copy;
        }
    }

    public static class TapestryModuleSettings
    {
        private Map<String, TapestryServiceSettings> services;
        
        public TapestryModuleSettings()
        {
            services = new HashMap<String, TapestryServiceSettings>();
        }
        
        public Map<String, TapestryServiceSettings> getTapestryServices()
        {
            return services;
        }
    }
    
    private Map<String, TapestryModuleSettings> tapestryModules;
    private Map<String, String> javaScriptModules;
    private Map<String, Map<String, String>> symbols;
    private Map<String, String> fileNamingConventions;

    private static final String CONFIG_NAME = "config";
    private static final String CONFIG_EXT = "json";

    public static final String CONFIG_FILE_NAME = CONFIG_NAME + "." + CONFIG_EXT;

    public ProjectSettings()
    {
        tapestryModules = new HashMap<String, TapestryModuleSettings>();
        javaScriptModules = new HashMap<String, String>();
        symbols = new HashMap<String, Map<String, String>>();
        fileNamingConventions = new LinkedHashMap<String, String>();
    }

    public void setSource(IFile source)
    {
        this.source = source;
    }

    @Override
    public void openInEditor()
    {
        if (source != null)
        {
            EclipseUtils.openFile(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), source);
        }
    }

    public String getReferenceLabel()
    {
        return isDefault
                ? "default settings"
                : source.getFullPath().toPortableString();
    }

    public Map<String, TapestryModuleSettings> getTapestryModules()
    {
    	// XXX Values are still modifiable
        return Collections.unmodifiableMap(tapestryModules);
    }

    public Map<String, Map<String, String>> getSymbols()
    {
        // XXX Values are still modifiable
        return Collections.unmodifiableMap(symbols);
    }

    public Map<String, String> getFileNamingConventions()
    {
        return Collections.unmodifiableMap(fileNamingConventions);
    }
    
    public static InputStream getDefaultContents(TapestryProject tapestryProject)
    {
        return TapestryTemplates.get(tapestryProject)
                .openTemplate(null, CONFIG_NAME, CONFIG_EXT);
    }

    public static ProjectSettings getDefault(TapestryProject tapestryProject)
    {
        try
        {
            ProjectSettings settings = parse(getDefaultContents(tapestryProject));
            settings.isDefault = true;
            return settings;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Error parsing default settings", e);
        }
    }

    public boolean isDefault()
    {
        return isDefault;
    }

    public static ProjectSettings parse(InputStream input)
            throws JsonIOException, JsonSyntaxException, IOException
    {
        JsonReader reader = new JsonReader(new InputStreamReader(input, "UTF-8"));
        reader.setStrictness(Strictness.LENIENT);
        try
        {
            return gson.fromJson(reader, ProjectSettings.class);
        }
        finally
        {
            reader.close();
        }
    }

    public ProjectSettings extend(ProjectSettings with)
    {
        ProjectSettings newSettings = new ProjectSettings();

        deepCopyMap(newSettings.symbols, this.symbols);
        deepCopyMap(newSettings.symbols, with.symbols);

        newSettings.javaScriptModules.putAll(this.javaScriptModules);
        newSettings.javaScriptModules.putAll(with.javaScriptModules);

        newSettings.tapestryModules.putAll(this.tapestryModules);
        newSettings.tapestryModules.putAll(with.tapestryModules);

        newSettings.fileNamingConventions.putAll(this.fileNamingConventions);
        newSettings.fileNamingConventions.putAll(with.fileNamingConventions);

        return newSettings;
    }

    private <T extends Object> void deepCopyMap(Map<String, Map<String, T>> to, Map<String, Map<String, T>> from)
    {
        for (Entry<String, Map<String, T>> entry : from.entrySet())
        {
            Map<String, T> copyOfValueMap = to.get(entry.getKey());

            if (copyOfValueMap == null)
            {
                copyOfValueMap = new HashMap<String, T>();
                to.put(entry.getKey(), copyOfValueMap);
            }

            copyOfValueMap.putAll(entry.getValue());
        }
    }

}
