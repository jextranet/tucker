package net.jextra.tucker.tucker;

import java.util.*;

class HookBinding
{
    // ============================================================
    // Fields
    // ============================================================

    private String tagName;
    private String id;
    private Set<String> styleClasses;

    private Hook hook;

    // ============================================================
    // Constructors
    // ============================================================

    public HookBinding()
    {
        styleClasses = new HashSet<>();
    }

    public HookBinding( String hookSelector, Hook hook )
    {
        this();
        parseSelector( hookSelector );
        this.hook = hook;
    }

    public HookBinding( HookBinding other )
    {
        this();
        tagName = other.tagName;
        id = other.id;
        for ( String clss : other.styleClasses )
        {
            styleClasses.add( clss );
        }
        hook = other.hook;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName( String tagName )
    {
        this.tagName = tagName;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public Set<String> getStyleClasses()
    {
        return styleClasses;
    }

    public void addStyleClass( String clss )
    {
        this.styleClasses.add( clss );
    }

    public boolean matches( String hookSelector )
    {
        HookBinding test = new HookBinding();
        test.parseSelector( hookSelector );

        return matches( test.getTagName(), test.getId(), test.getStyleClasses() );
    }

    public boolean matches( Node node )
    {
        return matches( node.getTagName(), node.getId(), node.getStyleClasses() );
    }

    public boolean matches( String testTagName, String testId, Collection<String> testStyleClasses )
    {
        if ( !testTagName.equals( tagName ) )
        {
            return false;
        }

        if ( id != null && !testId.equals( id ) )
        {
            return false;
        }

        for ( String clss : styleClasses )
        {
            if ( !testStyleClasses.contains( clss ) )
            {
                return false;
            }
        }

        return true;
    }

    public Hook getHook()
    {
        return hook;
    }

    public void setHook( Hook hook )
    {
        this.hook = hook;
    }

    // ----------
    // private
    // ----------

    private void parseSelector( String string )
    {
        string = string.trim();

        StringBuilder tagName = new StringBuilder();
        StringBuilder id = new StringBuilder();
        StringBuilder className = new StringBuilder();

        int state = 0;
        for ( int pos = 0; pos < string.length(); pos++ )
        {
            char c = string.charAt( pos );
            switch ( c )
            {
                case '#':
                    state = 1;
                    break;

                case '.':
                    state = 2;
                    if ( className.length() > 0 )
                    {
                        addStyleClass( className.toString() );
                        className = new StringBuilder();
                    }
                    break;

                default:
                    switch ( state )
                    {
                        case 0:
                            tagName.append( c );
                            break;

                        case 1:
                            id.append( c );
                            break;

                        case 2:
                            className.append( c );
                            break;
                    }
                    break;
            }
        }

        setTagName( tagName.toString() );

        if ( id.length() > 0 )
        {
            setId( id.toString() );
        }

        if ( className.length() > 0 )
        {
            addStyleClass( className.toString() );
        }
    }
}
