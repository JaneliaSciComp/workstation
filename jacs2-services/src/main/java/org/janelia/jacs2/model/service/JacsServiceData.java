package org.janelia.jacs2.model.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.model.domain.support.MongoMapping;
import org.janelia.jacs2.model.BaseEntity;
import org.janelia.jacs2.utils.ISODateDeserializer;
import org.janelia.jacs2.utils.MongoNumberBigIntegerDeserializer;
import org.janelia.jacs2.utils.MongoNumberLongDeserializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

@MongoMapping(collectionName="jacsService", label="JacsService")
public class JacsServiceData implements BaseEntity, HasIdentifier {
    @JsonProperty("_id")
    @JsonDeserialize(using = MongoNumberBigIntegerDeserializer.class)
    private Number id;
    private String name;
    private String serviceType;
    private String serviceCmd;
    private JacsServiceState state = JacsServiceState.CREATED;
    private Integer priority = 0;
    private String owner;
    private String inputPath;
    private String outputPath;
    private String errorPath;
    private List<String> args = new ArrayList<>();
    private String workspace;
    @JsonDeserialize(using = MongoNumberBigIntegerDeserializer.class)
    private Number parentServiceId;
    @JsonDeserialize(using = MongoNumberBigIntegerDeserializer.class)
    private Number rootServiceId;
    private List<JacsServiceEvent> events;
    @JsonDeserialize(using = ISODateDeserializer.class)
    private Date creationDate = new Date();
    @JsonIgnore
    private JacsServiceData parentService;
    @JsonIgnore
    private List<JacsServiceData> childServices = new ArrayList<>();

    public Number getId() {
        return id;
    }

    public void setId(Number id) {
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

    public String[] getArgsArray() {
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

    public Number getParentServiceId() {
        return parentServiceId;
    }

    public void setParentServiceId(Number parentServiceId) {
        this.parentServiceId = parentServiceId;
    }

    public Number getRootServiceId() {
        return rootServiceId;
    }

    public void setRootServiceId(Number rootServiceId) {
        this.rootServiceId = rootServiceId;
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
        this.events.add(se);
    }

    public List<JacsServiceData> getChildServices() {
        return childServices;
    }

    public void addChildService(JacsServiceData childService) {
        childServices.add(childService);
        childService.updateParentService(this);
    }

    public JacsServiceData getParentService() {
        return parentService;
    }

    public void updateParentService(JacsServiceData parentService) {
        if (parentService != null) {
            this.parentService = parentService;
            setParentServiceId(parentService.getId());
            if (parentService.getRootServiceId() == null) {
                setRootServiceId(parentService.getId());
            } else {
                setRootServiceId(parentService.getRootServiceId());
            }
            if (priority == null || priority() <= parentService.priority()) {
                priority = parentService.priority() + 1;
            }
        } else {
            this.parentService = null;
            setParentServiceId(null);
            setRootServiceId(null);
        }
    }

    public Stream<JacsServiceData> serviceHierarchyStream() {
        return Stream.concat(
                Stream.of(this),
                childServices.stream().flatMap(JacsServiceData::serviceHierarchyStream)
        );
    }

    public int priority() {
        return priority != null ? priority.intValue() : 0;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    public boolean hasCompleted() {
        return state == JacsServiceState.CANCELED || state == JacsServiceState.ERROR || state == JacsServiceState.SUCCESSFUL;
    }

}
