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
import net.jextra.tucker.tucker.*;
import org.junit.jupiter.api.*;

public class ReadBasicTest
{
    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    @Test
    @DisplayName( "Test Basic" )
    public void testBasic()
        throws IOException
    {
        Tucker tucker = new Tucker( getClass().getResourceAsStream( "basic.thtml" ) );
        //        tucker.registerCustomTag( "w:text-input", new FirstCustomTag() );
        Block block = tucker.buildBlock( "basic" );
        block.setVariable( "title", "THIS IS A TITLE" );
        block.setVariable( "name", "THIS IS A NAME" );
        block.setVariable( "slug", "THIS IS A SLUG" );

        for ( int i = 0; i < 5; i++ )
        {
            Block item = tucker.buildBlock( "item" );
            item.setVariable( "name", "ITEM " + i );
            int count = block.insert( "outline-item", item );
        }

//        StringWriter stringWriter = new StringWriter();
//        TuckerWriter writer = new TuckerWriter( new PrintWriter( stringWriter ) );
//        writer.writeNode( block );
//        writer.close();
//
//        System.out.println( stringWriter );
    }
}
