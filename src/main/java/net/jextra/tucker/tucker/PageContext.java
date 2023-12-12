/*
 * Copyright (C) Exact Sciences
 * All Rights Reserved.
 */

package net.jextra.tucker.tucker;

import java.io.*;

/**
 * The PageContext is used by when rendering to allow the rendering to potentially effect the overall Page that is being written into.
 * <p>
 * An example is to add a link to required javascript to the page.
 */
public interface PageContext
{
    boolean addStyleSheet( String url, boolean inline )
        throws Exception;

    default boolean addStyleSheet( String url )
        throws Exception
    {
        return addStyleSheet( url, false );
    }

    void addFont( String font )
        throws Exception;

    void addJavascript( String url )
        throws Exception;

    void addJavascriptModule( String url )
        throws Exception;

    void addSvgSymbol( String id, String resourcePath )
        throws Exception;

    void addSvgSymbol( String id, InputStream resource )
        throws Exception;
}
