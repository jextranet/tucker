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
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Template-HTML parser.
 */
public class Tucker
{
    // ============================================================
    // Fields
    // ============================================================

    public static final char LEFT_BRACE = 1;
    public static final char RIGHT_BRACE = 2;
    public static final char BACK_TICK = 3;
    public static final char VAR_START = 4;
    public static final char VAR_END = 5;
    public static final char PHRASE_START = 6;
    public static final char PHRASE_END = 7;
    public static final char BOOL_START = 28;   // arbitrary control character that does not conflict with processing (8 does conflict).
    public static final char BOOL_END = 29;

    public static final String LT = "&lt;";
    public static final String GT = "&gt;";

    private TuckerParser parser;
    private List<Hook> customTags;

    // ============================================================
    // Constructors
    // ============================================================

    public Tucker()
    {
        parser = new TuckerParser();
        customTags = new ArrayList<>();
    }

    public Tucker( Path path )
        throws IOException
    {
        this();
        parse( path );
    }

    public Tucker( File file )
        throws IOException
    {
        this();
        parse( file );
    }

    public Tucker( InputStream inputStream )
        throws IOException
    {
        this();
        parse( inputStream );
    }

    public Tucker( URL asset )
        throws IOException
    {
        this();
        parse( asset );
    }

    public Tucker( BufferedReader reader )
        throws IOException
    {
        this();
        parse( reader );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public void parse( Path path )
        throws IOException
    {
        parser.parse( path );
    }

    public void parse( File file )
        throws IOException
    {
        parser.parse( file );
    }

    public void parse( InputStream inputStream )
        throws IOException
    {
        parser.parse( inputStream );
    }

    public void parse( URL asset )
        throws IOException
    {
        parser.parse( asset );
    }

    public void parse( BufferedReader reader )
        throws IOException
    {
        parser.parse( reader );
    }

    public void registerCustomTags( Hook tag )
    {
        customTags.add( tag );
    }

    /**
     * Locate block with the given blockName and return a clone of it.
     */
    public Block buildBlock( String blockName )
    {
        Block template = parser.getBlock( blockName );
        if ( template == null )
        {
            return null;
        }

        Block block = new Block( template );

        return block;
    }

    // ----------
    // private
    // ----------

    private void printTokens()
    {
        for ( Block block : parser.getBlocks() )
        {
            System.out.printf( "In | %-9s | %s\n", "Type", "Value" );
            System.out.println( "----------------------------------------------------------------------" );
            System.out.printf( "%02d | %-9s | %s\n", block.getIndent(), block.getType(), block.getTagName() );
            for ( Node node : block.getChildren() )
            {
                System.out.printf( "%02d | %-9s | %s\n", node.getIndent(), node.getType(), node.getTagName() );
                for ( Segment seg : node.getSegments() )
                {
                    System.out.printf( " . | %-9s | %s\n", seg.getType(), seg.getValue() );
                }
            }
            System.out.println();
        }
    }

    public void printBlock( Block block )
    {
        System.out.printf( "block name[%s]\n", block.getTagName() );
        printChildren( block.getChildren(), 1 );
        System.out.println();
    }

    private void printChildren( List<Node> nodes, int depth )
    {
        if ( nodes == null || nodes.isEmpty() )
        {
            return;
        }

        for ( Node node : nodes )
        {
            StringBuilder prefix = new StringBuilder();
            for ( int i = 0; i < depth; i++ )
            {
                prefix.append( ".   " );
            }

            StringBuilder atts = new StringBuilder();
            for ( String key : node.getAttributes().keySet() )
            {
                Attribute att = node.getAttribute( key );
                atts.append( String.format( " %s=\"%s\"", key, att == null ? "null" : att.getValue() ) );
            }

            System.out.printf( "%s%s(%s)%s\n", prefix, node.getTagName(), node.getType(), atts );
            if ( atts.length() > 0 )
            {
                System.out.printf( "%s|%s\n", prefix, atts );
            }
            printChildren( node.getChildren(), depth + 1 );
        }
    }
}
