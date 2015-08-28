package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.workstation.gui.framework.outline.Refreshable;
import org.janelia.it.workstation.gui.geometric_search.viewer.dataset.Dataset;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.EventManager;
import org.janelia.it.workstation.gui.geometric_search.viewer.gui.ActorPanel;
import org.janelia.it.workstation.gui.geometric_search.viewer.gui.DatasetPanel;
import org.janelia.it.workstation.gui.geometric_search.viewer.gui.RenderablePanel;
import org.janelia.it.workstation.gui.geometric_search.viewer.renderable.Renderable;
import org.janelia.it.workstation.gui.geometric_search.viewer.gui.ScrollableRowPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Created by murphys on 7/29/2015.
 */
public class VoxelViewerMainPanel extends JPanel implements Refreshable {

    private final Logger logger = LoggerFactory.getLogger(VoxelViewerMainPanel.class);

    VoxelViewerProperties properties=new VoxelViewerProperties();
    VoxelViewerGLPanel viewer;
    VoxelViewerModel model;
    VoxelViewerBasicController controller;
    VoxelViewerData data;
    TransferHandler transferHandler;

    JPanel datasetManagementPanel = new JPanel();
    DatasetPanel datasetPanel;
    RenderablePanel renderablePanel;
    JPanel actorManagermentPanel = new JPanel();
    ActorPanel actorPanel;


    public VoxelViewerMainPanel() {

        // Dataset Panel
        datasetManagementPanel.setLayout(new BoxLayout(datasetManagementPanel, BoxLayout.X_AXIS));
        add(datasetManagementPanel, BorderLayout.WEST);

        // Actor Panel
        actorManagermentPanel.setLayout(new BoxLayout(actorManagermentPanel, BoxLayout.X_AXIS));
        add(actorManagermentPanel, BorderLayout.EAST);

        controller = new VoxelViewerBasicController();
    }

    @Override
    public void refresh() {

        if ( viewer == null ) {
            createGL4Viewer();
        }

        viewer.refresh();
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    private void createGL4Viewer() {

        int width=0;
        int height=0;

        try {
            width=properties.getInteger(VoxelViewerProperties.GL_VIEWER_WIDTH_INT);
            height=properties.getInteger(VoxelViewerProperties.GL_VIEWER_HEIGHT_INT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if ( viewer != null ) {
            viewer.releaseMenuActions();
        }
        model=new VoxelViewerModel(properties);
        viewer = new VoxelViewerGLPanel(width, height, model);
        EventManager.setViewer(viewer);

        viewer.setProperties(properties);
        viewer.setVisible(true);
        viewer.setResetFirstRedraw(true);
        viewer.setTransferHandler(transferHandler);

        controller.setModel(model);
        controller.setViewer(viewer);

        model.setViewer(viewer);
        model.getGLModel().setViewer(viewer);
        model.getGLModel().setModel(model);
        model.getGLModel().setProperties(properties);

        model.getGLModel().postViewerIntegrationSetup();

        // Experiment setup goes here ==============

        //ArrayCubeExperiment arrayCubeExperiment=new ArrayCubeExperiment();
        //arrayCubeExperiment.setup(viewer);

        //===========================================

        add(viewer, BorderLayout.CENTER);
        add(actorManagermentPanel, BorderLayout.EAST);

        setupDatasetPanel();
        setupRenderablePanel();
        setupActorPanel();

        datasetManagementPanel.add(datasetPanel);
        datasetManagementPanel.add(renderablePanel);

        actorManagermentPanel.add(actorPanel);
    }

    protected void setupDatasetPanel() {
        datasetPanel = new DatasetPanel();
        EventManager.addListener(model.getDatasetModel(), datasetPanel);
        EventManager.addListener(datasetPanel, model.getDatasetModel());
    }

    public void setupRenderablePanel() {
        renderablePanel = new RenderablePanel();
        EventManager.addListener(model.getRenderableModel(), renderablePanel);
    }

    public void setupActorPanel() {
        actorPanel = new ActorPanel();
        EventManager.addListener(model.getActorModel(), actorPanel);
    }

    public void displayReady() {
        if (viewer==null) {
            createGL4Viewer();
        }
        viewer.resetView();
        viewer.refresh();
    }

    public VoxelViewerController getController(VoxelViewerData data) throws Exception {
        if (this.data!=null && data!=null) {
            throw new Exception("Data API may only be populated once");
        }
        if (data!=null) {
            this.data=data;
        }
        return controller;
    }

    @Override
    public void setTransferHandler(TransferHandler transferHandler) {
        this.transferHandler=transferHandler;
        if (viewer!=null) {
            viewer.setTransferHandler(transferHandler);
        }
    }

}
