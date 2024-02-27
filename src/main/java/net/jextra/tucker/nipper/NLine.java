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

package net.jextra.tucker.nipper;

import java.util.*;

public class NLine
{
    // ============================================================
    // Enums
    // ============================================================

    public enum Type
    {
        comment,
        include,
        variableDeclaration,
        atRule,    // @media, @support
        selector,
        property,
        continuation;
    }

    // ============================================================
    // Fields
    // ============================================================

    private Type type;
    private NLine parent;
    private int row;
    private int spaces;
    private int indent;
    private String content;
    private String name;
    private String value;
    private String comment;
    private ArrayList<NLine> children;

    // ============================================================
    // Constructors
    // ============================================================

    public NLine( int row, int spaces, String content, String comment )
    {
        this.row = row;
        this.spaces = spaces;
        this.content = content;
        this.comment = comment;
        children = new ArrayList<>();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public NLine getParent()
    {
        return parent;
    }

    public void addChild( NLine child )
    {
        children.add( child );
        child.parent = this;
    }

    public boolean hasChildren()
    {
        return children != null && !children.isEmpty();
    }

    public List<NLine> getChildren()
    {
        return children;
    }

    public List<NLine> getChildren( Type type )
    {
        ArrayList<NLine> list = new ArrayList<>();
        for ( NLine line : children )
        {
            if ( line.getType() == type )
            {
                list.add( line );
            }
        }

        return list;
    }

    public int getRow()
    {
        return row;
    }

    public void setRow( int row )
    {
        this.row = row;
    }

    public int getSpaces()
    {
        return spaces;
    }

    public void setSpaces( int spaces )
    {
        this.spaces = spaces;
    }

    public int getIndent()
    {
        return indent;
    }

    public void setIndent( int indent )
    {
        this.indent = indent;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent( String content )
    {
        this.content = content;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    public Type getType()
    {
        return type;
    }

    public void setType( Type type )
    {
        this.type = type;
    }

    @Override
    public String toString()
    {
        String p = String.format( "%3d:-  ", row );
        if ( parent != null )
        {
            p = String.format( "%3d:%-3d", row, parent.getRow() );
        }

        StringBuilder gap = new StringBuilder();
        for ( int i = 0; i < indent; i++ )
        {
            gap.append( "    " );
        }

        if ( comment != null )
        {
            return String.format( "%s %-12s %s%s[%s] %s", p, type, gap, content, comment, name );
        }

        return String.format( "%s %-12s %s%s %s", p, type, gap, content, name );
    }
}
