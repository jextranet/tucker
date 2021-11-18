package net.jextra.tucker.encoder;

public class Encoder {

    public static String encodeForHtml(String input) {
        if (input != null && !input.isEmpty()) {
            StringBuilder out = new StringBuilder();

            for ( int i = 0; i < input.length(); i++ ) {
                char c = input.charAt( i );
                CharSequence s = c + "";

                if ( Html4EntityMap.ESCAPE.containsKey( s ) ) {
                    out.append( Html4EntityMap.ESCAPE.get( s ) );
                } else if ( c >= '\200' && c < '\377' ) {
                    String hexChars = "0123456789ABCDEF";
                    int a = c % 16;
                    int b = (c - a) / 16;
                    out.append( "&#x" ).append( hexChars.charAt( b ) ).append( hexChars.charAt( a ) ).append( ';' );
                } else {
                    out.append( c );
                }
            }

            return out.toString();
        } else {
            return input;
        }
    }

    public static String decodeForHtml(String input) {
        if (input != null && !input.isEmpty()) {
            // Decode (entity)
            for ( CharSequence entity : Html4EntityMap.UNESCAPE.keySet() ) {
                if ( input.contains( entity ) ) {
                    CharSequence c = Html4EntityMap.UNESCAPE.get( entity );
                    input = input.replace( entity, c );
                }
            }

            // Decode (hex)
            for ( CharSequence entity : Html4HexMap.UNESCAPE.keySet() ) {
                if ( input.contains( entity ) ) {
                    CharSequence c = Html4HexMap.UNESCAPE.get( entity );
                    input = input.replace( entity, c );
                }
            }

            // Decode (decimal)
            for ( CharSequence entity : Html4DecimalMap.UNESCAPE.keySet() ) {
                if ( input.contains( entity ) ) {
                    CharSequence c = Html4DecimalMap.UNESCAPE.get( entity );
                    input = input.replace( entity, c );
                }
            }
        }

        return input;
    }

    public static boolean test() {
        for ( CharSequence c : Html4EntityMap.ESCAPE.keySet()) {
            CharSequence entity = Html4EntityMap.ESCAPE.get( c );
            CharSequence e = Html4EntityMap.UNESCAPE.get( entity );
            String encode = encodeForHtml( c.toString() );
            String decode = decodeForHtml( encode );
            if (!encode.equals( decode )) {
                return false;
            }
        }
        return true;
    }
}
