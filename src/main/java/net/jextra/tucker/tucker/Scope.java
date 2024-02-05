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

import java.lang.reflect.*;
import java.util.*;
import net.jextra.tucker.encoder.*;

public class Scope
{
    // ============================================================
    // Fields
    // ============================================================

    private Map<String, String> varValues;
    private Map<String, Boolean> boolValues;
    private List<HookBinding> bindings;

    // ============================================================
    // Constructors
    // ============================================================

    public Scope()
    {
        varValues = new HashMap<>();
        boolValues = new HashMap<>();
        bindings = new ArrayList<>();
    }

    public Scope( Scope other )
    {
        this();

        for ( String key : other.varValues.keySet() )
        {
            varValues.put( key, other.getVariable( key ) );
        }

        for ( String key : other.boolValues.keySet() )
        {
            boolValues.put( key, other.getBoolean( key ) );
        }

        for ( HookBinding binding : other.bindings )
        {
            bindings.add( new HookBinding( binding ) );
        }
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public void clear()
    {
        varValues.clear();
        boolValues.clear();
    }

    public Set<String> getVariableNames()
    {
        return varValues.keySet();
    }

    public boolean hasVariable( String name )
    {
        return varValues.containsKey( name );
    }

    public String getVariable( String name )
    {
        return varValues.get( name );
    }

    public Scope setVariable( String name, String value )
    {
        return setVariable( name, value, true );
    }

    public Scope setVariable( String name, String... values )
    {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for ( String v : values )
        {
            if ( v == null )
            {
                continue;
            }
            else if ( builder.length() > 0 )
            {
                builder.append( " " );
            }
            builder.append( v );
            count++;
        }

        return setVariable( name, count > 0 ? builder.toString() : null );
    }

    public Scope setVariable( String name, Collection<String> values )
    {
        if ( values == null )
        {
            return setVariable( name, (String) null );
        }

        StringBuilder builder = new StringBuilder();
        int count = 0;
        for ( String v : values )
        {
            if ( v == null )
            {
                continue;
            }
            else if ( builder.length() > 0 )
            {
                builder.append( " " );
            }
            builder.append( v );
            count++;
        }

        return setVariable( name, count > 0 ? builder.toString() : null );
    }

    public Scope setVariable( String name, String value, Boolean encode )
    {
        if ( encode )
        {
            varValues.put( name, Encoder.encodeForHtml( value ) );
        }
        else
        {
            varValues.put( name, value );
        }

        return this;
    }

    public boolean hasBoolean( String name )
    {
        return boolValues.containsKey( name );
    }

    public boolean getBoolean( String name )
    {
        return boolValues.get( name ) == null ? false : boolValues.get( name );
    }

    public Scope setBoolean( String name )
    {
        return setBoolean( name, true );
    }

    public Scope clearBoolean( String name )
    {
        boolValues.remove( name );
        return this;
    }

    public Scope setBoolean( String name, boolean value )
    {
        boolValues.put( name, value );
        return this;
    }

    public void bind( String hookSelector, Hook hook )
    {
        bindings.add( new HookBinding( hookSelector, hook ) );
    }

    public <T extends Hook> T bind( String hookSelector, Class<T> hookClass )
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        T hook = hookClass.getDeclaredConstructor().newInstance();
        bind( hookSelector, hook );

        return hook;
    }

    public List<HookBinding> getBindings()
    {
        return bindings;
    }
}
