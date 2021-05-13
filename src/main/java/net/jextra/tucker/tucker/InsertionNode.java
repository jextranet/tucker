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
 * A Node that represents an insertion point into a block.
 * <pre>
 *      >insertion-point
 * </pre>
 */
public class InsertionNode extends Node
{
    // ============================================================
    // Fields
    // ============================================================

    private boolean auto;
    private String name;
    private Map<String, Attribute> attributes;
    private List<Node> children;

    // ============================================================
    // Constructors
    // ============================================================

    public InsertionNode()
    {
        attributes = new LinkedHashMap<>();
        children = new ArrayList<>();
    }

    public InsertionNode( String name )
    {
        this();
        this.name = name;
    }

    public InsertionNode( InsertionNode other )
    {
        this();
        this.name = other.name;

        for ( String key : other.attributes.keySet() )
        {
            attributes.put( key, new Attribute( other.attributes.get( key ) ) );
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
        return NodeType.insertion;
    }

    @Override
    public void write( OutputContext ctx, boolean inline )
    {
        OutputContext childCtx = new OutputContext( ctx, ctx.getDepth() );

        ctx.writeIndent();
        //        writer.println( "<!-- begin insertionPoint:" + name + " -->" );

        for ( Node child : children )
        {
            child.write( childCtx, inline );
        }

        ctx.writeIndent();
        //        writer.println( "<!-- end insertionPoint:" + name + " -->" );
    }

    public boolean isAuto()
    {
        return auto;
    }

    public void setAuto( boolean auto )
    {
        this.auto = auto;
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

    public void insert( Node node )
    {
        if ( node == null )
        {
            throw new RuntimeException( "Child node cannot be null " + node );
        }
        children.add( node );
        // Copy nodes from block.
        // for (Node blockNode : block.getNodes()) {
        // Node node = blockNode.cloneNode();
        //
        // children.add(node);
        // }
    }

    public List<Node> getChildren()
    {
        return children;
    }
}
