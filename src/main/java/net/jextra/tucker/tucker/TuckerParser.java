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
public class TuckerParser
{
    // ============================================================
    // Enums
    // ============================================================

    // @formatter:off
    private enum State
    {
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
        SCAN,
        bang,
        varStart,
        var,
        boolStart,
        bool
    }
    // @formatter:on

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

    private int row;
    private HashMap<String, Block> blocks;
    private Block activeBlock;
    private List<Problem> problems;

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public List<Problem> parse( Path path )
        throws IOException
    {
        try ( BufferedReader reader = Files.newBufferedReader( path ) )
        {
            return parse( reader );
        }
    }

    public List<Problem> parse( File file )
        throws IOException
    {
        try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) )
        {
            return parse( reader );
        }
    }

    public List<Problem> parse( InputStream inputStream )
        throws IOException
    {
        try ( BufferedReader reader = new BufferedReader( new InputStreamReader( inputStream ) ) )
        {
            return parse( reader );
        }
    }

    public List<Problem> parse( URL asset )
        throws IOException
    {
        try ( BufferedReader reader = new BufferedReader( new InputStreamReader( asset.openStream() ) ) )
        {
            return parse( reader );
        }
    }

    public List<Problem> parse( BufferedReader in )
        throws IOException
    {
        blocks = new LinkedHashMap<>();
        problems = new ArrayList<>();
        activeBlock = null;
        lexLines( in );

        // Building the depth-hierarchy is done after lexing all the lines in the file.
        for ( Block block : blocks.values() )
        {
            buildHierarchy( block );
        }

        return problems;
    }

    public Collection<Block> getBlocks()
    {
        return blocks.values();
    }

    public Block getBlock( String blockName )
    {
        return blocks.get( blockName );
    }

    // ----------
    // private
    // ----------

    /**
     * Break the BufferedReader into lines and parse each line separately.
     */
    private void lexLines( BufferedReader in )
        throws IOException
    {
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

            // Ignore empty line?
            if ( i >= line.length() )
            {
                continue;
            }

            lexLine( line, i );
        }
    }

    /**
     * Read a single line. First figure out the line type, then tokenize the rest of the line accordingly.
     */
    private void lexLine( String line, int indent )
        throws IOException
    {
        char c = line.charAt( indent );
        switch ( c )
        {
            // Comment ... ignore.
            case '/':
            case '#':
                break;

            // Block
            case '=':
                lexBlock( line, indent + 1 );
                break;

            // Insertion Point
            case '>':
                if ( activeBlock != null )
                {
                    Node node = lexInsertionPoint( line, indent + 1 );
                    if ( node != null )
                    {
                        node.setIndent( indent );
                        node.setRow( row );
                        activeBlock.addChild( node );
                    }
                }
                else
                {
                    problems.add( new Problem( row, "Insertion Point cannot be outside of a block definition" ) );
                }
                break;

        /*    // Continuation
            case '~':
                if ( activeBlock != null )
                {
                    InsertionNode additionNode = parseInsertionPoint( line, indent + 1 );
                    additionNode.setRow( row );
                    if ( additionNode != null )
                    {
                        additionNode.setIndent( indent );
                        additionNode.setAuto( true );
                        activeBlock.addChild( additionNode );
                    }
                }
                else
                {
                    problems.add( new Problem( row, "Continuation Point is outside of a block definition" ) );
                }
                break;*/

            default:
                if ( activeBlock != null )
                {
                    line = transformSpecials( line, indent );

                    Node node = lexTag( line );
                    node.setIndent( indent );
                    node.setRow( row );
                    activeBlock.addChild( node );

                    // Fill out inline Nodes.
                    for ( Segment segment : node.getSegments() )
                    {
                        if ( segment.getType() == Segment.Type.inline )
                        {
                            Node inlineNode = lexTag( segment.getValue() );
                            inlineNode.setInline( true );
                            segment.setNode( inlineNode );
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
     * Convert variable/boolean names to variable/boolean sections.
     */
    private String transformSpecials( String line, int indent )
    {
        VarState state = VarState.SCAN;
        StringBuilder builder = new StringBuilder();
        StringBuilder varNameBuilder = new StringBuilder();
        boolean inPhrase = false;
        boolean inVarParen = false;
        for ( int i = indent; i < line.length(); i++ )
        {
            char c = line.charAt( i );
            switch ( state )
            {
                case SCAN:
                    switch ( c )
                    {
                        case '\\':
                            state = VarState.bang;
                            break;

                        case '$':
                            state = VarState.varStart;
                            inVarParen = false;
                            break;

                        case '&':
                            state = VarState.boolStart;
                            break;

                        case '`':
                            builder.append( inPhrase ? PHRASE_END : PHRASE_START );
                            inPhrase = !inPhrase;
                            break;

                        default:
                            builder.append( c );
                            break;
                    }
                    break;

                case bang:
                    switch ( c )
                    {
                        // backslash.
                        case '\\':
                            builder.append( '\\' );
                            break;

                        case '$':
                            builder.append( '$' );
                            break;

                        case '&':
                            builder.append( '&' );
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
                            problems.add( new Problem( row, "Illegal character banged '" + c + "'" ) );
                            break;
                    }
                    state = VarState.SCAN;
                    break;

                case varStart:
                case boolStart:
                    VarState nextState = state == VarState.varStart ? VarState.var : VarState.bool;
                    switch ( c )
                    {
                        case ' ':
                        case '\t':
                            // Ignore
                            break;

                        case '(':
                            state = nextState;
                            inVarParen = true;
                            break;

                        default:
                            if ( isVarChar( c ) )
                            {
                                varNameBuilder.append( c );
                                state = nextState;
                            }
                            else
                            {
                                // $,& following by something that cannot be a variable character is a problem. Back up to reprocess.
                                i--;
                                state = VarState.SCAN;
                            }
                            break;
                    }
                    break;

                case var:
                case bool:
                    boolean isVarDone = false;
                    switch ( c )
                    {
                        case ')':
                            isVarDone = true;
                            if ( !inVarParen )
                            {
                                i--; // Back up so ) character can get reprocessed as not part of the variable.
                            }
                            break;

                        case ' ':
                        case '\t':
                            // Ignore any spaces in a variable surrounded by parens. If not in parens, variable must be done.
                            if ( !inVarParen )
                            {
                                isVarDone = true;
                            }
                            break;

                        default:
                            if ( isVarChar( c ) || inVarParen )
                            {
                                varNameBuilder.append( c );
                            }
                            else
                            {
                                isVarDone = true;
                                // Any character that cannot be in a variable name (e.g. a space ' ') terminates the variable name.
                                // Back up and reprocess the terminating character.
                                i--;
                            }
                    }

                    if ( isVarDone )
                    {
                        builder.append( state == VarState.bool ? BOOL_START : VAR_START );
                        builder.append( varNameBuilder );
                        builder.append( state == VarState.bool ? BOOL_END : VAR_END );
                        varNameBuilder.setLength( 0 );
                        state = VarState.SCAN;
                        inVarParen = false;
                    }

                    break;
            }
        }

        //
        // Handle open-ended state.
        //
        switch ( state )
        {
            case var:
                builder.append( VAR_START );
                builder.append( varNameBuilder );
                builder.append( VAR_END );
                varNameBuilder.setLength( 0 );
                break;

            case bool:
                builder.append( BOOL_START );
                builder.append( varNameBuilder );
                builder.append( BOOL_END );
                varNameBuilder.setLength( 0 );
                break;
        }

        return builder.toString();
    }

    /**
     * Variables can contain alphanumerics dashes and underscores only.
     */
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
     * Parses/tokenize name of the block and make it the activeBlock.
     * <pre>
     *     =name
     * </pre>
     */
    private Block lexBlock( String line, int indent )
    {
        StringBuilder builder = new StringBuilder();
        for ( int i = indent; i < line.length(); i++ )
        {
            char c = line.charAt( i );
            switch ( c )
            {
                // Ignore all redundant prefix "="'s.
                case '=':
                    break;

                // Any spaces in name is thrown away.
                case ' ':
                case '\t':
                    break;

                default:
                    builder.append( c );
                    break;
            }
        }

        String name = builder.toString().trim();
        if ( name.isEmpty() )
        {
            return null;
        }

        // make new block the active block.
        activeBlock = new Block( name );
        blocks.put( activeBlock.getTagName(), activeBlock );

        return activeBlock;
    }

    /**
     * Parses name on an insertion line.
     * <pre>
     *     >name
     * </pre>
     */
    private Node lexInsertionPoint( String line, int indent )
    {
        StringBuilder builder = new StringBuilder();
        for ( int i = indent; i < line.length(); i++ )
        {
            char c = line.charAt( i );
            switch ( c )
            {
                // Any spaces in name is thrown away.
                case ' ':
                case '\t':
                    break;

                default:
                    builder.append( c );
                    break;
            }
        }

        String name = builder.toString().trim();
        if ( name.isEmpty() )
        {
            return null;
        }

        Node node = new Node( Node.NodeType.insertion );
        node.setTagName( name );
        return node;
    }

    private Node lexTag( String line )
        throws IOException
    {
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

        Node node = new Node( Node.NodeType.tag );
        node.setTagName( tagBuilder.toString().trim() );
        if ( node.getTagName().isEmpty() )
        {
            problems.add( new Problem( row, String.format( "Tag cannot have an empty name. Defaulting to '%s'", Node.DEFAULT_TAG ) ) );
            node.setTagName( Node.DEFAULT_TAG );
        }

        if ( state == State.id || state == State.className )
        {
            pos = lexTagShortcuts( node, line, pos + 1, state );
        }

        pos = lexAttributes( node, line, pos );
        lexPipeData( node, line, pos );

        return node;
    }

    private int lexTagShortcuts( Node node, String line, int indent, State state )
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
                            node.addAttribute( "id", id.toString() );
                            id.setLength( 0 );
                            state = State.className;
                            break;

                        case ' ':
                            node.addAttribute( "id", id.toString() );
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
                            node.addAttribute( "class", className.toString() );
                            className.setLength( 0 );
                            state = State.id;
                            break;

                        case '.':
                            node.addAttribute( "class", className.toString() );
                            className.setLength( 0 );
                            state = State.className;
                            break;

                        case ' ':
                            node.addAttribute( "class", className.toString() );
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
            node.addAttribute( "id", id.toString() );
        }

        if ( className.length() > 0 )
        {
            node.addAttribute( "class", className.toString() );
        }

        // This is a signal that there is nothing left on this line.
        return -1;
    }

    private int lexAttributes( Node node, String line, int indent )
    {
        if ( indent == -1 )
        {
            return -1;
        }

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
                                node.addAttribute( activeAttName );
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
                            if ( activeAttName != null && !activeAttName.isEmpty() )
                            {
                                node.addAttribute( activeAttName );
                            }

                            // Attributes are done.
                            return pos + 1;

                        // Attribute has not set value. This character must be next attribute
                        default:
                            if ( activeAttName != null && !activeAttName.isEmpty() )
                            {
                                node.addAttribute( activeAttName );
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
                                node.addAttribute( activeAttName, attValueBuilder.toString() );
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

        // Make sure to capture last attribute with no value
        if ( attNameBuilder.length() > 0 )
        {
            node.addAttribute( attNameBuilder.toString() );
        }

        // This is a signal that there is nothing left on this line.
        return -1;
    }

    private int lexPipeData( Node node, String line, int indent )
    {
        if ( indent == -1 )
        {
            return -1;
        }

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
                            node.addSegment( segment );
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
                            node.addSegment( segment );
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
            node.addSegment( segment );
        }

        // This is a signal that there is nothing left on this line.
        return -1;
    }

    private void printTokens()
    {
        for ( Block block : blocks.values() )
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

    private void printBlock( Block block )
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

            System.out.printf( "%s%s type[%s] segs[%d]%s\n", prefix, node.getTagName(), node.getType(), node.getSegments().size(), atts );
            for ( Segment seg : node.getSegments() )
            {
                System.out.printf( "%s|%s\n", prefix, seg );
            }
            printChildren( node.getChildren(), depth + 1 );
        }
    }

    /**
     * Using the indents on Nodes, add child-Nodes to appropriate parent-Nodes.
     */
    private void buildHierarchy( Block block )
    {
        //
        // Scan linear list of Nodes associate each Node with a depth.
        // Note, "indent" != "depth". indent is the actual number of spaces characters, depth is the calculated hierarchical lineage depth.
        //
        HashMap<Integer, Integer> map = new HashMap<>();
        HashMap<Node, Integer> depths = new HashMap<>();
        int depth = 0;
        for ( Node node : block.getChildren() )
        {
            int indent = node.getIndent();

            // The first element is always considered a depth of 0, even it was indented.
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

        //
        // Find roots (no parent) and put children in those parents.
        //
        HashMap<Integer, Node> parents = new HashMap<>();
        ArrayList<Node> roots = new ArrayList<>();
        for ( Node node : block.getChildren() )
        {
            int d = depths.get( node );
            Node parent = parents.get( d - 1 );
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
                parents.put( d, node );
            }
        }

        //
        // Clear out block and put only roots back in (children are sub-items now).
        //
        block.clearChildren();
        for ( Node root : roots )
        {
            block.addChild( root );
        }
    }

 /*   private void autoInsert( Node node )
    {
        switch ( node.getNodeType() )
        {
            case tag:
                for ( OldNode child : ( (TagNode) node ).getChildren() )
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
    }*/

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
