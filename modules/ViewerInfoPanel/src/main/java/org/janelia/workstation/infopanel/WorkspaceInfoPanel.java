package org.janelia.workstation.infopanel;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;


/**
 * this panel shows info on the selected workspace
 *
 * djo, 6/13
 */
public class WorkspaceInfoPanel extends JPanel {

    private JLabel titleLabel;
    private JLabel workspaceNameLabel;
    private JLabel sampleNameLabel;

    public WorkspaceInfoPanel() {
        setupUI();
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    
        titleLabel = new JLabel("Workspace", JLabel.LEADING);
        
        // workspace information; show name, whatever attributes
        add(titleLabel);
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
            setSampleName("");
        } 
        else {
            setWorkspaceName(workspace.getName());

            SimpleWorker labelFiller = new SimpleWorker() {
                String sampleName;

                @Override
                protected void doStuff() throws Exception {
                    sampleName = TiledMicroscopeDomainMgr.getDomainMgr().getSample(workspace).getName();
                }

                @Override
                protected void hadSuccess() {
                    if (ClientDomainUtils.hasWriteAccess(workspace)) {
                        setTitle("Workspace");
                    }
                    else {
                        setTitle("Workspace (read-only)");
                    }
                    
                    setSampleName(sampleName);
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };
            labelFiller.execute();
        }
    }

    private void setTitle(String title) {
        titleLabel.setText(title);
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

