package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.anjlab.eclipse.tapestry5.AssetException;
import com.anjlab.eclipse.tapestry5.AssetPath;

public class ViewLabelProvider extends LabelProvider
{
    @Override
    public String getText(Object obj)
    {
        return obj.toString();
    }

    @SuppressWarnings("restriction")
    @Override
    public Image getImage(Object obj)
    {
        if (obj instanceof TreeParent)
        {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
        }
        
        if (obj instanceof TreeObject)
        {
            Object data = ((TreeObject) obj).getData();
            
            if (data instanceof IFile)
            {
                Image image = PlatformUI.getWorkbench().getEditorRegistry()
                        .getImageDescriptor(((IFile) data).getName())
                        .createImage();
                
                if (data instanceof AssetPath)
                {
                    ImageDescriptor[] overlays;
                    
                    try
                    {
                        ((AssetPath) data).resolveFile(false);
                        
                        overlays = new ImageDescriptor[0];
                    }
                    catch (AssetException e)
                    {
                        overlays = new ImageDescriptor[]
                        {
                            org.eclipse.jdt.internal.ui.JavaPluginImages.DESC_OVR_WARNING
                        };
                    }
                    
                    DecorationOverlayIcon overlayIcon = new DecorationOverlayIcon(image, overlays);
                    
                    return overlayIcon.createImage();
                }
                
                return image;
            }
        }
        
        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
    }
}