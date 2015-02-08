package com.anjlab.eclipse.tapestry5.internal.visitors;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.DeclarationReference.ASTNodeReference;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.ObjectCallback;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

public class LibraryMappingCapturingVisitor extends ASTVisitor
{
    private final IProgressMonitor monitor;
    private final TapestryModule tapestryModule;
    private final ObjectCallback<LibraryMapping, RuntimeException> libraryMappingFound;

    public LibraryMappingCapturingVisitor(IProgressMonitor monitor, TapestryModule tapestryModule,
            ObjectCallback<LibraryMapping, RuntimeException> libraryMappingFound)
    {
        this.monitor = monitor;
        this.tapestryModule = tapestryModule;
        this.libraryMappingFound = libraryMappingFound;
    }

    @Override
    public boolean visit(MethodInvocation node)
    {
        if (monitor.isCanceled())
        {
            return false;
        }
        
        if (Arrays.binarySearch(TapestryModule.ADD_OVERRIDE, node.getName().toString()) < 0
                || node.arguments().size() != 1)
        {
            return super.visit(node);
        }
        
        Object arg = node.arguments().get(0);
        
        if (!(arg instanceof ClassInstanceCreation))
        {
            return super.visit(node);
        }
        
        ClassInstanceCreation creation = (ClassInstanceCreation) arg;
        
        Type creationType = creation.getType();
        
        if (creationType.isSimpleType())
        {
            Name name = ((SimpleType) creationType).getName();
            
            if (name.isSimpleName())
            {
                if ("LibraryMapping".equals(((SimpleName) name).getIdentifier())
                        && creation.arguments().size() == 2)
                {
                    Object prefixExpr = creation.arguments().get(0);
                    Object packageExpr = creation.arguments().get(1);
                    
                    String prefix = EclipseUtils.evalExpression(tapestryModule.getEclipseProject(), prefixExpr);
                    String pkg = "".equals(prefix)
                               ? TapestryUtils.getAppPackage(tapestryModule.getEclipseProject())
                               : EclipseUtils.evalExpression(tapestryModule.getEclipseProject(), packageExpr);
                    
                    if (prefix != null && !StringUtils.isEmpty(pkg))
                    {
                        libraryMappingFound.callback(
                                new LibraryMapping(prefix, pkg,
                                        new ASTNodeReference(tapestryModule, tapestryModule.getModuleClass(), node)));
                    }
                    else
                    {
                        Activator.getDefault().logWarning(
                                "Unable to evaluate LibraryMapping(" + prefixExpr + ", " + packageExpr + ")");
                    }
                }
            }
        }
        
        return super.visit(node);
    }
}