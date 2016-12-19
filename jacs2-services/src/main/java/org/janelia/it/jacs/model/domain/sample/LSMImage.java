package org.janelia.it.jacs.model.domain.sample;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.jacs2.utils.ISODateDeserializer;

import java.util.Date;

public class LSMImage extends Image {
    private Reference sampleRef;
    private Boolean sageSynced = false;
    private String channelColors;
    private String channelDyeNames;
    private String brightnessCompensation;
    @JsonDeserialize(using = ISODateDeserializer.class)
    private Date completionDate;

    // SAGE Terms
    @JsonDeserialize(using = ISODateDeserializer.class)
    private Date tmogDate;
    private Integer sageId;
    private String line;
    private Boolean representative;
    private String age;
    private String annotatedBy;
    private String anatomicalArea;
    private String bcCorrection1;
    private String bcCorrection2;
    private Integer bitsPerSample;
    @JsonDeserialize(using = ISODateDeserializer.class)
    private Date captureDate;
    private String chanSpec;
    private String detectionChannel1DetectorGain;
    private String detectionChannel2DetectorGain;
    private String detectionChannel3DetectorGain;
    private String driver;
    private Long fileSize;
    private String effector;
    private Integer crossBarcode;
    private String gender;
    private String fullAge;
    private String mountingProtocol;
    private String heatShockHour;
    private String heatShockInterval;
    private String heatShockMinutes;
    private String illuminationChannel1Name;
    private String illuminationChannel2Name;
    private String illuminationChannel3Name;
    private String illuminationChannel1PowerBC1;
    private String illuminationChannel2PowerBC1;
    private String illuminationChannel3PowerBC1;
    private String imageFamily;
    private String createdBy;
    private String dataSet;
    private String imagingProject;
    private String interpolationElapsed;
    private Integer interpolationStart;
    private Integer interpolationStop;
    private String microscope;
    private String microscopeFilename;
    private String macAddress;
    private String objectiveName;
    private String sampleZeroTime;
    private String sampleZeroZ;
    private Integer scanStart;
    private Integer scanStop;
    private String scanType;
    private String screenState;
    private String slideCode;
    private String tile;
    private String tissueOrientation;
    private String totalPixels;
    private Integer tracks;
    private String voxelSizeX;
    private String voxelSizeY;
    private String voxelSizeZ;
    private String dimensionX;
    private String dimensionY;
    private String dimensionZ;
    private String zoomX;
    private String zoomY;
    private String zoomZ;
    private String vtLine;
    private String qiScore;
    private String qmScore;
    private String organism;
    private String genotype;
    private Integer flycoreId;
    private String flycoreAlias;
    private String flycoreLabId;
    private String flycoreLandingSite;
    private String flycorePermission;
    private String flycoreProject;
    private String flycorePSubcategory;
    private String lineHide;

    public Reference getSampleRef() {
        return sampleRef;
    }

    public void setSampleRef(Reference sampleRef) {
        this.sampleRef = sampleRef;
    }

    public Boolean getSageSynced() {
        return sageSynced;
    }

    public void setSageSynced(Boolean sageSynced) {
        this.sageSynced = sageSynced;
    }

    public String getChannelColors() {
        return channelColors;
    }

    public void setChannelColors(String channelColors) {
        this.channelColors = channelColors;
    }

    public String getChannelDyeNames() {
        return channelDyeNames;
    }

    public void setChannelDyeNames(String channelDyeNames) {
        this.channelDyeNames = channelDyeNames;
    }

    public String getBrightnessCompensation() {
        return brightnessCompensation;
    }

    public void setBrightnessCompensation(String brightnessCompensation) {
        this.brightnessCompensation = brightnessCompensation;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public Date getTmogDate() {
        return tmogDate;
    }

    public void setTmogDate(Date tmogDate) {
        this.tmogDate = tmogDate;
    }

    public Integer getSageId() {
        return sageId;
    }

    public void setSageId(Integer sageId) {
        this.sageId = sageId;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public Boolean getRepresentative() {
        return representative;
    }

    public void setRepresentative(Boolean representative) {
        this.representative = representative;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getAnnotatedBy() {
        return annotatedBy;
    }

    public void setAnnotatedBy(String annotatedBy) {
        this.annotatedBy = annotatedBy;
    }

    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }

    public String getBcCorrection1() {
        return bcCorrection1;
    }

    public void setBcCorrection1(String bcCorrection1) {
        this.bcCorrection1 = bcCorrection1;
    }

    public String getBcCorrection2() {
        return bcCorrection2;
    }

    public void setBcCorrection2(String bcCorrection2) {
        this.bcCorrection2 = bcCorrection2;
    }

    public Integer getBitsPerSample() {
        return bitsPerSample;
    }

    public void setBitsPerSample(Integer bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public Date getCaptureDate() {
        return captureDate;
    }

    public void setCaptureDate(Date captureDate) {
        this.captureDate = captureDate;
    }

    public String getChanSpec() {
        return chanSpec;
    }

    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
    }

    public String getDetectionChannel1DetectorGain() {
        return detectionChannel1DetectorGain;
    }

    public void setDetectionChannel1DetectorGain(String detectionChannel1DetectorGain) {
        this.detectionChannel1DetectorGain = detectionChannel1DetectorGain;
    }

    public String getDetectionChannel2DetectorGain() {
        return detectionChannel2DetectorGain;
    }

    public void setDetectionChannel2DetectorGain(String detectionChannel2DetectorGain) {
        this.detectionChannel2DetectorGain = detectionChannel2DetectorGain;
    }

    public String getDetectionChannel3DetectorGain() {
        return detectionChannel3DetectorGain;
    }

    public void setDetectionChannel3DetectorGain(String detectionChannel3DetectorGain) {
        this.detectionChannel3DetectorGain = detectionChannel3DetectorGain;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getEffector() {
        return effector;
    }

    public void setEffector(String effector) {
        this.effector = effector;
    }

    public Integer getCrossBarcode() {
        return crossBarcode;
    }

    public void setCrossBarcode(Integer crossBarcode) {
        this.crossBarcode = crossBarcode;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getFullAge() {
        return fullAge;
    }

    public void setFullAge(String fullAge) {
        this.fullAge = fullAge;
    }

    public String getMountingProtocol() {
        return mountingProtocol;
    }

    public void setMountingProtocol(String mountingProtocol) {
        this.mountingProtocol = mountingProtocol;
    }

    public String getHeatShockHour() {
        return heatShockHour;
    }

    public void setHeatShockHour(String heatShockHour) {
        this.heatShockHour = heatShockHour;
    }

    public String getHeatShockInterval() {
        return heatShockInterval;
    }

    public void setHeatShockInterval(String heatShockInterval) {
        this.heatShockInterval = heatShockInterval;
    }

    public String getHeatShockMinutes() {
        return heatShockMinutes;
    }

    public void setHeatShockMinutes(String heatShockMinutes) {
        this.heatShockMinutes = heatShockMinutes;
    }

    public String getIlluminationChannel1Name() {
        return illuminationChannel1Name;
    }

    public void setIlluminationChannel1Name(String illuminationChannel1Name) {
        this.illuminationChannel1Name = illuminationChannel1Name;
    }

    public String getIlluminationChannel2Name() {
        return illuminationChannel2Name;
    }

    public void setIlluminationChannel2Name(String illuminationChannel2Name) {
        this.illuminationChannel2Name = illuminationChannel2Name;
    }

    public String getIlluminationChannel3Name() {
        return illuminationChannel3Name;
    }

    public void setIlluminationChannel3Name(String illuminationChannel3Name) {
        this.illuminationChannel3Name = illuminationChannel3Name;
    }

    public String getIlluminationChannel1PowerBC1() {
        return illuminationChannel1PowerBC1;
    }

    public void setIlluminationChannel1PowerBC1(String illuminationChannel1PowerBC1) {
        this.illuminationChannel1PowerBC1 = illuminationChannel1PowerBC1;
    }

    public String getIlluminationChannel2PowerBC1() {
        return illuminationChannel2PowerBC1;
    }

    public void setIlluminationChannel2PowerBC1(String illuminationChannel2PowerBC1) {
        this.illuminationChannel2PowerBC1 = illuminationChannel2PowerBC1;
    }

    public String getIlluminationChannel3PowerBC1() {
        return illuminationChannel3PowerBC1;
    }

    public void setIlluminationChannel3PowerBC1(String illuminationChannel3PowerBC1) {
        this.illuminationChannel3PowerBC1 = illuminationChannel3PowerBC1;
    }

    public String getImageFamily() {
        return imageFamily;
    }

    public void setImageFamily(String imageFamily) {
        this.imageFamily = imageFamily;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getImagingProject() {
        return imagingProject;
    }

    public void setImagingProject(String imagingProject) {
        this.imagingProject = imagingProject;
    }

    public String getInterpolationElapsed() {
        return interpolationElapsed;
    }

    public void setInterpolationElapsed(String interpolationElapsed) {
        this.interpolationElapsed = interpolationElapsed;
    }

    public Integer getInterpolationStart() {
        return interpolationStart;
    }

    public void setInterpolationStart(Integer interpolationStart) {
        this.interpolationStart = interpolationStart;
    }

    public Integer getInterpolationStop() {
        return interpolationStop;
    }

    public void setInterpolationStop(Integer interpolationStop) {
        this.interpolationStop = interpolationStop;
    }

    public String getMicroscope() {
        return microscope;
    }

    public void setMicroscope(String microscope) {
        this.microscope = microscope;
    }

    public String getMicroscopeFilename() {
        return microscopeFilename;
    }

    public void setMicroscopeFilename(String microscopeFilename) {
        this.microscopeFilename = microscopeFilename;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getObjectiveName() {
        return objectiveName;
    }

    public void setObjectiveName(String objectiveName) {
        this.objectiveName = objectiveName;
    }

    public String getSampleZeroTime() {
        return sampleZeroTime;
    }

    public void setSampleZeroTime(String sampleZeroTime) {
        this.sampleZeroTime = sampleZeroTime;
    }

    public String getSampleZeroZ() {
        return sampleZeroZ;
    }

    public void setSampleZeroZ(String sampleZeroZ) {
        this.sampleZeroZ = sampleZeroZ;
    }

    public Integer getScanStart() {
        return scanStart;
    }

    public void setScanStart(Integer scanStart) {
        this.scanStart = scanStart;
    }

    public Integer getScanStop() {
        return scanStop;
    }

    public void setScanStop(Integer scanStop) {
        this.scanStop = scanStop;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public String getScreenState() {
        return screenState;
    }

    public void setScreenState(String screenState) {
        this.screenState = screenState;
    }

    public String getSlideCode() {
        return slideCode;
    }

    public void setSlideCode(String slideCode) {
        this.slideCode = slideCode;
    }

    public String getTile() {
        return tile;
    }

    public void setTile(String tile) {
        this.tile = tile;
    }

    public String getTissueOrientation() {
        return tissueOrientation;
    }

    public void setTissueOrientation(String tissueOrientation) {
        this.tissueOrientation = tissueOrientation;
    }

    public String getTotalPixels() {
        return totalPixels;
    }

    public void setTotalPixels(String totalPixels) {
        this.totalPixels = totalPixels;
    }

    public Integer getTracks() {
        return tracks;
    }

    public void setTracks(Integer tracks) {
        this.tracks = tracks;
    }

    public String getVoxelSizeX() {
        return voxelSizeX;
    }

    public void setVoxelSizeX(String voxelSizeX) {
        this.voxelSizeX = voxelSizeX;
    }

    public String getVoxelSizeY() {
        return voxelSizeY;
    }

    public void setVoxelSizeY(String voxelSizeY) {
        this.voxelSizeY = voxelSizeY;
    }

    public String getVoxelSizeZ() {
        return voxelSizeZ;
    }

    public void setVoxelSizeZ(String voxelSizeZ) {
        this.voxelSizeZ = voxelSizeZ;
    }

    public String getDimensionX() {
        return dimensionX;
    }

    public void setDimensionX(String dimensionX) {
        this.dimensionX = dimensionX;
    }

    public String getDimensionY() {
        return dimensionY;
    }

    public void setDimensionY(String dimensionY) {
        this.dimensionY = dimensionY;
    }

    public String getDimensionZ() {
        return dimensionZ;
    }

    public void setDimensionZ(String dimensionZ) {
        this.dimensionZ = dimensionZ;
    }

    public String getZoomX() {
        return zoomX;
    }

    public void setZoomX(String zoomX) {
        this.zoomX = zoomX;
    }

    public String getZoomY() {
        return zoomY;
    }

    public void setZoomY(String zoomY) {
        this.zoomY = zoomY;
    }

    public String getZoomZ() {
        return zoomZ;
    }

    public void setZoomZ(String zoomZ) {
        this.zoomZ = zoomZ;
    }

    public String getVtLine() {
        return vtLine;
    }

    public void setVtLine(String vtLine) {
        this.vtLine = vtLine;
    }

    public String getQiScore() {
        return qiScore;
    }

    public void setQiScore(String qiScore) {
        this.qiScore = qiScore;
    }

    public String getQmScore() {
        return qmScore;
    }

    public void setQmScore(String qmScore) {
        this.qmScore = qmScore;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getGenotype() {
        return genotype;
    }

    public void setGenotype(String genotype) {
        this.genotype = genotype;
    }

    public Integer getFlycoreId() {
        return flycoreId;
    }

    public void setFlycoreId(Integer flycoreId) {
        this.flycoreId = flycoreId;
    }

    public String getFlycoreAlias() {
        return flycoreAlias;
    }

    public void setFlycoreAlias(String flycoreAlias) {
        this.flycoreAlias = flycoreAlias;
    }

    public String getFlycoreLabId() {
        return flycoreLabId;
    }

    public void setFlycoreLabId(String flycoreLabId) {
        this.flycoreLabId = flycoreLabId;
    }

    public String getFlycoreLandingSite() {
        return flycoreLandingSite;
    }

    public void setFlycoreLandingSite(String flycoreLandingSite) {
        this.flycoreLandingSite = flycoreLandingSite;
    }

    public String getFlycorePermission() {
        return flycorePermission;
    }

    public void setFlycorePermission(String flycorePermission) {
        this.flycorePermission = flycorePermission;
    }

    public String getFlycoreProject() {
        return flycoreProject;
    }

    public void setFlycoreProject(String flycoreProject) {
        this.flycoreProject = flycoreProject;
    }

    public String getFlycorePSubcategory() {
        return flycorePSubcategory;
    }

    public void setFlycorePSubcategory(String flycorePSubcategory) {
        this.flycorePSubcategory = flycorePSubcategory;
    }

    public String getLineHide() {
        return lineHide;
    }

    public void setLineHide(String lineHide) {
        this.lineHide = lineHide;
    }
}
