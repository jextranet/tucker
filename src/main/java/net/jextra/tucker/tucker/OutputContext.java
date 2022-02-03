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
 * Used to store the state for output of blocks.
 */
public class OutputContext
{
    // ============================================================
    // Fields
    // ============================================================

    private PrintWriter writer;
    private int depth;
    private Map<String, String> varValues;
    private Map<String, Boolean> boolValues;
    private Translator translator;

    // ============================================================
    // Constructors
    // ============================================================

    public OutputContext( PrintWriter writer )
    {
        this.writer = writer;
        varValues = new HashMap<>();
        boolValues = new HashMap<>();
    }

    public OutputContext( OutputContext parent, int depth )
    {
        this( parent.writer );
        for ( String key : parent.varValues.keySet() )
        {
            varValues.put( key, parent.varValues.get( key ) );
        }
        for ( String key : parent.boolValues.keySet() )
        {
            boolValues.put( key, parent.boolValues.get( key ) );
        }
        this.translator = parent.translator;

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

    public Translator getTranslator()
    {
        return translator;
    }

    public void setTranslator( Translator translator )
    {
        this.translator = translator;
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

    public boolean getBooleanValue( String name )
    {
        return boolValues.get( name ) == null ? false : boolValues.get( name );
    }

    public void setBooleanValue( String name, Boolean value )
    {
        if ( name == null )
        {
            return;
        }

        boolValues.put( name, value );
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
        String string = transformString( value );
        if ( string != null )
        {
            writer.write( string );
        }
    }

    public String transformString( String value )
    {
        if ( value == null )
        {
            return null;
        }

        //
        // Replace all replacement characters.
        //
        value = value.replace( Tucker.LEFT_BRACE, '{' );
        value = value.replace( Tucker.RIGHT_BRACE, '}' );
        value = value.replace( Tucker.BACK_TICK, '`' );
        value = value.replace( "<", Tucker.LT );
        value = value.replace( ">", Tucker.GT );

        //
        // Replace variable values
        //
        int varReplacedCount = 0;
        int varNotSetCount = 0;
        Pattern varPattern = Pattern.compile(
            "([^" + Tucker.VAR_START + "]*)" + Tucker.VAR_START + "([^" + Tucker.VAR_END + "]*)" + Tucker.VAR_END + "(.*)" );
        for ( Matcher m = varPattern.matcher( value ); m.matches(); m = varPattern.matcher( value ) )
        {
            String var = m.group( 2 );
            String varValue = varValues.get( var );
            if ( varValue == null )
            {
                varNotSetCount++;
                value = m.group( 1 ) + m.group( 3 );
            }
            else
            {
                varReplacedCount++;
                value = m.group( 1 ) + varValue + m.group( 3 );
            }
        }

        //
        // Replace boolean values
        //
        Pattern boolPattern = Pattern.compile(
            "([^" + Tucker.BOOL_START + "]*)" + Tucker.BOOL_START + "([^" + Tucker.BOOL_END + "]*)" + Tucker.BOOL_END + "(.*)" );
        for ( Matcher m = boolPattern.matcher( value ); m.matches(); m = boolPattern.matcher( value ) )
        {
            String var = m.group( 2 );
            System.out.println( "var=" + var );
            boolean boolValue = boolValues.get( var ) == null ? false : boolValues.get( var );
            System.out.println( "boolValue=" + boolValue );
            if ( boolValue )
            {
                varReplacedCount++;
                value = m.group( 1 ) + var + m.group( 3 );
            }
            else
            {
                varNotSetCount++;
                value = m.group( 1 ) + m.group( 3 );
            }
        }

        //
        // Translate any phrases on the line.
        //
        if ( translator != null )
        {
            Pattern phrasePattern = Pattern.compile(
                "([^" + Tucker.PHRASE_START + "]*)" + Tucker.PHRASE_START + "([^" + Tucker.PHRASE_END + "]*)" + Tucker.PHRASE_END + "(.*)" );
            for ( Matcher m = phrasePattern.matcher( value ); m.matches(); m = phrasePattern.matcher( value ) )
            {
                value = m.group( 1 ) + translate( m.group( 2 ) ) + m.group( 3 );
            }
        }

        // Clear our the phrase markers if they were not replaced.
        value = value.replaceAll( "" + Tucker.PHRASE_START, "" );
        value = value.replaceAll( "" + Tucker.PHRASE_END, "" );

        //
        // Special case if the single variable was never set, the value should be null (not "").
        //
        if ( varReplacedCount == 0 && varNotSetCount == 1 && value.trim().isEmpty() )
        {
            return null;
        }

        return value;
    }

    // ----------
    // protected
    // ----------

    protected String translate( String sourceString )
    {
        transformString( "translate(" + translator + ") " + sourceString );
        if ( sourceString == null || translator == null )
        {
            return sourceString;
        }

        try
        {
            String targetString = translator.translate( sourceString );

            return targetString == null ? sourceString : targetString;
        }
        catch ( Exception e )
        {
            // Default is to fail the translation and do nothing.
            return sourceString;
        }
    }
}
