package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    private final static String TIME_DATE_FORMAT = "HH:mm";
    private final static String DAY_DATE_FORMAT = "MM/dd";
    private final static String YEAR_DATE_FORMAT = "yyyy";

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

    public String getCreationDateText() {
        Calendar oneDayAgo = Calendar.getInstance();
        oneDayAgo.setTime(new Date());
        oneDayAgo.add(Calendar.DATE, -1);

        Calendar oneYearAgo = Calendar.getInstance();
        oneYearAgo.setTime(new Date());
        oneYearAgo.add(Calendar.YEAR, -1);

        Calendar creation = Calendar.getInstance();
        creation.setTime(getCreationDate());

        String dateFormat;
        if (oneDayAgo.compareTo(creation) < 0) {
            // hour:minute if recent (24h clock)
            dateFormat = TIME_DATE_FORMAT;
        } else if (oneYearAgo.compareTo(creation) < 0){
            // month/day if older than 1 day
            dateFormat = DAY_DATE_FORMAT;
        } else {
            // if older than 1 year, just year
            dateFormat = YEAR_DATE_FORMAT;
        }
        return new SimpleDateFormat(dateFormat).format(creationDate);
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
