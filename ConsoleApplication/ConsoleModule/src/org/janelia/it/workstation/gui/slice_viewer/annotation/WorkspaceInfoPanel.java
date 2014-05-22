package org.janelia.it.workstation.gui.slice_viewer.annotation;

import javax.swing.*;

import java.awt.*;


import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.signal.Slot1;
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


    // ----- slots
    public Slot1<TmWorkspace> workspaceLoadedSlot = new Slot1<TmWorkspace>() {
        @Override
        public void execute(TmWorkspace workspace) {
            loadWorkspace(workspace);
        }
    };


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
            workspaceNameLabel.setText("Name: (no workspace)");
            sampleNameLabel.setText("Sample:");
        } else {
            workspaceNameLabel.setText("Name: " + workspace.getName());

            SimpleWorker labelFiller = new SimpleWorker() {
                String sampleName;

                @Override
                protected void doStuff() throws Exception {
                    sampleName = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntityById(workspace.getSampleID()).getName();
                }

                @Override
                protected void hadSuccess() {
                    sampleNameLabel.setText("Sample: " + sampleName);
                }

                @Override
                protected void hadError(Throwable error) {
                    org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(error);
                }
            };
            labelFiller.execute();
        }
    }
}

