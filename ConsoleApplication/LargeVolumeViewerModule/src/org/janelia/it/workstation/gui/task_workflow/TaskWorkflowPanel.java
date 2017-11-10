package org.janelia.it.workstation.gui.task_workflow;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel contains the UI for a task workflow similar to what I implemented
 * in Neu3 for Fly EM.  Users will be given a list of tasks to complete.
 */
public class TaskWorkflowPanel extends JPanel {
    private final TaskDataSourceI dataSource;

    private static final Logger log = LoggerFactory.getLogger(TaskWorkflowPanel.class);

    public TaskWorkflowPanel(TaskDataSourceI dataSource) {

        this.dataSource = dataSource;

        // I want to be sure I understand when this thing is created
        log.info("TaskWorkflowPanel constructor");


        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        JLabel label = new JLabel("     placeholder     ", JLabel.CENTER);
        add(label);

    }

    public void close() {

        // do clean up here, which I expect I will need

        log.info("TaskWorkflowPanel.close()");

    }
}
