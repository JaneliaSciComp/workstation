package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

/**
 *
 * @author olbrisd
 */
public class InterestingAnnotation {

    private Long annotationID;
    private Long neuronID;
    private String noteText;
    private AnnotationGeometry geometry;

    public InterestingAnnotation(Long annotationID, Long neuronID, AnnotationGeometry geometry) {
        new InterestingAnnotation(annotationID, neuronID, geometry, "");
    }

    public InterestingAnnotation(Long annotationID, Long neuronID, AnnotationGeometry geometry, String noteText) {
        this.annotationID = annotationID;
        this.neuronID = neuronID;
        this.noteText = noteText;
        this.geometry = geometry;
    }

    public Long getAnnotationID() {
        return annotationID;
    }

    public Long getNeuronID() {
        return neuronID;
    }

    public String getAnnIDText() {
        String annID = annotationID.toString();
        return annID.substring(annID.length() - 4);
    }

    public boolean hasNote() {
        return getNoteText().length() > 0;
    }

    public String getNoteText() {
        return noteText;
    }

    public AnnotationGeometry getGeometry() {
        return geometry;
    }

    public String getGeometryText() {
        return geometry.getTexticon();
    }
}
