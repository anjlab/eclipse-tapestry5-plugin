package com.anjlab.eclipse.tapestry5;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class TapestryService implements Openable
{
    public enum InstrumenterType
    {
        DECORATOR, ADVISOR, CONTRIBUTOR
    }

    public static interface Matcher
    {
        boolean matches(TapestryService service);
    }
    
    public static class ServiceInstrumenter implements Openable
    {
        private Matcher serviceMatcher;
        private DeclarationReference reference;
        private String id;
        private String[] constraints;
        private InstrumenterType type;
        private boolean optional;
        
        public InstrumenterType getType()
        {
            return type;
        }
        public ServiceInstrumenter setType(InstrumenterType type)
        {
            this.type = type;
            return this;
        }
        public ServiceInstrumenter setOptional(boolean optional)
        {
            this.optional = optional;
            return this;
        }
        public boolean isOptional()
        {
            return optional;
        }
        public Matcher getServiceMatcher()
        {
            return serviceMatcher;
        }
        public ServiceInstrumenter setServiceMatcher(Matcher serviceMatcher)
        {
            this.serviceMatcher = serviceMatcher;
            return this;
        }
        public DeclarationReference getReference()
        {
            return reference;
        }
        public ServiceInstrumenter setReference(DeclarationReference reference)
        {
            this.reference = reference;
            return this;
        }
        public String getId()
        {
            return id;
        }
        public ServiceInstrumenter setId(String id)
        {
            this.id = id;
            return this;
        }
        public String[] getConstraints()
        {
            return constraints;
        }
        public ServiceInstrumenter setConstraints(String[] constraints)
        {
            this.constraints = constraints;
            return this;
        }
        @Override
        public void openInEditor()
        {
            getReference().openInEditor();
        }
    }
    
    public static class ServiceDefinition
    {
        private final Set<String> markers = new HashSet<String>();
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
        public Set<String> getMarkers()
        {
            return markers;
        }
        public ServiceDefinition addMarker(String className)
        {
            markers.add(className);
            return this;
        }
        public ServiceDefinition addMarkers(Collection<String> markers)
        {
            this.markers.addAll(markers);
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
    
    @Override
    public void openInEditor()
    {
        getReference().openInEditor();
    }
}