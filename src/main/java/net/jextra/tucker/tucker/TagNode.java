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

import java.io.*;
import java.util.*;

/**
 * A {@link Node} that represents an HTML tag (e.g. div) and its attributes and inner text.
 */
public class TagNode extends Node
{
    // ============================================================
    // Fields
    // ============================================================

    public static final String ATT_ID = "id";
    public static final String ATT_CLASS = "class";

    private String name;
    private Map<String, Attribute> attributes;
    private List<Segment> segments;
    private List<Node> children;

    // ============================================================
    // Constructors
    // ============================================================

    public TagNode()
    {
        attributes = new LinkedHashMap<>();
        segments = new ArrayList<>();
        children = new ArrayList<>();
    }

    public TagNode( String name )
    {
        this();
        this.name = name;
    }

    public TagNode( TagNode other )
    {
        this();
        this.name = other.name;

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
            children.add( child.cloneNode() );
        }
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @Override
    public NodeType getNodeType()
    {
        return NodeType.tag;
    }

    @Override
    public void write( OutputContext ctx, boolean inline )
    {
        PrintWriter writer = ctx.getWriter();
        OutputContext childCtx = new OutputContext( ctx, ctx.getDepth() + 1 );

        //
        // Write indent and tag
        //
        if ( !inline )
        {
            ctx.writeIndent();
        }
        writer.write( '<' );
        ctx.writeString( name );

        //
        // Write attributes
        //
        // Always write id first, then class, then everything else.
        if ( attributes.containsKey( "id" ) )
        {
            writeAttribute( childCtx, "id" );
        }

        if ( attributes.containsKey( "class" ) )
        {
            writeAttribute( childCtx, "class" );
        }

        for ( String key : attributes.keySet() )
        {
            if ( "id".equals( key ) || "class".equals( key ) )
            {
                continue;
            }

            writeAttribute( childCtx, key );
        }
        writer.write( ">" );

        for ( Segment segment : segments )
        {
            switch ( segment.getType() )
            {
                case text:
                    ctx.writeString( segment.getValue() );
                    break;

                case inline:
                    segment.getTagNode().write( ctx, true );
                    break;
            }
        }

        //
        // Write children. Note, currently sitting on same line as closed ">" in case inline text is possible.
        //
        boolean multiline = false;
        for ( Node child : children )
        {
            if ( writeChild( childCtx, child, multiline ) )
            {
                multiline = true;
            }
        }

        //
        // Closing tag
        //
        if ( multiline )
        {
            // writer.println();
            ctx.writeIndent();
        }
        writer.write( "</" );
        ctx.writeString( name );
        if ( !inline )
        {
            writer.println( '>' );
        }
        else
        {
            writer.write( ">" );
        }
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public Map<String, Attribute> getAttributes()
    {
        return attributes;
    }

    @Override
    public void addAttribute( String key )
    {
        addAttribute( key, null );
    }

    @Override
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

    public Attribute removeAttribute( String key )
    {
        return attributes.remove( key );
    }

    /**
     * This does a bit more work than just a simple addAttribute to make sure there are no redundant classes in the list.
     */
    public boolean addStyleClass( String clss )
    {
        Attribute att = attributes.get( ATT_CLASS );
        if ( att != null )
        {
            String value = att.getValue();

            // If no class defined, set this as the new value
            if ( value == null || value.isEmpty() )
            {
                att.setValue( clss );

                return true;
            }

            for ( String string : value.split( "\\s" ) )
            {
                if ( clss.equals( string ) )
                {
                    // Already has this class
                    return false;
                }
            }

            att.setValue( att.getValue() + " " + clss );
        }
        else
        {
            att = new Attribute( ATT_CLASS, clss );
            attributes.put( ATT_CLASS, att );
        }

        return true;
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

    @Override
    public void addSegment( Segment segment )
    {
        segments.add( segment );
    }

    public List<Segment> getSegments()
    {
        return segments;
    }

    public List<Node> getChildren()
    {
        return children;
    }

    public void clear()
    {
        children.clear();
    }

    public void addChild( Node node )
    {
        children.add( node );
    }

    public int insert( String insertionName, Node node )
    {
        int count = 0;

        for ( Node child : children )
        {
            switch ( child.getNodeType() )
            {
                // Recurse
                case tag:
                    count += ( (TagNode) child ).insert( insertionName, node );
                    break;

                case insertion:
                    InsertionNode insertionNode = ( (InsertionNode) child );
                    if ( insertionName.equals( insertionNode.getName() ) )
                    {
                        insertionNode.insert( node );
                        count++;
                    }
            }
        }

        return count;
    }

    public TagNode getFirstElement()
    {
        for ( Node c : children )
        {
            switch ( c.getNodeType() )
            {
                case tag:
                    return (TagNode) c;
            }
        }

        return null;
    }

    public TagNode findFirstElementByClass( String clss )
    {
        for ( Node c : children )
        {
            if ( c.getNodeType() != NodeType.tag )
            {
                continue;
            }

            TagNode element = (TagNode) c;
            if ( element.hasStyleClass( clss ) )
            {
                return element;
            }

            // Recurse.
            TagNode child = element.findFirstElementByClass( clss );
            if ( child != null )
            {
                return child;
            }
        }

        return null;
    }

    // ----------
    // private
    // ----------

    private void writeAttribute( OutputContext ctx, String key )
    {
        Attribute att = attributes.get( key );
        if ( att == null )
        {
            return;
        }

        key = ctx.transformString( key );
        if ( key == null || key.trim().isEmpty() )
        {
            return;
        }

        // This is the case where the key is specified by itself with no equals (e.g. checked)
        if ( att.getValue() == null )
        {
            ctx.getWriter().print( " " );
            ctx.writeString( key );
        }
        else
        {
            String value = ctx.transformString( att.getValue() );
            if ( value == null )
            {
                // Special case, Instead of something like checked="", this simply means no attribute.
            }
            else
            {
                PrintWriter writer = ctx.getWriter();
                writer.print( " " );
                ctx.writeString( key );
                writer.print( "=\"" );
                ctx.writeString( value );
                writer.print( "\"" );
            }
        }
    }

    private boolean writeChild( OutputContext ctx, Node node, boolean oldMultiline )
    {
        boolean multiline = false;

        switch ( node.getNodeType() )
        {
            case tag:
            case insertion:
                if ( !oldMultiline )
                {
                    ctx.getWriter().println();
                }
                multiline = true;
                node.write( ctx, false );
                break;

            default:
                node.write( ctx, false );
                break;
        }

        return multiline;
    }
}
