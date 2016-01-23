package com.anjlab.eclipse.tapestry5.internal;

import java.util.HashMap;
import java.util.Map;

import com.anjlab.eclipse.tapestry5.templates.ProjectSettings;

/**
 * 
 * @deprecated Built-in JavaScript modules moved to default
 *             {@link ProjectSettings}
 *
 */
public class TapestryBuiltinJavaScriptModuleConfiguration
{
    private static final String TAPESTRY_ASSET_ROOT = "${tapestry.asset.root}";
    private static final String BOOTSTRAP_ROOT = "${tapestry.bootstrap-root}";
    
    private Map<String, String> modules = new HashMap<String, String>();
    
    public TapestryBuiltinJavaScriptModuleConfiguration()
    {
        //  T5.4 Contributions to ModuleManager from JavaScriptModule
        
        //  From JavaScriptModule#setupBaseModules()
        
        modules.put("underscore", TAPESTRY_ASSET_ROOT + "/underscore-shim.js");
        modules.put("jquery", TAPESTRY_ASSET_ROOT + "/jquery-shim.js");
        
        for (String name : new String[] {
                "transition",
                "affix",
                "alert",
                "button",
                "carousel",
                "collapse",
                "dropdown",
                "modal",
                "scrollspy",
                "tab",
                "tooltip",
                "popover"
            })
        {
            modules.put("bootstrap/" + name, BOOTSTRAP_ROOT + "/js/" + name + ".js");
        }
        
        modules.put("t5/core/typeahead", TAPESTRY_ASSET_ROOT + "/typeahead.js");
        modules.put("moment", TAPESTRY_ASSET_ROOT + "/moment-2.8.4.js");
        
        //  JavaScriptModule#setupFoundationFramework
        
        modules.put("t5/core/dom", "classpath:org/apache/tapestry5/t5-core-dom-${tapestry.javascript-infrastructure-provider}.js");
        
        //  TODO From JavaScriptModule#setupApplicationCatalogModules
        //  (i.e., "t5/core/messages/")
    }
    
    public String getPath(String moduleName)
    {
        return modules.get(moduleName);
    }
}
