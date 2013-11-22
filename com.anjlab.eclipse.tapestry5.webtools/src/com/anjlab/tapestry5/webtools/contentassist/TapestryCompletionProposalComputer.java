package com.anjlab.tapestry5.webtools.contentassist;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.DefaultXMLCompletionProposalComputer;

import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

@SuppressWarnings({ "restriction", "unused" })
public class TapestryCompletionProposalComputer
    extends DefaultXMLCompletionProposalComputer
{

    @Override
    protected void addTagNameProposals(
            ContentAssistRequest contentAssistRequest, int childPosition,
            CompletionProposalInvocationContext context)
    {
        //  TODO
//        int offset = context.getInvocationOffset();
//        
//        contentAssistRequest.addProposal(new CompletionProposal(
//                "ReplacementString1",
//                // replacementOffset -- index of where the replacement should start from
//                // current position of rewind some chars back to tag/attributeName/attributeValue start
//                offset,
//                // replacementLength -- how many chars right from current position will be overwritten by replacement (others will be shifted right)
//                0,
//                // cursorPosition: this value will be added to replacementOffset
//                "ReplacementString1".length()));
    }
    
    @Override
    protected void addAttributeNameProposals(
            ContentAssistRequest contentAssistRequest,
            CompletionProposalInvocationContext context)
    {
        //  TODO Page/Component parameters
        
        Shell shell = context.getViewer().getTextWidget().getShell();
        
        IWorkbenchWindow window = TapestryUtils.getWorkbenchWindow(shell);
        
        if (window == null)
        {
            return;
        }
        
        //  TODO Read component name from contentAssistRequest
        String componentName = null;
        
        if (componentName == null)
        {
            return;
        }
        
        TapestryContext tapestryContext = TapestryUtils.getTapestryContext(window, componentName);
        
        if (tapestryContext == null)
        {
            return;
        }
        
        TapestryModule tapestryModule = TapestryUtils.getTapestryModule(window, tapestryContext.getProject());
        
        if (tapestryModule == null)
        {
            //  No tapestry project available for the context
            return;
        }
        
        TapestryProject tapestryProject = tapestryModule.getProject();
        
        //  TODO Compute proposals from all modules
        tapestryProject.modules();
    }
    
    @Override
    protected void addAttributeValueProposals(
            ContentAssistRequest contentAssistRequest,
            CompletionProposalInvocationContext context)
    {
        //  TODO Page/Component properties
    }
}

