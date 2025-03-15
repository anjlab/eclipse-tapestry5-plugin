package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;

import com.anjlab.eclipse.tapestry5.internal.CompilationUnitContext.CompilationUnitLifecycle;

public class JarTapestryContext extends TapestryContext
{
    public JarTapestryContext(IJarEntryResource jarEntry)
    {
        initFromFile(new JarEntryFile(this, jarEntry));
    }

    public JarTapestryContext(IOrdinaryClassFile classFile)
    {
        initFromFile(new ClassFile(this, classFile));
    }

    @Override
    protected CompilationUnitLifecycle getCompilationUnit()
    {
        return new CompilationUnitLifecycle()
        {
            @Override
            public ICompilationUnit createCompilationUnit()
            {
                TapestryFile javaFile = getJavaFile();
                
                if (javaFile instanceof ClassFile && javaFile.exists())
                {
                    IClassFile classFile = ((ClassFile) javaFile).getClassFile();
                    
                    try
                    {
                        return classFile.getWorkingCopy(new WorkingCopyOwner() { }, new NullProgressMonitor());
                    }
                    catch (JavaModelException e)
                    {
                        Activator.getDefault().logError("Error getting compilation unit", e);
                    }
                }
                
                return null;
            }
            
            @Override
            public void dispose(ICompilationUnit compilationUnit)
            {
                try
                {
                    compilationUnit.discardWorkingCopy();
                }
                catch (JavaModelException e)
                {
                    //  Ignore
                }
            }
        };
    }
    
    @Override
    public String getPackageName()
    {
        TapestryFile javaFile = getJavaFile();
        
        return (javaFile instanceof ClassFile)
             ? ((ClassFile) javaFile).getClassFile().getParent().getElementName()
             : "unknown";
    }

    @Override
    public List<TapestryFile> findTapestryFiles(TapestryFile forFile, boolean findFirst, FileNameBuilder fileNameBuilder)
    {
        List<TapestryFile> files = new ArrayList<TapestryFile>();
        
        IPackageFragment pkg = null;
        
        if (forFile instanceof ClassFile)
        {
            IJavaElement parent = ((ClassFile) forFile).getClassFile().getParent();
            
            if (parent instanceof IPackageFragment)
            {
                pkg = (IPackageFragment) parent;
            }
        }
        else if (forFile instanceof JarEntryFile)
        {
            Object parent = ((JarEntryFile) forFile).getJarEntry().getParent();
            
            if (parent instanceof IPackageFragment)
            {
                pkg = (IPackageFragment) parent;
            }
        }
        
        if (pkg != null)
        {
            try
            {
                String fileName = fileNameBuilder.getFileName(forFile.getName(), forFile.getFileExtension());
                
                if (fileName.endsWith(".class"))
                {
                    IClassFile classFile = pkg.getClassFile(fileName);
                    
                    if (classFile instanceof IOrdinaryClassFile)
                    {
                        files.add(new ClassFile(this, (IOrdinaryClassFile) classFile));
                        
                        if (findFirst)
                        {
                            return files;
                        }
                    }
                }
                else
                {
                    Object[] resources = pkg.getNonJavaResources();
                    
                    for (Object resource : resources)
                    {
                        if (resource instanceof IJarEntryResource)
                        {
                            IJarEntryResource jarEntry = (IJarEntryResource) resource;
                            
                            boolean matches = false;
                            
                            if (fileName.contains("*"))
                            {
                                matches = Pattern.matches(fileName, jarEntry.getName());
                            }
                            
                            matches = matches || fileName.equals(jarEntry.getName());
                            
                            if (matches)
                            {
                                files.add(new JarEntryFile(this, jarEntry));
                                
                                if (findFirst)
                                {
                                    return files;
                                }
                            }
                        }
                    }
                    
                    TapestryFile file = createLookup().findClasspathFileCaseInsensitive(fileName);
                    
                    if (file != null)
                    {
                        files.add(file);
                        
                        if (findFirst)
                        {
                            return files;
                        }
                    }
                }
            }
            catch (JavaModelException e)
            {
                // Ignore
            }
        }
        return files;
    }

    protected Map<String, String> codeDesignExtensionMappings()
    {
        Map<String, String> result = new HashMap<String, String>();
        result.put("tml", "class");
        result.put("class", "tml");
        return result;
    }

    @Override
    public boolean isReadOnly()
    {
        return true;
    }
    
    private TapestryComponentSpecification specification;
    
    @Override
    public TapestryComponentSpecification getSpecification()
    {
        if (specification == null)
        {
            IType type = getJavaType();
            
            if (type == null)
            {
                return TapestryComponentSpecification.EMPTY;
            }
            
            specification = new TapestryComponentSpecification(type, this);
        }
        
        return specification;
    }
    
    @Override
    public IType getJavaType()
    {
        TapestryFile javaFile = getJavaFile();
        
        if (javaFile == null)
        {
            return null;
        }
        
        IType type = ((ClassFile)javaFile).getClassFile().findPrimaryType();
        
        return type;
    }
    
    @Override
    public FileLookup createLookup()
    {
        return new JarFileLookup(getJavaType());
    }
}
