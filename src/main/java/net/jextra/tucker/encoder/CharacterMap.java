package net.jextra.tucker.encoder;

import java.util.*;

public abstract class CharacterMap {

    public CharacterMap() {
    }

    public static Map<CharSequence, CharSequence> invert(Map<CharSequence, CharSequence> map) {
        Map<CharSequence, CharSequence> inverted = new HashMap<>();

        for ( Map.Entry<CharSequence, CharSequence> entry : map.entrySet()) {
            inverted.put( entry.getValue(), entry.getKey() );
        }

        return inverted;
    }
}
