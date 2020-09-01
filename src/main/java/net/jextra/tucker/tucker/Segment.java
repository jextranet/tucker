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
 * A portion on the inner text of a tag. It represents either plain text or an inline.
 */
public class Segment
{
    // ============================================================
    // Enums
    // ============================================================

    public enum Type
    {
        text,
        inline
    }

    // ============================================================
    // Fields
    // ============================================================

    private Type type;
    private String value;
    private TagNode tagNode;

    // ============================================================
    // Constructors
    // ============================================================

    public Segment()
    {
        type = Type.text;
    }

    public Segment( Type type )
    {
        this.type = type;
    }

    public Segment( Type type, String value )
    {
        this.type = type;
        this.value = value;
    }

    public Segment( String text )
    {
        type = Type.text;
        this.value = text;
    }

    public Segment( Segment other )
    {
        type = other.type;
        value = other.value;
        this.tagNode = other.tagNode;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public Type getType()
    {
        return type;
    }

    public void setType( Type type )
    {
        this.type = type;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public Segment append( String text )
    {
        value += text;

        return this;
    }

    public TagNode getTagNode()
    {
        return tagNode;
    }

    public void setTagNode( TagNode tagNode )
    {
        this.tagNode = tagNode;
    }

    @Override
    public String toString()
    {
        return String.format( "[%s] %s", type, value );
    }
}
