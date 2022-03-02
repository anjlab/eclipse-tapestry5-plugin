package com.anjlab.tapestry5.webtools.contentassist;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.DefaultXMLCompletionProposalComputer;
import org.eclipse.wst.xml.ui.internal.contentassist.MarkupCompletionProposal;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.anjlab.eclipse.tapestry5.Activator;
import com.anjlab.eclipse.tapestry5.Component;
import com.anjlab.eclipse.tapestry5.EclipseUtils;
import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.Parameter;
import com.anjlab.eclipse.tapestry5.Property;
import com.anjlab.eclipse.tapestry5.TapestryComponentSpecification;
import com.anjlab.eclipse.tapestry5.TapestryContext;
import com.anjlab.eclipse.tapestry5.TapestryContextScope;
import com.anjlab.eclipse.tapestry5.TapestryModule;
import com.anjlab.eclipse.tapestry5.TapestryProject;
import com.anjlab.eclipse.tapestry5.TapestryUtils;

@SuppressWarnings({ "restriction" })
public class TapestryCompletionProposalComputer extends DefaultXMLCompletionProposalComputer
{
    private static interface ProposalCallback
    {
        void newProposal(TapestryContext tapestryContext, String tagName, String displayString);
    }
    
    //  TODO Remove addTagInsertionProposals
//    @Override
//    protected void addTagInsertionProposals(final ContentAssistRequest contentAssistRequest,
//                                            int childPosition,
//                                            CompletionProposalInvocationContext context)
//    {
//        enumProposals(contentAssistRequest, context, new ProposalCallback()
//        {
//            @Override
//            public void newProposal(TapestryContext tapestryContext,
//                                    String tagName,
//                                    String displayString)
//            {
//                //  TODO Generate required attributes in addTagNameProposals too
//                
//                StringBuilder tagTemplate = new StringBuilder();
//                
//                tagTemplate.append("<")
//                           .append(tagName);
//                
//                //  XXX How to check if component may have/has content?
//                boolean mayHaveContent = true;
//                
//                for (Parameter parameter : tapestryContext.getSpecification().getParameters())
//                {
//                    if (parameter.isRequired())
//                    {
//                        tagTemplate.append(" ")
//                                   .append(parameter.getName())
//                                   .append("=\"")
//                                   .append(StringUtils.isEmpty(parameter.getValue()) ? "" : parameter.getValue())
//                                   .append("\"");
//                    }
//                }
//                
//                if (mayHaveContent)
//                {
//                    tagTemplate.append(">")
//                               .append("</")
//                               .append(tagName)
//                               .append(">");
//                }
//                else
//                {
//                    tagTemplate.append(" />");
//                }
//                
//                addProposal(contentAssistRequest,
//                            tapestryContext,
//                            tagTemplate.toString(),
//                            displayString);
//            }
//        });
//    }
    
    @Override
    protected void addTagNameProposals(final ContentAssistRequest request,
                                       int childPosition,
                                       CompletionProposalInvocationContext context)
    {
        enumProposals(request, context, new ProposalCallback()
        {
            @Override
            public void newProposal(TapestryContext tapestryContext,
                                    String tagName,
                                    String displayString)
            {
                addProposal(request, tapestryContext, tagName, displayString);
            }
        });
    }
    
    private void addProposal(ContentAssistRequest request,
                             TapestryContext tapestryContext,
                             String replacementString,
                             String displayString)
    {
        request.addProposal(new MarkupCompletionProposal(
                replacementString,  // replacementString
                request.getReplacementBeginPosition(),
                request.getReplacementLength(),
                replacementString.length(),
                Activator.getTapestryLogoIcon(), // image
                displayString, // displayString
                null, // contextInfo
                tapestryContext.getJavadoc(),  // additionalProposalInfo
                3000 - (StringUtils.countMatches(replacementString, ".") > 0 ? 1 : 0), // relevance
                true  // updateReplacementLengthOnValidate
                ));
    }
    
    private void enumProposals(final ContentAssistRequest request, CompletionProposalInvocationContext context,
            ProposalCallback proposalCallback)
    {
        TapestryProject tapestryProject = getTapestryProject(context);
        
        if (tapestryProject == null)
        {
            //  No tapestry project available for the context
            return;
        }
        
        Map<String, String> xmlnsMappings = findXmlnsMappingsRelativeTo(request.getNode());
        
        for (TapestryModule tapestryModule : tapestryProject.modules())
        {
            for (TapestryContext tapestryContext : tapestryModule.getComponents())
            {
                //  If tapestry-library: prefix defined, try to use it first for completion proposals
                String tagName = getComponentTagName(tapestryModule, tapestryContext, xmlnsMappings, true);
                String displayString = tagName;
                
                String userInput = request.getMatchString();
                
                if (!isComponentNameProposalMatches(tagName, userInput, xmlnsMappings))
                {
                    //  ... and fall back to full component name if library prefixed name doesn't match user input:
                    //  maybe the user is entering full component name
                    String fullTagName = getComponentTagName(tapestryModule, tapestryContext, xmlnsMappings, false);
                    
                    if (!isComponentNameProposalMatches(fullTagName, userInput, xmlnsMappings))
                    {
                        continue;
                    }
                    
                    //  User tries to input full component name, but prefix is available for this library.
                    //  Force using prefixed tag name for completion proposal, otherwise why would user defined xmlns for it?
                    displayString = fullTagName;
                }
                
                proposalCallback.newProposal(tapestryContext, tagName, displayString);
            }
        }
    }

    @Override
    protected void addAttributeNameProposals(
            ContentAssistRequest request,
            CompletionProposalInvocationContext context)
    {
        //  Display Page/Component parameters proposals
        
        TapestryContextScope scope = getCurrentTagSpecification(request, context);
        
        if (scope == null)
        {
            return;
        }
        
        NamedNodeMap attributes = request.getNode().getAttributes();
        
        //  TODO Add parameters of applied t:mixins
        
        //  Current component may also have embedded definition via @Component annotation
        Component embeddedDefinition = null;
        
        String componentId = TapestryUtils.findTapestryAttribute(request.getNode(), "id");
        
        if (StringUtils.isNotEmpty(componentId))
        {
            TapestryContextScope scope2 = getCurrentTapestryContextSpecification(request, context);
            
            for (Component component : scope2.specification.getComponents())
            {
                if (StringUtils.equals(componentId, component.getId()))
                {
                    embeddedDefinition = component;
                    break;
                }
            }
        }
        
        for (Parameter parameter : scope.specification.getParameters(scope.project))
        {
            if (!parameter.getName().startsWith(request.getMatchString()))
            {
                continue;
            }
            
            if (parameterBoundUsingAttribute(parameter, attributes)
                || parameterBoundUsingChildNode(parameter, request.getNode())
                || parameterBoundUsingEmbeddedComponent(parameter, embeddedDefinition))
            {
                continue;
            }
            
            String replacementString = parameter.getName() + "=\"\"";
            request.addProposal(new MarkupCompletionProposal(
                    replacementString,
                    request.getReplacementBeginPosition(),
                    request.getReplacementLength(),
                    replacementString.length() - 1,
                    Activator.getTapestryLogoIcon(), // image
                    parameter.getName(), // displayString
                    null, // contextInfo
                    parameter.getJavadoc(),  // additionalProposalInfo
                    3000, // relevance
                    true  // updateReplacementLengthOnValidate
                    ));
        }
    }

    private boolean parameterBoundUsingAttribute(Parameter parameter, NamedNodeMap attributes)
    {
        return attributes.getNamedItem(parameter.getName()) != null;
    }

    private boolean parameterBoundUsingChildNode(Parameter parameter, Node node)
    {
        NodeList childNodes = node.getChildNodes();
        
        if (childNodes != null)
        {
            for (int i = 0; i < childNodes.getLength(); i++)
            {
                Node child = childNodes.item(i);
                if (StringUtils.equals("tapestry:parameter", child.getNamespaceURI())
                        && StringUtils.equalsIgnoreCase(parameter.getName(), child.getLocalName()))
                {
                    return true;
                }
            }
        }
        
        return false;
    }

    private boolean parameterBoundUsingEmbeddedComponent(Parameter parameter, Component embeddedDefinition)
    {
        if (embeddedDefinition == null)
        {
            return false;
        }
        
        for (String param : embeddedDefinition.getParameters())
        {
            //  TODO Only check within list of publishParameters
            String[] nameValue = param.split("=");
            if (nameValue.length == 2
                    && StringUtils.equalsIgnoreCase(nameValue[0], parameter.getName()))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void addAttributeValueProposals(
            ContentAssistRequest request,
            CompletionProposalInvocationContext context)
    {
        //  Display Page/Component properties
        
        TapestryContextScope scope = getCurrentTapestryContextSpecification(request, context);
        
        if (scope == null)
        {
            return;
        }
        //  TODO Support comma-separated lists (like for t:mixins), maps, and different binding prefixes
        //  TODO Support properties in dot-notation, like: user.firstName
        
        for (Property property : scope.specification.getProperties())
        {
            if (!property.getName().startsWith(request.getMatchString().replaceAll("\"|'", "")))
            {
                continue;
            }
            
            String replacementString = '"' + property.getName() + '"';
            request.addProposal(new MarkupCompletionProposal(
                    replacementString,
                    request.getReplacementBeginPosition(),
                    request.getReplacementLength(),
                    replacementString.length() - 1,
                    Activator.getTapestryLogoIcon(), // image
                    property.getName(), // displayString
                    null, // contextInfo
                    property.getJavadoc(),  // additionalProposalInfo
                    3000, // relevance
                    true  // updateReplacementLengthOnValidate
                    ));
        }
    }

    private TapestryProject getTapestryProject(CompletionProposalInvocationContext context)
    {
        Shell shell = context.getViewer().getTextWidget().getShell();
        
        IWorkbenchWindow window = EclipseUtils.getWorkbenchWindow(shell);
        
        if (window == null)
        {
            return null;
        }
        
        TapestryProject tapestryProject = Activator.getDefault().getTapestryProject(window);
        
        return tapestryProject;
    }

    private boolean isComponentNameProposalMatches(String proposal, String userInput, Map<String, String> xmlnsMappings)
    {
        if (StringUtils.isEmpty(userInput) || proposal.startsWith(userInput))
        {
            return true;
        }
        
        for (Entry<String, String> xmlnsMapping : xmlnsMappings.entrySet())
        {
            if (TapestryUtils.isTapestryComponentsNamespace(xmlnsMapping.getValue())
                    && proposal.startsWith(xmlnsMapping.getKey() + ":" + userInput))
            {
                return true;
            }
        }
        
        return false;
    }

    private String getComponentTagName(TapestryModule tapestryModule,
                                       TapestryContext tapestryContext,
                                       Map<String, String> xmlnsMappings,
                                       boolean useLibraryPrefix)
    {
        if (useLibraryPrefix)
        {
            String packageName = tapestryContext.getPackageName();
            
            //  1. Check if this component is from some library
            for (LibraryMapping library : tapestryModule.libraryMappings())
            {
                if (packageName.startsWith(library.getRootPackage()))
                {
                    //  2. Check if there are prefix defined for this library in xmlnsMappings
                    for (Entry<String, String> xmlnsMapping : xmlnsMappings.entrySet())
                    {
                        if (xmlnsMapping.getValue().equals("tapestry-library:" + library.getPathPrefix()))
                        {
                            //  3. Use this prefix for tag name
                            return xmlnsMapping.getKey()
                                 + ":"
                                 + tapestryModule.getComponentName(tapestryContext)
                                     .substring(library.getPathPrefix().length() + ".".length());
                        }
                    }
                }
            }
        }
        
        //  4. Use default tapestry NS prefix
        
        for (Entry<String, String> xmlnsMapping : xmlnsMappings.entrySet())
        {
            if (TapestryUtils.isTapestryDefaultNamespace(xmlnsMapping.getValue()))
            {
                return xmlnsMapping.getKey()
                     + ":"
                     + tapestryModule.getComponentName(tapestryContext);
            }
        }
        
        //  5. Something went wrong -- probably document is not well formed yet, use "de-facto" default NS prefix
        return "t:" + tapestryModule.getComponentName(tapestryContext);
    }

    private Map<String, String> findXmlnsMappingsRelativeTo(Node node)
    {
        Map<String, String> mappings = new HashMap<String, String>();
        
        findXmlnsMappingsAt(node, mappings);
        
        return mappings;
    }

    private void findXmlnsMappingsAt(Node node, Map<String, String> mappings)
    {
        if (node == null)
        {
            return;
        }
        
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null)
        {
            for (int i = 0; i < attributes.getLength(); i++)
            {
                Node item = attributes.item(i);
                if (!item.getNodeName().startsWith("xmlns:"))
                {
                    continue;
                }
                String prefix = item.getNodeName().substring("xmlns:".length());
                mappings.put(prefix, item.getNodeValue());
            }
        }
        findXmlnsMappingsAt(node.getParentNode(), mappings);
    }

    private TapestryContextScope getCurrentTagSpecification(
            ContentAssistRequest request,
            CompletionProposalInvocationContext context)
    {
        Shell shell = context.getViewer().getTextWidget().getShell();
        
        IWorkbenchWindow window = EclipseUtils.getWorkbenchWindow(shell);
        
        if (window == null)
        {
            return null;
        }
        
        String componentName = TapestryUtils.getComponentName(window, request.getNode());
        
        if (componentName == null)
        {
            return null;
        }
        
        TapestryContextScope scope = TapestryUtils.getTapestryContext(window, componentName);
        
        if (scope == null)
        {
            return null;
        }
        
        TapestryComponentSpecification specification = scope.context.getSpecification();
        
        return new TapestryContextScope(window, scope.project, scope.context, specification);
    }
    
    private TapestryContextScope getCurrentTapestryContextSpecification(
            ContentAssistRequest request,
            CompletionProposalInvocationContext context)
    {
        Shell shell = context.getViewer().getTextWidget().getShell();
        
        IWorkbenchWindow window = EclipseUtils.getWorkbenchWindow(shell);
        
        if (window == null)
        {
            return null;
        }
        
        TapestryContext tapestryContext = Activator.getDefault().getTapestryContext(window);
        
        if (tapestryContext == null)
        {
            return null;
        }
        
        TapestryProject tapestryProject = Activator.getDefault().getTapestryProject(window);
        
        TapestryComponentSpecification specification = tapestryContext.getSpecification();
        
        return new TapestryContextScope(window, tapestryProject, tapestryContext, specification);
    }
    
}
