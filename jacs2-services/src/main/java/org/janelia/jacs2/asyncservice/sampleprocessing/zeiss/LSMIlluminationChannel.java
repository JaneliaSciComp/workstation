package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LSMIlluminationChannel {
    @JsonProperty("ILLUMCHANNEL_ENTRY_ACQUIRE")
    private String acquire;
    @JsonProperty("ILLUMCHANNEL_ENTRY_NAME")
    private String name;
    @JsonProperty("ILLUMCHANNEL_ENTRY_POWER")
    private String power;
    @JsonProperty("ILLUMCHANNEL_ENTRY_POWER_BC1")
    private String powerBc1;
    @JsonProperty("ILLUMCHANNEL_ENTRY_POWER_BC2")
    private String powerBc2;
    @JsonProperty("ILLUMCHANNEL_ENTRY_WAVELENGTH")
    private String wavelength;

    public String getAcquire() {
        return acquire;
    }

    public String getName() {
        return name;
    }

    public String getPower() {
        return power;
    }

    public String getPowerBc1() {
        return powerBc1;
    }

    public String getPowerBc2() {
        return powerBc2;
    }

    public String getWavelength() {
        return wavelength;
    }
}
