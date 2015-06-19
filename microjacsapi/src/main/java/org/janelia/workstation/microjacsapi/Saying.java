package org.janelia.workstation.microjacsapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Saying {

    private String content;

    public Saying() {
    }

    public Saying(String content) {
        this.content = content;
    }

    @JsonProperty
    public String getContent() {
        return content;
    }
}