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

package net.jextra.tucker;

import java.io.*;
import java.util.*;
import net.jextra.tucker.tucker.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class BasicTest
{
    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @Test
    @DisplayName( "Basic" )
    public void testBasic()
        throws IOException
    {
        Tucker tucker = new Tucker( getClass().getResourceAsStream( "basic-in.thtml" ) );
        Block block = tucker.buildBlock( "root" );

        String goal = new Scanner( getClass().getResourceAsStream( "basic-out.html" ) ).useDelimiter( "\\A" ).next();

        //        System.out.println( block.toString().replace( "\r\n", "\n" ) );
        //        System.out.println( goal );
        assertTrue( goal.equals( block.toString().replace( "\r\n", "\n" ) ) );
    }

    @Test
    @DisplayName( "Shortcut" )
    public void testShortcuts()
        throws IOException
    {
        Tucker tucker = new Tucker( getClass().getResourceAsStream( "shortcut-in.thtml" ) );
        Block block = tucker.buildBlock( "root" );

        String goal = new Scanner( getClass().getResourceAsStream( "shortcut-out.html" ) ).useDelimiter( "\\A" ).next();

        //        System.out.println( block.toString().replace( "\r\n", "\n" ) );
        //        System.out.println( goal );
        assertTrue( goal.equals( block.toString().replace( "\r\n", "\n" ) ) );
    }

    @Test
    @DisplayName( "Variable" )
    public void testVariables()
        throws IOException
    {
        Tucker tucker = new Tucker( getClass().getResourceAsStream( "variable-in.thtml" ) );
        Block block = tucker.buildBlock( "root" );
        block.setVariable( "title", "This is my first variable" );
        block.setVariable( "one", "Variable ONE" );
        block.setVariable( "two", "Variable TWO" );
        block.setVariable( "three", "Variable THREE" );
        block.setVariable( "level", "0" );
        block.setVariable( "level1", "1" );
        block.setVariable( "off1", "off" );

        String goal = new Scanner( getClass().getResourceAsStream( "variable-out.html" ) ).useDelimiter( "\\A" ).next();

        //        System.out.println( block.toString().replace( "\r\n", "\n" ) );
        //        System.out.println( goal );
        assertTrue( goal.equals( block.toString().replace( "\r\n", "\n" ) ) );
    }

    @Test
    @DisplayName( "Insert" )
    public void testInserts()
        throws IOException
    {
        Tucker tucker = new Tucker( getClass().getResourceAsStream( "insert-in.thtml" ) );
        Block block = tucker.buildBlock( "root" );
        block.setVariable( "title", "This is my first insert" );

        for ( int i = 0; i < 5; i++ )
        {
            Block item = tucker.buildBlock( "item" );
            item.setVariable( "name", "ITEM " + i );
            if ( i == 3 )
            {
                item.setVariable( "style", "strong" );
            }
            int count = block.insert( "item", item );
        }

        String goal = new Scanner( getClass().getResourceAsStream( "insert-out.html" ) ).useDelimiter( "\\A" ).next();

        System.out.println( block.toString().replace( "\r\n", "\n" ) );
        System.out.println( goal );
        assertTrue( goal.equals( block.toString().replace( "\r\n", "\n" ) ) );
    }

    @Test
    @DisplayName( "Hook" )
    public void testHooks()
        throws IOException
    {
        Tucker tucker = new Tucker( getClass().getResourceAsStream( "hook-in.thtml" ) );
        Block block = tucker.buildBlock( "root" );
        CustomDiv hook = new CustomDiv();
        block.bind( "hook:custom-div", hook );
        block.setVariable( "title", "This is my first hook" );

        String goal = new Scanner( getClass().getResourceAsStream( "hook-out.html" ) ).useDelimiter( "\\A" ).next();

        System.out.println( block.toString().replace( "\r\n", "\n" ) );
        System.out.println( goal );
        assertTrue( goal.equals( block.toString().replace( "\r\n", "\n" ) ) );
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    public static final class CustomDiv implements Hook
    {
        @Override
        public Node doHook( HookContext context )
            throws IOException
        {
            Tucker tucker = new Tucker( getClass().getResourceAsStream( "hook-in.thtml" ) );
            return tucker.buildBlock( "custom-div" );
        }
    }
}
