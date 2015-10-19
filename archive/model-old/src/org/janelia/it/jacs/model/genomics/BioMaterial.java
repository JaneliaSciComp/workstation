
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 5, 2006
 * Time: 3:14:47 PM
 */
public class BioMaterial implements Serializable, IsSerializable, Comparable {

    private Long materialId;
    private String materialAcc = "";
    private String project = "";
    private CollectionSite collectionSite;
    private CollectionHost collectionHost;
    private Date collectionStartTime;
    private Date collectionStopTime;

    private Map<String, CollectionObservation> observations = new HashMap<String, CollectionObservation>();
    private Set<Sample> samples;

    public BioMaterial() {
    }

    // The unique id is defined as not null in the mapping file; therefore, the BioMaterial object cannot be
    // persisted unless that value is defined.  Consequently, uniqueId ensures uniqueness of the entire graph of objects
    // under BioMaterial within the db.

    public BioMaterial(String materialAcc, String latitude, String longitude, String location) {
        this.materialAcc = materialAcc;
        this.collectionSite = new GeoPoint(latitude, longitude);
        collectionSite.setLocation(location);
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialAcc() {
        return materialAcc;
    }

    public void setMaterialAcc(String materialAcc) {
        this.materialAcc = materialAcc;
    }

    public CollectionSite getCollectionSite() {
        return collectionSite;
    }

    public void setCollectionSite(CollectionSite collectionSite) {
        this.collectionSite = collectionSite;
    }

    public CollectionHost getCollectionHost() {
        return collectionHost;
    }

    public void setCollectionHost(CollectionHost collectionHost) {
        this.collectionHost = collectionHost;
    }

    public Date getCollectionStartTime() {
        return collectionStartTime;
    }

    public void setCollectionStartTime(Date collectionStartTime) {
        this.collectionStartTime = collectionStartTime;
    }

    public Date getCollectionStopTime() {
        return collectionStopTime;
    }

    public void setCollectionStopTime(Date collectionStopTime) {
        this.collectionStopTime = collectionStopTime;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    /**
     * @return map of observations
     */
    public Map<String, CollectionObservation> getObservations() {
        return observations;
    }

    /**
     * Required by the Google Web Toolkit compiler - Must be directly before the argument
     *
     * @param observations observations about the BioMaterial
     */
    public void setObservations(Map<String, CollectionObservation> observations) {
        this.observations = observations;
    }

    public void addObservation(String observationType, String value, String units, String instrument, String comment) {
        if (null == value || "".equals(value)) {
            //logger.debug("There is no observation info to add.");
            return;
        }
        CollectionObservation newObservation = new CollectionObservation(value, units, instrument, comment);
        this.observations.put(observationType, newObservation);
    }

    public void addObservation(String observationType, String comment) {
        if (null == comment || "".equals(comment)) {
            //logger.debug("There is no observation info to add.");
            return;
        }
        CollectionObservation newObservation = new CollectionObservation(null, null, null, comment);
        this.observations.put(observationType, newObservation);
    }

    public String getObservationAsString(String observationType) {
        if (observations == null || observations.get(observationType) == null)
            return "";
        else
            return observations.get(observationType).toString();
    }

    public String toString() {
        return "BioMaterial{" +
                "materialId=" + materialId +
                ", materialAcc=" + materialAcc +
                ", project=" + project +
                ", startDate=" + collectionStartTime +
                ", stopDate=" + collectionStopTime +
                ", acquisitionPath=" + collectionSite +
                ", observations=" + observations +
                '}';
    }

    /**
     * Required by the Google Web Toolkit compiler - Must be directly before the argument
     *
     * @return the set of samples relating to this BioMaterial
     */
    public Set<Sample> getSamples() {
        return samples;
    }

    /**
     * Required by the Google Web Toolkit compiler - Must be directly before the argument
     *
     * @param samples samples to be related with this BioMaterial
     */
    public void setSamples(Set<Sample> samples) {
        this.samples = samples;
    }

    public int compareTo(Object o) {
        return this.materialAcc.compareTo(((BioMaterial) o).getMaterialAcc());
    }
}