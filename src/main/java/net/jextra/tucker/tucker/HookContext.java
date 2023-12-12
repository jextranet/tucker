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

public class HookContext extends ScopeContext
{
    // ============================================================
    // Fields
    // ============================================================

    private Node node;
    private PageContext page;

    // ============================================================
    // Constructors
    // ============================================================

    public HookContext( PageContext page, ScopeContext scope, Node node )
    {
        super( scope );
        this.page = page;
        this.node = node;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public Node getNode()
    {
        return node;
    }

    public void setNode( Node node )
    {
        this.node = node;
    }

    public PageContext getPage()
    {
        return page;
    }

    public void setPage( PageContext page )
    {
        this.page = page;
    }
}
