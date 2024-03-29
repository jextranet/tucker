package net.jextra.tucker.encoder;

import java.util.*;

public class Html4DecimalMap extends CharacterMap
{

    public static Map<CharSequence, CharSequence> ESCAPE;
    public static Map<CharSequence, CharSequence> UNESCAPE;

    static
    {
        ESCAPE = new HashMap<>();
        ESCAPE.put( " ", "&#160;" );
        ESCAPE.put( "¡", "&#161;" );
        ESCAPE.put( "¢", "&#162;" );
        ESCAPE.put( "£", "&#163;" );
        ESCAPE.put( "¤", "&#164;" );
        ESCAPE.put( "¥", "&#165;" );
        ESCAPE.put( "¦", "&#166;" );
        ESCAPE.put( "§", "&#167;" );
        ESCAPE.put( "¨", "&#168;" );
        ESCAPE.put( "©", "&#169;" );
        ESCAPE.put( "ª", "&#170;" );
        ESCAPE.put( "«", "&#171;" );
        ESCAPE.put( "¬", "&#172;" );
        ESCAPE.put( "\u00ad", "&#173;" );
        ESCAPE.put( "®", "&#174;" );
        ESCAPE.put( "¯", "&#175;" );
        ESCAPE.put( "Ā", "&#256;" );
        ESCAPE.put( "ā", "&#257;" );
        ESCAPE.put( "Ē", "&#274;" );
        ESCAPE.put( "ē", "&#275;" );
        ESCAPE.put( "Ī", "&#298;" );
        ESCAPE.put( "ī", "&#299;" );
        ESCAPE.put( "Ō", "&#332;" );
        ESCAPE.put( "ō", "&#333;" );
        ESCAPE.put( "Ū", "&#362;" );
        ESCAPE.put( "ū", "&#363;" );
        ESCAPE.put( "°", "&#176;" );
        ESCAPE.put( "±", "&#177;" );
        ESCAPE.put( "²", "&#178;" );
        ESCAPE.put( "³", "&#179;" );
        ESCAPE.put( "´", "&#180;" );
        ESCAPE.put( "µ", "&#181;" );
        ESCAPE.put( "¶", "&#182;" );
        ESCAPE.put( "·", "&#183;" );
        ESCAPE.put( "¸", "&#184;" );
        ESCAPE.put( "¹", "&#185;" );
        ESCAPE.put( "º", "&#186;" );
        ESCAPE.put( "»", "&#187;" );
        ESCAPE.put( "¼", "&#188;" );
        ESCAPE.put( "½", "&#189;" );
        ESCAPE.put( "¾", "&#190;" );
        ESCAPE.put( "¿", "&#191;" );
        ESCAPE.put( "À", "&#192;" );
        ESCAPE.put( "Á", "&#193;" );
        ESCAPE.put( "Â", "&#194;" );
        ESCAPE.put( "Ã", "&#195;" );
        ESCAPE.put( "Ä", "&#196;" );
        ESCAPE.put( "Å", "&#197;" );
        ESCAPE.put( "Æ", "&#198;" );
        ESCAPE.put( "Ç", "&#199;" );
        ESCAPE.put( "È", "&#200;" );
        ESCAPE.put( "É", "&#201;" );
        ESCAPE.put( "Ê", "&#202;" );
        ESCAPE.put( "Ë", "&#203;" );
        ESCAPE.put( "Ì", "&#204;" );
        ESCAPE.put( "Í", "&#205;" );
        ESCAPE.put( "Î", "&#206;" );
        ESCAPE.put( "Ï", "&#207;" );
        ESCAPE.put( "Ð", "&#208;" );
        ESCAPE.put( "Ñ", "&#209;" );
        ESCAPE.put( "Ò", "&#210;" );
        ESCAPE.put( "Ó", "&#211;" );
        ESCAPE.put( "Ô", "&#212;" );
        ESCAPE.put( "Õ", "&#213;" );
        ESCAPE.put( "Ö", "&#214;" );
        ESCAPE.put( "×", "&#215;" );
        ESCAPE.put( "Ø", "&#216;" );
        ESCAPE.put( "Ù", "&#217;" );
        ESCAPE.put( "Ú", "&#218;" );
        ESCAPE.put( "Û", "&#219;" );
        ESCAPE.put( "Ü", "&#220;" );
        ESCAPE.put( "Ý", "&#221;" );
        ESCAPE.put( "Þ", "&#222;" );
        ESCAPE.put( "ß", "&#223;" );
        ESCAPE.put( "à", "&#224;" );
        ESCAPE.put( "á", "&#225;" );
        ESCAPE.put( "â", "&#226;" );
        ESCAPE.put( "ã", "&#227;" );
        ESCAPE.put( "ä", "&#228;" );
        ESCAPE.put( "å", "&#229;" );
        ESCAPE.put( "æ", "&#230;" );
        ESCAPE.put( "ç", "&#231;" );
        ESCAPE.put( "è", "&#232;" );
        ESCAPE.put( "é", "&#233;" );
        ESCAPE.put( "ê", "&#234;" );
        ESCAPE.put( "ë", "&#235;" );
        ESCAPE.put( "ì", "&#236;" );
        ESCAPE.put( "í", "&#237;" );
        ESCAPE.put( "î", "&#238;" );
        ESCAPE.put( "ï", "&#239;" );
        ESCAPE.put( "ð", "&#240;" );
        ESCAPE.put( "ñ", "&#241;" );
        ESCAPE.put( "ò", "&#242;" );
        ESCAPE.put( "ó", "&#243;" );
        ESCAPE.put( "ô", "&#244;" );
        ESCAPE.put( "õ", "&#245;" );
        ESCAPE.put( "ö", "&#246;" );
        ESCAPE.put( "÷", "&#247;" );
        ESCAPE.put( "ø", "&#248;" );
        ESCAPE.put( "ù", "&#249;" );
        ESCAPE.put( "ú", "&#250;" );
        ESCAPE.put( "û", "&#251;" );
        ESCAPE.put( "ü", "&#252;" );
        ESCAPE.put( "ý", "&#253;" );
        ESCAPE.put( "þ", "&#254;" );
        ESCAPE.put( "ÿ", "&#255;" );
        ESCAPE.put( "ƒ", "&#402;" );
        ESCAPE.put( "Α", "&#913;" );
        ESCAPE.put( "Β", "&#914;" );
        ESCAPE.put( "Γ", "&#915;" );
        ESCAPE.put( "Δ", "&#916;" );
        ESCAPE.put( "Ε", "&#917;" );
        ESCAPE.put( "Ζ", "&#918;" );
        ESCAPE.put( "Η", "&#919;" );
        ESCAPE.put( "Θ", "&#920;" );
        ESCAPE.put( "Ι", "&#921;" );
        ESCAPE.put( "Κ", "&#922;" );
        ESCAPE.put( "Λ", "&#923;" );
        ESCAPE.put( "Μ", "&#924;" );
        ESCAPE.put( "Ν", "&#925;" );
        ESCAPE.put( "Ξ", "&#926;" );
        ESCAPE.put( "Ο", "&#927;" );
        ESCAPE.put( "Π", "&#928;" );
        ESCAPE.put( "Ρ", "&#929;" );
        ESCAPE.put( "Σ", "&#931;" );
        ESCAPE.put( "Τ", "&#932;" );
        ESCAPE.put( "Υ", "&#933;" );
        ESCAPE.put( "Φ", "&#934;" );
        ESCAPE.put( "Χ", "&#935;" );
        ESCAPE.put( "Ψ", "&#936;" );
        ESCAPE.put( "Ω", "&#937;" );
        ESCAPE.put( "α", "&#945;" );
        ESCAPE.put( "β", "&#946;" );
        ESCAPE.put( "γ", "&#947;" );
        ESCAPE.put( "δ", "&#948;" );
        ESCAPE.put( "ε", "&#949;" );
        ESCAPE.put( "ζ", "&#950;" );
        ESCAPE.put( "η", "&#951;" );
        ESCAPE.put( "θ", "&#952;" );
        ESCAPE.put( "ι", "&#953;" );
        ESCAPE.put( "κ", "&#954;" );
        ESCAPE.put( "λ", "&#955;" );
        ESCAPE.put( "μ", "&#956;" );
        ESCAPE.put( "ν", "&#957;" );
        ESCAPE.put( "ξ", "&#958;" );
        ESCAPE.put( "ο", "&#959;" );
        ESCAPE.put( "π", "&#960;" );
        ESCAPE.put( "ρ", "&#961;" );
        ESCAPE.put( "ς", "&#962;" );
        ESCAPE.put( "σ", "&#963;" );
        ESCAPE.put( "τ", "&#964;" );
        ESCAPE.put( "υ", "&#965;" );
        ESCAPE.put( "φ", "&#966;" );
        ESCAPE.put( "χ", "&#967;" );
        ESCAPE.put( "ψ", "&#968;" );
        ESCAPE.put( "ω", "&#969;" );
        ESCAPE.put( "ϑ", "&#977;" );
        ESCAPE.put( "ϒ", "&#978;" );
        ESCAPE.put( "ϖ", "&#982;" );
        ESCAPE.put( "•", "&#8226;" );
        ESCAPE.put( "…", "&#8230;" );
        ESCAPE.put( "′", "&#8242;" );
        ESCAPE.put( "″", "&#8243;" );
        ESCAPE.put( "‾", "&#8254;" );
        ESCAPE.put( "⁄", "&#8260;" );
        ESCAPE.put( "℘", "&#8472;" );
        ESCAPE.put( "ℑ", "&#8465;" );
        ESCAPE.put( "ℜ", "&#8476;" );
        ESCAPE.put( "™", "&#8482;" );
        ESCAPE.put( "ℵ", "&#8501;" );
        ESCAPE.put( "←", "&#8592;" );
        ESCAPE.put( "↑", "&#8593;" );
        ESCAPE.put( "→", "&#8594;" );
        ESCAPE.put( "↓", "&#8595;" );
        ESCAPE.put( "↔", "&#8596;" );
        ESCAPE.put( "↵", "&#8629;" );
        ESCAPE.put( "⇐", "&#8656;" );
        ESCAPE.put( "⇑", "&#8657;" );
        ESCAPE.put( "⇒", "&#8658;" );
        ESCAPE.put( "⇓", "&#8659;" );
        ESCAPE.put( "⇔", "&#8660;" );
        ESCAPE.put( "∀", "&#8704;" );
        ESCAPE.put( "∂", "&#8706;" );
        ESCAPE.put( "∃", "&#8707;" );
        ESCAPE.put( "∅", "&#8709;" );
        ESCAPE.put( "∇", "&#8711;" );
        ESCAPE.put( "∈", "&#8712;" );
        ESCAPE.put( "∉", "&#8713;" );
        ESCAPE.put( "∋", "&#8715;" );
        ESCAPE.put( "∏", "&#8719;" );
        ESCAPE.put( "∑", "&#8721;" );
        ESCAPE.put( "−", "&#8722;" );
        ESCAPE.put( "∗", "&#8727;" );
        ESCAPE.put( "√", "&#8730;" );
        ESCAPE.put( "∝", "&#8733;" );
        ESCAPE.put( "∞", "&#8734;" );
        ESCAPE.put( "∠", "&#8736;" );
        ESCAPE.put( "∧", "&#8743;" );
        ESCAPE.put( "∨", "&#8744;" );
        ESCAPE.put( "∩", "&#8745;" );
        ESCAPE.put( "∪", "&#8746;" );
        ESCAPE.put( "∫", "&#8747;" );
        ESCAPE.put( "∴", "&#8756;" );
        ESCAPE.put( "∼", "&#8764;" );
        ESCAPE.put( "≅", "&#8773;" );
        ESCAPE.put( "≈", "&#8776;" );
        ESCAPE.put( "≠", "&#8800;" );
        ESCAPE.put( "≡", "&#8801;" );
        ESCAPE.put( "≤", "&#8804;" );
        ESCAPE.put( "≥", "&#8805;" );
        ESCAPE.put( "⊂", "&#8834;" );
        ESCAPE.put( "⊃", "&#8835;" );
        ESCAPE.put( "⊄", "&#8836;" );
        ESCAPE.put( "⊆", "&#8837;" );
        ESCAPE.put( "⊇", "&#8838;" );
        ESCAPE.put( "⊕", "&#8853;" );
        ESCAPE.put( "⊗", "&#8855;" );
        ESCAPE.put( "⊥", "&#8869;" );
        ESCAPE.put( "⋅", "&#8901;" );
        ESCAPE.put( "⌈", "&#8968;" );
        ESCAPE.put( "⌉", "&#8969;" );
        ESCAPE.put( "⌊", "&#8970;" );
        ESCAPE.put( "⌋", "&#8971;" );
        ESCAPE.put( "〈", "&#9001;" );
        ESCAPE.put( "〉", "&#9002;" );
        ESCAPE.put( "◊", "&#9674;" );
        ESCAPE.put( "♠", "&#9824;" );
        ESCAPE.put( "♣", "&#9827;" );
        ESCAPE.put( "♥", "&#9829;" );
        ESCAPE.put( "♦", "&#9830;" );
        ESCAPE.put( "Œ", "&#338;" );
        ESCAPE.put( "œ", "&#339;" );
        ESCAPE.put( "Š", "&#352;" );
        ESCAPE.put( "š", "&#353;" );
        ESCAPE.put( "Ÿ", "&#376;" );
        ESCAPE.put( "ˆ", "&#710;" );
        ESCAPE.put( "˜", "&#732;" );
        ESCAPE.put( " ", "&#8194;" );
        ESCAPE.put( " ", "&#8195;" );
        ESCAPE.put( " ", "&#8201;" );
        ESCAPE.put( "\u200c", "&#8204;" );
        ESCAPE.put( "\u200d", "&#8205;" );
        ESCAPE.put( "\u200e", "&#8206;" );
        ESCAPE.put( "\u200f", "&#8207;" );
        ESCAPE.put( "–", "&#8211;" );
        ESCAPE.put( "—", "&#8212;" );
        ESCAPE.put( "‘", "&#8216;" );
        ESCAPE.put( "’", "&#8217;" );
        ESCAPE.put( "‚", "&#8218;" );
        ESCAPE.put( "“", "&#8220;" );
        ESCAPE.put( "”", "&#8221;" );
        ESCAPE.put( "„", "&#8222;" );
        ESCAPE.put( "†", "&#8224;" );
        ESCAPE.put( "‡", "&#8225;" );
        ESCAPE.put( "‰", "&#8240;" );
        ESCAPE.put( "‹", "&#8249;" );
        ESCAPE.put( "›", "&#8250;" );
        ESCAPE.put( "€", "&#8364;" );
        ESCAPE.put( "\"", "&#34;" );
        ESCAPE.put( "&", "&#38;" );
        ESCAPE.put( "<", "&#60;" );
        ESCAPE.put( ">", "&#62;" );
        ESCAPE.put( "'", "&#39;" );

        ESCAPE = Collections.unmodifiableMap( ESCAPE );
        UNESCAPE = Collections.unmodifiableMap( invert( ESCAPE ) );
    }
}
