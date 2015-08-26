package com.anjlab.eclipse.tapestry5.views;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.anjlab.eclipse.tapestry5.ClassFile;
import com.anjlab.eclipse.tapestry5.ClassNameReference;
import com.anjlab.eclipse.tapestry5.DeclarationReference;
import com.anjlab.eclipse.tapestry5.JavaScriptStack;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryFileReference;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryService;
import com.anjlab.eclipse.tapestry5.TapestrySymbol;
import com.anjlab.eclipse.tapestry5.UnresolvableReferenceException;
import com.anjlab.eclipse.tapestry5.views.TreeParent.DataObject;

public class LabelProvider implements IStyledLabelProvider, ILabelProvider
{

    @Override
    public Image getImage(Object obj)
    {
        return getImageDescriptor(obj).createImage();
    }

    @SuppressWarnings("restriction")
    public ImageDescriptor getImageDescriptor(Object element)
    {
        if (element instanceof TapestryFile)
        {
            return getImageDescriptor((TapestryFile) element);
        }
        
        if (element instanceof TreeObject)
        {
            Object data = ((TreeObject) element).getData();
            
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
            else if (data instanceof TapestryService)
            {
                return PlatformUI.getWorkbench().getEditorRegistry()
                        .getImageDescriptor(element.toString());
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
                else if (stack.isOverride())
                {
                    return getImageDescriptor(
                            imageDescriptor,
                            org.eclipse.jdt.internal.ui.JavaPluginImages.DESC_OVR_OVERRIDES);
                }
            }
            else if (data instanceof TapestrySymbol)
            {
                //  TODO Introduce new interface for override/overridden objects, TapestrySymbol & JavaScriptStack will implement it
                
                TapestrySymbol symbol = (TapestrySymbol) data;
                        
                ImageDescriptor imageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE);
                
                if (symbol.isOverridden())
                {
                    return getImageDescriptor(
                                imageDescriptor,
                                org.eclipse.jdt.internal.ui.JavaPluginImages.DESC_OVR_DEPRECATED);
                }
                else if (symbol.isOverride())
                {
                    return getImageDescriptor(
                            imageDescriptor,
                            org.eclipse.jdt.internal.ui.JavaPluginImages.DESC_OVR_OVERRIDES);
                }
            }
        }
        
        if (element instanceof TreeParent)
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
    
    @Override
    public StyledString getStyledText(Object element)
    {
        StyledString styledString = new StyledString(element.toString());
        
        if (element instanceof TreeObject)
        {
            Object data = ((TreeObject) element).getData();
            
            if (data instanceof TapestryModule)
            {
                styledString.append(
                        " " + StringUtils.join(((TapestryModule) data).references(), ", "),
                        StyledString.QUALIFIER_STYLER);
            }
            else if (data instanceof LibraryMapping)
            {
                styledString.append(
                        " " + ((LibraryMapping) data).getRootPackage(),
                        StyledString.DECORATIONS_STYLER);
            }
            else if (data instanceof JavaScriptStack)
            {
                styledString.append(
                        " " + ((JavaScriptStack) data).getType().getFullyQualifiedName(),
                        StyledString.DECORATIONS_STYLER);
            }
            else if (data instanceof TapestrySymbol)
            {
                TapestrySymbol symbol = (TapestrySymbol) data;
                
                styledString.append(
                        " =" + symbol.getValue(),
                        StyledString.DECORATIONS_STYLER);

                styledString.append(
                        ", from " + symbol.getSymbolProvider().getDefinition().getId()
                        + " in " + symbol.getReference().getTapestryModule().getName(),
                        StyledString.QUALIFIER_STYLER);
            }
            else if (data instanceof TapestryService)
            {
                styledString.append(
                        " " + StringUtils.defaultIfEmpty(
                                ((TapestryService) data).getDefinition().getIntfClass(),
                                "No interface"),
                        StyledString.DECORATIONS_STYLER);
            }
            else if (data instanceof DeclarationReference)
            {
                styledString.append(
                        " " + ((DeclarationReference) data).getTapestryModule().getName(),
                        StyledString.DECORATIONS_STYLER);
            }
            else if (data instanceof ClassNameReference)
            {
                styledString.append(
                        " " + ((ClassNameReference) data).getClassName(),
                        StyledString.DECORATIONS_STYLER);
            }
            else if (data instanceof Throwable)
            {
                styledString.append(
                        " " + ((Throwable) data).getMessage(),
                        StyledString.createColorRegistryStyler(
                                JFacePreferences.ERROR_COLOR, null));
            }
            else if (data instanceof String)
            {
                styledString.append(" " + data, StyledString.DECORATIONS_STYLER);
            }
            else if (element instanceof TreeParent)
            {
                Object parentData = ((TreeObject) element).getData();
                
                if (parentData instanceof DataObject)
                {
                    int childCount = ((TreeParent) element).getChildCount();
                    if (childCount > 0)
                    {
                        styledString.append(
                                " " + childCount,
                                StyledString.COUNTER_STYLER);
                    }
                }
                else
                {
                    //  This could be, for example, TapestryModule root
                    //  from TapestryContextView
                    return getStyledText(new TreeObject(
                            ((TreeParent) element).getName(),
                            parentData));
                }
            }
        }
        
        return styledString;
    }

    @Override
    public void addListener(ILabelProviderListener listener)
    {
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public boolean isLabelProperty(Object element, String property)
    {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener)
    {
    }

    @Override
    public String getText(Object element)
    {
        return getStyledText(element).toString();
    }

}
