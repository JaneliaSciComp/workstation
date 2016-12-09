package org.janelia.jacs2.model.service;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.janelia.jacs2.model.BaseEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "jacs_service_data")
@Access(AccessType.FIELD)
public class JacsServiceData implements BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "jacs_service_data_id")
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "service_type")
    private String serviceType;
    @Column(name = "service_cmd")
    private String serviceCmd;
    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private JacsServiceState state;
    @Column(name = "priority")
    private Integer priority = 0;
    @Column(name = "owner")
    private String owner;
    @Column(name = "input_path")
    private String inputPath;
    @Column(name = "output_path")
    private String outputPath;
    @Column(name = "error_path")
    private String errorPath;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "jacs_service_arg")
    private List<String> args = new ArrayList<>();
    @Column(name = "workspace")
    private String workspace;
    @Column(name = "parent_jacs_service_data_id")
    private Long parentServiceId;
    @ManyToOne
    @JoinColumn(name = "parent_jacs_service_data_id", referencedColumnName = "jacs_service_data_id", insertable = false, updatable = false)
    private JacsServiceData parentService;
    @OneToMany()
    @JoinColumn(name = "parent_jacs_service_data_id", insertable = false, updatable = false)
    private List<JacsServiceData> childServices;
    @Column(name = "root_jacs_service_data_id")
    private Long rootServiceId;
    @ManyToOne
    @JoinColumn(name = "root_jacs_service_data_id", referencedColumnName = "jacs_service_data_id", insertable = false, updatable = false)
    private JacsServiceData rootService;
    @OneToMany(mappedBy = "jacsServiceData")
    private List<JacsServiceEvent> events;
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

    public JacsServiceState getState() {
        return state;
    }

    public void setState(JacsServiceState state) {
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

    public String[] getArgsAsArray() {
        if (args == null) {
            return new String[0];
        } else {
            return args.toArray(new String[0]);
        }
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

    public JacsServiceData getParentService() {
        return parentService;
    }

    public void setParentService(JacsServiceData parentService) {
        this.parentService = parentService;
    }

    public List<JacsServiceData> getChildServices() {
        return childServices;
    }

    public void setChildServices(List<JacsServiceData> childServices) {
        this.childServices = childServices;
    }

    public Long getRootServiceId() {
        return rootServiceId;
    }

    public void setRootServiceId(Long rootServiceId) {
        this.rootServiceId = rootServiceId;
    }

    public JacsServiceData getRootService() {
        return rootService;
    }

    public void setRootService(JacsServiceData rootService) {
        this.rootService = rootService;
    }

    public List<JacsServiceEvent> getEvents() {
        return events;
    }

    public void setEvents(List<JacsServiceEvent> events) {
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

    public void addEvent(JacsServiceEvent se) {
        if (this.events == null) {
            this.events = new ArrayList<>();
        }
        se.setJacsServiceData(this);
        this.events.add(se);
    }

    public void addChildService(JacsServiceData si) {
        if (this.childServices == null) {
            this.childServices = new ArrayList<>();
        }
        si.updateParentService(this);
    }

    public void updateParentService(JacsServiceData parentService) {
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
        return ReflectionToStringBuilder.toStringExclude(this, Arrays.asList("parentService", "rootService", "childServices", "events"));
    }

    public boolean hasCompleted() {
        return state == JacsServiceState.CANCELED || state == JacsServiceState.ERROR || state == JacsServiceState.SUCCESSFUL;
    }

}
