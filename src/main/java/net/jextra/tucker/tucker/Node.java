/*
 * Copyright (C) jextra.net.
 *
 * This file is part of the jextra.net software.
 *
 * The jextra software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * The jextra software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with the jextra software; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA.
 */

package net.jextra.tucker.tucker;

import java.util.*;

/**
 * A Node is typically a single line in the .thtml file. It can be either a root block, a tag (typical html tag with attributes and text),
 * an insertion to identify location that insertions can occur, or a rawTest to represent raw text.
 */
public class Node
{
    // ============================================================
    // Enums
    // ============================================================

    public enum NodeType
    {
        block,  // = root node with variable scope and bindings
        insertion,  // > insertion point
        tag, // typical Node
        rawText // override output with non-interpreted rawText
    }

    // ============================================================
    // Fields
    // ============================================================

    public static final String DEFAULT_TAG = "div";
    public static final String ATT_ID = "id";
    public static final String ATT_CLASS = "class";

    private NodeType type;
    private int indent;
    private int row;
    private boolean inline;
    private String tagName;
    private Map<String, Attribute> attributes;
    private List<Segment> segments;
    private List<Node> children;
    private String rawText;

    // ============================================================
    // Constructors
    // ============================================================

    public Node()
    {
        type = NodeType.tag;
        tagName = DEFAULT_TAG;
        attributes = new LinkedHashMap<>();
        segments = new ArrayList<>();
        children = new ArrayList<>();
    }

    public Node( NodeType type )
    {
        this();
        this.type = type;
    }

    public Node( Node other )
    {
        this();
        type = other.type;
        indent = other.indent;
        row = other.row;
        inline = other.inline;
        tagName = other.tagName;
        rawText = other.rawText;

        for ( String key : other.attributes.keySet() )
        {
            Attribute att = other.attributes.get( key );
            Attribute newAtt = new Attribute( att );
            attributes.put( key, newAtt );
        }

        for ( Segment segment : other.segments )
        {
            segments.add( new Segment( segment ) );
        }

        for ( Node child : other.children )
        {
            children.add( new Node( child ) );
        }
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public static Node newRawNode( String rawText )
    {
        Node node = new Node( Node.NodeType.rawText );
        node.setRawText( rawText );
        return node;
    }

    public NodeType getType()
    {
        return type;
    }

    public Node setType( NodeType type )
    {
        this.type = type;
        return this;
    }

    public NodeType getNodeType()
    {
        return type;
    }

    public Node setIndent( int indent )
    {
        this.indent = indent;
        return this;
    }

    public int getIndent()
    {
        return indent;
    }

    public int getRow()
    {
        return row;
    }

    public void setRow( int row )
    {
        this.row = row;
    }

    public boolean isInline()
    {
        return inline;
    }

    public void setInline( boolean inline )
    {
        this.inline = inline;
    }

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName( String tagName )
    {
        this.tagName = tagName;
    }

    public Map<String, Attribute> getAttributes()
    {
        return attributes;
    }

    public void addAttribute( String key )
    {
        addAttribute( key, null );
    }

    public void addAttribute( String key, String value )
    {
        if ( attributes.containsKey( key ) )
        {
            Attribute att = attributes.get( key );
            if ( att == null || att.getValue() == null || att.getValue().isEmpty() )
            {
                attributes.put( key, att );
            }
            else
            {
                att.setValue( att.getValue() + " " + value );
            }
        }
        else
        {
            Attribute att = new Attribute( key, value );
            attributes.put( key, att );
        }
    }

    public void addAttribute( Attribute att )
    {
        if ( att == null )
        {
            return;
        }

        attributes.put( att.getKey(), att );
    }

    public Attribute getAttribute( String key )
    {
        return attributes.get( key );
    }

    public Attribute removeAttribute( String key )
    {
        return attributes.remove( key );
    }

    /**
     * Same as addAttribute but overrides any value that was there before.
     */
    public void setAttribute( String key, String value )
    {
        removeAttribute( key );
        addAttribute( key, value );
    }

    public String getId()
    {
        Attribute att = attributes.get( ATT_ID );

        return att == null ? null : att.getValue();
    }

    public Node findByElementId( String id )
    {
        if ( getId() != null && getId().equals( id ) )
        {
            return this;
        }

        for ( Node child : getChildren() )
        {
            Node foundNode = child.findByElementId( id );
            if ( foundNode != null )
            {
                return foundNode;
            }
        }

        return null;
    }

    public Set<String> getStyleClasses()
    {
        HashSet<String> set = new HashSet<>();
        Attribute att = attributes.get( ATT_CLASS );
        if ( att == null )
        {
            return set;
        }

        for ( String string : att.getValue().split( "\\s" ) )
        {
            set.add( string );
        }

        return set;
    }

    public boolean hasStyleClass( String clss )
    {
        Attribute att = attributes.get( ATT_CLASS );
        if ( att == null )
        {
            return false;
        }

        for ( String string : att.getValue().split( "\\s" ) )
        {
            if ( clss.equals( string ) )
            {
                return true;
            }
        }

        return false;
    }

    public Node findByStyleClass( String clss )
    {
        if ( hasStyleClass( clss ) )
        {
            return this;
        }

        for ( Node child : getChildren() )
        {
            Node foundNode = child.findByStyleClass( clss );
            if ( foundNode != null )
            {
                return foundNode;
            }
        }

        return null;
    }

    public void addSegment( Segment segment )
    {
        segments.add( segment );
    }

    public void addText( String text )
    {
        segments.add( new Segment( text ) );
    }

    public List<Segment> getSegments()
    {
        return segments;
    }

    public List<Node> getChildren()
    {
        return children;
    }

    public void clearChildren()
    {
        children.clear();
    }

    public void addChild( Node node )
    {
        children.add( node );
    }

    public int insert( String insertionName, Node insertNode )
    {
        int count = 0;
        if ( type == NodeType.insertion && insertionName.equals( tagName ) )
        {
            addChild( insertNode );
            count++;
        }

        // If no insertion happened at this node. Try to find them in child nodes.
        if ( count == 0 )
        {
            for ( Node node : getChildren() )
            {
                count += node.insert( insertionName, insertNode );
            }
        }

        return count;
    }

    public int insert( String insertionName, String text )
    {
        Node rawNode = new Node( NodeType.rawText );
        rawNode.setRawText( text );
        return insert( insertionName, rawNode );
    }

    public String getRawText()
    {
        return rawText;
    }

    public Node setRawText( String rawText )
    {
        this.rawText = rawText;

        return this;
    }
}
