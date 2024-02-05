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

import java.util.*;

/**
 * Specifics about the context of the tag, such as variables, etc.
 */
public class ScopeContext
{
    // ============================================================
    // Fields
    // ============================================================

    public static final String INDENT_WHITESPACE = "  ";

    private Deque<Scope> stack;
    private int indent;

    // ============================================================
    // Constructors
    // ============================================================

    public ScopeContext()
    {
        stack = new ArrayDeque<>();
    }

    public ScopeContext( ScopeContext other )
    {
        this();
        for ( Scope scope : other.stack )
        {
            stack.add( new Scope( scope ) );
        }
        indent = other.indent;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public void push( Scope scope )
    {
        stack.push( scope );
    }

    public Scope pop()
    {
        return stack.pop();
    }

    public String getVariable( String name )
    {
        //        for ( Scope s : stack )
        //        {
        //            if ( !s.hasVariable( name ) )
        //            {
        //                continue;
        //            }
        //
        //            return s.getVariable( name );
        //        }
        Scope s = stack.peek();
        if ( s != null )
        {
            return s.getVariable( name );
        }

        return null;
    }

    public boolean getBoolean( String name )
    {
        //        for ( Scope s : stack )
        //        {
        //            if ( !s.hasBoolean( name ) )
        //            {
        //                continue;
        //            }
        //
        //            return s.getBoolean( name );
        //        }
        Scope s = stack.peek();
        if ( s != null )
        {
            return s.getBoolean( name );
        }

        return false;
    }

    public int getIndent()
    {
        return indent;
    }

    public void setIndent( int indent )
    {
        this.indent = indent;
    }

    public String getIndentWhitespace()
    {
        return getIndentWhitespace( 0 );
    }

    public String getIndentWhitespace( int extraIndent )
    {
        StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < indent + extraIndent; i++ )
        {
            builder.append( INDENT_WHITESPACE );
        }

        return builder.toString();
    }

    /**
     * Find binding down through the entire scope stack.
     */
    public Hook findHook( Node node )
    {
        for ( HookBinding binding : stack.peek().getBindings() )
        {
            if ( binding.matches( node ) )
            {
                return binding.getHook();
            }
        }

        return null;
    }
}
