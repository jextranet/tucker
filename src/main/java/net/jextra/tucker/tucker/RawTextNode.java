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

public class RawTextNode extends Node
{
    // ============================================================
    // Fields
    // ============================================================

    private String text;

    // ============================================================
    // Constructors
    // ============================================================

    public RawTextNode()
    {
    }

    public RawTextNode( String text )
    {
        this();
        this.text = text;
    }

    public RawTextNode( RawTextNode other )
    {
        this();
        text = other.getText();
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
        return NodeType.rawText;
    }

    @Override
    public void write( OutputContext ctx, boolean inline )
    {
        PrintWriter writer = ctx.getWriter();
        writer.write( text );
    }

    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    public void append( String extra )
    {
        this.text += extra;
    }
}
