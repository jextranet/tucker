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

/**
 * A Node is typically a single line in the .thtml file. It can be either a TagNode (typical html tag with attributes and text), an InsertionNode to
 * identify location that insertions can occur, or a RawTextNode to represent
 */
public abstract class Node
{
    // ============================================================
    // Enums
    // ============================================================

    enum NodeType
    {
        block,
        tag,
        insertion,
        rawText
    }

    // ============================================================
    // Fields
    // ============================================================

    private int indent;

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public abstract NodeType getNodeType();

    public abstract void write( OutputContext ctx, boolean inline );

    public Node cloneNode()
    {
        switch ( getNodeType() )
        {
            case block:
                return new Block( (Block) this );

            case tag:
                return new TagNode( (TagNode) this );

            case insertion:
                return new InsertionNode( (InsertionNode) this );

            case rawText:
                return new RawTextNode( (RawTextNode) this );
        }

        throw new RuntimeException( "Unexpected node type to clone: " + getNodeType() );
    }

    public void setIndent( int indent )
    {
        this.indent = indent;
    }

    public int getIndent()
    {
        return indent;
    }

    public void addAttribute( String key )
    {
        // Default is to throw away
    }

    public void addAttribute( String key, String value )
    {
        // Default is to throw away
    }

    public void addSegment( Segment segment )
    {
        // Default is to throw away
    }
}
