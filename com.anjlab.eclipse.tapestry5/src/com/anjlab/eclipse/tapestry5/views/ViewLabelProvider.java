package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.anjlab.eclipse.tapestry5.ClassFile;
import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryFileReference;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.UnresolvableReferenceException;

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

    @SuppressWarnings("restriction")
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
            else if (data instanceof JavaScriptStack)
            {
                JavaScriptStack stack = (JavaScriptStack) data;
                
                ImageDescriptor imageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE);
                
                //  isOverridden will be set only for objects that came from TapestryProject,
                //  which is the case for TapestryProjectOutlineView - we only need to mark overridden stacks for this view
                if (stack.isOverridden())
                {
                    return getImageDescriptor(
                                imageDescriptor,
                                org.eclipse.jdt.internal.ui.JavaPluginImages.DESC_OVR_DEPRECATED);
                }
                else if (stack.isOverrides())
                {
                    return getImageDescriptor(
                            imageDescriptor,
                            org.eclipse.jdt.internal.ui.JavaPluginImages.DESC_OVR_OVERRIDES);
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
        
        if (file instanceof TapestryFileReference)
        {
            try
            {
                ((TapestryFileReference) file).resolveFile(false);
                
                overlays = new ImageDescriptor[0];
            }
            catch (UnresolvableReferenceException e)
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
        
        return getImageDescriptor(imageDesc, overlays);
    }

    private ImageDescriptor getImageDescriptor(ImageDescriptor imageDesc, ImageDescriptor... overlays)
    {
        if (overlays != null)
        {
            DecorationOverlayIcon overlayIcon = new DecorationOverlayIcon(imageDesc.createImage(), overlays);
            
            return overlayIcon;
        }
        
        return imageDesc;
    }
}