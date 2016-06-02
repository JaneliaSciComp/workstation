package org.janelia.it.workstation.gui.alignment_board.util;

import java.awt.Color;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardReference;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.viewer3d.masking.RenderMappingI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for formatting and other niceties, in a standard way.
 *
 * @author fosterl
 */
public class RenderUtils {

    private final static Logger log = LoggerFactory.getLogger(RenderUtils.class);

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
    
    public static ABItem getObjectForItem(AlignmentBoardItem item) {
        AlignmentBoardReference ref = item.getTarget();
        if (ref==null) {
            log.warn("Null reference in item {}",item.getName());
            return null;
        }
        DomainModel domainModel = DomainMgr.getDomainMgr().getModel();
        DomainObject domainObject = domainModel.getDomainObject(ref.getObjectRef());
        if (domainObject==null) {
            return null;
        }
        if (domainObject instanceof CompartmentSet) {
            CompartmentSet cs = (CompartmentSet)domainObject;
            if (ref.getItemId()!=null) {
                return new ABCompartment(cs.getCompartment(ref.getItemId()));
            }
            else {
                return new ABCompartmentSet(cs);
            }
        }
        else if (domainObject instanceof Sample) {
            return new ABSample((Sample)domainObject);
        }
        else if (domainObject instanceof NeuronFragment) {
            return new ABNeuronFragment((NeuronFragment)domainObject);
        }
        throw new IllegalStateException("Unrecognized item type: "+domainObject.getType());
    }

}
