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
import jdk.nashorn.api.scripting.*;

/**
 * Template-HTML parser and engine that outputs the corresponding HTML text.
 */
public class Tucker
{
    // ============================================================
    // Enums
    // ============================================================

    // @formatter:off
    private enum State
    {
        start,
        commentStart,
        multilineComment,
        multilineCommentEnd,
        ignoreLine,
        tag,
        id,
        className,
        attributeStart,
        attributeName,
        attributeValueStart,
        attributeValue,
        attributeValueBang,
        inline,
        inlineStart,
        pipeStart,
        pipe,
        extend,
        raw,
        bang,
        varStart,
        var,
        varEnd,
        done
    }
    // @formatter:on

    // ============================================================
    // Fields
    // ============================================================

    private int row;
    private HashMap<String, Block> blocks;
    // This is a place to put pipeTexts until it gets processed after the Templar file read has been read.
    private HashMap<TagNode, String> pipeTexts;

    private Block activeBlock;
    private TagNode activeTagNode;

    // ============================================================
    // Constructors
    // ============================================================

    public Tucker()
    {
        blocks = new HashMap<>();
    }

    public Tucker( Path path )
        throws IOException
    {
        this();
        BufferedReader reader = Files.newBufferedReader( path );
        parse( reader );
        reader.close();
    }

    public Tucker( File file )
        throws IOException
    {
        this();
        BufferedReader reader = new BufferedReader( new FileReader( file ) );
        parse( reader );
        reader.close();
    }

    public Tucker( InputStream inputStream )
        throws IOException
    {
        this();
        BufferedReader reader = new BufferedReader( new InputStreamReader( inputStream ) );
        parse( reader );
        reader.close();
    }

    public Tucker( URL asset )
        throws IOException
    {
        this();
        BufferedReader reader = new BufferedReader( new URLReader( asset ) );
        parse( reader );
        reader.close();
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

    public void parse( BufferedReader in )
        throws IOException
    {
        parseLines( in );

        // Building the hierarchy is done after the fact
        for ( Block block : blocks.values() )
        {
            buildHierarchy( block );
        }
    }

    /**
     * Locate block with the given blockName and return a clone of it.
     */
    public Block buildBlock( String blockName )
    {
        Block block = blocks.get( blockName );

        return block == null ? null : new Block( block );
    }

    // ----------
    // private
    // ----------

    private void parseLines( BufferedReader in )
        throws IOException
    {
        State state = State.start;
        HashMap<Node, Integer> indents = new HashMap<>();
        activeBlock = null;

        //
        // Scan each line and turn into nodes.
        //
        row = 1;
        for ( String line = in.readLine(); line != null; line = in.readLine(), row++ )
        {
            // Find first non-space character on the line.
            int i = 0;
            while ( i < line.length() && ( line.charAt( i ) == ' ' || line.charAt( i ) == '\t' ) )
            {
                i++;
            }

            // Empty line?
            if ( i >= line.length() )
            {
                continue;
            }

            parseLine( line, i );
        }
    }

    private void parseLine( String line, int indent )
        throws IOException
    {
        activeTagNode = null;

        char c = line.charAt( indent );
        switch ( c )
        {
            case '/':
                // Comment ... ignore.
                break;

            case '>':
                if ( activeBlock != null )
                {
                    InsertionNode insertionNode = parseInsertionPoint( line, indent + 1 );
                    insertionNode.setIndent( indent );
                    activeBlock.addNode( insertionNode );
                }
                break;

            case '=':
                parseBlockName( line, indent + 1 );
                break;

            default:
                if ( activeBlock != null )
                {
                    line = processVars( line, indent );
                    TagNode tagNode = parseTag( line );
                    tagNode.setIndent( indent );
                    activeBlock.addNode( tagNode );

                    // Fill out inlines
                    for ( Segment segment : activeTagNode.getSegments() )
                    {
                        if ( segment.getType() == Segment.Type.inline )
                        {
                            TagNode inlineNode = parseTag( segment.getValue() );
                            segment.setTagNode( inlineNode );
                        }
                    }
                }
                break;
        }
    }

    private InsertionNode parseInsertionPoint( String line, int indent )
    {
        StringBuilder builder = new StringBuilder();
        for ( int pos = indent; pos < line.length(); pos++ )
        {
            char c = line.charAt( pos );
            switch ( c )
            {
                // Any spaces in insertion names are thrown away.
                case ' ':
                case '\t':
                    break;

                default:
                    builder.append( c );
                    break;
            }
        }

        InsertionNode insertionNode = new InsertionNode();
        insertionNode.setName( builder.toString() );

        return insertionNode;
    }

    private void parseBlockName( String line, int indent )
    {
        StringBuilder builder = new StringBuilder();
        for ( int pos = indent; pos < line.length(); pos++ )
        {
            char c = line.charAt( pos );
            switch ( c )
            {
                // Ignore all redundant prefix "="'s.
                case '=':
                    break;

                // Any spaces in block names are thrown away.
                case ' ':
                case '\t':
                    break;

                default:
                    builder.append( c );
                    break;
            }
        }

        activeBlock = new Block( builder.toString() );
        blocks.put( activeBlock.getName(), activeBlock );
    }

    private String processVars( String line, int indent )
    {
        State state = State.start;
        StringBuilder builder = new StringBuilder();
        StringBuilder varNameBuilder = new StringBuilder();

        int pos = indent;
        for ( ; pos < line.length(); pos++ )
        {
            char c = line.charAt( pos );
            switch ( state )
            {
                case start:
                    switch ( c )
                    {
                        case '\\':
                            state = State.bang;
                            break;

                        case '{':
                            state = State.varStart;
                            break;

                        default:
                            builder.append( c );
                            break;
                    }
                    break;

                case bang:
                    switch ( c )
                    {
                        case '\\':
                            builder.append( '\\' );
                            state = State.start;
                            break;

                        case '{':
                            builder.append( '\001' );
                            state = State.start;
                            break;

                        case '}':
                            builder.append( '\002' );
                            state = State.start;
                            break;

                        default:
                            builder.append( c );
                            break;
                    }
                    break;

                case varStart:
                    switch ( c )
                    {
                        case ' ':
                        case '\t':
                            // Ignore
                            break;

                        case '{':
                            state = State.var;
                            break;

                        default:
                            builder.append( '{' );
                            builder.append( c );
                            state = State.start;
                            break;
                    }
                    break;

                case var:
                    switch ( c )
                    {
                        case ' ':
                        case '\t':
                            // Ignore
                            break;

                        case '}':
                            state = State.varEnd;
                            break;

                        default:
                            varNameBuilder.append( c );
                            break;
                    }
                    break;

                case varEnd:
                    switch ( c )
                    {
                        case ' ':
                        case '\t':
                            // Ignore
                            break;

                        case '}':
                            builder.append( '\003' );
                            builder.append( varNameBuilder.toString() );
                            varNameBuilder.setLength( 0 );
                            builder.append( '\004' );
                            state = State.start;
                            break;

                        default:
                            // TODO maybe deal with all the text
                            varNameBuilder.setLength( 0 );
                            state = State.start;
                            break;
                    }
                    break;
            }
        }

        return builder.toString();
    }

    private TagNode parseTag( String line )
        throws IOException
    {
        activeTagNode = null;
        State state = State.tag;
        StringBuilder tagBuilder = new StringBuilder();

        int pos = 0;
        for ( ; pos < line.length(); pos++ )
        {
            char c = line.charAt( pos );
            switch ( c )
            {
                case '#':
                    state = State.id;
                    break;

                case '.':
                    state = State.className;
                    break;

                case '|':
                    state = State.pipeStart;
                    break;

                // End of tag
                case ' ':
                case '\t':
                    state = State.attributeStart;
                    break;

                default:
                    tagBuilder.append( c );
                    break;
            }

            if ( state != State.tag )
            {
                break;
            }
        }

        activeTagNode = new TagNode();
        activeTagNode.setName( tagBuilder.toString() );
        if ( activeTagNode.getName().isEmpty() )
        {
            throw new IOException( String.format( "Tag cannot have an empty name [row:%d]", row ) );
        }

        if ( state == State.id || state == State.className )
        {
            pos = parseTagShortcuts( line, pos + 1, state );
        }

        if ( pos != -1 )
        {
            pos = parseAttributes( line, pos );
        }

        if ( pos != -1 )
        {
            parsePipeData( line, pos );
        }

        return activeTagNode;
    }

    private int parseTagShortcuts( String line, int indent, State state )
        throws IOException
    {
        StringBuilder id = new StringBuilder();
        StringBuilder className = new StringBuilder();

        for ( int pos = indent; pos < line.length(); pos++ )
        {
            char c = line.charAt( pos );
            switch ( state )
            {
                case id:
                    switch ( c )
                    {
                        case '#':
                            throw new IOException( "Cannot specify more than one # id shortcut" );

                        case '.':
                            activeTagNode.addAttribute( "id", id.toString() );
                            id.setLength( 0 );
                            state = State.className;
                            break;

                        case ' ':
                            activeTagNode.addAttribute( "id", id.toString() );
                            id.setLength( 0 );

                            // Done with shortcuts
                            return pos + 1;

                        default:
                            id.append( c );
                            break;
                    }
                    break;

                case className:
                    switch ( c )
                    {
                        case '#':
                            activeTagNode.addAttribute( "class", className.toString() );
                            className.setLength( 0 );
                            state = State.id;
                            break;

                        case '.':
                            activeTagNode.addAttribute( "class", className.toString() );
                            className.setLength( 0 );
                            state = State.className;
                            break;

                        case ' ':
                            activeTagNode.addAttribute( "class", className.toString() );
                            className.setLength( 0 );

                            // Done with shortcuts
                            return pos + 1;

                        default:
                            className.append( c );
                            break;
                    }
                    break;
            }
        }

        if ( id.length() > 0 )
        {
            activeTagNode.addAttribute( "id", id.toString() );
        }

        if ( className.length() > 0 )
        {
            activeTagNode.addAttribute( "class", className.toString() );
        }

        // This is a signal that there is nothing left on this line.
        return -1;
    }

    private int parseAttributes( String line, int indent )
        throws IOException
    {
        State state = State.attributeStart;
        StringBuilder attNameBuilder = new StringBuilder();
        StringBuilder attValueBuilder = new StringBuilder();
        String activeAttName = null;

        for ( int pos = indent; pos < line.length(); pos++ )
        {
            char c = line.charAt( pos );
            switch ( state )
            {
                case attributeStart:
                    switch ( c )
                    {
                        case ' ':
                        case '\t':
                            // Ignore whitespace before attribute name.
                            break;

                        case '|':
                            // Attributes are done.
                            return pos + 1;

                        default:
                            attNameBuilder.append( c );
                            state = State.attributeName;
                            break;
                    }
                    break;

                case attributeName:
                    switch ( c )
                    {
                        // Space between attName and = or end of attName
                        case ' ':
                        case '\t':
                            activeAttName = attNameBuilder.toString();
                            attNameBuilder.setLength( 0 );
                            break;

                        case '=':
                            activeAttName = attNameBuilder.toString();
                            attNameBuilder.setLength( 0 );
                            state = State.attributeValueStart;
                            break;

                        case '|':
                            activeAttName = attNameBuilder.toString();
                            attNameBuilder.setLength( 0 );
                            if ( activeAttName != null && !activeAttName.isEmpty() )
                            {
                                activeTagNode.addAttribute( activeAttName );
                            }

                            // Attributes are done.
                            return pos + 1;

                        default:
                            attNameBuilder.append( c );
                            break;
                    }
                    break;

                case attributeValueStart:
                    switch ( c )
                    {
                        case ' ':
                        case '\t':
                            // Ignore whitespace between = and value.
                            break;

                        case '"':
                            state = State.attributeValue;
                            break;

                        // Parse error. Missing start "
                        default:
                            attNameBuilder.setLength( 0 );
                            activeAttName = null;
                            state = State.attributeStart;
                            break;
                    }
                    break;

                case attributeValue:
                    switch ( c )
                    {
                        case '"': // End of value
                            if ( activeAttName != null && !activeAttName.isEmpty() )
                            {
                                activeTagNode.addAttribute( activeAttName, attValueBuilder.toString() );
                                activeAttName = null;
                            }
                            attNameBuilder.setLength( 0 );
                            attValueBuilder.setLength( 0 );
                            state = State.attributeStart;
                            break;

                        // TODO Is this needed?
                        case '\\':
                            state = State.attributeValueBang;
                            break;

                        default:
                            attValueBuilder.append( c );
                            break;
                    }
                    break;

                case attributeValueBang:
                    switch ( c )
                    {
                        default:
                            attValueBuilder.append( c );
                            state = State.attributeValue;
                            break;
                    }
                    break;
            }
        }

        // This is a signal that there is nothing left on this line.
        return -1;
    }

    private int parsePipeData( String line, int indent )
    {
        State state = State.pipeStart;
        StringBuilder textBuilder = new StringBuilder();
        StringBuilder inlineBuilder = new StringBuilder();
        StringBuilder varBuilder = new StringBuilder();

        for ( int pos = indent; pos < line.length(); pos++ )
        {
            char c = line.charAt( pos );
            switch ( state )
            {
                case pipeStart:
                    switch ( c )
                    {
                        case ' ':
                        case '\t':
                            // Ignore whitespace just after |.
                            break;

                        case '{':
                            state = State.inlineStart;
                            break;

                        default:
                            textBuilder.append( c );
                            state = State.pipe;
                            break;
                    }
                    break;

                case pipe:
                    switch ( c )
                    {
                        case '{':
                            Segment segment = new Segment( Segment.Type.text, textBuilder.toString() );
                            activeTagNode.addSegment( segment );
                            textBuilder.setLength( 0 );
                            state = State.inlineStart;
                            break;

                        default:
                            textBuilder.append( c );
                            break;
                    }
                    break;

                case inlineStart:
                    switch ( c )
                    {
                        case ' ':
                        case '\t':
                            // Ignore whitespace just after {.
                            break;

                        default:
                            inlineBuilder.append( c );
                            state = State.inline;
                            break;
                    }
                    break;

                case inline:
                    switch ( c )
                    {
                        case '}':
                            Segment segment = new Segment( Segment.Type.inline, inlineBuilder.toString() );
                            activeTagNode.addSegment( segment );
                            inlineBuilder.setLength( 0 );
                            state = State.pipe;
                            break;

                        default:
                            inlineBuilder.append( c );
                            break;
                    }
                    break;
            }
        }

        if ( textBuilder.length() > 0 )
        {
            Segment segment = new Segment( Segment.Type.text, textBuilder.toString() );
            activeTagNode.addSegment( segment );
        }

        // This is a signal that there is nothing left on this line.
        return -1;
    }

    /**
     * Takes the actual # spaces in a file and converts them to explicit hierarchical "indents".
     */
    private void buildHierarchy( Block block )
    {
        //
        // Scan nodes and calculate depths from indents.
        //
        HashMap<Integer, Integer> map = new HashMap<>();
        HashMap<Node, Integer> depths = new HashMap<>();
        int depth = 0;
        for ( Node node : block.getNodes() )
        {
            int indent = node.getIndent();

            // First element is always considered a depth of 0, even it is was indented.
            if ( map.isEmpty() )
            {
                map.put( depth, indent );
            }
            else if ( indent == map.get( depth ) )
            {
                // Depth is the same.
            }
            // Child?
            else if ( indent > map.get( depth ) )
            {
                map.put( ++depth, indent );
            }
            // Must be at parent-level
            else
            {
                while ( depth > 0 && indent < map.get( depth ) )
                {
                    depth--;
                }
                map.put( depth, indent );
            }

            depths.put( node, depth );
        }

        HashMap<Integer, TagNode> parents = new HashMap<>();
        ArrayList<TagNode> roots = new ArrayList<>();
        for ( Node node : block.getNodes() )
        {
            int d = depths.get( node );
            TagNode parent = parents.get( d - 1 );
            if ( parent == null )
            {
                roots.add( (TagNode) node );
            }
            else
            {
                parent.addChild( node );
            }

            if ( node.getNodeType() == Node.NodeType.tag )
            {
                parents.put( d, (TagNode) node );
            }
        }

        //
        // Clear out block and put only roots back (children are sub-items now).
        //
        block.clear();
        for ( TagNode root : roots )
        {
            block.addNode( root );
        }
    }
}
