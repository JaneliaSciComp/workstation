package org.janelia.workstation.infopanel;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.api.ClientDomainUtils;

import javax.swing.*;
import java.awt.*;


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
    
        titleLabel = new JLabel("WORKSPACE", JLabel.LEADING);
        Font font = titleLabel.getFont();
        titleLabel.setFont(new Font(font.getName(), Font.BOLD, font.getSize() + 2));


        // workspace information; show name, whatever attributes
        add(titleLabel);

        workspaceNameLabel = new JLabel("", JLabel.LEADING);
        add(workspaceNameLabel);

        sampleNameLabel = new JLabel("", JLabel.LEADING);
        add(sampleNameLabel);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(250,60);
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
        if (TmModelManager.getInstance().getCurrentSample() == null) {
            setSampleName("(no sample");
            setWorkspaceName("(no workspace)", false);
            return;
        } else {
            setSampleName(TmModelManager.getInstance().getCurrentSample().getName());
        }
        if (workspace == null) {
            setWorkspaceName("(no workspace)", false);
        }
        else {
            setWorkspaceName(workspace.getName(), !ClientDomainUtils.hasWriteAccess(workspace));
        }
    }

    private void setTitle(String title) {
        titleLabel.setText(title);
    }
    
    private void setSampleName(String name) {
        // if name is too wide, it messes up our panel width; tooltip has full name
        // 2024: increased width; not sure what the limit is, but we have more width
        //  available than we used to
        sampleNameLabel.setToolTipText(name);
        if (name.length() > 40) {
            name = name.substring(0, 38) + "...";
        }
        sampleNameLabel.setText("Sample: " + name);
    }

    private void setWorkspaceName(String name, boolean readOnly) {
        // see comment above on width
        String displayName = (readOnly ? " (read-only) " : "") + name;
        workspaceNameLabel.setToolTipText(displayName);
        if (displayName.length() > 40) {
            displayName = displayName.substring(0, 38) + "...";
        }
        workspaceNameLabel.setText("Name: " + displayName);
    }

}

