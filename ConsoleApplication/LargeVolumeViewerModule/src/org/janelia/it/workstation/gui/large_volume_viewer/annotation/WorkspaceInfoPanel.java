package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import javax.swing.*;

import java.awt.*;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

/**
 * this panel shows info on the selected workspace
 *
 * djo, 6/13
 */
public class WorkspaceInfoPanel extends JPanel 
{
    private JLabel workspaceNameLabel;
    private JLabel sampleNameLabel;

    public WorkspaceInfoPanel() {
        setupUI();
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    
        // workspace information; show name, whatever attributes
        add(new JLabel("Workspace", JLabel.LEADING));
        add(Box.createRigidArea(new Dimension(0, 10)));

        workspaceNameLabel = new JLabel("", JLabel.LEADING);
        add(workspaceNameLabel);

        sampleNameLabel = new JLabel("", JLabel.LEADING);
        add(sampleNameLabel);

        loadWorkspace(null);
    }



    /**
     * populate the UI with info from the input workspace
     */
    public void loadWorkspace(TmWorkspace workspace) {
        updateMetaData(workspace);
    }

    /**
     * update the labels that display data about the workspace
     */
    private void updateMetaData(final TmWorkspace workspace) {
        if (workspace == null) {
            setWorkspaceName("(no workspace)");
            // sampleNameLabel.setText("Sample:");
            setSampleName("");
        } else {
            setWorkspaceName(workspace.getName());

            SimpleWorker labelFiller = new SimpleWorker() {
                String sampleName;

                @Override
                protected void doStuff() throws Exception {
                    sampleName = ModelMgr.getModelMgr().getEntityById(workspace.getSampleID()).getName();
                }

                @Override
                protected void hadSuccess() {
                    // sampleNameLabel.setText("Sample: " + sampleName);
                    setSampleName(sampleName);
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            labelFiller.execute();
        }
    }

    private void setSampleName(String name) {
        // if name is too wide, it messes up our panel width; tooltip has full name
        sampleNameLabel.setToolTipText(name);
        if (name.length() > 22) {
            name = name.substring(0, 20) + "...";
        }
        sampleNameLabel.setText("Sample: " + name);
    }

    private void setWorkspaceName(String name) {
        // if name is too wide, it messes up our panel width; tooltip has full name
        workspaceNameLabel.setToolTipText(name);
        if (name.length() > 24) {
            name = name.substring(0, 22) + "...";
        }
        workspaceNameLabel.setText("Name: " + name);
    }

}

