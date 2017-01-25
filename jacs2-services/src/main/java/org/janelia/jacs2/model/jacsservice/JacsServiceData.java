package org.janelia.jacs2.model.jacsservice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.model.domain.support.MongoMapping;
import org.janelia.jacs2.model.BaseEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@MongoMapping(collectionName="jacsService", label="JacsService")
public class JacsServiceData implements BaseEntity, HasIdentifier {
    @JsonProperty("_id")
    private Number id;
    private String name;
    private ProcessingLocation processingLocation;
    private String serviceCmd;
    private JacsServiceState state = JacsServiceState.CREATED;
    private Integer priority = 0;
    private String owner;
    private String inputPath;
    private String outputPath;
    private String errorPath;
    private List<String> args = new ArrayList<>();
    private Map<String, String> env = new LinkedHashMap<>();
    private Map<String, String> resources = new LinkedHashMap<>(); // this could/should be used for grid jobs resources
    private String stringifiedResult;
    private String workspace;
    private Number parentServiceId;
    private Number rootServiceId;
    private List<JacsServiceEvent> events;
    private Date creationDate = new Date();
    @JsonIgnore
    private JacsServiceData parentService;
    @JsonIgnore
    private List<JacsServiceData> dependencies = new ArrayList<>();
    private List<Number> dependeciesIds = new ArrayList<>();
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

    public ProcessingLocation getProcessingLocation() {
        return processingLocation;
    }

    public void setProcessingLocation(ProcessingLocation processingLocation) {
        this.processingLocation = processingLocation;
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

    @JsonIgnore
    public String[] getArgsArray() {
        if (args == null) {
            return new String[0];
        } else {
            return args.toArray(new String[0]);
        }
    }

    public void addArg(String arg) {
        if (this.args == null) {
            this.args = new ArrayList<>();
        }
        this.args.add(arg);
    }

    public void clearArgs() {
        if (this.args != null) {
            this.args.clear();
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

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public void addToEnv(String name, String value) {
        this.env.put(name, value);
    }

    public void clearEnv() {
        this.env.clear();
    }

    public Map<String, String> getResources() {
        return resources;
    }

    public void setResources(Map<String, String> resources) {
        this.resources = resources;
    }

    public void addToResources(String name, String value) {
        this.resources.put(name, value);
    }

    public void clearResources() {
        this.resources.clear();
    }

    public String getStringifiedResult() {
        return stringifiedResult;
    }

    public void setStringifiedResult(String stringifiedResult) {
        this.stringifiedResult = stringifiedResult;
    }

    public void addEvent(JacsServiceEvent se) {
        if (this.events == null) {
            this.events = new ArrayList<>();
        }
        this.events.add(se);
    }

    public List<JacsServiceData> getDependencies() {
        return dependencies;
    }

    public void addServiceDependency(JacsServiceData dependency) {
        dependencies.add(dependency);
        addServiceDependencyId(dependency);
        dependency.updateParentService(this);
    }

    public List<Number> getDependeciesIds() {
        return dependeciesIds;
    }

    public void addServiceDependencyId(JacsServiceData dependency) {
        if (dependency.getId() != null && !dependeciesIds.contains(dependency.getId())) {
            dependeciesIds.add(dependency.getId());
        }
    }

    public JacsServiceData getParentService() {
        return parentService;
    }

    public void updateParentService(JacsServiceData parentService) {
        if (parentService != null) {
            if (this.parentService == null) {
                this.parentService = parentService;
                parentService.addServiceDependencyId(this);
            }
            if (this.getParentServiceId() == null) {
                setParentServiceId(parentService.getId());
            }
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
        return serviceHierarchyStream(new LinkedHashSet<>());
    }

    private Stream<JacsServiceData> serviceHierarchyStream(Set<JacsServiceData> collectedDependencies) {
        return Stream.concat(
                Stream.of(this),
                dependencies.stream()
                        .filter(sd -> !collectedDependencies.contains(sd))
                        .flatMap(sd -> {
                            collectedDependencies.add(sd);
                            return sd.serviceHierarchyStream(collectedDependencies);
                        })
        );
    }

    public int priority() {
        return priority != null ? priority.intValue() : 0;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toStringExclude(this, ImmutableList.of("dependencies"));
    }

    public boolean hasCompletedUnsuccessfully() {
        return state == JacsServiceState.CANCELED || state == JacsServiceState.ERROR;
    }

    public boolean hasCompletedSuccessfully() {
        return state == JacsServiceState.SUCCESSFUL;
    }

}
