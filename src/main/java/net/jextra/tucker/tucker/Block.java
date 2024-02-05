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

import java.lang.reflect.*;
import java.util.*;

/**
 * A Block is a named grouping of {@link Node}s. It does not manifest into any specific tag in the output HTML.
 */
public class Block extends Node
{
    // ============================================================
    // Fields
    // ============================================================

    private Scope scope;
    private NodeWriter writer;

    // ============================================================
    // Constructors
    // ============================================================

    public Block()
    {
        super( NodeType.block );
        scope = new Scope();
        writer = new NodeWriter();
    }

    public Block( String name )
    {
        this();
        setTagName( name );
    }

    public Block( Block other )
    {
        super( other );
        scope = new Scope( other.scope );
        writer = new NodeWriter( other.writer );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public Scope getScope()
    {
        return scope;
    }

    public void setScope( Scope scope )
    {
        this.scope = scope;
    }

    public NodeWriter getWriter()
    {
        return writer;
    }

    public void setWriter( NodeWriter writer )
    {
        this.writer = writer;
    }

    public Set<String> getVariableNames()
    {
        return scope.getVariableNames();
    }

    public String getVariable( String name )
    {
        return scope.getVariable( name );
    }

    public Block setVariable( String name, String value )
    {
        return setVariable( name, value, true );
    }

    public Block setVariable( String name, String... values )
    {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for ( String v : values )
        {
            if ( v == null )
            {
                continue;
            }
            else if ( builder.length() > 0 )
            {
                builder.append( " " );
            }
            builder.append( v );
            count++;
        }

        return setVariable( name, count > 0 ? builder.toString() : null );
    }

    public Block setVariable( String name, Collection<String> values )
    {
        if ( values == null )
        {
            return setVariable( name, (String) null );
        }

        StringBuilder builder = new StringBuilder();
        int count = 0;
        for ( String v : values )
        {
            if ( v == null )
            {
                continue;
            }
            else if ( builder.length() > 0 )
            {
                builder.append( " " );
            }
            builder.append( v );
            count++;
        }

        return setVariable( name, count > 0 ? builder.toString() : null );
    }

    public Block setVariable( String name, String value, Boolean encode )
    {
        scope.setVariable( name, value, encode );

        return this;
    }

    public Block setBoolean( String name )
    {
        return setBoolean( name, true );
    }

    public Block clearBoolean( String name )
    {
        scope.clearBoolean( name );
        return this;
    }

    public Block setBoolean( String name, boolean value )
    {
        scope.setBoolean( name, value );
        return this;
    }

    public boolean getBoolean( String name )
    {
        return scope.getBoolean( name );
    }

    public Node findByStyleClass( String clss )
    {
        for ( Node node : getChildren() )
        {
            if ( node.getNodeType() != Node.NodeType.tag )
            {
                continue;
            }

            if ( node.hasStyleClass( clss ) )
            {
                return node;
            }

            Node child = node.findByStyleClass( clss );
            if ( child != null )
            {
                return child;
            }
        }

        return null;
    }

    public void bind( String tag, Hook hook )
    {
        scope.bind( tag, hook );
    }

    public <T extends Hook> T bind( String tag, Class<T> elementClass )
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        return scope.bind( tag, elementClass );
    }

    public String render( PageContext page )
    {
        return render( page, getIndent() );
    }

    public String render( PageContext page, int indent )
    {
        writer.setPageContext( page );
        writer.setIndent( indent );
        return writer.render( this );
    }

    /**
     * @use render(PageContext)
     */
    @Deprecated
    public void write( OutputContext ctx, boolean inline )
    {
        ctx.write( this );
    }

    @Override
    public String toString()
    {
        return render( new NodeWriter.PageContextStub() );
    }

    // ----------
    // private
    // ----------

    private Node findByElementId( Node node, String id )
    {
        if ( id.equals( node.getId() ) )
        {
            return node;
        }

        for ( Node child : node.getChildren() )
        {
            Node e = findByElementId( child, id );
            if ( e != null )
            {
                return e;
            }
        }

        return null;
    }
}
