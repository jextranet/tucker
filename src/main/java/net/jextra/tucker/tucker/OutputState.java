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
 * Used to store the state for output of a block.
 */
public class OutputState
{
    // ============================================================
    // Fields
    // ============================================================

    private PrintWriter writer;
    private int depth;
    private Map<String, String> varValues;

    // ============================================================
    // Constructors
    // ============================================================

    public OutputState( PrintWriter writer )
    {
        this.writer = writer;
        varValues = new HashMap<>();
    }

    public OutputState( OutputState other, int depth )
    {
        this( other.writer );
        for ( String key : other.varValues.keySet() )
        {
            String value = other.varValues.get( key );
            if ( value != null )
            {
                varValues.put( key, value );
            }
        }

        this.depth = depth;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public PrintWriter getWriter()
    {
        return writer;
    }

    public void setWriter( PrintWriter writer )
    {
        this.writer = writer;
    }

    public int getDepth()
    {
        return depth;
    }

    public void setDepth( int depth )
    {
        this.depth = depth;
    }

    public void clearVariableValues()
    {
        varValues.clear();
    }

    public String getVariableValue( String name )
    {
        return varValues.get( name );
    }

    public void setVariableValue( String name, String value )
    {
        if ( value != null )
        {
            varValues.put( name, value );
        }
        else
        {
            varValues.remove( name );
        }
    }

    public void writeIndent()
    {
        writeIndent( depth );
    }

    public void writeIndent( int d )
    {
        for ( int i = 0; i < d; i++ )
        {
            writer.print( "  " );
        }
    }

    public void writeString( String value )
    {
        writer.write( processString( value ) );
    }

    public String processString( String value )
    {
        if ( value == null )
        {
            return value;
        }

        value = value.replace( '\001', '{' );
        value = value.replace( '\002', '}' );

        for ( String key : varValues.keySet() )
        {
            value = value.replace( "\003" + key + "\004", varValues.get( key ) );
        }
        value = value.replaceAll( "\003.*\004", "" );

        return value;
    }
}
