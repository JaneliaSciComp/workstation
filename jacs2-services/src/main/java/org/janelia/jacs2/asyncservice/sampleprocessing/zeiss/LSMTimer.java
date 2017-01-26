package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LSMTimer {
    @JsonProperty("TIMER_NAME")
    private String name;
    @JsonProperty("INTERVAL")
    private Double interval;
    @JsonProperty("TRIGGER_IN")
    private String triggerIn;
    @JsonProperty("TRIGGER_OUT")
    private String triggerOut;

    public String getName() {
        return name;
    }

    public Double getInterval() {
        return interval;
    }

    public String getTriggerIn() {
        return triggerIn;
    }

    public String getTriggerOut() {
        return triggerOut;
    }
}
