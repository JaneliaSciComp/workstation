package org.janelia.it.workstation.browser.util;

/**
 * Utility methods for building help text with consistent syntax. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class HelpTextUtils {

    public static final String LEFT_ARROW = "←";
    public static final String RIGHT_ARROW = "→";

    public static String getBoldedLabel(String label) {
        StringBuilder sb = new StringBuilder("<b>");
        sb.append(label);
        sb.append("</b>");
        return sb.toString();
    }
    
    public static String getMenuItemLabel(String ...menuItems) {
        
        StringBuilder sb = new StringBuilder();
        for (String menuItem : menuItems) {
            if (sb.length() > 0) {
                sb.append(" ");
                sb.append(RIGHT_ARROW);
                sb.append(" ");
            }
            sb.append("<b>");
            sb.append(menuItem);
            sb.append("</b>");
        }
        
        return sb.toString();
    }
}
