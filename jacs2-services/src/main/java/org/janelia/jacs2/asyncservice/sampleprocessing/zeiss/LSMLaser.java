package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LSMLaser {
    @JsonProperty("OLEDB_LASER_ENTRY_ACQUIRE")
    private String acquire;
    @JsonProperty("OLEDB_LASER_ENTRY_NAME")
    private String name;
    @JsonProperty("OLEDB_LASER_ENTRY_POWER")
    private String power;

    public String getAcquire() {
        return acquire;
    }

    public String getName() {
        return name;
    }

    public String getPower() {
        return power;
    }
}
