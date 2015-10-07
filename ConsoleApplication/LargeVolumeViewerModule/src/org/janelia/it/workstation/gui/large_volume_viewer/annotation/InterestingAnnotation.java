package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.util.Date;

/**
 *
 * @author olbrisd
 */
public class InterestingAnnotation {

    private Long annotationID;
    private Long neuronID;
    private String noteText;
    private AnnotationGeometry geometry;
    private Date creationDate;

    public InterestingAnnotation(Long annotationID, Long neuronID, Date creationDate, AnnotationGeometry geometry) {
        new InterestingAnnotation(annotationID, neuronID, creationDate, geometry, "");
    }

    public InterestingAnnotation(Long annotationID, Long neuronID, Date creationDate,
         AnnotationGeometry geometry, String noteText) {
        this.annotationID = annotationID;
        this.neuronID = neuronID;
        this.creationDate = creationDate;
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

    public Date getCreationDate() {
        return creationDate;
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
