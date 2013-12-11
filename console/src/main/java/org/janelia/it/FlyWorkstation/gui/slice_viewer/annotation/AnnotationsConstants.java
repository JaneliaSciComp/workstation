package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;

import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: olbrisd
 * Date: 12/10/13
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class AnnotationsConstants {

    // ---------- preferences ----------

    // in the entity, it's stored as R:G:B:A string, each channel 0-255 int
    public static final String PREF_ANNOTATION_COLOR_GLOBAL = "annotation-color-global";


    // ---------- annotation appearance ----------
    // this color is pale yellow-green (Christopher's original color)
    public static final Color DEFAULT_ANNOTATION_COLOR_GLOBAL = new Color(0.8f, 1.0f, 0.3f);


}
