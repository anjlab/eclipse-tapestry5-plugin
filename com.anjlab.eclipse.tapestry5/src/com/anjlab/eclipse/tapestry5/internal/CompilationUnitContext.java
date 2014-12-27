package com.anjlab.eclipse.tapestry5.internal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ui.services.IDisposable;

import com.anjlab.eclipse.tapestry5.EclipseUtils;

public class CompilationUnitContext implements IDisposable
{
    public static abstract class CompilationUnitLifecycle
    {
        public abstract ICompilationUnit createCompilationUnit();
        
        public void dispose(ICompilationUnit compilationUnit)
        {
            //  Default implementation does nothing
        }
    }
    
    public static interface ContextRunnable
    {
        void run (CompilationUnitContext context) throws JavaModelException;
    }
    
    private final CompilationUnitLifecycle lifecycle;
    private ICompilationUnit compilationUnit;
    private boolean cuCreated;
    private CompilationUnit parsedUnit;
    private boolean cuParsed;
    
    public CompilationUnitContext(CompilationUnitLifecycle lifecycle)
    {
        this.lifecycle = lifecycle;
    }
    
    public ICompilationUnit getCompilationUnit()
    {
        if (!cuCreated)
        {
            cuCreated = true;
            
            compilationUnit = lifecycle.createCompilationUnit();
        }
        
        return compilationUnit;
    }
    
    private CompilationUnit getParsedUnit()
    {
        if (!cuParsed)
        {
            cuParsed = true;
            try
            {
                parsedUnit = EclipseUtils.parse(compilationUnit);
            }
            catch (Exception e)
            {
                //  Ignore
            }
        }
        
        return parsedUnit;
    }
    
    public void accept(ASTVisitor visitor)
    {
        CompilationUnit unit = getParsedUnit();
        if (unit != null)
        {
            unit.accept(visitor);
        }
    }
    
    public AST getAST()
    {
        CompilationUnit unit = getParsedUnit();
        return unit == null ? null : unit.getAST();
    }
    
    @Override
    public final void dispose()
    {
        if (compilationUnit != null)
        {
            lifecycle.dispose(compilationUnit);
        }
    }
}