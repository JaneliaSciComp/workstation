package org.janelia.jacs2.model.service;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "task_info")
public class TaskInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "task_info_id")
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "service_type")
    private String serviceType;
    @Column(name = "service_cmd")
    private String serviceCmd;
    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private TaskState state;
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
    @CollectionTable(name = "task_args")
    private List<String> args;
    @Column(name = "workspace")
    private String workspace;
    @Column(name = "parent_task_info_id")
    private Long parentTaskId;
    @ManyToOne
    @JoinColumn(name = "parent_task_info_id", referencedColumnName = "task_info_id", insertable = false, updatable = false)
    private TaskInfo parentTask;
    @OneToMany
    @JoinColumn(name = "parent_task_info_id", insertable = false, updatable = false)
    private List<TaskInfo> subTasks;
    @Column(name = "root_task_info_id")
    private Long rootTaskId;
    @ManyToOne
    @JoinColumn(name = "root_task_info_id", referencedColumnName = "task_info_id", insertable = false, updatable = false)
    private TaskInfo rootTask;
    @OneToMany(mappedBy = "taskInfo")
    private List<TaskEvent> events;
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

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
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

    public Long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public TaskInfo getParentTask() {
        return parentTask;
    }

    public void setParentTask(TaskInfo parentTask) {
        this.parentTask = parentTask;
    }

    public List<TaskInfo> getSubTasks() {
        return subTasks;
    }

    public void setSubTasks(List<TaskInfo> subTasks) {
        this.subTasks = subTasks;
    }

    public Long getRootTaskId() {
        return rootTaskId;
    }

    public void setRootTaskId(Long rootTaskId) {
        this.rootTaskId = rootTaskId;
    }

    public TaskInfo getRootTask() {
        return rootTask;
    }

    public void setRootTask(TaskInfo rootTask) {
        this.rootTask = rootTask;
    }

    public List<TaskEvent> getEvents() {
        return events;
    }

    public void setEvents(List<TaskEvent> events) {
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

    public void addEvent(TaskEvent se) {
        if (this.events == null) {
            this.events = new ArrayList<>();
        }
        se.setTaskInfo(this);
        this.events.add(se);
    }

    public void addSubTask(TaskInfo si) {
        if (this.subTasks == null) {
            this.subTasks = new ArrayList<>();
        }
        si.updateParentTask(this);
    }

    public void updateParentTask(TaskInfo parentTask) {
        setParentTask(parentTask);
        if (parentTask != null) {
            setParentTaskId(parentTask.getId());
            if (parentTask.getRootTaskId() == null) {
                setRootTaskId(parentTask.getId());
                setRootTask(parentTask);
            } else {
                setRootTaskId(parentTask.getRootTaskId());
                setRootTask(parentTask.getRootTask());
            }
        }
    }

    public int priority() {
        return priority != null ? priority.intValue() : 0;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toStringExclude(this, Arrays.asList("parentTask", "rootTask", "subTasks", "events"));
    }
}
