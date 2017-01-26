package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LSMDetectionChannel {
    @JsonProperty("DETCHANNEL_AMPLIFIER_NAME")
    private String amplifierName;
    @JsonProperty("DETCHANNEL_DETECTION_CHANNEL_NAME")
    private String name;
    @JsonProperty("DETCHANNEL_ENTRY_ACQUIRE")
    private String acquire;
    @JsonProperty("DETCHANNEL_ENTRY_DYE_FOLDER")
    private String dyeFolder;
    @JsonProperty("DETCHANNEL_ENTRY_DYE_NAME")
    private String dyeName;
    @JsonProperty("DETCHANNEL_FILTER_SET_NAME")
    private String filterSetName;
    @JsonProperty("DETCHANNEL_INTEGRATOR_NAME")
    private String integratorName;
    @JsonProperty("DETCHANNEL_PINHOLE_NAME")
    private String pinholeName;
    @JsonProperty("DETCHANNEL_POINT_DETECTOR_NAME")
    private String pointDetectorName;
    @JsonProperty("DETCHANNEL_ENTRY_SPI_WAVELENGTH_START")
    private String wavelengthStart;
    @JsonProperty("DETCHANNEL_ENTRY_SPI_WAVELENGTH_END")
    private String wavelengthEnd;
    @JsonProperty("DETCHANNEL_SPI_WAVELENGTH_START2")
    private String wavelengthStart2;
    @JsonProperty("DETCHANNEL_SPI_WAVELENGTH_END2")
    private String wavelengthEnd2;
    @JsonProperty("DETCHANNEL_ENTRY_DETECTOR_GAIN")
    private Double detectorGain;
    @JsonProperty("DETCHANNEL_ENTRY_DETECTOR_GAIN_BC1")
    private Double detectorGainBc1;
    @JsonProperty("DETCHANNEL_ENTRY_DETECTOR_GAIN_BC2")
    private Double detectorGainBc2;
    @JsonProperty("DETCHANNEL_ENTRY_DETECTOR_GAIN_LAST")
    private Double detectorGainLast;

    public String getAmplifierName() {
        return amplifierName;
    }

    public String getName() {
        return name;
    }

    public String getAcquire() {
        return acquire;
    }

    public String getDyeFolder() {
        return dyeFolder;
    }

    public String getDyeName() {
        return dyeName;
    }

    public String getFilterSetName() {
        return filterSetName;
    }

    public String getIntegratorName() {
        return integratorName;
    }

    public String getPinholeName() {
        return pinholeName;
    }

    public String getPointDetectorName() {
        return pointDetectorName;
    }

    public String getWavelengthStart() {
        return wavelengthStart;
    }

    public String getWavelengthEnd() {
        return wavelengthEnd;
    }

    public String getWavelengthStart2() {
        return wavelengthStart2;
    }

    public String getWavelengthEnd2() {
        return wavelengthEnd2;
    }

    public Double getDetectorGain() {
        return detectorGain;
    }

    public Double getDetectorGainBc1() {
        return detectorGainBc1;
    }

    public Double getDetectorGainBc2() {
        return detectorGainBc2;
    }

    public Double getDetectorGainLast() {
        return detectorGainLast;
    }
}
