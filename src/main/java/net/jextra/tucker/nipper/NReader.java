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

public class NReader
{
    // ============================================================
    // Fields
    // ============================================================

    private BufferedReader reader;
    private ReaderProvider includeProvider;
    private NBlock model;

    // ============================================================
    // Constructors
    // ============================================================

    public NReader( BufferedReader reader, ReaderProvider includeProvider )
    {
        this.reader = reader;
        this.includeProvider = includeProvider;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public NBlock parse()
        throws IOException
    {
        model = new NBlock();

        //
        // Scan each line and turn into raw NLine tokens
        //
        int row = 0;
        ArrayList<NLine> lines = new ArrayList<>();
        for ( String string = reader.readLine(); string != null; string = reader.readLine(), row++ )
        {
            // Find first non-space character on the line.
            int i = 0;
            while ( i < string.length() && ( string.charAt( i ) == ' ' || string.charAt( i ) == '\t' ) )
            {
                i++;
            }

            // Empty line?
            if ( i >= string.length() )
            {
                continue;
            }

            string = string.substring( i );

            // Full comment line?
            if ( string.startsWith( "//" ) )
            {
                NLine line = new NLine( row, i, null, string );
                lines.add( line );

                continue;
            }

            String comment = null;
            int c = string.indexOf( "//" );
            if ( c >= 0 )
            {
                comment = string.substring( c + 2 ).trim();
                string = string.substring( 0, c ).trim();
            }

            NLine line = new NLine( row, i, string, comment );
            lines.add( line );
        }

        //
        // Interpret prefix spaces to calculated indents.
        //
        Stack<NLine> stack = new Stack<>();
        int indent = 0;
        for ( NLine line : lines )
        {
            if ( stack.isEmpty() )
            {
                stack.push( line );
            }
            else if ( line.getSpaces() < stack.peek().getSpaces() )
            {
                while ( !stack.isEmpty() && line.getSpaces() < stack.peek().getSpaces() )
                {
                    stack.pop();
                    indent--;
                }
            }
            else if ( line.getSpaces() > stack.peek().getSpaces() )
            {
                indent++;
                stack.push( line );
            }

            line.setIndent( indent );
        }

        //
        // Calculate line types.
        //
        HashMap<Integer, NLine> parents = new HashMap<>();
        for ( NLine line : lines )
        {
            if ( line.getContent() == null || line.getContent().isEmpty() )
            {
                line.setType( NLine.Type.comment );

                continue;
            }
            else if ( line.getContent().startsWith( "$" ) )
            {
                line.setType( NLine.Type.variableDeclaration );
                parseVar( line );

                continue;
            }
            else if ( line.getContent().toLowerCase().startsWith( "%include" ) )
            {
                line.setType( NLine.Type.include );
                parseInclude( line );

                continue;
            }

            NLine parent = line.getIndent() == 0 ? null : parents.get( line.getIndent() - 1 );
            if ( parent != null )
            {
                // Continuations-in-continuation need to be flattened.
                while ( parent != null && parent.getType() == NLine.Type.continuation )
                {
                    parent = parent.getParent();
                }

                // Convert a parent that is a non-: ending property to a selector if it has children (and not looking for continuations).
                if ( parent.getType() == NLine.Type.property && !parent.getContent().endsWith( ":" ) )
                {
                    parent.setType( NLine.Type.selector );
                }
                parent.addChild( line );
            }

            // At Rules are processed differently than typical selector hierarchy.
            if ( line.getContent().startsWith( "@" ) )
            {
                line.setType( NLine.Type.atRule );
            }
            else if ( parent == null /*|| parent.getType() == NLine.Type.atRule*/ )
            {
                line.setType( NLine.Type.selector );
            }
            else
            {
                // It is assumed at this time to be property. If this line ends up acquiring children it will be switched to a selector.
                line.setType( parent.getType() == NLine.Type.property ? NLine.Type.continuation : NLine.Type.property );
                parseProperty( line );
            }

            parents.put( line.getIndent(), line );
        }

        //
        // Transfer all lines to the model.
        //
        for ( NLine line : lines )
        {
            switch ( line.getType() )
            {
                case include:
                    // Go get the included nipper model and inject it into this model
                    NBlock child = readInclude( line.getValue() );
                    if ( child != null )
                    {
                        model.addBlock( child );
                    }
                    break;

                case atRule:
                case selector:
                case property:
                case continuation:
                    model.addLine( line );
                    break;
            }
        }

        reader.close();

        return model;
    }

    // ----------
    // private
    // ----------

    private void parseVar( NLine line )
    {
        int colon = line.getContent().indexOf( ':' );
        if ( colon >= 0 )
        {
            line.setName( line.getContent().substring( 1, colon ).trim() );
            line.setValue( line.getContent().substring( colon + 1 ).trim() );
            model.setVariableValue( line.getName(), line.getValue() );
        }
        // Set value to a blank string.
        else
        {
            line.setName( line.getContent().substring( 1 ).trim() );
            model.setVariableValue( line.getName(), "" );
        }
    }

    private void parseInclude( NLine line )
    {
        int colon = line.getContent().indexOf( ' ' );
        if ( colon >= 0 )
        {
            line.setName( line.getContent().substring( 1, colon ).trim() );
            line.setValue( line.getContent().substring( colon + 1 ).trim() );
        }
        else
        {
            line.setName( line.getContent().substring( 1 ).trim() );
        }
    }

    private void parseProperty( NLine line )
    {
        int colon = line.getContent().indexOf( ':' );
        if ( colon >= 0 )
        {
            line.setName( line.getContent().substring( 0, colon ).trim() );
            String value = line.getContent().substring( colon + 1 ).trim();
            // Throw away ; at end. It is probably an accident.
            if ( value.endsWith( ";" ) )
            {
                value = value.substring( 0, value.length() - 1 );
            }
            line.setValue( value );
        }
        else
        {
            line.setName( line.getContent().trim() );
        }
    }

    private NBlock readInclude( String name )
        throws IOException
    {
        if ( includeProvider == null || name == null || name.trim().isEmpty() )
        {
            return null;
        }

        BufferedReader in = includeProvider.findReader( name.trim() );
        if ( in == null )
        {
            return null;
        }

        NReader reader = new NReader( in, includeProvider );
        NBlock block = reader.parse();
        in.close();

        return block;
    }
}
