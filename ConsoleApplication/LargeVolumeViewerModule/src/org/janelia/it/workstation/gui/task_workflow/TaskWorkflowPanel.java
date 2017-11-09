package org.janelia.it.workstation.gui.task_workflow;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by olbrisd on 11/8/17.
 */
public class TaskWorkflowPanel extends JPanel {
    private final TaskDataSourceI dataSource;

    private static final Logger log = LoggerFactory.getLogger(TaskWorkflowPanel.class);

    public TaskWorkflowPanel(TaskDataSourceI dataSource) {

        this.dataSource = dataSource;
        // this.setLayout(new BorderLayout());

        // I want to be sure I understand when this thing is created
        log.info("TaskWorkflowPanel construtor");


        // placeholder

        JLabel label = new JLabel("     placeholder     ", JLabel.CENTER);
        add(label);

    }

    public void close() {

        // do clean up here, which I expect I will need

        log.info("TaskWorkflowPanel.close()");

    }
}
