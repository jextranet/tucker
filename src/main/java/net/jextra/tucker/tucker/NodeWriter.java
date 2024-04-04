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

public class NodeWriter
{
    // ============================================================
    // Fields
    // ============================================================

    private PageContext pageContext;
    private ScopeContext scopeContext;
    private Translator translator;

    private PrintWriter out;

    // ============================================================
    // Constructors
    // ============================================================

    public NodeWriter()
    {
        scopeContext = new ScopeContext();
    }

    public NodeWriter( NodeWriter other )
    {
        pageContext = other.pageContext;
        scopeContext = new ScopeContext( other.scopeContext );
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public PageContext getPageContext()
    {
        return pageContext;
    }

    public void setPageContext( PageContext pageContext )
    {
        this.pageContext = pageContext;
    }

    public Translator getTranslator()
    {
        return translator;
    }

    public void setTranslator( Translator translator )
    {
        this.translator = translator;
    }

    public ScopeContext getScopeContext()
    {
        return scopeContext;
    }

    public void setScopeContext( ScopeContext scopeContext )
    {
        this.scopeContext = scopeContext;
    }

    public int getIndent()
    {
        return scopeContext.getIndent();
    }

    public NodeWriter setIndent( int indent )
    {
        scopeContext.setIndent( indent );
        return this;
    }

    public String render( Node node )
    {
        List<Node> hardNodes = hardenNode( node );

        StringWriter stringWriter = new StringWriter();
        out = new PrintWriter( stringWriter );
        for ( Node n : hardNodes )
        {
            writeNode( n );
        }
        out.close();

        return stringWriter.toString();
    }

    public void writeIndent()
    {
        out.print( scopeContext.getIndentWhitespace() );
    }

    public void writeIndent( int d )
    {
        for ( int i = 0; i < d; i++ )
        {
            out.print( ScopeContext.INDENT_WHITESPACE );
        }
    }

    public void writeString( String value )
    {
        String string = cleanString( value );
        if ( string != null )
        {
            out.write( string );
        }
    }

    public String cleanString( String value )
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
        // Replace variable values.
        //
        int varReplacedCount = 0;
        int varNotSetCount = 0;
        Pattern varPattern = Pattern.compile(
            "([^" + Tucker.VAR_START + "]*)" + Tucker.VAR_START + "([^" + Tucker.VAR_END + "]*)" + Tucker.VAR_END + "(.*)" );
        for ( Matcher m = varPattern.matcher( value ); m.matches(); m = varPattern.matcher( value ) )
        {
            String var = m.group( 2 );
            String varValue = getVariable( var );
            if ( varValue != null )
            {
                varReplacedCount++;
                value = m.group( 1 ) + varValue + m.group( 3 );
            }
            else
            {
                varNotSetCount++;
                value = m.group( 1 ) + m.group( 3 );
            }
        }

        //
        // Replace boolean values.
        //
        Pattern boolPattern = Pattern.compile(
            "([^" + Tucker.BOOL_START + "]*)" + Tucker.BOOL_START + "([^" + Tucker.BOOL_END + "]*)" + Tucker.BOOL_END + "(.*)" );
        for ( Matcher m = boolPattern.matcher( value ); m.matches(); m = boolPattern.matcher( value ) )
        {
            String var = m.group( 2 );
            boolean boolValue = getBoolean( var );
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

    public void printNode( Node node, int depth )
    {
        if ( node == null )
        {
            return;
        }

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

        //            if ( atts.length() > 0 )
        //            {
        //                XLog.infof( LogKey.TUCKER_PARSER, "%s|%s", prefix, atts );
        //            }
        for ( Node child : node.getChildren() )
        {
            printNode( child, depth + 1 );
        }
    }

    // ----------
    // protected
    // ----------

    protected String translate( String sourceString )
    {
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

    // ----------
    // private
    // ----------

    /**
     * Substitute all variables with values.
     */
    private List<Node> hardenNode( Node node )
    {
        ArrayList<Node> list = new ArrayList<>();
        switch ( node.getNodeType() )
        {
            case block:
            {
                // Create block scope
                Block block = (Block) node;
                scopeContext.push( block.getScope() );

                // Block nodes are kept to signal the scope.
                Node hardNode = hardenBlock( block );
                for ( Node hardChild : hardenChildren( node ) )
                {
                    hardNode.addChild( hardChild );
                }
                list.add( hardNode );

                // Pop scope
                scopeContext.pop();
                break;
            }

            case tag:
            {
                Node hardNode = hardenTagNode( node );
                for ( Node hardChild : hardenChildren( node ) )
                {
                    hardNode.addChild( hardChild );
                }

                // If the node is bound to a hook do the replacement.
                Hook hook = scopeContext.findHook( hardNode );
                if ( hook != null )
                {
                    Node newNode = performHook( hook, hardNode );
                    if ( newNode != null )
                    {
                        List<Node> hardNodes = hardenNode( newNode );
                        if ( hardNodes != null )
                        {
                            for ( Node n : hardNodes )
                            {
                                list.add( n );
                            }
                        }
                    }
                }
                else
                {
                    list.add( hardNode );
                }
                break;
            }

            case insertion:
            {
                // Insertion nodes are removed and the outcome are its children.
                for ( Node hardChild : hardenChildren( node ) )
                {
                    list.add( hardChild );
                }
                break;
            }

            case rawText:
            {
                Node hardNode = new Node( Node.NodeType.rawText );
                hardNode.setRawText( node.getRawText() );
                list.add( hardNode );
                break;
            }
        }

        return list;
    }

    private List<Node> hardenChildren( Node parent )
    {
        ArrayList<Node> list = new ArrayList<>();
        for ( Node child : parent.getChildren() )
        {
            List<Node> outcome = hardenNode( child );
            if ( outcome != null && !outcome.isEmpty() )
            {
                list.addAll( outcome );
            }
        }

        return list;
    }

    private Block hardenBlock( Block src )
    {
        Block hardNode = new Block();
        hardNode.setTagName( src.getTagName() );
        hardNode.setScope( new Scope( src.getScope() ) );

        for ( Attribute att : src.getAttributes().values() )
        {
            hardNode.addAttribute( hardenAttributes( att ) );
        }

        return hardNode;
    }

    private Node hardenTagNode( Node src )
    {
        Node hardNode = new Node( Node.NodeType.tag );
        hardNode.setTagName( src.getTagName() );
        hardNode.setInline( src.isInline() );

        for ( Attribute att : src.getAttributes().values() )
        {
            hardNode.addAttribute( hardenAttributes( att ) );
        }

        for ( Segment seg : src.getSegments() )
        {
            Segment hardSeg = hardenSegment( seg );
            if ( hardSeg != null )
            {
                hardNode.addSegment( hardSeg );
            }
        }

        return hardNode;
    }

    private Attribute hardenAttributes( Attribute att )
    {
        if ( att == null )
        {
            return null;
        }

        String key = cleanString( att.getKey() );
        if ( key == null || key.trim().isEmpty() )
        {
            return null;
        }

        // This is the case where the key is specified by itself with no equals (e.g. checked)
        if ( att.getValue() == null )
        {
            Attribute newAtt = new Attribute( key, null );
            return newAtt;
        }

        String value = cleanString( att.getValue() );
        if ( value == null )
        {
            // Special case, Instead of something like checked="", this simply means no attribute.
            return null;
        }

        return new Attribute( key, value );
    }

    private Segment hardenSegment( Segment seg )
    {
        switch ( seg.getType() )
        {
            case text:
                return new Segment( cleanString( seg.getValue() ) );

            case inline:
                List<Node> nodes = hardenNode( seg.getNode() );
                if ( !nodes.isEmpty() )
                {
                    // TODO assume only one node. Not sure this is true.
                    Segment hardSeg = new Segment( Segment.Type.inline );
                    hardSeg.setNode( nodes.get( 0 ) );
                    return hardSeg;
                }
                break;
        }

        return null;
    }

    /**
     * Write the node and its children. It is assumed at this time that all variables have been processed and all that is needed here
     * is to print out the node and its children.
     */
    private void writeNode( Node node )
    {
        switch ( node.getNodeType() )
        {
            case block:
            {
                // Create block scope
                Block block = (Block) node;
                scopeContext.push( block.getScope() );

                writeChildren( node, getIndent() );

                // Pop scope
                scopeContext.pop();
                break;
            }

            case tag:
            {
                writeTagStart( node );
                writeSegments( node );
                if ( !node.getChildren().isEmpty() )
                {
                    out.println();
                }
                int childCount = writeChildren( node, getIndent() + 1 );
                writeTagEnd( node, childCount > 0 );
                break;
            }

            // Insertion should never occur. Already replaced in hardening.
            case insertion:
            {
                writeChildren( node, getIndent() );
                break;
            }

            case rawText:
            {
                writeIndent();
                out.write( node.getRawText() );
                out.println();
                break;
            }
        }
    }

    private void writeTagStart( Node node )
    {
        if ( !node.isInline() )
        {
            writeIndent();
        }
        out.write( '<' );
        writeString( node.getTagName() );

        //
        // Write attributes
        // Always write id first, then class, then everything else.
        //
        if ( node.getAttribute( "id" ) != null )
        {
            writeAttribute( node.getAttribute( "id" ) );
        }

        if ( node.getAttribute( "class" ) != null )
        {
            writeAttribute( node.getAttribute( "class" ) );
        }

        for ( Attribute att : node.getAttributes().values() )
        {
            if ( "id".equals( att.getKey() ) || "class".equals( att.getKey() ) )
            {
                continue;
            }

            writeAttribute( att );
        }
        out.write( ">" );
    }

    private void writeAttribute( Attribute att )
    {
        if ( att == null )
        {
            return;
        }

        String key = cleanString( att.getKey() );
        if ( key == null || key.trim().isEmpty() )
        {
            return;
        }

        // This is the case where the key is specified by itself with no equals (e.g. checked)
        if ( att.getValue() == null )
        {
            out.print( " " );
            writeString( key );
        }
        else
        {
            String value = cleanString( att.getValue() );
            if ( value == null )
            {
                // Special case, Instead of something like checked="", this simply means no attribute.
            }
            else
            {
                out.print( " " );
                writeString( key );
                out.print( "=\"" );
                writeString( value );
                out.print( "\"" );
            }
        }
    }

    private void writeSegments( Node node )
    {
        for ( Segment segment : node.getSegments() )
        {
            switch ( segment.getType() )
            {
                case text:
                    writeString( segment.getValue() );
                    break;

                case inline:
                    writeNode( segment.getNode() );
                    break;
            }
        }
    }

    private int writeChildren( Node node, int childIndent )
    {
        int childCount = 0;
        for ( Node child : node.getChildren() )
        {
            int oldIndex = scopeContext.getIndent();
            scopeContext.setIndent( childIndent );

            writeNode( child );
            childCount++;

            scopeContext.setIndent( oldIndex );
        }

        return childCount;
    }

    private void writeTagEnd( Node node, boolean hasChildren )
    {
        if ( !node.isInline() && hasChildren )
        {
            //printWriter.println();
            writeIndent();
        }
        out.write( "</" );
        writeString( node.getTagName() );
        out.write( ">" );
        if ( !node.isInline() )
        {
            out.println();
        }
    }

    private String getVariable( String name )
    {
        return scopeContext.getVariable( name );
    }

    private boolean getBoolean( String name )
    {
        return scopeContext.getBoolean( name );
    }

    private Node performHook( Hook hook, Node node )
    {
        try
        {
            HookContext ctx = new HookContext( pageContext, scopeContext, node );
            Node newNode = hook.doHook( ctx );

            return newNode == null ? node : newNode;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    public static class PageContextStub implements PageContext
    {
        @Override
        public boolean addStyleSheet( String url, boolean inline )
            throws Exception
        {
            return true;
        }

        @Override
        public void addFont( String font )
        {
        }

        @Override
        public void addJavascript( String url )
        {
        }

        @Override
        public void addJavascriptModule( String url )
        {
        }

        @Override
        public void addSvgSymbol( String id, String resourcePath )
        {
        }

        @Override
        public void addSvgSymbol( String id, InputStream resource )
        {
        }
    }
}
