package com.anjlab.eclipse.tapestry5;

import static com.anjlab.eclipse.tapestry5.EclipseUtils.evalExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.text.javadoc.JavadocContentAccess2;

@SuppressWarnings("restriction")
public class TapestryComponentSpecification
{
    private List<Property> properties = new ArrayList<Property>();
    private List<Parameter> parameters = new ArrayList<Parameter>();
    
    public static TapestryComponentSpecification EMPTY = new TapestryComponentSpecification(null);
    
    public TapestryComponentSpecification(IType type)
    {
        if (type == null)
        {
            //  Empty specification
            return;
        }
        
        try
        {
            findAllProperties(type);
            findAllParameters(type);
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error reading component fields", e);
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
        parameter.setName(field.getElementName());
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
        property.setName(name);
        property.setJavadoc(JavadocContentAccess2.getHTMLContent(member, true));
        return property;
    }
    
    public List<Parameter> getParameters()
    {
        return Collections.unmodifiableList(parameters);
    }

    public List<Property> getProperties()
    {
        return Collections.unmodifiableList(properties);
    }
}
