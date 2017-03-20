package org.janelia.it.workstation.gui.large_volume_viewer.api.model.sata;

public class SataFocalPoint {

    private String centerLocation;
    private String normalVector;

    public String getCenterLocation() {
        return centerLocation;
    }

    public void setCenterLocation(String centerLocation) {
        this.centerLocation = centerLocation;
    }

    public String getNormalVector() {
        return normalVector;
    }

    public void setNormalVector(String normalVector) {
        this.normalVector = normalVector;
    }
}
