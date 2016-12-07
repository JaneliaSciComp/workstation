package org.janelia.jacs2.model.service;

import org.janelia.jacs2.model.BaseEntity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "jacs_service_event")
@Access(AccessType.FIELD)
public class JacsServiceEvent implements BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "jacs_service_event_id")
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "value")
    private String value;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "event_time")
    private Date eventTime = new Date();
    @ManyToOne
    @JoinColumn(name = "jacs_service_data_id")
    private JacsServiceData jacsServiceData;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public JacsServiceData getJacsServiceData() {
        return jacsServiceData;
    }

    public void setJacsServiceData(JacsServiceData service) {
        this.jacsServiceData = jacsServiceData;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }
}
