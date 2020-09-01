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

public class NPrinter
{
    // ============================================================
    // Fields
    // ============================================================

    private PrintWriter out;
    private OutputParams p;
    private NBlock model;

    // ============================================================
    // Constructors
    // ============================================================

    public NPrinter( PrintWriter out, OutputParams params )
    {
        this.out = out;
        this.p = params;
    }

    public NPrinter( PrintWriter out )
    {
        this( out, new OutputParams() );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public void print( NBlock model )
    {
        this.model = model;
        printLines( 0, model.getRoots() );
    }

    // ----------
    // private
    // ----------

    private void printLines( int indent, List<NLine> list )
    {
        for ( NLine line : list )
        {
            switch ( line.getType() )
            {
                case wrapper:
                    printIndent( indent );
                    printValue( line.getContent() );
                    print( p.parentStart );
                    printCr();
                    printLines( indent + 1, line.getChildren() );
                    printIndent( indent );
                    print( p.parentEnd );
                    printCr();
                    if ( indent == 0 )
                    {
                        printCr();
                    }
                    break;

                case selector:
                    List<NLine> props = line.getChildren( NLine.Type.property );
                    if ( !props.isEmpty() )
                    {
                        for ( String fullSelector : expandSelector( line ) )
                        {
                            printIndent( indent );
                            print( fullSelector );
                            print( p.parentStart );
                            printCr();
                            printLines( indent + 1, props );
                            printIndent( indent );
                            print( p.parentEnd );
                            printCr();
                            if ( indent == 0 )
                            {
                                printCr();
                            }
                        }
                    }

                    List<NLine> subs = line.getChildren( NLine.Type.selector );
                    if ( !subs.isEmpty() )
                    {
                        printLines( indent, subs );
                    }
                    break;

                case property:
                    printIndent( indent );
                    print( line.getName() + p.propOp );
                    printValue( line.getValue() );

                    List<NLine> continuations = line.getChildren( NLine.Type.continuation );
                    if ( !continuations.isEmpty() )
                    {
                        printCr();
                        boolean hasPrior = false;
                        for ( NLine cLine : continuations )
                        {
                            if ( hasPrior )
                            {
                                printCr();
                            }
                            printIndent( indent + 1 );
                            print( cLine.getContent() );
                            hasPrior = true;
                        }
                    }

                    print( p.propEnd );
                    printCr();
                    break;

                case continuation:
                    printIndent( indent );
                    printValue( line.getContent() );
                    printCr();
                    printLines( indent, line.getChildren( NLine.Type.continuation ) );
                    break;

                case comment:
                    // Ignore comment lines
                    break;
            }
        }
    }

    private void printIndent( int indent )
    {
        if ( p.indent == null )
        {
            return;
        }

        StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < indent; i++ )
        {
            out.print( p.indent );
        }
    }

    private void printCr()
    {
        if ( p.useReturns )
        {
            out.println();
        }
    }

    private void print( String string )
    {
        out.print( string );
    }

    private void printValue( String string )
    {
        if ( string == null )
        {
            return;
        }

        out.print( model.substituteVariables( string ) );
    }

    private List<String> expandSelector( NLine line )
    {
        if ( line.getType() != NLine.Type.selector )
        {
            ArrayList<String> list = new ArrayList<>();
            list.add( "" );

            return list;
        }

        ArrayList<String> prefixes = new ArrayList<>();
        if ( line.getParent() != null )
        {
            List<String> parentList = expandSelector( line.getParent() );
            for ( String string : parentList )
            {
                prefixes.add( string );
            }
        }
        else
        {
            prefixes.add( "" );
        }

        ArrayList<String> selectors = new ArrayList<>();
        for ( String prefix : prefixes )
        {
            for ( String suffix : splitSelector( line.getContent() ) )
            {
                StringBuilder builder = new StringBuilder();
                builder.append( prefix );
                if ( prefix.length() > 0 && !line.getContent().startsWith( ":" ) )
                {
                    builder.append( " " );  // inside selector
                }

                builder.append( suffix );
                selectors.add( builder.toString() );
            }
        }

        return selectors;
    }

    /**
     * Selectors with commas need to be made separate.
     */
    private List<String> splitSelector( String string )
    {
        ArrayList<String> list = new ArrayList<>();
        for ( String v : string.split( "," ) )
        {
            list.add( v.trim() );
        }

        return list;
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    public static class OutputParams
    {
        public boolean useReturns = true;
        public String indent = "    ";
        public String parentStart = " {";
        public String parentEnd = "}";
        public String propOp = ": ";
        public String propEnd = ";";
    }
}
