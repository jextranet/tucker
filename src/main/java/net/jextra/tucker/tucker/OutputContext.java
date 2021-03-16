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
import java.util.regex.*;

/**
 * Used to store the state for output of a block.
 */
public class OutputContext
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

    public OutputContext( PrintWriter writer )
    {
        this.writer = writer;
        varValues = new HashMap<>();
    }

    public OutputContext( OutputContext other, int depth )
    {
        this( other.writer );
        for ( String key : other.varValues.keySet() )
        {
            varValues.put( key, other.varValues.get( key ) );
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
        if ( name == null )
        {
            return;
        }

        varValues.put( name, value );
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
        String string = processString( value );
        if ( string != null )
        {
            writer.write( string );
        }
    }

    public String processString( String value )
    {
        if ( value == null )
        {
            return null;
        }

        //
        // See if the value is the special case of being a single variable. If it is mark it for later processing.
        //
        Pattern pattern = Pattern.compile( Tucker.VAR_START + ".*" + Tucker.VAR_END );
        boolean singleVar = pattern.matcher( value ).matches();

        value = value.replace( Tucker.LEFT_BRACE, '{' );
        value = value.replace( Tucker.RIGHT_BRACE, '}' );
        value = value.replace( Tucker.BACK_TICK, '`' );
        value = value.replace( "<", Tucker.LT );
        value = value.replace( ">", Tucker.GT );

        for ( String key : varValues.keySet() )
        {
            String varValue = varValues.get( key );
            value = value.replace( Tucker.VAR_START + key + Tucker.VAR_END, varValue == null ? "" : varValue );
        }

        // Special case if the single variable was never set, the value should be null (not "").
        if ( singleVar && pattern.matcher( value ).matches() )
        {
            return null;
        }

        // Any variables not found should be replaced with blank.
        value = value.replaceAll( Tucker.VAR_START + ".*" + Tucker.VAR_END, "" );

        return value;
    }
}
