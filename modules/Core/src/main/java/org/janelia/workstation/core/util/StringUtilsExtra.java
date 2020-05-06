package org.janelia.workstation.core.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Some useful functions for dealing with strings.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StringUtilsExtra {

    private static final Logger log = Logger.getLogger(StringUtilsExtra.class);

    /**
     * Returns true if the given string is null or has zero-length.
     * @param s
     * @return
     */
    public static boolean isEmpty(String s) {
        return s==null || "".equals(s);
    }

    /**
     * Returns true if the given string is null or contains only whitespace.
     * @param s
     * @return
     */
    public static boolean isBlank(String s) {
        return s==null || "".equals(s.trim());
    }

    /**
     * Returns the given string, or the defaultString if the first sring is empty.
     * @param o
     * @param defaultString
     * @return
     */
    public static String defaultIfNullOrEmpty(String o, String defaultString) {
        if (isEmpty(o)) return defaultString;
        return o;
    }

    /**
     * Returns the toString() of the given object, or the empty string if the given object is null.
     * @param o
     * @return
     */
    public static String emptyIfNull(Object o) {
        if (o==null) return "";
        return o.toString();
    }

    /**
     * Abbreviate the given string to the given max length.
     * @param str
     * @param maxLength
     * @return
     */
    public static String abbreviate(String str, int maxLength) {
        return org.apache.commons.lang3.StringUtils.abbreviate(str, maxLength);
    }

    /**
     * Returns true if both objects are null or equal to each other, as tested with the equals() method.
     * @param s1
     * @param s2
     * @return
     */
    public static boolean areEqual(Object s1, Object s2) {
        if (s1==null) {
            return s2==null;
        }
        return s1.equals(s2);
    }

    /**
     * Returns true if all the strings in the given collection are empty.
     * @param strings
     * @return
     */
    public static boolean areAllEmpty(Collection<String> strings) {
        for (String s : strings) {
            if (!isEmpty(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the given string repeated level times.
     * @param level number of times to repeat
     * @param single string to repeat
     * @return
     */
    public static String getIndent(int level, String single) {
        StringBuilder indent = new StringBuilder();
        for(int i=0; i<level; i++) {
            indent.append(single);
        }
        return indent.toString();
    }

    /**
     * Converts the given string from underscore format (e.g. "my_test") to tile case format (e.g. "My Test"
     * @param name
     * @return
     */
    public static String underscoreToTitleCase(String name) {
        String[] words = name.split("_");
        StringBuffer buf = new StringBuffer();
        for(String word : words) {
            if (word.isEmpty()) continue;
            char c = Character.toUpperCase(word.charAt(0));
            if (buf.length()>0) buf.append(' ');
            buf.append(c);
            buf.append(word.substring(1).toLowerCase());
        }
        return buf.toString();
    }

    /**
     * Returns a comma-delimited string listing the toString() representations of all the objects in the given array.
     * @param objArray objects to list
     * @return comma-delimited string
     */
    public static String getCommaDelimited(Object... objArray) {
        return getCommaDelimited(Arrays.asList(objArray));
    }

    /**
     * Returns a comma-delimited string listing the toString() representations of all the objects in the given collection.
     * @param objs objects to list
     * @return comma-delimited string
     */
    public static String getCommaDelimited(Collection<?> objs) {
        return getCommaDelimited(objs, null);
    }

    /**
     * Returns a comma-delimited string listing the toString() representations of all the objects in the given collection.
     * @param objs objects to list
     * @param maxLength Maximum length of the output string. Outputs longer than this are truncated with an elipses.
     * @return comma-delimited string
     */
    public static String getCommaDelimited(Collection<?> objs, Integer maxLength) {
        if (objs==null) return null;
        StringBuffer buf = new StringBuffer();
        for(Object obj : objs) {
            if (maxLength!=null && buf.length()+3>=maxLength) {
                buf.append("...");
                break;
            }
            if (buf.length()>0) buf.append(", ");
            buf.append(obj.toString());
        }
        return buf.toString();
    }

    /**
     * Prototype color: 91 121 227 must be turned into a 6-digit hex representation.
     */
    public static String encodeToHex(String colors) {
        StringBuilder builder = new StringBuilder();
        String[] colorArr = colors.trim().split(" ");
        if ( colorArr.length != 3 ) {
            log.warn("Color parse did not yield three values.  Leaving all-red : " + colors);
        }
        else {
            for ( int i = 0; i < colorArr.length; i++ ) {
                try {
                    String hexStr = Integer.toHexString(Integer.parseInt(colorArr[i])).toUpperCase();
                    if ( hexStr.length() == 1 ) {
                        hexStr = "0" + hexStr;
                    }
                    if ( hexStr.length() == 1 ) {
                        hexStr = "0" + hexStr;
                    }
                    builder.append( hexStr );
                } catch ( NumberFormatException nfe ) {
                    log.warn("Failed to parse " + colorArr[i] + " as Int.  Leaving 80.");
                    builder.append( "80" );
                }
            }
        }
        return builder.toString();
    }

    /**
     * Returns the number of occurences of sub in str.
     * Borrowed from Apache Commons
     * @param str string to match
     * @param sub substring to find in str
     * @return number of matches for sub in str
     */
    public static int countMatches(final String str, final String sub) {
        if (isEmpty(str) || isEmpty(sub)) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * Given some string starting with an integer, find next pos after.
     */
    public static int findFirstNonDigitPosition(String inline) {
        int afterDigits = 0;
        while (Character.isDigit(inline.charAt(afterDigits))) {
            afterDigits++;
        }
        return afterDigits;
    }

    /**
     * Given some string ending with an integer, find where the int begins.
     */
    public static int lastDigitPosition(String inline) {
        int beforeDigits = inline.length() - 1;
        while (Character.isDigit(inline.charAt(beforeDigits))) {
            beforeDigits--;
        }
        beforeDigits ++; // Move back to a convenient position.
        return beforeDigits;
    }

    /**
     * Given some filename, return an 'iterated' version, containing a counter
     * offset.  In this fashion, 'sub names' iterated over a count can be
     * generated from a 'parent name'.
     *
     * Example: mytext.txt -> mytext_1.txt   OR   mytext_2.txt
     *
     * @param baseFileName make a variant of this
     * @param offset use this offset in the new variant
     * @return the iterated filename.
     */
    public static String getIteratedName(final String baseFileName, int offset) {
        String newName = baseFileName;
        int periodPos = newName.indexOf('.');
        if (periodPos > -1) {
            newName = newName.substring(0, periodPos)
                    + '_' + offset + newName.substring(periodPos);
        }
        return newName;
    }

    /**
     * Given a variable naming pattern, replace the variables with values from the given map. The pattern syntax is as follows:
     * {Variable Name} - Variable by name
     * {Variable Name|Fallback} - Variable, with a fallback value
     * {Variable Name|Fallback|"Value"} - Multiple fallback with static value
     * @param variablePattern
     * @param values
     * @return processed output string
     */
    public static String replaceVariablePattern(String variablePattern, Map<String,Object> values) {

        log.debug("Replacing variables in pattern: "+variablePattern);

        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(variablePattern);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String template = matcher.group(1);
            String replacement = null;
            log.debug("  Matched: "+template);
            for (String templatePart : template.split("\\|")) {
                String attrLabel = templatePart.trim();
                if (attrLabel.matches("\"(.*?)\"")) {
                    replacement = attrLabel.substring(1, attrLabel.length()-1);
                }
                else {
                    Object value = values.get(attrLabel);
                    replacement = value==null?null:value.toString();
                }
                if (replacement != null) {
                    matcher.appendReplacement(buffer, replacement);
                    log.debug("    '"+template+"'->'"+replacement+"' = '"+buffer+"'");
                    break;
                }
            }

            if (replacement==null) {
                log.trace("      Cannot find a replacement for: "+template);
                matcher.appendReplacement(buffer, "");
            }
        }
        matcher.appendTail(buffer);

        log.debug("Final buffer: "+buffer);

        return buffer.toString();
    }

    /**
     * Converts a camel case string (e.g. "splitCamelCase") into a sentence (e.g. "split Camel Case")
     *
     * Taken from https://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
     *
     * @param s camel-case string
     * @return processed string, or empty string if input is null
     */
    public static String splitCamelCase(String s) {
        if (s==null) return "";
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

}
