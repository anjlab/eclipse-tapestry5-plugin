package com.anjlab.eclipse.tapestry5.hyperlink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

import com.anjlab.eclipse.tapestry5.hyperlink.XmlFragment.XmlAttributeName;
import com.anjlab.eclipse.tapestry5.hyperlink.XmlFragment.XmlAttributeValue;
import com.anjlab.eclipse.tapestry5.hyperlink.XmlFragment.XmlTag;

public abstract class XmlFragment
{
    protected final IDocument document;
    protected final IRegion region;
    
    public XmlFragment(IDocument document, IRegion region)
    {
        this.document = document;
        this.region = region;
    }
    
    private String toString;
    @Override
    public String toString()
    {
        if (toString == null)
        {
            try
            {
                StringBuilder builder = new StringBuilder();
                builder.append(getClass().getSimpleName())
                       .append(" [region(offset: ")
                       .append(region == null ? "null" : region.getOffset())
                       .append(", length: ")
                       .append(region == null ? "null" : region.getLength())
                       .append(")]: ")
                       .append(region == null ? "null" : document.get(region.getOffset(), region.getLength()));
                toString = builder.toString();
            }
            catch (BadLocationException e)
            {
                toString = super.toString();
            }
        }
        return toString;
    }
    
    public static class XmlContextFragment extends XmlFragment
    {
        public XmlContextFragment(IDocument document, IRegion region)
        {
            super(document, region);
        }
        
        public XmlAtomicFragment getFragmentAt(int offset)
        {
            XmlTag currentTag = getCurrentTag();
            
            return currentTag == null
                 ? null
                 : currentTag.getFragmentAt(offset);
        }
        
        private Optional<XmlTag> rootTag;
        
        private XmlTag getRootTag()
        {
            if (rootTag == null)
            {
                try
                {
                    IRegion tagRegion = findRootTagRegion(document);
                    
                    rootTag = new Optional<XmlFragment.XmlTag>(
                                    tagRegion == null
                                         ? null
                                         : new XmlTag(this, tagRegion));
                }
                catch (BadLocationException e)
                {
                    rootTag = new Optional<XmlFragment.XmlTag>(null);
                }
            }
            
            return rootTag.value;
        }
        
        private Optional<XmlTag> currentTag;
        
        public XmlTag getCurrentTag()
        {
            if (currentTag == null)
            {
                try
                {
                    IRegion tagRegion = findTagRegion(document, region.getOffset());
                    
                    currentTag = new Optional<XmlFragment.XmlTag>(
                                        tagRegion == null
                                             ? null
                                             : new XmlTag(this, tagRegion));
                }
                catch (BadLocationException e)
                {
                    currentTag = new Optional<XmlFragment.XmlTag>(null);
                }
            }
            
            return currentTag.value;
        }
        
        private static IRegion findTagRegion(IDocument document, int offset) throws BadLocationException
        {
            int leftIndex = offset;
            int rightIndex = offset + 1;
            
            boolean foundTagStart = false;
            
            for (; leftIndex >= 0; leftIndex--)
            {
                char ch = document.getChar(leftIndex);
                if (ch == '<')
                {
                    foundTagStart = true;
                    break;
                }
                if (ch == '>')
                {
                    //  We're in tag's body
                    return null;
                }
            }
            if (!foundTagStart)
            {
                //  We can't do anything here, because we don't even know if we're in a tag
                return null;
            }
            for (; rightIndex < document.getLength(); rightIndex++)
            {
                char ch = document.getChar(rightIndex);
                if (ch == '>')
                {
                    return new Region(leftIndex, rightIndex - leftIndex + 1);
                }
                if (ch == '<')
                {
                    return new Region(leftIndex, rightIndex - leftIndex);
                }
            }
            return null;
        }
        
        private IRegion findRootTagRegion(IDocument document) throws BadLocationException
        {
            //  Find the beginning of the root XML tag - skip everything before first tag definition
            
            boolean inComment = false;
            
            for (int i = 0; i < document.getLength(); i++)
            {
                if (inComment)
                {
                    if (document.getChar(i) == '-')
                    {
                        if (i + "->".length() < document.getLength() && "->".equals(document.get(i + 1, "->".length())))
                        {
                            inComment = false;
                            i += "->".length();
                        }
                    }
                }
                else if (document.getChar(i) == '<')
                {
                    if (i + 1 < document.getLength() && isValidFirstCharForTagDefinition(document.getChar(i + 1)))
                    {
                        return findTagRegion(document, i);
                    }
                    else if (i + "!--".length() < document.getLength() && "!--".equals(document.get(i + 1, "!--".length())))
                    {
                        inComment = true;
                        i += "!--".length();
                    }
                }
            }
            return null;
        }
    }
    
    public static class XmlAtomicFragment extends XmlFragment
    {
        public final XmlTag xmlTag;
        public final Optional<String> value;
        public XmlAtomicFragment(XmlTag xmlTag, String value, int offset)
        {
            super(xmlTag.document, new Region(offset, StringUtils.isEmpty(value) ? 0 : value.length()));
            this.xmlTag = xmlTag;
            this.value = new Optional<String>(value);
        }
        public boolean hasValue()
        {
            return value.hasValue();
        }
        private FQName fqName;
        public FQName getFQName()
        {
            if (fqName == null)
            {
                fqName = FQName.parse(value);
            }
            return fqName;
        }

    }
    
    public static class XmlTagName extends XmlAtomicFragment
    {
        public XmlTagName(XmlTag xmlTag, String value, int offset)
        {
            super(xmlTag, value, offset);
        }
    }
    
    public static class XmlAttributeName extends XmlAtomicFragment
    {
        public XmlAttributeName(XmlTag xmlTag, String value, int offset)
        {
            super(xmlTag, value, offset);
        }
    }
    
    public static class XmlAttributeValue extends XmlAtomicFragment
    {
        public final XmlAttributeName attributeName;
        
        public XmlAttributeValue(XmlTag xmlTag, String value, int offset, XmlAttributeName attributeName)
        {
            super(xmlTag, value, offset);
            this.attributeName = attributeName;
        }
    }
    
    public static class FQName
    {
        public final String prefix;
        public final String name;
        
        public FQName(String prefix, String name)
        {
            this.prefix = prefix;
            this.name = name;
        }
        
        @Override
        public String toString()
        {
            return "{" + prefix + ":" + name + "}";
        }
        
        public static FQName parse(Optional<String> value)
        {
            if (!value.hasValue())
            {
                return new FQName(null, null);
            }
            int colonIndex = value.value.indexOf(':');
            return colonIndex < 0
                 ? new FQName(null, value.value)
                 : new FQName(value.value.substring(0, colonIndex),
                              colonIndex + 1 < value.value.length() ? value.value.substring(colonIndex + 1) : null);
        }
    }
    
    public static class Optional<T>
    {
        public final T value;
        
        public Optional(T value)
        {
            this.value = value;
        }
        
        public boolean hasValue()
        {
            return value != null;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof XmlFragment.Optional)
            {
                if (value == null)
                {
                    return this == obj;
                }
                
                return Objects.equals(value, ((XmlFragment.Optional<?>) obj).value);
            }
            return false;
        }
        
        @Override
        public int hashCode()
        {
            return value == null
                 ? super.hashCode()
                 : value.hashCode();
        }
    }
    
    public static class XmlTag extends XmlFragment
    {
        private XmlTagName tagName;
        private boolean selfClosing;
        private boolean closing;
        private boolean comment;
        
        private final Map<XmlAttributeName, XmlAttributeValue> attributes;
        
        public XmlAtomicFragment getFragmentAt(int offset)
        {
            if (tagName != null && inRegion(tagName.region, offset))
            {
                return tagName;
            }
            for (Entry<XmlAttributeName, XmlAttributeValue> attr : attributes.entrySet())
            {
                if (inRegion(attr.getKey().region, offset))
                {
                    return attr.getKey();
                }
                if (inRegion(attr.getValue().region, offset))
                {
                    return attr.getValue();
                }
            }
            return null;
        }
        
        public String resolveNamespacePrefix(String prefix)
        {
            if (StringUtils.isEmpty(prefix))
            {
                return null;
            }
            
            for (Entry<XmlAttributeName, XmlAttributeValue> attribute : attributes.entrySet())
            {
                if ("xmlns".equals(attribute.getKey().getFQName().prefix)
                        && prefix.equals(attribute.getKey().getFQName().name))
                {
                    return attribute.getValue().value.value;
                }
            }
            
            XmlTag rootTag = contextFragment.getRootTag();
            
            if (rootTag != null && rootTag != this)
            {
                return rootTag.resolveNamespacePrefix(prefix);
            }
            
            return null;
        }
        
        public boolean isClosing()
        {
            return closing;
        }
        public boolean isComment()
        {
            return comment;
        }
        public boolean isSelfClosing()
        {
            return selfClosing;
        }
        
        private enum ParsingContext
        {
            TAG_NAME, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, EQUAL_SIGN
        }
        
        public final XmlContextFragment contextFragment;
        
        public XmlTag(XmlContextFragment contextFragment, IRegion tagRegion) throws BadLocationException
        {
            super(contextFragment.document, tagRegion);
            this.contextFragment = contextFragment;
            this.attributes = new HashMap<XmlAttributeName, XmlAttributeValue>();
            parse(document, tagRegion);
        }

        private class ParserState
        {
            private XmlTag.ParsingContext parsingContext = ParsingContext.TAG_NAME;
            
            private XmlAttributeName attributeName  = null;
            private boolean valueInQuotes = false;
            private char quoteChar = 0;
            private int startOffset = -1;
            private int offset = -1;
            
            private StringBuilder builder = new StringBuilder();
            
            public ParserState(int startOffset)
            {
                this.startOffset = startOffset;
                this.offset = startOffset - 1;
            }

            private void waitForAttributeName()
            {
                builder.setLength(0);
                parsingContext = ParsingContext.ATTRIBUTE_NAME;
                valueInQuotes = false;
                startOffset = -1;
            }

            private void waitForAttributeValue()
            {
                builder.setLength(0);
                parsingContext = ParsingContext.ATTRIBUTE_VALUE;
                valueInQuotes = false;
                startOffset = -1;
            }
            
            private void waitForEqualSign()
            {
                builder.setLength(0);
                parsingContext = ParsingContext.EQUAL_SIGN;
                valueInQuotes = false;
                startOffset = -1;
            }

            private void waitForAttributeValueInQuotes(char quoteChar)
            {
                parsingContext = ParsingContext.ATTRIBUTE_VALUE;
                valueInQuotes = true;
                this.quoteChar = quoteChar;
                startOffset = -1;
            }

            public void pushChar(char ch)
            {
                offset++;
                
                if (parsingContext == ParsingContext.TAG_NAME)
                {
                    if (!isValidCharForFQName(ch))
                    {
                        tagName = new XmlTagName(XmlTag.this, builder.toString(), startOffset);
                        
                        waitForAttributeName();
                    }
                    else
                    {
                        builder.append(ch);
                    }
                }
                else if (parsingContext == ParsingContext.ATTRIBUTE_NAME)
                {
                    if (!isValidCharForFQName(ch))
                    {
                        if (startOffset != -1)
                        {
                            attributeName = new XmlAttributeName(XmlTag.this, builder.toString(), startOffset);
                            
                            if (ch == '=')
                            {
                                waitForAttributeValue();
                            }
                            else
                            {
                                waitForEqualSign();
                            }
                        }
                        else if (ch == '\'' || ch == '"')
                        {
                            newAttributeValueStartedWithoutAttributeName(ch);
                        }
                        else
                        {
                            //  There's nothing there in current buffer, just ignore this char as whitespace
                        }
                    }
                    else
                    {
                        rememberChar(ch);
                    }
                }
                else if (parsingContext == ParsingContext.EQUAL_SIGN)
                {
                    if (ch == '=')
                    {
                        waitForAttributeValue();
                    }
                    else if (Character.isWhitespace(ch))
                    {
                        //  Skip white spaces before =
                    }
                    else if (isValidCharForFQName(ch))
                    {
                        newAttributeNameStarted(ch);
                    }
                    else if (ch == '\'' || ch == '"')
                    {
                        newAttributeValueStartedWithoutAttributeName(ch);
                    }
                }
                else if (parsingContext == ParsingContext.ATTRIBUTE_VALUE)
                {
                    if (valueInQuotes)
                    {
                        if (quoteChar == ch)
                        {
                            addAttribute(XmlTag.this, attributeName, builder, startOffset);
                            
                            waitForAttributeName();
                        }
                        else
                        {
                            rememberChar(ch);
                        }
                    }
                    else if (ch == '\'' || ch == '"')
                    {
                        //  Begin looking for attribute value
                        valueInQuotes = true;
                        quoteChar = ch;
                    }
                    else if (Character.isWhitespace(ch))
                    {
                        //  Skip whitespaces after =
                    }
                    else if (isValidCharForFQName(ch))
                    {
                        newAttributeNameStarted(ch);
                    }
                }
            }

            private void newAttributeValueStartedWithoutAttributeName(char ch)
            {
                //  Attribute value started without attribute name
                attributeName = null;
                
                waitForAttributeValueInQuotes(ch);
            }

            private void newAttributeNameStarted(char ch)
            {
                //  New attribute name started
                //  Current attribute name has no corresponding attribute value
                
                //  Add current attribute
                addAttribute(XmlTag.this, attributeName, builder, startOffset);
                
                waitForAttributeName();
                
                rememberChar(ch);
            }

            private void rememberChar(char ch)
            {
                if (startOffset == -1)
                {
                    startOffset = offset;
                }
                builder.append(ch);
            }
        }
        
        private void parse(IDocument document, IRegion tagRegion)
        {
            try
            {
                //  Skip leading and trailing </ and />
                int leftIndex = tagRegion.getOffset();
                int rightIndex = tagRegion.getOffset() + tagRegion.getLength() - 1;
                
                if (document.getChar(leftIndex) == '<')
                {
                    leftIndex++;
                    if (document.getChar(leftIndex) == '/')
                    {
                        leftIndex++;
                        closing = true;
                    }
                    else
                    {
                        String commentStart = "!--";
                        if (leftIndex + commentStart.length() < document.getLength())
                        {
                            if (commentStart.equals(document.get(leftIndex, commentStart.length())))
                            {
                                comment = true;
                                return;
                            }
                        }
                    }
                }
                
                if (document.getChar(rightIndex) == '>')
                {
                    rightIndex--;
                    if (document.getChar(rightIndex) == '/')
                    {
                        rightIndex--;
                        selfClosing = true;
                    }
                }
                
                ParserState state = new ParserState(leftIndex);
                
                for (int i = leftIndex; i <= rightIndex ; i++)
                {
                    char ch = document.getChar(i);
                    
                    state.pushChar(ch);
                }
                
                //  Flush buffers
                state.pushChar((char) 0);
            }
            catch (BadLocationException e)
            {
                //  Ignore
            }
        }

        private void addAttribute(XmlTag xmlTag, XmlAttributeName attributeName, StringBuilder value, int offset)
        {
            if (attributeName == null)
            {
                attributeName = new XmlAttributeName(xmlTag, null, -1);
            }
            
            attributes.put(attributeName,
                           new XmlAttributeValue(xmlTag, value.toString(), offset, attributeName));
        }

        public Map<XmlAttributeName, XmlAttributeValue> attributes()
        {
            return attributes;
        }

        public FQName getFQName()
        {
            return tagName == null
                 ? new FQName(null, null)
                 : tagName.getFQName();
        }
    }
    
    private static boolean isValidFirstCharForTagDefinition(char ch)
    {
        return Character.isLetter(ch) || ch == '_' || ch == ':';
    }

    public static boolean inRegion(IRegion region, int offset)
    {
        return region != null
            && region.getOffset() <= offset
            && offset < region.getOffset() + region.getLength();
    }

    //  http://pic.dhe.ibm.com/infocenter/wci/v6r0m0/index.jsp?topic=%2Fcom.ibm.websphere.cast_iron.doc%2Fref_Valid_Node_Names.html
    private static final String INVALID_CHARS_FOR_SIMPLE_NAME = ":~/\\;?$&%@^=*+()|'\"`{}[]<>";
    private static boolean isValidCharForFQName(char ch)
    {
        return ch == ':'
            || (!Character.isWhitespace(ch)
                    && INVALID_CHARS_FOR_SIMPLE_NAME.indexOf(ch) < 0
                    && (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.'));
    }

}

class XmlTagNodeAdapter extends BaseNodeAdapter
{
    private XmlTag xmlTag;
    
    public XmlTagNodeAdapter(XmlTag xmlTag)
    {
        this.xmlTag = xmlTag;
    }

    @Override
    public NamedNodeMap getAttributes()
    {
        final List<Entry<XmlAttributeName, XmlAttributeValue>> attributes =
                new ArrayList<Map.Entry<XmlAttributeName,XmlAttributeValue>>(xmlTag.attributes().entrySet());
        
        return new NamedNodeMap()
        {
            @Override
            public int getLength()
            {
                return attributes.size();
            }
            
            @Override
            public Node item(int index)
            {
                final Entry<XmlAttributeName, XmlAttributeValue> attribute = attributes.get(index);
                return new BaseNodeAdapter()
                {
                    @Override
                    public String getNamespaceURI()
                    {
                        return xmlTag.resolveNamespacePrefix(attribute.getKey().getFQName().prefix);
                    }
                    @Override
                    public String getLocalName()
                    {
                        return attribute.getKey().getFQName().name;
                    }
                    @Override
                    public String getNodeValue() throws DOMException
                    {
                        return attribute.getValue().value.value;
                    }
                    @Override
                    public short getNodeType()
                    {
                        return Node.ATTRIBUTE_NODE;
                    }
                };
            }
            
            @Override
            public Node setNamedItemNS(Node arg) throws DOMException
            {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Node setNamedItem(Node arg) throws DOMException
            {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException
            {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Node removeNamedItem(String name) throws DOMException
            {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException
            {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public Node getNamedItem(String name)
            {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    @Override
    public String getLocalName()
    {
        return xmlTag.getFQName().name;
    }

    @Override
    public String getNamespaceURI()
    {
        String prefix = xmlTag.getFQName().prefix;
        return xmlTag.resolveNamespacePrefix(prefix);
    }
    
    @Override
    public String lookupNamespaceURI(String prefix)
    {
        return xmlTag.resolveNamespacePrefix(prefix);
    }
    
    @Override
    public short getNodeType()
    {
        return Node.ELEMENT_NODE;
    }
    
    @Override
    public String getPrefix()
    {
        return xmlTag.getFQName().prefix;
    }
    
    @Override
    public boolean hasAttributes()
    {
        return !xmlTag.attributes().isEmpty();
    }
}

class BaseNodeAdapter implements Node
{
    @Override
    public Node appendChild(Node newChild) throws DOMException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node cloneNode(boolean deep)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBaseURI()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeList getChildNodes()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getFeature(String feature, String version)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getFirstChild()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getLastChild()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getNextSibling()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNodeName()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNodeValue() throws DOMException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Document getOwnerDocument()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getParentNode()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getPreviousSibling()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTextContent() throws DOMException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getUserData(String key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasChildNodes()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node insertBefore(Node newChild, Node refChild) throws DOMException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDefaultNamespace(String namespaceURI)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEqualNode(Node arg)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSameNode(Node other)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSupported(String feature, String version)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String lookupPrefix(String namespaceURI)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void normalize()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node replaceChild(Node newChild, Node oldChild) throws DOMException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNodeValue(String nodeValue) throws DOMException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPrefix(String prefix) throws DOMException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTextContent(String textContent) throws DOMException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object setUserData(String key, Object data, UserDataHandler handler)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamedNodeMap getAttributes()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocalName()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNamespaceURI()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getNodeType()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPrefix()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAttributes()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String lookupNamespaceURI(String prefix)
    {
        throw new UnsupportedOperationException();
    }
}
