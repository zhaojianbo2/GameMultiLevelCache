package com.game.utils;

/**
 * 
 * @author WinkeyZhao
 *
 * 
 */
public class StringUtils {

    public static boolean isBlank(final CharSequence cs) {
	int strLen;
	if (cs == null || (strLen = cs.length()) == 0) {
	    return true;
	}
	for (int i = 0; i < strLen; i++) {
	    if (Character.isWhitespace(cs.charAt(i)) == false) {
		return false;
	    }
	}
	return true;
    }

    public static boolean isNotBlank(final CharSequence cs) {
	return !StringUtils.isBlank(cs);
    }

    public static boolean isEmpty(final CharSequence cs) {
	return cs == null || cs.length() == 0;
    }

    public static boolean equals(final CharSequence cs1, final CharSequence cs2) {
	if (cs1 == cs2) {
	    return true;
	}
	if (cs1 == null || cs2 == null) {
	    return false;
	}
	if (cs1 instanceof String && cs2 instanceof String) {
	    return cs1.equals(cs2);
	}
	return regionMatches(cs1, false, 0, cs2, 0, Math.max(cs1.length(), cs2.length()));
    }

    public static boolean regionMatches(final CharSequence cs, final boolean ignoreCase, final int thisStart,
	    final CharSequence substring, final int start, final int length) {
	if (cs instanceof String && substring instanceof String) {
	    return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
	} else {
	    int index1 = thisStart;
	    int index2 = start;
	    int tmpLen = length;

	    while (tmpLen-- > 0) {
		char c1 = cs.charAt(index1++);
		char c2 = substring.charAt(index2++);

		if (c1 == c2) {
		    continue;
		}

		if (!ignoreCase) {
		    return false;
		}

		// The same check as in String.regionMatches():
		if (Character.toUpperCase(c1) != Character.toUpperCase(c2)
			&& Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
		    return false;
		}
	    }

	    return true;
	}
    }
}
