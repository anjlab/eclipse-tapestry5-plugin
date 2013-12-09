package com.anjlab.tapestry5.webtools.contentassist;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.DefaultXMLCompletionProposalComputer;
import org.w3c.dom.NamedNodeMap;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.Member;
import com.anjlab.eclipse.tapestry5.Property;
import com.anjlab.eclipse.tapestry5.TapestryComponentSpecification;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

@SuppressWarnings({ "restriction" })
public class TapestryCompletionProposalComputer
    extends DefaultXMLCompletionProposalComputer
{

    @Override
    protected void addTagNameProposals(
            ContentAssistRequest contentAssistRequest, int childPosition,
            CompletionProposalInvocationContext context)
    {
//        Shell shell = context.getViewer().getTextWidget().getShell();
//        
//        IWorkbenchWindow window = TapestryUtils.getWorkbenchWindow(shell);
//        
//        if (window == null)
//        {
//            return;
//        }
//        TapestryProject tapestryProject = Activator.getDefault().getTapestryProject(window);
//        
//        if (tapestryProject == null)
//        {
//            //  No tapestry project available for the context
//            return;
//        }
//        
//        for (TapestryModule tapestryModule : tapestryProject.modules())
//        {
//            for (LibraryMapping libraryMapping : tapestryModule.libraryMappings())
//            {
//                //  TODO List all components from this library mapping
//                
//                String replacementString = contentAssistRequest.getMatchString();
//                contentAssistRequest.addProposal(new CompletionProposal(
//                        replacementString,
//                        contentAssistRequest.getReplacementBeginPosition(),
//                        contentAssistRequest.getReplacementLength(),
//                        replacementString.length(),
//                        null, // image
//                        "TODO " + libraryMapping.getRootPackage() + ".components.*", // displayString
//                        null, // contextInfo
//                        null  // additionalProposalInfo
//                        ));
//            }
//        }
    }
    
    @Override
    protected void addAttributeNameProposals(
            ContentAssistRequest contentAssistRequest,
            CompletionProposalInvocationContext context)
    {
        //  Display Page/Component parameters proposals
        
        if (!isTapestryTag(contentAssistRequest))
        {
            return;
        }
        
        TapestryComponentSpecification specification = getCurrentTagSpecification(contentAssistRequest, context);
        
        if (specification == null)
        {
            return;
        }
        
        NamedNodeMap attributes = contentAssistRequest.getNode().getAttributes();
        
        for (Member parameter : specification.getParameters())
        {
            //  Filter out parameters that are already present in this tag
            if (attributes.getNamedItem(parameter.getName()) == null)
            {
                String replacementString = parameter.getName() + "=\"\"";
                contentAssistRequest.addProposal(new CompletionProposal(
                        replacementString,
                        contentAssistRequest.getReplacementBeginPosition(),
                        contentAssistRequest.getReplacementLength(),
                        replacementString.length() - 1,
                        null, // image
                        parameter.getName(), // displayString
                        null, // contextInfo
                        parameter.getJavadoc()  // additionalProposalInfo
                        ));
            }
        }
    }

    private boolean isTapestryTag(ContentAssistRequest contentAssistRequest)
    {
        return contentAssistRequest.getNode().getNamespaceURI().startsWith("http://tapestry.apache.org/");
    }

    protected TapestryComponentSpecification getCurrentTagSpecification(
            ContentAssistRequest contentAssistRequest,
            CompletionProposalInvocationContext context)
    {
        Shell shell = context.getViewer().getTextWidget().getShell();
        
        IWorkbenchWindow window = TapestryUtils.getWorkbenchWindow(shell);
        
        if (window == null)
        {
            return null;
        }
        
        String componentName = contentAssistRequest.getNode().getLocalName();
        
        if (componentName == null)
        {
            return null;
        }
        
        TapestryContext tapestryContext = TapestryUtils.getTapestryContext(window, componentName);
        
        if (tapestryContext == null)
        {
            return null;
        }
        
        TapestryComponentSpecification specification = tapestryContext.getSpecification();
        
        return specification;
    }
    
    protected TapestryComponentSpecification getCurrentTapestryContextSpecification(
            ContentAssistRequest contentAssistRequest,
            CompletionProposalInvocationContext context)
    {
        Shell shell = context.getViewer().getTextWidget().getShell();
        
        IWorkbenchWindow window = TapestryUtils.getWorkbenchWindow(shell);
        
        if (window == null)
        {
            return null;
        }
        
        TapestryContext tapestryContext = Activator.getDefault().getTapestryContext(window);
        
        if (tapestryContext == null)
        {
            return null;
        }
        
        TapestryComponentSpecification specification = tapestryContext.getSpecification();
        
        return specification;
    }
    
    @Override
    protected void addAttributeValueProposals(
            ContentAssistRequest contentAssistRequest,
            CompletionProposalInvocationContext context)
    {
        //  Display Page/Component properties
        
        if (!isTapestryTag(contentAssistRequest))
        {
            return;
        }
        
        TapestryComponentSpecification specification = getCurrentTapestryContextSpecification(contentAssistRequest, context);
        
        if (specification == null)
        {
            return;
        }
        
        for (Property property : specification.getProperties())
        {
            String replacementString = '"' + property.getName() + '"';
            contentAssistRequest.addProposal(new CompletionProposal(
                    replacementString,
                    contentAssistRequest.getReplacementBeginPosition(),
                    contentAssistRequest.getReplacementLength(),
                    replacementString.length() - 1,
                    null, // image
                    property.getName(), // displayString
                    null, // contextInfo
                    property.getJavadoc()  // additionalProposalInfo
                    ));
        }
    }
}

