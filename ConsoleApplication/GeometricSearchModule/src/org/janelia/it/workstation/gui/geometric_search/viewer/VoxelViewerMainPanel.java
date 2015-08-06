package org.janelia.it.workstation.gui.geometric_search.viewer;

import de.javasoft.swing.JYScrollPaneMap;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;
import org.jdesktop.swingx.JXTextArea;
import org.jdesktop.swingx.JXTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.JTableHeader;
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

    public VoxelViewerMainPanel() {

        JPanel pa1 = new JPanel();
        JTable jt1 = new JTable(10,1);
        //JScrollPane js1 = new JScrollPane(jt1);
        for (int i=0;i<10;i++) {
            JXTextField ex1 = new JXTextField();
            ex1.setText("This is the text for="+i);
            ex1.setColumns(30);
            ex1.setVisible(true);
            jt1.add(ex1);
        }
       // pa1.add(js1);
        pa1.add(jt1);
        add(pa1, BorderLayout.WEST);

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
        viewer.setProperties(properties);
        viewer.setVisible(true);
        viewer.setResetFirstRedraw(true);
        viewer.setTransferHandler(transferHandler);

        controller.setModel(model);
        controller.setViewer(viewer);

        model.setViewer(viewer);
        model.postViewerIntegrationSetup();

        // Experiment setup goes here ==============

        //ArrayCubeExperiment arrayCubeExperiment=new ArrayCubeExperiment();
        //arrayCubeExperiment.setup(viewer);

        //===========================================

       add(viewer, BorderLayout.CENTER);

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

    private JPanel createScrollablePanelContainer() {
        JPanel containerPanel = new JPanel();
        JScrollPane scrollPane = new JScrollPane();
        for (int i=0;i<10;i++) {
            JLabel l = new JLabel("Hello there="+i);
            scrollPane.add(l);
        }
        containerPanel.add(scrollPane);
        return containerPanel;
    }

}
