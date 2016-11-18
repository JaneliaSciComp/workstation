package org.janelia.jacs2.model.service;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "task_event")
public class TaskEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "task_event_id")
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "value")
    private String value;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "event_time")
    private Date eventTime = new Date();
    @ManyToOne
    @JoinColumn(name = "task_info_id")
    private TaskInfo taskInfo;

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

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    public void setTaskInfo(TaskInfo service) {
        this.taskInfo = taskInfo;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }
}
