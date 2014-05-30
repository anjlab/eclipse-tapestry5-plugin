package com.anjlab.eclipse.tapestry5;

import static com.anjlab.eclipse.tapestry5.EclipseUtils.evalExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.text.javadoc.JavadocContentAccess2;

@SuppressWarnings("restriction")
public class TapestryComponentSpecification
{
    private TapestryContext tapestryContext;
    
    private List<Property> properties = new ArrayList<Property>();
    private List<Parameter> parameters = new ArrayList<Parameter>();
    private List<Component> components = new ArrayList<Component>();
    
    public static TapestryComponentSpecification EMPTY = new TapestryComponentSpecification(null, null);
    
    public TapestryComponentSpecification(IType type, TapestryContext tapestryContext)
    {
        if (type == null)
        {
            //  Empty specification
            return;
        }
        
        this.tapestryContext = tapestryContext;
        
        try
        {
            findAllProperties(type);
            findAllParameters(type);
            findAllEmbeddedComponents(type);
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error reading component fields", e);
        }
    }

    public TapestryContext getTapestryContext()
    {
        return tapestryContext;
    }
    
    private void findAllEmbeddedComponents(IType type) throws JavaModelException
    {
        findEmbeddedComponents(type);
        IType superclass = findSuperclass(type);
        if (superclass != null)
        {
            findEmbeddedComponents(superclass);
        }
    }

    private void findAllProperties(IType type) throws JavaModelException
    {
        findProperties(type);
        IType superclass = findSuperclass(type);
        if (superclass != null)
        {
            findProperties(superclass);
        }
    }

    private void findAllParameters(IType type) throws JavaModelException
    {
        findParameters(type);
        IType superclass = findSuperclass(type);
        if (superclass != null)
        {
            findParameters(superclass);
        }
    }

    protected IType findSuperclass(IType type) throws JavaModelException
    {
        String superclassName = type.getSuperclassName();
        if (superclassName == null)
        {
            //  Return "java.lang.Object"?
            return null;
        }
        
        if (!superclassName.contains("."))
        {
            ICompilationUnit compilationUnit = type.getCompilationUnit();
            if (compilationUnit != null)
            {
                IImportDeclaration[] imports = compilationUnit.getImports();
                if (imports.length > 0)
                {
                    for (IImportDeclaration importDecl : imports)
                    {
                        if (importDecl.getElementName().endsWith("." + superclassName))
                        {
                            superclassName = importDecl.getElementName();
                            break;
                        }
                    }
                }
                else
                {
                    //  Superclass is from the same package
                    superclassName = type.getPackageFragment().getElementName() + "." + superclassName;
                }
            }
        }
        
        IType superclass = EclipseUtils.findTypeDeclaration(type.getJavaProject().getProject(), superclassName);
        return superclass;
    }

    private interface AnnotatedFieldHandler
    {
        void handle(IType type, IField field, IAnnotation annotation) throws JavaModelException;
    }
    
    private void findEmbeddedComponents(IType type) throws JavaModelException
    {
        handleAnnotatedFields(type, "org.apache.tapestry5.annotations.Component", new AnnotatedFieldHandler()
        {
            @Override
            public void handle(IType type, IField field, IAnnotation annotation) throws JavaModelException
            {
                components.add(createComponent(type, field, annotation));
            }
        });
        handleAnnotatedFields(type, "org.apache.tapestry5.annotations.InjectComponent", new AnnotatedFieldHandler()
        {
            @Override
            public void handle(IType type, IField field, IAnnotation annotation) throws JavaModelException
            {
                
                Component newComponent = createComponentFromInjection(type, field, annotation);
                
                //  Find if there's any components already added with this ID, maybe via @Component annotation
                
                //  TODO Lookup component by name more efficiently
                for (Component component : components)
                {
                    if (StringUtils.equalsIgnoreCase(component.getId(), newComponent.getId()))
                    {
                        return;
                    }
                }
                
                components.add(newComponent);
            }
        });
    }

    private Component createComponentFromInjection(IType type, IField field, IAnnotation annotation) throws JavaModelException
    {
        Component component = new Component();
        component.setSpecification(this);
        component.setName(field.getElementName());
        component.setNameRange(field.getNameRange());
        component.setJavadoc(JavadocContentAccess2.getHTMLContent(field, true));
        
        for (IMemberValuePair pair : annotation.getMemberValuePairs())
        {
            component.setId(String.valueOf(pair.getValue()));
        }
        
        setComponentDefaults(type, field, component);
        
        return component;
    }

    protected Component createComponent(IType type, IField field, IAnnotation annotation) throws JavaModelException
    {
        Component component = new Component();
        component.setSpecification(this);
        component.setName(field.getElementName());
        component.setNameRange(field.getNameRange());
        component.setJavadoc(JavadocContentAccess2.getHTMLContent(field, true));
        
        for (IMemberValuePair pair : annotation.getMemberValuePairs())
        {
            if ("id".equals(pair.getMemberName()))
            {
                component.setId(String.valueOf(pair.getValue()));
            }
            else if ("type".equals(pair.getMemberName()))
            {
                component.setType(String.valueOf(pair.getValue()));
            }
            else if ("inheritInformalParameters".equals(pair.getMemberName()))
            {
                component.setInheritInformalParameters("true".equals(String.valueOf(pair.getValue())));
            }
            else if ("publishParameters".equals(pair.getMemberName()))
            {
                component.setPublishParameters(String.valueOf(pair.getValue()));
            }
            else if ("parameters".equals(pair.getMemberName()))
            {
                if (pair.getValue() instanceof Object[])
                {
                    Object[] values = (Object[]) pair.getValue();
                    String[] parameters = new String[values.length];
                    for (int i = 0; i < values.length; i++)
                    {
                        parameters[i] = String.valueOf(values[i]);
                    }
                    component.setParameters(parameters);
                }
                else if (pair.getValue() instanceof String)
                {
                    component.setParameters(new String[] { (String) pair.getValue() });
                }
            }
        }
        
        setComponentDefaults(type, field, component);
        
        //  TODO See if there's any @Mixins or @MixinClasses attached to this field
        
        return component;
    }

    private void setComponentDefaults(IType type, IField field, Component component) throws JavaModelException
    {
        if (StringUtils.isEmpty(component.getId()))
        {
            component.setId(component.getName());
        }
        if (StringUtils.isEmpty(component.getType()))
        {
            String typeName = Signature.toString(field.getTypeSignature());
            
            if (field.isBinary())
            {
                component.setType(typeName);
            }
            else
            {
                String[][] resolvedTypes = type.resolveType(typeName);
                
                if (resolvedTypes == null)
                {
                    component.setType(typeName);
                }
                else
                {
                    component.setType(resolvedTypes[0][0] + "." + resolvedTypes[0][1]);
                }
            }
            component.setJavaType(true);
        }
    }
    
    private void findParameters(IType type) throws JavaModelException
    {
        handleAnnotatedFields(type, "org.apache.tapestry5.annotations.Parameter", new AnnotatedFieldHandler()
        {
            @Override
            public void handle(IType type, IField field, IAnnotation annotation) throws JavaModelException
            {
                parameters.add(createParameter(type, field, annotation));
            }
        });
    }

    protected void handleAnnotatedFields(IType type, String annotationFQName, AnnotatedFieldHandler handler) throws JavaModelException
    {
        String annotationPackage = annotationFQName.substring(0, annotationFQName.lastIndexOf('.'));
        String annotationSimpleName = annotationFQName.substring(annotationFQName.lastIndexOf('.') + 1);
        
        boolean annotationInImports = false;
        
        ICompilationUnit compilationUnit = type.getCompilationUnit();
        
        if (compilationUnit != null)
        {
            for (IImportDeclaration importDecl : compilationUnit.getImports())
            {
                annotationInImports =
                        importDecl.getElementName().equals(annotationFQName)
                        || importDecl.getElementName().equals(annotationPackage + ".*");
                
                if (annotationInImports)
                {
                    break;
                }
            }
        }
        
        for (IField field : type.getFields())
        {
            for (IAnnotation annotation : field.getAnnotations())
            {
                if (annotation.getElementName().equals(annotationFQName)
                    || (annotation.getElementName().equals(annotationSimpleName) && annotationInImports))
                {
                    handler.handle(type, field, annotation);
                }
            }
        }
    }

    protected Parameter createParameter(IType type, IField field, IAnnotation annotation) throws JavaModelException
    {
        Parameter parameter = new Parameter();
        parameter.setSpecification(this);
        parameter.setName(field.getElementName());
        parameter.setNameRange(field.getNameRange());
        parameter.setJavadoc(JavadocContentAccess2.getHTMLContent(field, true));
        
        for (IMemberValuePair pair : annotation.getMemberValuePairs())
        {
            if ("value".equals(pair.getMemberName()))
            {
                parameter.setValue(String.valueOf(pair.getValue()));
            }
            else if ("required".equals(pair.getMemberName()))
            {
                parameter.setRequired("true".equals(String.valueOf(pair.getValue())));
            }
            else if ("principal".equals(pair.getMemberName()))
            {
                parameter.setPrincipal("true".equals(String.valueOf(pair.getValue())));
            }
            else if ("name".equals(pair.getMemberName()))
            {
                parameter.setName(String.valueOf(pair.getValue()));
            }
            else if ("defaultPrefix".equals(pair.getMemberName()))
            {
                parameter.setDefaultPrefix(
                        evalExpression(type.getJavaProject().getProject(), pair.getValue()));
            }
            else if ("cache".equals(pair.getMemberName()))
            {
                parameter.setCache("true".equals(String.valueOf(pair.getValue())));
            }
            else if ("autoconnect".equals(pair.getMemberName()))
            {
                parameter.setAutoconnect("true".equals(String.valueOf(pair.getValue())));
            }
            else if ("allowNull".equals(pair.getMemberName()))
            {
                parameter.setAllowNull("true".equals(String.valueOf(pair.getValue())));
            }
        }
        
        return parameter;
    }

    private void findProperties(IType type) throws JavaModelException
    {
        handleAnnotatedFields(type, "org.apache.tapestry5.annotations.Property", new AnnotatedFieldHandler()
        {
            @Override
            public void handle(IType type, IField field, IAnnotation annotation) throws JavaModelException
            {
                properties.add(createProperty(type, field, annotation));
            }
        });
        
        for (IMethod method : type.getMethods())
        {
            Property property;
            
            property = findProperty(method, "get");
            if (property != null)
            {
                property.setRead(true);
                continue;
            }
            
            property = findProperty(method, "is");
            if (property != null)
            {
                property.setRead(true);
                continue;
            }
            
            property = findProperty(method, "set");
            if (property != null)
            {
                property.setWrite(true);
                continue;
            }
        }
    }
    
    private Property findProperty(IMethod method, String prefixName) throws JavaModelException
    {
        Property property = null;
        
        if (method.getElementName().startsWith(prefixName))
        {
            if (method.getElementName().length() <= prefixName.length())
            {
                return null;
            }
            
            String propertyName = method.getElementName().substring(prefixName.length());
            
            propertyName = Character.toLowerCase(propertyName.charAt(0))
                         + (propertyName.length() == 1 ? "" : propertyName.substring(1));
            
            property = getProperty(propertyName);
            if (property == null)
            {
                property = createProperty(method, propertyName);
                properties.add(property);
            }
        }
        
        return property;
    }

    private Property getProperty(String propertyName)
    {
        for (Property property : properties)
        {
            if (propertyName.equals(property.getName()))
            {
                return property;
            }
        }
        return null;
    }

    protected Property createProperty(IType type, IField field, IAnnotation annotation) throws JavaModelException
    {
        Property property = createProperty(field, field.getElementName());
        
        for (IMemberValuePair pair : annotation.getMemberValuePairs())
        {
            if ("read".equals(pair.getMemberName()))
            {
                property.setRead("true".equals(String.valueOf(pair.getValue())));
            }
            else if ("write".equals(pair.getMemberName()))
            {
                property.setWrite("true".equals(String.valueOf(pair.getValue())));
            }
        }
        
        return property;
    }

    private Property createProperty(IMember member, String name) throws JavaModelException
    {
        Property property = new Property();
        property.setSpecification(this);
        property.setName(name);
        property.setNameRange(member.getNameRange());
        property.setJavadoc(JavadocContentAccess2.getHTMLContent(member, true));
        return property;
    }
    
    public List<Component> getComponents()
    {
        return Collections.unmodifiableList(components);
    }
    
    public List<Parameter> getParameters()
    {
        return Collections.unmodifiableList(parameters);
    }
    
    /**
     * TODO Optimize this heavy method
     * 
     * TODO Support a list of applied t:mixins,
     * probably better implement t:mixins support on a higher level than component specification
     * 
     * @param tapestryProject
     * @return parameters of this component including published parameters of its embedded components
     */
    public List<Parameter> getParameters(TapestryProject tapestryProject)
    {
        //  TODO Cache results somehow?
        
        List<Parameter> allParameters = new ArrayList<Parameter>();
        
        allParameters.addAll(parameters);
        
        for (Component component : components)
        {
            if (StringUtils.isEmpty(component.getPublishParameters()))
            {
                continue;
            }
            
            String componentName = TapestryUtils.getComponentName(tapestryProject, component);
            
            if (componentName != null)
            {
                try
                {
                    TapestryContext componentContext = tapestryProject.findComponentContext(componentName);
                    
                    TapestryComponentSpecification componentSpecification = componentContext.getSpecification();
                    
                    List<Parameter> componentParameters = componentSpecification.getParameters();
                    
                    String[] publishedParameters = component.getPublishParameters().split(",");
                    
                    for (String publishedParameter : publishedParameters)
                    {
                        for (Parameter parameter : componentParameters)
                        {
                            if (StringUtils.equals(publishedParameter.trim(), parameter.getName()))
                            {
                                allParameters.add(parameter);
                                break;
                            }
                        }
                    }
                }
                catch (JavaModelException e)
                {
                    //  Ignore
                }
            }
        }
        
        return allParameters;
    }
    
    public List<Property> getProperties()
    {
        return Collections.unmodifiableList(properties);
    }

    public Parameter getParameter(TapestryProject tapestryProject, String name)
    {
        for (Parameter parameter : getParameters(tapestryProject))
        {
            if (StringUtils.equalsIgnoreCase(name, parameter.getName()))
            {
                return parameter;
            }
        }
        return null;
    }
}
