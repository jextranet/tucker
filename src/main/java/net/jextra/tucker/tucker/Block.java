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
 * A block is a group of HTML Elements, potentially named.
 */
public class Block extends Node
{
    // ============================================================
    // Fields
    // ============================================================

    private String name;
    private ArrayList<Node> nodes;
    private Map<String, String> varValues;

    // ============================================================
    // Constructors
    // ============================================================

    public Block()
    {
        nodes = new ArrayList<>();
        varValues = new HashMap<>();
    }

    public Block( String name )
    {
        this();

        this.name = name;
    }

    public Block( Block other )
    {
        this();

        this.name = other.name;
        for ( Node element : other.nodes )
        {
            nodes.add( element.cloneNode() );
        }
        for ( String key : other.varValues.keySet() )
        {
            varValues.put( key, other.getVariable( key ) );
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
        return NodeType.block;
    }

    @Override
    public void write( OutputContext ctx, boolean inline )
    {
        // Copy varValues to the context.
        ctx.clearVariableValues();
        for ( String key : varValues.keySet() )
        {
            String value = varValues.get( key );
            ctx.setVariableValue( key, value );
        }

        for ( Node node : nodes )
        {
            node.write( ctx, false );
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

    public void addNode( Node node )
    {
        if ( node == null )
        {
            throw new RuntimeException( "A block node cannot be null" );
        }

        nodes.add( node );
    }

    public List<Node> getNodes()
    {
        return nodes;
    }

    public TagNode getFirstElement()
    {
        for ( Node node : nodes )
        {
            if ( node.getNodeType() == Node.NodeType.tag )
            {
                return (TagNode) node;
            }
        }

        return null;
    }

    public void clear()
    {
        nodes.clear();
    }

    public Set<String> getVariableNames()
    {
        return varValues.keySet();
    }

    public String getVariable( String name )
    {
        return varValues.get( name );
    }

    public Block setVariable( String name, String value )
    {
        varValues.put( name, value );

        return this;
    }

    public int insert( String insertionName, Block block )
    {
        int count = 0;

        for ( Node node : nodes )
        {
            switch ( node.getNodeType() )
            {
                case tag:
                    count += ( (TagNode) node ).insert( insertionName, block );
                    break;

                // Special case where there is an insertion point at the root level
                case insertion:
                    InsertionNode insertionNode = ( (InsertionNode) node );
                    if ( insertionName.equals( insertionNode.getName() ) )
                    {
                        insertionNode.insert( block );
                        count++;
                    }
            }
        }

        return count;
    }

    public TagNode findByElementId( String id )
    {
        for ( Node node : nodes )
        {
            TagNode element = findByElementId( node, id );
            if ( element != null )
            {
                return element;
            }
        }

        return null;
    }

    public TagNode findFirstElementByClass( String clss )
    {
        for ( Node node : nodes )
        {
            if ( node.getNodeType() != Node.NodeType.tag )
            {
                continue;
            }

            TagNode element = (TagNode) node;
            if ( element.hasStyleClass( clss ) )
            {
                return element;
            }

            TagNode child = element.findFirstElementByClass( clss );
            if ( child != null )
            {
                return child;
            }
        }

        return null;
    }

    @Override
    public String toString()
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter( stringWriter );
        OutputContext ctx = new OutputContext( writer );
        write( ctx, false );
        writer.close();

        return stringWriter.toString();
    }

    // ----------
    // private
    // ----------

    private TagNode findByElementId( Node node, String id )
    {
        switch ( node.getNodeType() )
        {
            case tag:
                TagNode element = (TagNode) node;
                if ( id.equals( element.getId().toString() ) )
                {
                    return element;
                }

                for ( Node child : element.getChildren() )
                {
                    TagNode e = findByElementId( child, id );
                    if ( e != null )
                    {
                        return e;
                    }
                }
                break;

            case insertion:
                InsertionNode insertionNode = (InsertionNode) node;
                for ( Node child : insertionNode.getChildren() )
                {
                    TagNode e = findByElementId( child, id );
                    if ( e != null )
                    {
                        return e;
                    }
                }
                break;
        }

        return null;
    }
}
