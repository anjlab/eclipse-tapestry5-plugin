package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.anjlab.eclipse.tapestry5.AssetException;
import com.anjlab.eclipse.tapestry5.AssetReference;
import com.anjlab.eclipse.tapestry5.ClassFile;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryModule;

public class ViewLabelProvider extends LabelProvider
{
    @Override
    public String getText(Object obj)
    {
        return obj.toString();
    }

    @Override
    public Image getImage(Object obj)
    {
        return getImageDescriptor(obj).createImage();
    }

    public ImageDescriptor getImageDescriptor(Object obj)
    {
        if (obj instanceof TreeObject)
        {
            Object data = ((TreeObject) obj).getData();
            
            if (data instanceof TapestryFile)
            {
                return getImageDescriptor((TapestryFile) data);
            }
            else if (data instanceof TapestryModule)
            {
                TapestryFile moduleFile = ((TapestryModule) data).getModuleFile();
                
                if (moduleFile != null)
                {
                    return getImageDescriptor(moduleFile);
                }
            }
        }
        
        if (obj instanceof TreeParent)
        {
            return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
        }
        
        return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE);
    }

    @SuppressWarnings("restriction")
    private ImageDescriptor getImageDescriptor(TapestryFile file)
    {
        ImageDescriptor imageDesc = PlatformUI.getWorkbench().getEditorRegistry()
                .getImageDescriptor(file.getName());
        
        ImageDescriptor[] overlays = null;
        
        if (file instanceof AssetReference)
        {
            try
            {
                ((AssetReference) file).resolveFile(false);
                
                overlays = new ImageDescriptor[0];
            }
            catch (AssetException e)
            {
                overlays = new ImageDescriptor[]
                {
                    org.eclipse.jdt.internal.ui.JavaPluginImages.DESC_OVR_WARNING
                };
            }
        }
        
        if (file instanceof ClassFile)
        {
            try
            {
                if (((ClassFile) file).getClassFile().getSource() == null)
                {
                    overlays = new ImageDescriptor[]
                    {
                        org.eclipse.jdt.internal.ui.JavaPluginImages.DESC_OVR_WARNING
                    };
                }
            }
            catch (JavaModelException e)
            {
                //  Ignore
            }
        }
        
        if (overlays != null)
        {
            DecorationOverlayIcon overlayIcon = new DecorationOverlayIcon(imageDesc.createImage(), overlays);
            
            return overlayIcon;
        }
        
        return imageDesc;
    }
}