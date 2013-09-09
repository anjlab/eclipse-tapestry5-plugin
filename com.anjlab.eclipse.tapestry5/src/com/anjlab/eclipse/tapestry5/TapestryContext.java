package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.anjlab.eclipse.tapestry5.TapestryUtils.FileNameBuilder;

public class TapestryContext
{
    private List<IFile> files;
    
    public TapestryContext(IFile file)
    {
        this.files = new ArrayList<IFile>();
        
        if (TapestryUtils.isJavaFile(file) || TapestryUtils.isTemplateFile(file))
        {
            initFromJavaOrTemplateFile(file);
        }
        else if (TapestryUtils.isPropertiesFile(file))
        {
            initFromPropertiesFile(file);
        }
        
        IFile javaFile = getJavaFile();
        
        if (javaFile != null)
        {
            try
            {
                ICompilationUnit compilationUnit = (ICompilationUnit) JavaCore.create(javaFile);
                
                for (IType type : compilationUnit.getAllTypes())
                {
                    for (IAnnotation annotation : type.getAnnotations())
                    {
                        if ("org.apache.tapestry5.annotations.Import".equals(annotation.getElementName())
                                || "Import".equals(annotation.getElementName()))
                        {
                            IMemberValuePair[] pairs = annotation.getMemberValuePairs();
                            for (IMemberValuePair pair : pairs)
                            {
                                if ("library".equals(pair.getMemberName())
                                        || "stylesheet".equals(pair.getMemberName()))
                                {
                                    processImport(annotation, pair.getMemberName(), pair.getValue());
                                }
                            }
                        }
                    }
                }
            }
            catch (JavaModelException e)
            {
                Activator.getDefault().logError("Error inspecting compilation unit", e);
            }
        }
    }
    
    private void processImport(IAnnotation annotation, String type, Object value)
    {
        if (value instanceof Object[])
        {
            for (Object item : (Object[])value)
            {
                processImportedFile(annotation, type, (String) item);
            }
        }
        else if (value instanceof String)
        {
            processImportedFile(annotation, type, (String) value);
        }
    }
    
    private void processImportedFile(IAnnotation annotation, String type, String fileName)
    {
        ISourceRange sourceRange = null;
        try
        {
            sourceRange = annotation.getSourceRange();
        }
        catch (JavaModelException e)
        {
            Activator.getDefault().logError("Error getting annotation location", e);
        }
        files.add(new AssetPath(getJavaFile(), sourceRange, fileName));
    }
    
    public List<IFile> getFiles()
    {
        return Collections.unmodifiableList(files);
    }
    
    public IFile getJavaFile()
    {
        for (IFile file : files)
        {
            if (TapestryUtils.isJavaFile(file))
            {
                return file;
            }
        }
        return null;
    }
    
    private void initFromPropertiesFile(IFile file)
    {
        List<IFile> files = TapestryUtils.findTapestryFiles(file, true, new FileNameBuilder()
        {
            @Override
            public String getFileName(String fileName, String fileExtension)
            {
                Matcher matcher = getLocalizedPropertiesPattern().matcher(fileName);
                if (matcher.find())
                {
                    return matcher.group(1) + ".java";
                }
                return fileName.substring(0, fileName.lastIndexOf(fileExtension)) + "java";
            }

        });
        
        if (!files.isEmpty())
        {
            IFile javaFile = files.get(0);
            addWithComplementFile(javaFile);
        }
        
        addPropertiesFiles(file);
    }

    private void initFromJavaOrTemplateFile(IFile file)
    {
        addWithComplementFile(file);
        addPropertiesFiles(file);
    }
    
    private void addWithComplementFile(IFile file)
    {
        this.files.add(file);
        IFile complement = TapestryUtils.findComplementFile(file);
        if (complement != null)
        {
            this.files.add(complement);
        }
    }
    
    private void addPropertiesFiles(IFile file)
    {
        List<IFile> propertiesFiles = TapestryUtils.findTapestryFiles(file, false, new FileNameBuilder()
        {
            @Override
            public String getFileName(String fileName, String fileExtension)
            {
                Matcher matcher = getLocalizedPropertiesPattern().matcher(fileName);
                if (matcher.find())
                {
                    return matcher.group(1) + ".*\\.properties";
                }
                return fileName.substring(0, fileName.lastIndexOf(fileExtension) - 1) + ".*\\.properties";
            }
        });
        
        for (IFile properties : propertiesFiles)
        {
            this.files.add(properties);
        }
    }

    private Pattern getLocalizedPropertiesPattern()
    {
        return Pattern.compile("([^_]*)(_.*)+\\.properties");
    }

    public boolean contains(IFile file)
    {
        if (file == null)
        {
            return false;
        }
        
        for (IFile f : files)
        {
            if (f.equals(file))
            {
                return true;
            }
            if (f instanceof AssetPath && !(file instanceof AssetPath))
            {
                try
                {
                    IFile resolvedAsset = ((AssetPath) f).resolveFile(false);
                    
                    if (resolvedAsset.equals(file))
                    {
                        return true;
                    }
                }
                catch (AssetException e)
                {
                    //  Ignore
                }
            }
        }
        return false;
    }

    public void validate()
    {
        for (IFile file : files)
        {
            if (file instanceof AssetPath)
            {
                try
                {
                    ((AssetPath) file).resolveFile(true);
                }
                catch (AssetException e)
                {
                    //  Ignore
                }
            }
        }
    }
    
    public void deleteMarkers()
    {
        deleteMarkers(getProject());
    }
    
    public static void deleteMarkers(IResource project)
    {
        try
        {
            IMarker[] markers = project.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
            
            for (IMarker marker : markers)
            {
                if (marker.getAttribute(AssetPath.ASSET_PATH_MARKER_ATTRIBUTE) != null)
                {
                    marker.delete();
                }
            }
        }
        catch (CoreException e)
        {
            Activator.getDefault().logError("Error deleting asset problem markers", e);
        }
    }

    public IProject getProject()
    {
        for (IFile file : files)
        {
            if (!(file instanceof AssetPath))
            {
                return file.getProject();
            }
        }
        return null;
    }

}