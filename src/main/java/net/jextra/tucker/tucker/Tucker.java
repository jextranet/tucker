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
        scan,
        tag,
        id,
        className,
        attributeStart,
        attributeName,
        attributeBetween,
        attributeValueStart,
        attributeValue,
        attributeValueBang,
        inline,
        inlineStart,
        pipeStart,
        pipe,
        raw
    }

    private enum VarState
    {
        scan,
        bang,
        varStart,
        var,
        varComplex,
        phrase
    }

    private enum InsertState
    {
        scan,
        name,
        end
    }
    // @formatter:on

    // ============================================================
    // Fields
    // ============================================================

    public static final char LEFT_BRACE = '\001';
    public static final char RIGHT_BRACE = '\002';
    public static final char BACK_TICK = '\003';
    public static final char VAR_START = '\004';
    public static final char VAR_END = '\005';
    public static final char PHRASE = '\006';
    public static final String LT = "&lt;";
    public static final String GT = "&gt;";

    private int row;
    private HashMap<String, Block> blocks;
    private Block activeBlock;
    private Node activeNode;
    private List<Problem> problems;

    // ============================================================
    // Constructors
    // ============================================================

    public Tucker()
    {
        blocks = new HashMap<>();
        problems = new ArrayList<>();
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

        // Building the hierarchy is done after reading all the raw lines.
        for ( Block block : blocks.values() )
        {
            buildHierarchy( block );
        }

        // Replace all the auto-insertions
        for ( Block block : blocks.values() )
        {
            for ( Node node : block.getNodes() )
            {
                autoInsert( node );
            }
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

    /**
     * Break the BufferedReader into lines and parse each line separately.
     */
    private void parseLines( BufferedReader in )
        throws IOException
    {
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

    /**
     * Read a single line. First figure out the line type, then parse the rest of the line accordingly.
     */
    private void parseLine( String line, int indent )
        throws IOException
    {
        activeNode = null;

        char c = line.charAt( indent );
        switch ( c )
        {
            // Comment ... ignore.
            case '/':
                break;

            // Block Definition
            case '=':
                parseBlockDefinition( line, indent + 1 );
                break;

            // Insertion Point
            case '>':
                if ( activeBlock != null )
                {
                    InsertionNode insertionNode = parseInsertionPoint( line, indent + 1 );
                    if ( insertionNode != null )
                    {
                        insertionNode.setIndent( indent );
                        activeBlock.addNode( insertionNode );
                    }
                }
                else
                {
                    problems.add( new Problem( row, "Insertion Point is outside of a block definition" ) );
                }
                break;

            case '+':
                if ( activeBlock != null )
                {
                    InsertionNode additionNode = parseInsertionPoint( line, indent + 1 );
                    if ( additionNode != null )
                    {
                        additionNode.setIndent( indent );
                        additionNode.setAuto( true );
                        activeBlock.addNode( additionNode );
                    }
                }
                else
                {
                    problems.add( new Problem( row, "Insertion Point is outside of a block definition" ) );
                }
                break;

            default:
                // All tags need to be in a block.
                if ( activeBlock != null )
                {
                    line = transformSpecials( line, indent );
                    TagNode tagNode = parseTag( line );
                    tagNode.setIndent( indent );
                    activeBlock.addNode( tagNode );

                    // Fill out inlines
                    for ( Segment segment : tagNode.getSegments() )
                    {
                        if ( segment.getType() == Segment.Type.inline )
                        {
                            TagNode inlineNode = parseTag( segment.getValue() );
                            segment.setTagNode( inlineNode );
                        }
                    }
                }
                else
                {
                    problems.add( new Problem( row, "Tag is outside of a block definition" ) );
                }
                break;
        }
    }

    /**
     * Convert banged characters to single characters.
     * Convert variable names to variable sections.
     */
    private String transformSpecials( String line, int indent )
    {
        VarState state = VarState.scan;
        StringBuilder builder = new StringBuilder();
        StringBuilder varNameBuilder = new StringBuilder();

        int p = indent;
        for ( ; p < line.length(); p++ )
        {
            char c = line.charAt( p );
            switch ( state )
            {
                case scan:
                    switch ( c )
                    {
                        case '\\':
                            state = VarState.bang;
                            break;

                        case '$':
                            state = VarState.varStart;
                            break;

                        case '`':
                            builder.append( PHRASE );
                            state = VarState.phrase;
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
                            break;

                        case '$':
                            builder.append( '$' );
                            break;

                        case '{':
                            builder.append( LEFT_BRACE );
                            break;

                        case '}':
                            builder.append( RIGHT_BRACE );
                            break;

                        case '`':
                            builder.append( BACK_TICK );
                            break;

                        default:
                            builder.append( '\\' );
                            builder.append( c );
                            problems.add( new Problem( row, "Illegal banged character '" + c + "'" ) );
                            break;
                    }
                    state = VarState.scan;
                    break;

                case varStart:
                    switch ( c )
                    {
                        case ' ':
                        case '\t':
                            // Ignore
                            break;

                        case '(':
                            state = VarState.varComplex;
                            break;

                        default:
                            if ( isVarChar( c ) )
                            {
                                varNameBuilder.append( c );
                                state = VarState.var;
                            }
                            else
                            {
                                p--;
                                state = VarState.scan;
                            }
                            break;
                    }
                    break;

                case var:
                    if ( isVarChar( c ) )
                    {
                        varNameBuilder.append( c );
                    }
                    else
                    {
                        builder.append( VAR_START );
                        builder.append( varNameBuilder.toString() );
                        varNameBuilder.setLength( 0 );
                        builder.append( VAR_END );
                        state = VarState.scan;
                        p--;    // Back up so this character can get reprocessed outside of var state.
                    }
                    break;

                case varComplex:
                    switch ( c )
                    {
                        case ' ':
                        case '\t':
                            // Ignore
                            break;

                        case ')':
                            builder.append( VAR_START );
                            builder.append( varNameBuilder.toString() );
                            varNameBuilder.setLength( 0 );
                            builder.append( VAR_END );
                            state = VarState.scan;
                            break;

                        default:
                            varNameBuilder.append( c );
                            break;
                    }
                    break;

                case phrase:
                    switch ( c )
                    {
                        case '`':
                            builder.append( PHRASE );
                            state = VarState.scan;
                            break;

                        default:
                            builder.append( c );
                            break;
                    }
                    break;
            }
        }

        return builder.toString();
    }

    private boolean isVarChar( char c )
    {
        if ( Character.isAlphabetic( c ) )
        {
            return true;
        }

        switch ( c )
        {
            case '-':
            case '_':
                return true;
        }

        return false;
    }

    /**
     * Parses name of the block and makes it the activeBlock.
     * <pre>
     *     =name
     * </pre>
     */
    private void parseBlockDefinition( String line, int indent )
    {
        StringBuilder builder = new StringBuilder();
        for ( int p = indent; p < line.length(); p++ )
        {
            char c = line.charAt( p );
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

        String name = builder.toString();
        if ( name.trim().isEmpty() )
        {
            return;
        }

        activeBlock = new Block( name );
        blocks.put( activeBlock.getName(), activeBlock );
    }

    /**
     * Parses name on an insertion line.
     * <pre>
     *     >name
     * </pre>
     */
    private InsertionNode parseInsertionPoint( String line, int indent )
    {
        // 0 start scan, 1 name, 2 end.
        InsertState state = InsertState.scan;
        StringBuilder builder = new StringBuilder();
        int p = indent;
        for ( ; p < line.length(); p++ )
        {
            char c = line.charAt( p );
            switch ( c )
            {
                // Space before tag or after tag
                case ' ':
                case '\t':
                    if ( state == InsertState.name )
                    {
                        state = InsertState.end;
                    }
                    break;

                default:
                    builder.append( c );
                    state = InsertState.name;
                    break;
            }

            if ( state == InsertState.end )
            {
                break;
            }
        }

        String name = builder.toString().trim();
        if ( name.isEmpty() )
        {
            return null;
        }

        InsertionNode insertionNode = new InsertionNode();
        activeNode = insertionNode;
        insertionNode.setName( name );

        return insertionNode;
    }

    private TagNode parseTag( String line )
        throws IOException
    {
        activeNode = null;
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

        TagNode tagNode = new TagNode();
        activeNode = tagNode;
        tagNode.setName( tagBuilder.toString() );
        if ( tagNode.getName().isEmpty() )
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

        return tagNode;
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
                            activeNode.addAttribute( "id", id.toString() );
                            id.setLength( 0 );
                            state = State.className;
                            break;

                        case ' ':
                            activeNode.addAttribute( "id", id.toString() );
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
                            activeNode.addAttribute( "class", className.toString() );
                            className.setLength( 0 );
                            state = State.id;
                            break;

                        case '.':
                            activeNode.addAttribute( "class", className.toString() );
                            className.setLength( 0 );
                            state = State.className;
                            break;

                        case ' ':
                            activeNode.addAttribute( "class", className.toString() );
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
            activeNode.addAttribute( "id", id.toString() );
        }

        if ( className.length() > 0 )
        {
            activeNode.addAttribute( "class", className.toString() );
        }

        // This is a signal that there is nothing left on this line.
        return -1;
    }

    private int parseAttributes( String line, int indent )
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
                            // Must be no attributes.
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
                            state = State.attributeBetween;
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
                                activeNode.addAttribute( activeAttName );
                            }

                            // Attributes are done.
                            return pos + 1;

                        default:
                            attNameBuilder.append( c );
                            break;
                    }
                    break;

                case attributeBetween:
                    switch ( c )
                    {
                        // Space between attName and = or end of attName
                        case ' ':
                        case '\t':
                            break;

                        case '=':
                            state = State.attributeValueStart;
                            break;

                        case '|':
                            activeAttName = attNameBuilder.toString();
                            attNameBuilder.setLength( 0 );
                            if ( activeAttName != null && !activeAttName.isEmpty() )
                            {
                                activeNode.addAttribute( activeAttName );
                            }

                            // Attributes are done.
                            return pos + 1;

                        // Attribute has not set value. This character must be next attribute
                        default:
                            if ( activeAttName != null && !activeAttName.isEmpty() )
                            {
                                activeNode.addAttribute( activeAttName );
                            }
                            attNameBuilder.append( c );
                            state = State.attributeStart;
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
                                activeNode.addAttribute( activeAttName, attValueBuilder.toString() );
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
                            activeNode.addSegment( segment );
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
                            activeNode.addSegment( segment );
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
            activeNode.addSegment( segment );
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
        ArrayList<Node> roots = new ArrayList<>();
        for ( Node node : block.getNodes() )
        {
            int d = depths.get( node );
            TagNode parent = parents.get( d - 1 );
            if ( parent == null )
            {
                roots.add( node );
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
        for ( Node root : roots )
        {
            block.addNode( root );
        }
    }

    private void autoInsert( Node node )
    {
        switch ( node.getNodeType() )
        {
            case tag:
                for ( Node child : ( (TagNode) node ).getChildren() )
                {
                    autoInsert( child );
                }
                break;

            case insertion:
                InsertionNode insertionNode = (InsertionNode) node;
                if ( insertionNode.isAuto() )
                {
                    String name = insertionNode.getName();
                    for ( Block block : blocks.values() )
                    {
                        if ( block.getName().equals( name ) )
                        {
                            Block clone = new Block( block );
                            for ( Attribute att : insertionNode.getAttributes().values() )
                            {
                                clone.setVariable( att.getKey(), att.getValue() );
                            }
                            insertionNode.insert( clone );
                            break;
                        }
                    }
                    return;
                }
                break;
        }
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    public static class Problem
    {
        private int row;
        private String message;

        Problem( int row, String message )
        {
            this.row = row;
            this.message = message;
        }

        public int getRow()
        {
            return row;
        }

        public void setRow( int row )
        {
            this.row = row;
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage( String message )
        {
            this.message = message;
        }

        @Override
        public String toString()
        {
            return String.format( "%d: %s", row, message );
        }
    }
}
