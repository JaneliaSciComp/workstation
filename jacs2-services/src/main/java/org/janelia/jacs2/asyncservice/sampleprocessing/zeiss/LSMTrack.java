package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LSMTrack {
    @JsonProperty("TRACK_ENTRY_ACQUIRE")
    private String acquire;
    @JsonProperty("TRACK_ENTRY_BLEACH_COUNT")
    private String bleachCount;
    @JsonProperty("TRACK_ENTRY_BLEACH_SCAN_NUMBER")
    private String bleachScanNumber;
    @JsonProperty("TRACK_ENTRY_IS_BLEACH_AFTER_SCAN_NUMBER")
    private String isBleachAfterScanNumber;
    @JsonProperty("TRACK_ENTRY_IS_BLEACH_TRACK")
    private String isBleachTrack;
    @JsonProperty("TRACK_ENTRY_IS_RATIO_STACK")
    private String isRatioStack;
    @JsonProperty("TRACK_ENTRY_MULTIPLEX_ORDER")
    private String multiplexOrder;
    @JsonProperty("TRACK_ENTRY_MULTIPLEX_TYPE")
    private String multiplexType;
    @JsonProperty("TRACK_ENTRY_NAME")
    private String name;
    @JsonProperty("TRACK_ENTRY_PIXEL_TIME")
    private String pixelTime;
    @JsonProperty("TRACK_ENTRY_SAMPLING_METHOD")
    private String samplingMethod;
    @JsonProperty("TRACK_ENTRY_SAMPLING_MODE")
    private String samplingMode;
    @JsonProperty("TRACK_ENTRY_SAMPLING_NUMBER")
    private String samplingNumber;
    @JsonProperty("TRACK_ENTRY_SPI_CENTER_WAVELENGTH")
    private String spiCenterWavelength;
    @JsonProperty("TRACK_ENTRY_TIME_BETWEEN_STACKS")
    private String timeBetweenStacks;
    @JsonProperty("TRACK_ENTRY_TRIGGER_IN")
    private String triggerIn;
    @JsonProperty("TRACK_ENTRY_TRIGGER_OUT")
    private String triggerOut;
    @JsonProperty("TRACK_LASER_SUPRESSION_MODE")
    private String laserSupressionMode;
    @JsonProperty("TRACK_REFLECTED_LIGHT")
    private String reflectedLight;
    @JsonProperty("TRACK_TRANSMITTED_LIGHT")
    private String transmittedLight;
    @JsonProperty("beam_splitters")
    private List<LSMBeamSplitter> beamSplitters;
    @JsonProperty("data_channels")
    private List<LSMDataChannel> dataChannels;
    @JsonProperty("detection_channels")
    private List<LSMDetectionChannel> detectionChannels;
    @JsonProperty("illumination_channels")
    private List<LSMIlluminationChannel> illuminationChannels;

    public String getAcquire() {
        return acquire;
    }

    public String getBleachCount() {
        return bleachCount;
    }

    public String getBleachScanNumber() {
        return bleachScanNumber;
    }

    public String getIsBleachAfterScanNumber() {
        return isBleachAfterScanNumber;
    }

    public String getIsBleachTrack() {
        return isBleachTrack;
    }

    public String getIsRatioStack() {
        return isRatioStack;
    }

    public String getMultiplexOrder() {
        return multiplexOrder;
    }

    public String getMultiplexType() {
        return multiplexType;
    }

    public String getName() {
        return name;
    }

    public String getPixelTime() {
        return pixelTime;
    }

    public String getSamplingMethod() {
        return samplingMethod;
    }

    public String getSamplingMode() {
        return samplingMode;
    }

    public String getSamplingNumber() {
        return samplingNumber;
    }

    public String getSpiCenterWavelength() {
        return spiCenterWavelength;
    }

    public String getTimeBetweenStacks() {
        return timeBetweenStacks;
    }

    public String getTriggerIn() {
        return triggerIn;
    }

    public String getTriggerOut() {
        return triggerOut;
    }

    public String getLaserSupressionMode() {
        return laserSupressionMode;
    }

    public String getReflectedLight() {
        return reflectedLight;
    }

    public String getTransmittedLight() {
        return transmittedLight;
    }

    public List<LSMBeamSplitter> getBeamSplitters() {
        return beamSplitters;
    }

    public List<LSMDataChannel> getDataChannels() {
        return dataChannels;
    }

    public List<LSMDetectionChannel> getDetectionChannels() {
        return detectionChannels;
    }

    public List<LSMIlluminationChannel> getIlluminationChannels() {
        return illuminationChannels;
    }
}
