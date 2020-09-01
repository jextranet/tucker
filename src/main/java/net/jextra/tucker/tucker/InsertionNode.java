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
 */
public class InsertionNode extends Node
{
    // ============================================================
    // Fields
    // ============================================================

    private String name;
    private List<Node> children;

    // ============================================================
    // Constructors
    // ============================================================

    public InsertionNode()
    {
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
    public void write( OutputState state, boolean inline )
    {
        PrintWriter writer = state.getWriter();
        OutputState childState = new OutputState( state, state.getDepth() );

        state.writeIndent();
        writer.println( "<!-- begin insPoint:" + name + " -->" );

        for ( Node child : children )
        {
            child.write( childState, inline );
        }

        state.writeIndent();
        writer.println( "<!-- end insPoint:" + name + " -->" );
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void insert( Node node )
    {
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
