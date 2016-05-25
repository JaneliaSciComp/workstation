/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board.util;

import java.awt.Color;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.viewer3d.masking.RenderMappingI;

/**
 * Utilities for formatting and other niceties, in a standard way.
 *
 * @author fosterl
 */
public class RenderUtils {
    /**
     * Tells if passthrough is used for this item.
     * @param item has a rendering type.
     * @return T: type is pass-through.
     */
    public static boolean isPassthroughRendering(AlignmentBoardItem item) {
        return RenderMappingI.PASSTHROUGH_RENDER_ATTRIBUTE.equals(item.getRenderMethod());
    }

    /**
     * Note: at time of writing, there is only one override being used, even
     * though there are several possible rendering attribute types. If this is
     * not used, the value will be Neuron or Compartment.
     *
     * @param passthroughRendering T to override rendering as passthrough
     * @throws Exception for the setter.
     */
    public static void setPassthroughRendering(boolean passthroughRendering, AlignmentBoardItem item) throws Exception {
        String value = passthroughRendering ? RenderMappingI.PASSTHROUGH_RENDER_ATTRIBUTE : null;
        item.setRenderMethod(value);
    }
    
    /**
     * @param rgbStr format is RRGGBB
     * @return AWT converted color.
     */
    public static Color getColorFromRGBStr(String rgbStr) {
        if (rgbStr == null) {
            return null;
        }
        else {
            if (rgbStr.length() > 6) {
                rgbStr = rgbStr.substring(rgbStr.length() - 6);
            }
            return Color.decode("0x" + rgbStr.toUpperCase());
        }
    }
    
    public static String getRGBStrFromColor(Color color) {
        return String.format("%06x", color.getRGB());
    }

    /**
     * Class names can be made more presentable to users.
     * 
     * @param className original, fully-qualified name.
     * @return trimmed, viewable.
     */
    public static String getViewableClassName(String className) throws ClassNotFoundException {
        Class clazz = Class.forName(className);
        String simpleName = clazz.getSimpleName();
        StringBuilder niceBuilder = new StringBuilder();
        boolean beginning = true;
        for (char ch: simpleName.toCharArray()) {
            if (Character.isUpperCase(ch)  &&  ! beginning) {
                niceBuilder.append(" ");
            }
            else if (beginning) {
                niceBuilder.append(Character.toUpperCase(ch));                
            }
            else {
                niceBuilder.append(ch);
            }
            beginning = false;
        }
        return niceBuilder.toString();
    }
    
    public static DomainObject getObjectForItem(AlignmentBoardItem item) {
        DomainModel domainModel = DomainMgr.getDomainMgr().getModel();
        return domainModel.getDomainObject(item.getTarget());
    }

}
