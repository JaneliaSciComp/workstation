package org.janelia.jacs2.model.service;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by goinac on 11/8/16.
 */
@Entity
@Table(name = "service_event")
public class ServiceEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "service_event_id")
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "value")
    private String value;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "event_time")
    private Date eventTime = new Date();

    @ManyToOne
    @JoinColumn(name = "service_info_id")
    private ServiceInfo serviceInfo;

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

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(ServiceInfo service) {
        this.serviceInfo = serviceInfo;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }
}
