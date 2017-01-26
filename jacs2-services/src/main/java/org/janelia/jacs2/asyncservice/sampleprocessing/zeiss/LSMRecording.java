package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LSMRecording {
    @JsonProperty("RECORDING_ENTRY_CAMERA_BINNING")
    private String cameraBinning;
    @JsonProperty("RECORDING_ENTRY_CAMERA_FRAME_HEIGHT")
    private Integer cameraFrameHeight;
    @JsonProperty("RECORDING_ENTRY_CAMERA_FRAME_WIDTH")
    private Integer cameraFrameWidth;
    @JsonProperty("RECORDING_ENTRY_CAMERA_OFFSETX")
    private Integer cameraOffsetX;
    @JsonProperty("RECORDING_ENTRY_CAMERA_OFFSETY")
    private Integer cameraOffsetY;
    @JsonProperty("RECORDING_ENTRY_CAMERA_SUPERSAMPLING")
    private Integer cameraSuperSampling;
    @JsonProperty("RECORDING_ENTRY_DESCRIPTION")
    private String entryDescription;
    @JsonProperty("RECORDING_ENTRY_IMAGES_HEIGHT")
    private Integer imagesHeight;
    @JsonProperty("RECORDING_ENTRY_IMAGES_WIDTH")
    private Integer imagesWidth;
    @JsonProperty("RECORDING_ENTRY_IMAGES_NUMBER_CHANNELS")
    private Integer imagesNumberChannels;
    @JsonProperty("RECORDING_ENTRY_IMAGES_NUMBER_PLANES")
    private Integer imagesNumberPlanes;
    @JsonProperty("RECORDING_ENTRY_IMAGES_NUMBER_STACKS")
    private Integer imagesNumberStacks;
    @JsonProperty("RECORDING_ENTRY_INTERPOLATIONY")
    private Integer interpolation;
    @JsonProperty("RECORDING_ENTRY_LINES_PER_PLANE")
    private Integer linesPerPlane;
    @JsonProperty("RECORDING_ENTRY_LINE_SPACING")
    private Integer lineSpacing;
    @JsonProperty("RECORDING_ENTRY_LINSCAN_XY_SIZE")
    private Integer lineScanXYSize;
    @JsonProperty("RECORDING_ENTRY_NAME")
    private String entryName;
    @JsonProperty("RECORDING_ENTRY_NOTES")
    private String entryNotes;
    @JsonProperty("RECORDING_ENTRY_NUMBER_OF_STACKS")
    private Integer numberOfStacks;
    @JsonProperty("RECORDING_ENTRY_NUTATION")
    private Integer entryMutation;
    @JsonProperty("RECORDING_ENTRY_OBJECTIVE")
    private String entryObjective;
    @JsonProperty("RECORDING_ENTRY_ORIGINAL_SCAN_DATA")
    private Integer originalScanData;
    @JsonProperty("RECORDING_ENTRY_PLANES_PER_VOLUME")
    private Integer planesPerVolume;
    @JsonProperty("RECORDING_ENTRY_PLANE_SPACING")
    private Integer planeSpacing;
    @JsonProperty("RECORDING_ENTRY_POSITIONBCCORRECTION1")
    private Double positionBCorrection1;
    @JsonProperty("RECORDING_ENTRY_POSITIONBCCORRECTION2")
    private Double positionBCorrection2;
    @JsonProperty("RECORDING_ENTRY_PRECESSION")
    private Integer precession;
    @JsonProperty("RECORDING_ENTRY_PRESCAN")
    private Integer prescan;
    @JsonProperty("RECORDING_ENTRY_ROTATION")
    private Integer rotation;
    @JsonProperty("RECORDING_ENTRY_RT_BINNING")
    private Integer rtBinning;
    @JsonProperty("RECORDING_ENTRY_RT_FRAME_HEIGHT")
    private Integer rtFrameHeight;
    @JsonProperty("RECORDING_ENTRY_RT_FRAME_WIDTH")
    private Integer rtFrameWidth;
    @JsonProperty("RECORDING_ENTRY_RT_LINEPERIOD")
    private Integer rtLinePeriod;
    @JsonProperty("RECORDING_ENTRY_RT_OFFSETX")
    private Integer rtOffsetX;
    @JsonProperty("RECORDING_ENTRY_RT_OFFSETY")
    private Integer rtOffsetY;
    @JsonProperty("RECORDING_ENTRY_RT_REGION_HEIGHT")
    private Integer rtRegionHeight;
    @JsonProperty("RECORDING_ENTRY_RT_REGION_WIDTH")
    private Integer rtRegionWidth;
    @JsonProperty("RECORDING_ENTRY_RT_SUPERSAMPLING")
    private Integer rtSupersampling;
    @JsonProperty("RECORDING_ENTRY_RT_ZOOM")
    private Integer rtZoom;
    @JsonProperty("RECORDING_ENTRY_SAMPLES_PER_LINE")
    private Integer samplesPerLine;
    @JsonProperty("RECORDING_ENTRY_SAMPLE_0TIME")
    private Double sample0Time;
    @JsonProperty("RECORDING_ENTRY_SAMPLE_0X")
    private Integer sample0X;
    @JsonProperty("RECORDING_ENTRY_SAMPLE_0Y")
    private Integer sample0Y;
    @JsonProperty("RECORDING_ENTRY_SAMPLE_0Z")
    private Integer sample0Z;
    @JsonProperty("RECORDING_ENTRY_SAMPLE_SPACING")
    private Double sampleSpacing;
    @JsonProperty("RECORDING_ENTRY_SCAN_DIRECTION")
    private Integer scanDirection;
    @JsonProperty("RECORDING_ENTRY_SCAN_DIRECTIONZ")
    private Integer scanDirectionZ;
    @JsonProperty("RECORDING_ENTRY_SCAN_LINE")
    private Integer scanLine;
    @JsonProperty("RECORDING_ENTRY_SCAN_MODE")
    private String scanMode;
    @JsonProperty("RECORDING_ENTRY_SPECIAL_SCAN_MODE")
    private String specialScanMode;
    @JsonProperty("RECORDING_ENTRY_START_SCAN_EVENT")
    private String startScanEvent;
    @JsonProperty("RECORDING_ENTRY_START_SCAN_TIME")
    private Long startScanTime;
    @JsonProperty("RECORDING_ENTRY_START_SCAN_TRIGGER_IN")
    private String startScanTriggerIn;
    @JsonProperty("RECORDING_ENTRY_START_SCAN_TRIGGER_OUT")
    private String startScanTriggerOut;
    @JsonProperty("RECORDING_ENTRY_STOP_SCAN_EVENT")
    private String stopScanEvent;
    @JsonProperty("RECORDING_ENTRY_STOP_SCAN_TIME")
    private Long stopScanTime;
    @JsonProperty("RECORDING_ENTRY_STOP_SCAN_TRIGGER_IN")
    private String stopScanTriggerIn;
    @JsonProperty("RECORDING_ENTRY_STOP_SCAN_TRIGGER_OUT")
    private String stopScanTriggerOut;
    @JsonProperty("RECORDING_ENTRY_TIME_SERIES")
    private String timeSeries;
    @JsonProperty("RECORDING_ENTRY_USEBCCORRECTION")
    private String uberCorrection;
    @JsonProperty("RECORDING_ENTRY_USER")
    private String user;
    @JsonProperty("RECORDING_ENTRY_USE_REDUCED_MEMORY_ROIS")
    private Integer useReducedMemoryROIs;
    @JsonProperty("RECORDING_ENTRY_USE_ROIS")
    private Integer useROIs;
    @JsonProperty("RECORDING_ENTRY_ZOOM_X")
    private Double zoomX;
    @JsonProperty("RECORDING_ENTRY_ZOOM_Y")
    private Double zoomY;
    @JsonProperty("RECORDING_ENTRY_ZOOM_Z")
    private Double zoomZ;

    public String getCameraBinning() {
        return cameraBinning;
    }

    public Integer getCameraFrameHeight() {
        return cameraFrameHeight;
    }

    public Integer getCameraFrameWidth() {
        return cameraFrameWidth;
    }

    public Integer getCameraOffsetX() {
        return cameraOffsetX;
    }

    public Integer getCameraOffsetY() {
        return cameraOffsetY;
    }

    public Integer getCameraSuperSampling() {
        return cameraSuperSampling;
    }

    public String getEntryDescription() {
        return entryDescription;
    }

    public Integer getImagesHeight() {
        return imagesHeight;
    }

    public Integer getImagesWidth() {
        return imagesWidth;
    }

    public Integer getImagesNumberChannels() {
        return imagesNumberChannels;
    }

    public Integer getImagesNumberPlanes() {
        return imagesNumberPlanes;
    }

    public Integer getImagesNumberStacks() {
        return imagesNumberStacks;
    }

    public Integer getInterpolation() {
        return interpolation;
    }

    public Integer getLinesPerPlane() {
        return linesPerPlane;
    }

    public Integer getLineSpacing() {
        return lineSpacing;
    }

    public Integer getLineScanXYSize() {
        return lineScanXYSize;
    }

    public String getEntryName() {
        return entryName;
    }

    public String getEntryNotes() {
        return entryNotes;
    }

    public Integer getNumberOfStacks() {
        return numberOfStacks;
    }

    public Integer getEntryMutation() {
        return entryMutation;
    }

    public String getEntryObjective() {
        return entryObjective;
    }

    public Integer getOriginalScanData() {
        return originalScanData;
    }

    public Integer getPlanesPerVolume() {
        return planesPerVolume;
    }

    public Integer getPlaneSpacing() {
        return planeSpacing;
    }

    public Double getPositionBCorrection1() {
        return positionBCorrection1;
    }

    public Double getPositionBCorrection2() {
        return positionBCorrection2;
    }

    public Integer getPrecession() {
        return precession;
    }

    public Integer getPrescan() {
        return prescan;
    }

    public Integer getRotation() {
        return rotation;
    }

    public Integer getRtBinning() {
        return rtBinning;
    }

    public Integer getRtFrameHeight() {
        return rtFrameHeight;
    }

    public Integer getRtFrameWidth() {
        return rtFrameWidth;
    }

    public Integer getRtLinePeriod() {
        return rtLinePeriod;
    }

    public Integer getRtOffsetX() {
        return rtOffsetX;
    }

    public Integer getRtOffsetY() {
        return rtOffsetY;
    }

    public Integer getRtRegionHeight() {
        return rtRegionHeight;
    }

    public Integer getRtRegionWidth() {
        return rtRegionWidth;
    }

    public Integer getRtSupersampling() {
        return rtSupersampling;
    }

    public Integer getRtZoom() {
        return rtZoom;
    }

    public Integer getSamplesPerLine() {
        return samplesPerLine;
    }

    public Double getSample0Time() {
        return sample0Time;
    }

    public Integer getSample0X() {
        return sample0X;
    }

    public Integer getSample0Y() {
        return sample0Y;
    }

    public Integer getSample0Z() {
        return sample0Z;
    }

    public Double getSampleSpacing() {
        return sampleSpacing;
    }

    public Integer getScanDirection() {
        return scanDirection;
    }

    public Integer getScanDirectionZ() {
        return scanDirectionZ;
    }

    public Integer getScanLine() {
        return scanLine;
    }

    public String getScanMode() {
        return scanMode;
    }

    public String getSpecialScanMode() {
        return specialScanMode;
    }

    public String getStartScanEvent() {
        return startScanEvent;
    }

    public Long getStartScanTime() {
        return startScanTime;
    }

    public String getStartScanTriggerIn() {
        return startScanTriggerIn;
    }

    public String getStartScanTriggerOut() {
        return startScanTriggerOut;
    }

    public String getStopScanEvent() {
        return stopScanEvent;
    }

    public Long getStopScanTime() {
        return stopScanTime;
    }

    public String getStopScanTriggerIn() {
        return stopScanTriggerIn;
    }

    public String getStopScanTriggerOut() {
        return stopScanTriggerOut;
    }

    public String getTimeSeries() {
        return timeSeries;
    }

    public String getUberCorrection() {
        return uberCorrection;
    }

    public String getUser() {
        return user;
    }

    public Integer getUseReducedMemoryROIs() {
        return useReducedMemoryROIs;
    }

    public Integer getUseROIs() {
        return useROIs;
    }

    public Double getZoomX() {
        return zoomX;
    }

    public Double getZoomY() {
        return zoomY;
    }

    public Double getZoomZ() {
        return zoomZ;
    }
}
