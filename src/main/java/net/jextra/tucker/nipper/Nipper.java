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

/**
 * Nip parser and engine that outputs the corresponding CSS text.
 */
public class Nipper
{
    // ============================================================
    // Fields
    // ============================================================

    private ReaderProvider readerProvider;
    private NBlock model;

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public NBlock getBlock()
    {
        return model;
    }

    public Nipper setReaderProvider( ReaderProvider provider )
    {
        readerProvider = provider;

        return this;
    }

    public NBlock parse( Path path )
        throws IOException
    {
        BufferedReader in = Files.newBufferedReader( path );
        NBlock model = parse( in );
        in.close();

        return model;
    }

    public NBlock parse( File file )
        throws IOException
    {
        BufferedReader in = new BufferedReader( new FileReader( file ) );
        parse( in );
        in.close();

        return model;
    }

    public NBlock parse( InputStream inputStream )
        throws IOException
    {
        BufferedReader in = new BufferedReader( new InputStreamReader( inputStream ) );
        NBlock model = parse( in );
        in.close();

        return model;
    }

    public NBlock parse( URL asset )
        throws IOException
    {
        BufferedReader in = new BufferedReader( new InputStreamReader( asset.openStream() ) );
        NBlock model = parse( in );
        in.close();

        return model;
    }

    public NBlock parse( BufferedReader in )
        throws IOException
    {
        NReader reader = new NReader( in, readerProvider );
        model = reader.parse();

        return model;
    }
}
