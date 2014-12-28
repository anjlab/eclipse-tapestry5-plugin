package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.List;


public class TapestryService
{
    public static class ServiceDefinition
    {
        private final List<String> markers = new ArrayList<String>();
        private boolean preventReloading;
        private boolean preventDecoration;
        private boolean eagerLoad;
        private String scope;
        private String id;
        private boolean simpleId;
        private String intfClass;
        private String implClass;
        
        public boolean isPreventReloading()
        {
            return preventReloading;
        }
        public ServiceDefinition setPreventReloading(boolean preventReloading)
        {
            this.preventReloading = preventReloading;
            return this;
        }
        public boolean isPreventDecoration()
        {
            return preventDecoration;
        }
        public ServiceDefinition setPreventDecoration(boolean preventDecoration)
        {
            this.preventDecoration = preventDecoration;
            return this;
        }
        public boolean isEagerLoad()
        {
            return eagerLoad;
        }
        public ServiceDefinition setEagerLoad(boolean eagerLoad)
        {
            this.eagerLoad = eagerLoad;
            return this;
        }
        public String getScope()
        {
            return scope;
        }
        public ServiceDefinition setScope(String scope)
        {
            this.scope = scope;
            return this;
        }
        public String getId()
        {
            return id;
        }
        public ServiceDefinition setId(String id)
        {
            this.id = id;
            return this;
        }
        public boolean isSimpleId()
        {
            return simpleId;
        }
        public ServiceDefinition setSimpleId(boolean simpleId)
        {
            this.simpleId = simpleId;
            return this;
        }
        public String getIntfClass()
        {
            return intfClass;
        }
        public ServiceDefinition setIntfClass(String intfClass)
        {
            this.intfClass = intfClass;
            return this;
        }
        public String getImplClass()
        {
            return implClass;
        }
        public ServiceDefinition setImplClass(String implClass)
        {
            this.implClass = implClass;
            return this;
        }
        public List<String> getMarkers()
        {
            return markers;
        }
        public ServiceDefinition addMarker(String className)
        {
            markers.add(className);
            return this;
        }
    }

    private TapestryModule tapestryModule;
    private DeclarationReference reference;
    private ServiceDefinition definition;
    
    public TapestryService(TapestryModule tapestryModule, ServiceDefinition definition, DeclarationReference reference)
    {
        this.tapestryModule = tapestryModule;
        this.definition = definition;
        this.reference = reference;
    }
    
    public TapestryModule getTapestryModule()
    {
        return tapestryModule;
    }
    
    public ServiceDefinition getDefinition()
    {
        return definition;
    }
    
    public DeclarationReference getReference()
    {
        return reference;
    }
}
