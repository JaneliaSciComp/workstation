package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LSMMarker {
    @JsonProperty("MARKER_NAME")
    private String name;
    @JsonProperty("DESCRIPTION")
    private String description;
    @JsonProperty("TRIGGER_IN")
    private String triggerIn;
    @JsonProperty("TRIGGER_OUT")
    private String triggerOut;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTriggerIn() {
        return triggerIn;
    }

    public String getTriggerOut() {
        return triggerOut;
    }
}
