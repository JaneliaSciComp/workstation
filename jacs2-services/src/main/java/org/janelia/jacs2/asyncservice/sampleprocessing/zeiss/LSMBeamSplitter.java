package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LSMBeamSplitter {
    @JsonProperty("BEAMSPLITTER_ENTRY_NAME")
    private String name;
    @JsonProperty("BEAMSPLITTER_ENTRY_FILTER")
    private String filter;
    @JsonProperty("BEAMSPLITTER_ENTRY_FILTER_SET")
    private String filterSet;

    public String getName() {
        return name;
    }

    public String getFilter() {
        return filter;
    }

    public String getFilterSet() {
        return filterSet;
    }
}
