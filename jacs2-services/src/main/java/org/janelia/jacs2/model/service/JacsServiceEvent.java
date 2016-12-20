package org.janelia.jacs2.model.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.janelia.jacs2.utils.ISODateDeserializer;

import java.util.Date;

public class JacsServiceEvent {
    private String name;
    private String value;
    @JsonDeserialize(using = ISODateDeserializer.class)
    private Date eventTime = new Date();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }
}
