package com.anjlab.eclipse.tapestry5.hyperlink;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.Component;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.Parameter;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryContextScope;
import com.anjlab.eclipse.tapestry5.TapestryFile;
import com.anjlab.eclipse.tapestry5.TapestryUtils;
import com.anjlab.eclipse.tapestry5.TextEditorCallback;
import com.anjlab.eclipse.tapestry5.hyperlink.XmlFragment.XmlAtomicFragment;
import com.anjlab.eclipse.tapestry5.hyperlink.XmlFragment.XmlAttributeName;
import com.anjlab.eclipse.tapestry5.hyperlink.XmlFragment.XmlAttributeValue;
import com.anjlab.eclipse.tapestry5.hyperlink.XmlFragment.XmlContextFragment;
import com.anjlab.eclipse.tapestry5.hyperlink.XmlFragment.XmlTagName;

public class TapestryComponentHyperlinkDetector extends AbstractHyperlinkDetector
{
    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks)
    {
        //  TODO Cache recent XmlContextFragment?
        final XmlContextFragment xmlContextFragment = getXmlFragment(textViewer, region);
        
        final XmlAtomicFragment atomicFragment = xmlContextFragment.getFragmentAt(region.getOffset());
        
        if (atomicFragment == null)
        {
            return null;
        }
        
        final IWorkbenchWindow window = EclipseUtils.getWorkbenchWindow(textViewer.getTextWidget().getShell());
        
        if (window == null)
        {
            return null;
        }
        
        final TapestryContextScope targetScope = findTargetContext(window, atomicFragment);
        
        if (targetScope == null)
        {
            return null;
        }
        
        if (atomicFragment instanceof XmlTagName)
        {
            //  TODO Check if it's a tapestry:parameter
            return filesFromContext(atomicFragment, targetScope.context);
        }
        else if (atomicFragment instanceof XmlAttributeName)
        {
            if (!StringUtils.isEmpty(atomicFragment.getFQName().prefix))
            {
                //  Ignore prefixed attributes
                return null;
            }
            
            //  XXX This works very slow now:
            //      - See TapestryProject#findComponentContext(..., ..., ...)
            //      - Cache parameters in specification once retrieved?
            
            //  Find parameter in target context
            Parameter parameter = targetScope.context.getSpecification()
                    .getParameter(targetScope.project, atomicFragment.getFQName().name);
            
            return parameter != null
                 ? new IHyperlink[]
                   {
                        new MemberHyperlink(atomicFragment,
                                            parameter.getSpecification().getTapestryContext().getJavaFile(),
                                            parameter)
                   }
                 : null;
        }
        else if (atomicFragment instanceof XmlAttributeValue)
        {
            XmlAttributeName attributeName = ((XmlAttributeValue) atomicFragment).attributeName;
            
            if (TapestryUtils.isTapestryDefaultNamespace(attributeName.xmlTag.resolveNamespacePrefix(attributeName.getFQName().prefix)))
            {
                //  1) t:type => show files from tapestry context
                if (StringUtils.equals("type", attributeName.getFQName().name))
                {
                    return filesFromContext(atomicFragment, targetScope.context);
                }
                
                //  2) t:id => open field declaration (if any)
                if (StringUtils.equals("id", attributeName.getFQName().name))
                {
                    TapestryContext currentContext = Activator.getDefault().getTapestryContext(window);
                    
                    if (currentContext == null)
                    {
                        return null;
                    }
                    
                    //  @Component or @InjectComponent
                    for (Component component : currentContext.getSpecification().getComponents())
                    {
                        if (StringUtils.equalsIgnoreCase(atomicFragment.value.value, component.getId()))
                        {
                            return new IHyperlink[]
                            {
                                new MemberHyperlink(atomicFragment,
                                                    component.getSpecification().getTapestryContext().getJavaFile(),
                                                    component)
                            };
                        }
                    }
                    
                    return null;
                }
                
                //  3) t:mixins => show separate hyperlink for every referenced mixin
                if (StringUtils.equals("mixins", attributeName.getFQName().name))
                {
                    //  TODO
                    return null;
                }
            }
            
            Parameter parameter = targetScope.context.getSpecification().getParameter(
                    targetScope.project, attributeName.getFQName().name);
            
            if (parameter == null)
            {
                return null;
            }
            
            //  It there's a parameter with in target context and this is its value,
            //  then find hyperlink target depending on binding prefix (default and actual)
        }
        
        return new IHyperlink[]
        {
                new IHyperlink()
                {
                    @Override
                    public void open()
                    {
                        EclipseUtils.openInformation(window, atomicFragment.toString());
                    }
                    
                    @Override
                    public String getTypeLabel()
                    {
                        return atomicFragment.toString();
                    }
                    
                    @Override
                    public String getHyperlinkText()
                    {
                        return atomicFragment.toString();
                    }
                    
                    @Override
                    public IRegion getHyperlinkRegion()
                    {
                        return atomicFragment.region;
                    }
                }
        };
    }

    private IHyperlink[] filesFromContext(final XmlAtomicFragment atomicFragment, final TapestryContext targetContext)
    {
        final List<TapestryFile> files = targetContext.getFiles();
        
        IHyperlink[] links = new IHyperlink[files.size()];
        
        for (int i = 0; i < files.size(); i++)
        {
            final int index = i;
            
            links[index] = new IHyperlink()
            {
                @Override
                public void open()
                {
                    final TapestryFile tapestryFile = files.get(index);
                    
                    EclipseUtils.openFile(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), tapestryFile, new TextEditorCallback()
                    {
                        @Override
                        public void editorOpened(ITextEditor textEditor)
                        {
                            //  Highlight Java type when opening Java file for the first time
                            
                            IRegion highlightedRange = textEditor.getHighlightRange();
                            if ((highlightedRange == null
                                    || highlightedRange.getOffset() == 0 && highlightedRange.getLength() == 0)
                                    && targetContext.getJavaFile() == tapestryFile)
                            {
                                IType type = targetContext.getJavaType();
                                
                                if (type != null)
                                {
                                    try
                                    {
                                        ISourceRange nameRange = type.getNameRange();
                                        if (nameRange != null)
                                        {
                                            textEditor.selectAndReveal(nameRange.getOffset(),
                                                                       nameRange.getLength());
                                        }
                                    }
                                    catch (JavaModelException e)
                                    {
                                        //  Ignore
                                    }
                                }
                            }
                        }
                    });
                }
                
                @Override
                public String getTypeLabel()
                {
                    return files.get(index).getName();
                }
                
                @Override
                public String getHyperlinkText()
                {
                    return files.get(index).getName();
                }
                
                @Override
                public IRegion getHyperlinkRegion()
                {
                    return atomicFragment.region;
                }
            };
        }
        
        return links;
    }

    private TapestryContextScope findTargetContext(IWorkbenchWindow window, XmlAtomicFragment atomicFragment)
    {
        if (!atomicFragment.hasValue())
        {
            return null;
        }
        
        if (atomicFragment.xmlTag.isComment())
        {
            return null;
        }
        
        String componentName = TapestryUtils.getComponentName(window, new XmlTagNodeAdapter(atomicFragment.xmlTag));
        
        if (componentName == null)
        {
            return null;
        }
        
        return TapestryUtils.getTapestryContext(window, componentName);
    }

    protected XmlContextFragment getXmlFragment(ITextViewer textViewer, IRegion region)
    {
        if (region == null || textViewer == null)
        {
            return null;
        }
        
        IDocument document = textViewer.getDocument();
        
        if (document == null)
        {
            return null;
        }
        
        return new XmlContextFragment(document, region);
    }
}