/*
 * Copyright (C) Exact Sciences
 * All Rights Reserved.
 */

package net.jextra.tucker.tucker;

/**
 * An attribute on a tag. For example name="fubar".
 */
public class Attribute
{
    // ============================================================
    // Fields
    // ============================================================

    private String key;
    // A value=null means it should output just the attribute (e.g. selected).
    // A value="" (empty) means it should output a blank string (e.g. name="")
    private String value;

    // ============================================================
    // Constructors
    // ============================================================

    public Attribute( String key, String seq )
    {
        this.key = key;
        this.value = seq;
    }

    public Attribute( Attribute other )
    {
        key = other.key;
        value = other.value;
    }

    // ============================================================
    // Methods
    // ============================================================

    // ----------
    // public
    // ----------

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String seq )
    {
        this.value = seq;
    }

    public String toString()
    {
        return key + "=" + value;
    }
}
