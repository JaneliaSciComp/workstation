package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.FragmentBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/5/13
 * Time: 3:04 PM
 *
 * Simplistic implementation of a color mapping.
 */
public class ColorWheelColorMapping implements ColorMappingI {
    public Map<Integer,byte[]> getMapping( List<FragmentBean> fragments ) {
        Map<Integer,byte[]> maskMappings = new HashMap<Integer,byte[]>();
//for (int i=0; i < 65535; i++) {
//    maskMappings.put(i, new byte[]{ (byte)0xff, (byte)0, (byte)0xff });
//}

        byte[][] colorWheel = {
                { (byte)0x00, (byte)0x00, (byte)0xff },
                { (byte)0x00, (byte)0xff, (byte)0x00 },
                { (byte)0xff, (byte)0x00, (byte)0x00 },
                { (byte)0x00, (byte)0xff, (byte)0xff },
                { (byte)0xff, (byte)0x00, (byte)0xff },
                { (byte)0xff, (byte)0xff, (byte)0x00 },
                { (byte)0x8f, (byte)0x00, (byte)0x00 },
                { (byte)0x00, (byte)0x8f, (byte)0x00 },
        };
        for ( FragmentBean fragmentBean: fragments ) {
            // Make the "back map" to the original fragment number.
            int translatedNum = fragmentBean.getTranslatedNum();
            byte[] rgb = fragmentBean.getRgb();
            if ( rgb == null ) {
                maskMappings.put( translatedNum, colorWheel[ translatedNum % colorWheel.length ] );
            }
            else {
                maskMappings.put( translatedNum, rgb );
            }
        }

        return maskMappings;
    }
}
