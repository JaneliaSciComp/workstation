package org.janelia.jacs2.model.service;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringExclude;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by goinac on 11/8/16.
 */
@Entity
@Table(name = "service_info")
public class ServiceInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "service_info_id")
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "service_type")
    private String serviceType;
    @Column(name = "service_cmd")
    private String serviceCmd;
    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private ServiceState state;
    @Column(name = "priority")
    private Integer priority;
    @Column(name = "owner")
    private String owner;
    @Column(name = "input_path")
    private String inputPath;
    @Column(name = "output_path")
    private String outputPath;
    @Column(name = "error_path")
    private String errorPath;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_args")
    private List<String> args;
    @Column(name = "workspace")
    private String workspace;
    @Column(name = "parent_service_info_id")
    private Long parentServiceId;
    @ManyToOne
    @JoinColumn(name = "parent_service_info_id", referencedColumnName = "service_info_id", insertable = false, updatable = false)
    private ServiceInfo parentService;
    @OneToMany
    @JoinColumn(name = "parent_service_info_id", insertable = false, updatable = false)
    private List<ServiceInfo> childrenServices;
    @Column(name = "root_service_info_id")
    private Long rootServiceId;
    @ManyToOne
    @JoinColumn(name = "root_service_info_id", referencedColumnName = "service_info_id", insertable = false, updatable = false)
    private ServiceInfo rootService;
    @OneToMany(mappedBy = "serviceInfo")
    private List<ServiceEvent> events;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date")
    private Date creationDate = new Date();

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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceCmd() {
        return serviceCmd;
    }

    public void setServiceCmd(String serviceCmd) {
        this.serviceCmd = serviceCmd;
    }

    public ServiceState getState() {
        return state;
    }

    public void setState(ServiceState state) {
        this.state = state;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getErrorPath() {
        return errorPath;
    }

    public void setErrorPath(String errorPath) {
        this.errorPath = errorPath;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public Long getParentServiceId() {
        return parentServiceId;
    }

    public void setParentServiceId(Long parentServiceId) {
        this.parentServiceId = parentServiceId;
    }

    public ServiceInfo getParentService() {
        return parentService;
    }

    public void setParentService(ServiceInfo parentService) {
        this.parentService = parentService;
    }

    public List<ServiceInfo> getChildrenServices() {
        return childrenServices;
    }

    public void setChildrenServices(List<ServiceInfo> childrenServices) {
        this.childrenServices = childrenServices;
    }

    public Long getRootServiceId() {
        return rootServiceId;
    }

    public void setRootServiceId(Long rootServiceId) {
        this.rootServiceId = rootServiceId;
    }

    public ServiceInfo getRootService() {
        return rootService;
    }

    public void setRootService(ServiceInfo rootService) {
        this.rootService = rootService;
    }

    public List<ServiceEvent> getEvents() {
        return events;
    }

    public void setEvents(List<ServiceEvent> events) {
        this.events = events;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void addArg(String arg) {
        if (this.args == null) {
            this.args = new ArrayList<>();
        }
        this.args.add(arg);
    }

    public void addEvent(ServiceEvent se) {
        if (this.events == null) {
            this.events = new ArrayList<>();
        }
        se.setServiceInfo(this);
        this.events.add(se);
    }

    public void addChildService(ServiceInfo si) {
        if (this.childrenServices == null) {
            this.childrenServices = new ArrayList<>();
        }
        si.updateParentService(this);
    }

    public void updateParentService(ServiceInfo parentService) {
        setParentService(parentService);
        if (parentService != null) {
            setParentServiceId(parentService.getId());
            if (parentService.getRootServiceId() == null) {
                setRootServiceId(parentService.getId());
                setRootService(parentService);
            } else {
                setRootServiceId(parentService.getRootServiceId());
                setRootService(parentService.getRootService());
            }
        }
    }

    public int priority() {
        return priority != null ? priority.intValue() : 0;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toStringExclude(this, Arrays.asList("parentService", "rootService", "childrenServices", "events"));
    }
}
