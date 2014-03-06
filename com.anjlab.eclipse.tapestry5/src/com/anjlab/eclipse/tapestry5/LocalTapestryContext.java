package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;


public class LocalTapestryContext extends TapestryContext
{

    public LocalTapestryContext(IFile file)
    {
        super();
        initFromFile(new LocalFile(this, file));
    }

    public LocalTapestryContext(TapestryFile file)
    {
        super();
        initFromFile(file);
    }
    
    @Override
    protected ICompilationUnit getCompilationUnit()
    {
        IFile javaFile = getJavaFileInternal();
        
        if (javaFile == null)
        {
            return null;
        }
        
        return (ICompilationUnit) JavaCore.create(javaFile);
    }

    private IFile getJavaFileInternal()
    {
        TapestryFile javaFile = getJavaFile();
        return javaFile == null ? null : ((LocalFile)javaFile).getFile();
    }
    
    @Override
    public String getPackageName()
    {
        IFile javaFile = getJavaFileInternal();
        
        IContainer root = TapestryUtils.getRoot(javaFile);
        
        if (root != null)
        {
            return TapestryUtils.pathToPackageName(TapestryUtils.getRelativeFileName(javaFile.getParent(), root), false);
        }
        
        LocalFile localTemplateFile = (LocalFile) getTemplateFile();
        
        if (localTemplateFile == null)
        {
            return null;
        }
        
        IFile templateFile = localTemplateFile.getFile();
        
        root = TapestryUtils.getRoot(templateFile);
        
        if (root != null)
        {
            if (TapestryUtils.isWebApp(root))
            {
                //  Page from web context
                return TapestryUtils.getTapestryPackage(getProject(), "pages" + 
                        TapestryUtils.pathToPackageName(TapestryUtils.getRelativeFileName(templateFile.getParent(), root), true));
            }
            
            return TapestryUtils.pathToPackageName(TapestryUtils.getRelativeFileName(templateFile.getParent(), root), false);
        }
        
        return null;
    }
    
    @Override
    public List<TapestryFile> findTapestryFiles(TapestryFile forFile, boolean findFirst, FileNameBuilder fileNameBuilder)
    {
        List<IFile> files = findTapestryFiles(((LocalFile) forFile).getFile(), findFirst, fileNameBuilder);
        List<TapestryFile> tapestryFiles = new ArrayList<TapestryFile>();
        for (IFile file : files)
        {
            tapestryFiles.add(new LocalFile(this, file));
        }
        return tapestryFiles;
    }

    @Override
    protected Map<String, String> codeDesignExtensionMappings()
    {
        Map<String, String> result = new HashMap<String, String>();
        result.put("tml", "java");
        result.put("java", "tml");
        return result;
    }

    private List<IFile> findTapestryFiles(IFile forFile, boolean findFirst, FileNameBuilder fileNameBuilder)
    {
        try
        {
            String complementFileName = null;
            
            //  Check if the file is in the web application context
            
            boolean fromWebapp = false;
            
            IProject project = forFile.getProject();
            
            IContainer webapp = TapestryUtils.findWebapp(project);
            
            if (fromWebapp = TapestryUtils.isInFolder(forFile, webapp))
            {
                if (isWebappContextAValidLocationForTheFile(forFile))
                {
                    String relativeFileName = TapestryUtils.getRelativeFileName(forFile, webapp);
                    
                    complementFileName = fileNameBuilder.getFileName(
                            TapestryUtils.joinPath(TapestryUtils.getPagesPath(project), relativeFileName),
                            forFile.getFileExtension());
                }
            }
            
            List<IFile> resources = new ArrayList<IFile>();
            
            if (!fromWebapp)
            {
                complementFileName = fileNameBuilder.getFileName(forFile.getName(), forFile.getFileExtension());
                
                //  Try searching in the same folder first
                
                resources = findMembers(forFile.getParent(), complementFileName);
                
                if (findFirst && !resources.isEmpty())
                {
                    return resources;
                }
            }
            
            //  Look in the source folders
            
            IJavaProject javaProject = JavaCore.create(forFile.getProject());
            
            IContainer container = null;
            
            if (!fromWebapp)
            {
                IContainer adaptedProject = (IContainer) javaProject.getCorrespondingResource().getAdapter(IContainer.class);
                
                IResource adaptedFile = adaptedProject.findMember(forFile.getProjectRelativePath());
                
                if (adaptedFile == null)
                {
                    return resources;
                }
                
                container = adaptedFile.getParent();
                
                while (container != null && !EclipseUtils.isSourceFolder(container))
                {
                    container = container.getParent();
                }
                
                if (container != null)
                {
                    //  Get the file name relative to source folder
                    String relativeFileName = TapestryUtils.getRelativeFileName(forFile, container);
                    
                    complementFileName = fileNameBuilder.getFileName(relativeFileName, forFile.getFileExtension());
                }
            }
            
            if (StringUtils.isEmpty(complementFileName))
            {
                return Collections.emptyList();
            }
            
            for (IPackageFragmentRoot root : javaProject.getAllPackageFragmentRoots())
            {
                if (!EclipseUtils.isSourceFolder(root))
                {
                    continue;
                }
                
                IContainer resourceContainer = (IContainer) root.getCorrespondingResource().getAdapter(IContainer.class);
                
                if (container != null && resourceContainer.getFullPath().equals(container.getFullPath()))
                {
                    continue;
                }
                
                List<IFile> resources2 = findMembers(resourceContainer, complementFileName);
                
                if (findFirst && !resources2.isEmpty())
                {
                    return resources2;
                }
                
                resources.addAll(resources2);
            }
            
            //  Look for TML files in web application context
            //  https://github.com/anjlab/eclipse-tapestry5-plugin/issues/2
            
            if (complementFileName.endsWith(".tml"))
            {
                if (webapp != null)
                {
                    String pagesPath = TapestryUtils.getPagesPath(project);
                    
                    if (complementFileName.length() > pagesPath.length())
                    {
                        IResource file = webapp.findMember(complementFileName.substring(pagesPath.length()));
                        
                        if (file instanceof IFile)
                        {
                            resources.add((IFile) file);
                        }
                    }
                }
            }
            
            return resources;
        }
        catch (CoreException e)
        {
            Activator.getDefault().logError("Error finding complement file", e);
            
            return Collections.emptyList();
        }
    }

    private boolean isWebappContextAValidLocationForTheFile(IFile file)
    {
        return TapestryUtils.isTemplateFile(file.getProjectRelativePath())
            || TapestryUtils.isStyleSheetFile(file.getProjectRelativePath())
            || TapestryUtils.isJavaScriptFile(file.getProjectRelativePath());
    }
    
    private List<IFile> findMembers(IContainer container, String path)
    {
        List<IFile> resources = new ArrayList<IFile>();
        
        if (path.contains("*"))
        {
            //  Find files by mask
            int slashIndex = path.lastIndexOf("/");
            
            String parentPath = slashIndex < 0 ? "/" : path.substring(0, slashIndex);
            String mask = slashIndex < 0 ? path : path.substring(slashIndex + 1);
            
            Pattern pattern = Pattern.compile(mask);
            
            IResource resource = container.findMember(parentPath);
            
            if (resource instanceof IFolder)
            {
                try
                {
                    IResource[] members = ((IFolder) resource).members();
                    
                    for (IResource member : members)
                    {
                        if (pattern.matcher(member.getName()).matches())
                        {
                            resources.add((IFile) member);
                        }
                    }
                }
                catch (CoreException e)
                {
                    Activator.getDefault().logError("Error finding files by mask", e);
                }
            }
        }
        else
        {
            //  Exact match
            IResource resource = container.findMember(path);
            if (resource != null && resource instanceof IFile)
            {
                resources.add((IFile) resource);
            }
        }
        return resources;
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }
    
    @Override
    public TapestryComponentSpecification getSpecification()
    {
        TapestryFile javaFile = getJavaFile();
        
        if (javaFile == null)
        {
            return TapestryComponentSpecification.EMPTY;
        }
        
        IFile file = ((LocalFile)javaFile).getFile();
        
        ICompilationUnit compilationUnit = (ICompilationUnit) JavaCore.create(file);
        
        IType type = compilationUnit.findPrimaryType();
        
        if (type == null)
        {
            return TapestryComponentSpecification.EMPTY;
        }
        
        return new TapestryComponentSpecification(type);
    }
}
