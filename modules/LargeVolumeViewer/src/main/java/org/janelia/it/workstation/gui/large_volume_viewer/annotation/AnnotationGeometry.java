package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

/**
 * terms for describing annotation geometry
 * @author olbrisd
 */
public enum AnnotationGeometry {

    // note: this is also the sort order
    ROOT("o--"),
    LINK("---"),
    BRANCH("--<"),
    END("--o");

    private String texticon;

    AnnotationGeometry(String texticon) {
        this.texticon = texticon;
    }

    public String getTexticon() {
        return texticon;
    }

    public String toString() {return texticon; }
}
