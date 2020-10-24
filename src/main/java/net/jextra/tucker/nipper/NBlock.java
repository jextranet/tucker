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

import java.io.*;
import java.util.*;

public class NBlock
{
    // ============================================================
    // Fields
    // ============================================================

    private List<NLine> lines;
    private Map<String, String> varValues;

    // ============================================================
    // Constructors
    // ============================================================

    public NBlock()
    {
        lines = new ArrayList<>();
        varValues = new LinkedHashMap<>();
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public NBlock addBlock( NBlock other )
    {
        for ( NLine line : other.lines )
        {
            lines.add( line );
        }

        for ( String key : other.varValues.keySet() )
        {
            varValues.put( key, other.varValues.get( key ) );
        }

        return this;
    }

    public void addLine( NLine line )
    {
        lines.add( line );
    }

    public void setVariableValue( String name, String value )
    {
        varValues.put( name, value );
    }

    public Map<String, String> getVarValues()
    {
        return varValues;
    }

    public List<NLine> getRoots()
    {
        List<NLine> roots = new ArrayList<>();
        for ( NLine line : lines )
        {
            if ( line.getParent() == null )
            {
                roots.add( line );
            }
        }

        return roots;
    }

    public String substituteVariables( String string )
    {
        ArrayList<Fragment> frags = new ArrayList<>();
        frags.add( new Fragment( string ) );

        for ( String key : varValues.keySet() )
        {
            for ( int s = 0; s < 2; s++ )
            {
                String name = s == 0 ? "$" + key : "$(" + key + ")";
                String value = varValues.get( key );
                ArrayList<Fragment> newFrags = new ArrayList<>();
                for ( Fragment f : frags )
                {
                    // If a value from a variable, do not let double-substitutions.
                    if ( f.getValue() != null )
                    {
                        newFrags.add( f );
                    }
                    else
                    {
                        newFrags.addAll( fragment( f.getText(), name, value ) );
                    }
                }
                frags = newFrags;
            }
        }

        StringBuilder builder = new StringBuilder();
        for ( Fragment f : frags )
        {
            builder.append( f.toString() );
        }

        return builder.toString();
    }

    @Override
    public String toString()
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter( stringWriter );
        NPrinter printer = new NPrinter( writer );
        printer.print( this );
        writer.close();

        return stringWriter.toString();
    }

    // ----------
    // private
    // ----------

    private List<Fragment> fragment( String string, String key, String value )
    {
        ArrayList<Fragment> frags = new ArrayList<>();

        int i = string.indexOf( key );
        if ( i < 0 )
        {
            frags.add( new Fragment( string ) );
            return frags;
        }

        int afterI = i + key.length();

        // Found at end?
        if ( afterI >= string.length() )
        {
            if ( i > 0 )
            {
                frags.add( new Fragment( string.substring( 0, i ) ) );
            }
            frags.add( new Fragment( string.substring( i, afterI ), value ) );
        }
        // Found in middle?
        else if ( afterI >= string.length() || string.charAt( afterI ) == ' ' )
        {
            if ( i > 0 )
            {
                frags.add( new Fragment( string.substring( 0, i ) ) );
            }
            frags.add( new Fragment( string.substring( i, afterI ), value ) );
            frags.addAll( fragment( string.substring( afterI ), key, value ) );
        }
        else
        {
            frags.add( new Fragment( string ) );
        }

        return frags;
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    private static class Fragment
    {
        private String text;
        private String value;

        public Fragment( String text )
        {
            this.text = text;
        }

        public Fragment( String text, String value )
        {
            this.text = text;
            this.value = value;
        }

        public String getText()
        {
            return text;
        }

        public void setText( String text )
        {
            this.text = text;
        }

        public String getValue()
        {
            return value;
        }

        public void setValue( String value )
        {
            this.value = value;
        }

        public String toString()
        {
            return value == null ? text : value;
        }
    }
}
