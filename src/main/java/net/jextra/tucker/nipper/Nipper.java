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
import java.net.*;
import java.nio.file.*;
import jdk.nashorn.api.scripting.*;

/**
 * Nip parser and engine that outputs the corresponding CSS text.
 */
public class Nipper
{
    // ============================================================
    // Fields
    // ============================================================

    private NBlock model;

    // ============================================================
    // Constructors
    // ============================================================

    public Nipper()
    {
    }

    public Nipper( Path path )
        throws IOException
    {
        this();
        BufferedReader in = Files.newBufferedReader( path );
        parse( in );
        in.close();
    }

    public Nipper( File file )
        throws IOException
    {
        this();
        BufferedReader in = new BufferedReader( new FileReader( file ) );
        Path directory = Paths.get( file.getParent() );
        parse( in );
        in.close();
    }

    public Nipper( InputStream inputStream )
        throws IOException
    {
        this();
        BufferedReader in = new BufferedReader( new InputStreamReader( inputStream ) );
        parse( in );
        in.close();
    }

    public Nipper( URL asset )
        throws IOException
    {
        this();
        BufferedReader in = new BufferedReader( new URLReader( asset ) );
        parse( in );
        in.close();
    }

    public Nipper( BufferedReader in )
        throws IOException
    {
        this();
        parse( in );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public NBlock buildBlock()
    {
        return model;
    }

    public NBlock parse( BufferedReader in )
        throws IOException
    {
        NReader reader = new NReader( in );
        model = reader.parse();

        return model;
    }
}
