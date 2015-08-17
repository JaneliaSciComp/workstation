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

    JPanel datasetManagementPanel = new JPanel();
    JPanel actorManagermentPanel = new JPanel();


    public VoxelViewerMainPanel() {

        // Dataset Panel
        datasetManagementPanel.setLayout(new BoxLayout(datasetManagementPanel, BoxLayout.X_AXIS));

        JPanel datasetPanel = createScrollableTablePanel();
        JPanel renderablePanel = createScrollableTablePanel();

        datasetManagementPanel.add(datasetPanel);
        datasetManagementPanel.add(renderablePanel);

        add(datasetManagementPanel, BorderLayout.WEST);

        // Actor Panel
        actorManagermentPanel.setLayout(new BoxLayout(actorManagermentPanel, BoxLayout.X_AXIS));

        JPanel actorPanel = createScrollableTablePanel();
        actorManagermentPanel.add(actorPanel);

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
        add(actorManagermentPanel, BorderLayout.EAST);

//        logger.info("NUMBER DEBUG START");
//
//        byte b0 = 99;
//
//        int i0 = b0;
//
//        byte b1 = -99;
//
//        int i1 = b1;
//
//        int i2 = 0x000000ff & b1;
//
//        logger.info("i0="+i0+" i1="+i1+" i2="+i2);
//
//        byte b3=-1;
//        byte b4=-10;
//        int i3 = b3 & 0x000000ff;
//        int i4 = b4 & 0x000000ff;
//        int i5 = i4 << 8;
//        int i6 = i3 | i5;
//        short s = ((short) (i6 & 0x0000ffff));
//        int is = s & 0x0000ffff;
//
//        logger.info("b4="+b4+" b3="+b3+" s="+s+" is="+is);
//
//        logger.info("NUMBER DEBUG END");

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

    private JPanel createScrollableTablePanel() {
        JPanel containerPanel = new JPanel();
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(rowPanel);
        scrollPane.setPreferredSize(new Dimension(250, 800));
        for (int i=0;i<10;i++) {
            JButton l = new JButton("Hello there="+i);
            l.putClientProperty("Synthetica.opaque", Boolean.FALSE);
            l.setBorder(BorderFactory.createBevelBorder(1));
            rowPanel.add(l);
        }
        containerPanel.add(scrollPane);
        return containerPanel;
    }

}
