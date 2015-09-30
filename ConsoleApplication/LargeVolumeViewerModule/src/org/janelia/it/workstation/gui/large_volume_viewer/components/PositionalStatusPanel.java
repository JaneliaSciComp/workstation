/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JPanel;
import org.janelia.it.workstation.cache.large_volume.GeometricNeighborhood;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModel;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModel.Status;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModelBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a panel to show the states of tiles that are in specific relative
 * positions.
 *
 * @author fosterl
 */
public class PositionalStatusPanel extends JPanel {

    private static final int PANEL_WIDTH = 120;
    private static final int PANEL_HEIGHT = 36;
    public static final Color UNDEFINED_COLOR = Color.lightGray;
    public static final Color COMPLETED_COLOR = Color.green;
    public static final Color IN_PROGRESS_COLOR = Color.yellow;
    public static final Color UNFILLED_COLOR = Color.red;
    public static final Color CLEAR_COLOR = Color.black;

    private Logger log = LoggerFactory.getLogger(PositionalStatusPanel.class);
    // Key is expected to be some sort of file-path.
    private GeometricNeighborhood neighborhood;

    public PositionalStatusPanel() {
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }    

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(PANEL_WIDTH * 2 / 3, PANEL_HEIGHT);
    }    

    public synchronized void set3DCacheNeighborhood(GeometricNeighborhood neighborhood) {
        log.debug("New neighborhood established.");
        this.neighborhood = neighborhood;
        repaint();
    }

    public void setLoadInProgress(File infile) {
        setLoadStatus(infile, Status.InProgress);
    }

    public void setLoadComplete(File infile) {
        setLoadStatus(infile, Status.Filled);
    }

    @Override
    public void paint(Graphics graphics) {
        log.info("Size is: " + getSize().width + ":" + getSize().height);
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setBackground(CLEAR_COLOR);
        g2d.clearRect(0, 0, getSize().width, getSize().height);

        int[] tileExtents = new int[3];
        Collection<PositionalStatusModel> posModels = new ArrayList<>();
        synchronized (this) {
            if (neighborhood == null) {
                return;
            }

            int[] tempTileExtents = neighborhood.getTileExtents();
            for (int i = 0; i < tileExtents.length; i++) {
                tileExtents[i] = tempTileExtents[i];
            }

            posModels.addAll(neighborhood.getPositionalModels().values());
        }

        if (tileExtents[0] == 0 || tileExtents[1] == 0) {
            return;
        }
            
        /* Not working.
         int zPanelWidth = getSize().width / tileExtents[2];
         Dimension markerDims = new Dimension(
         zPanelWidth / tileExtents[0],
         getSize().height / tileExtents[1]
         );
         */
        Dimension markerDims = new Dimension();
        markerDims.width = 4;
        markerDims.height = 6;

        int zPanelWidth = markerDims.width * tileExtents[2];

        int allMarkersHeight = markerDims.height * tileExtents[2];
        for (PositionalStatusModel model : posModels) {
            int[] xyz = model.getTileXyz();
            g2d.setColor(decodeColor(model));
            g2d.fillRect(xyz[2] * zPanelWidth + xyz[0] * markerDims.width, allMarkersHeight - (xyz[1] * markerDims.height),
                markerDims.width - 1, markerDims.height - 1);
        }

    }

    private void setLoadStatus(File infile, Status status) {
        PositionalStatusModelBean bean = null;
        bean = getModelBean( infile.getAbsolutePath() );
        if (bean != null) {
            bean.setStatus(status);
            log.debug("{} {}.  {},{},{}", status, infile, bean.getTileXyz()[0], bean.getTileXyz()[1], bean.getTileXyz()[2]);
        }
        repaint();
    }

    private synchronized PositionalStatusModelBean getModelBean(String infile) {
        if (neighborhood == null  ||  neighborhood.getFiles() == null) {
            return null;
        }
        PositionalStatusModel model = neighborhood.getPositionalModels().get(infile);
        if (model != null && model instanceof PositionalStatusModelBean) {
            return (PositionalStatusModelBean) model;
        } else {
            return null;
        }
    }

    private Color decodeColor(PositionalStatusModel model) {
        Color rtnVal = CLEAR_COLOR;
        switch (model.getStatus()) {
            case Unfilled:
                rtnVal = UNFILLED_COLOR;
                break;
            case InProgress:
                rtnVal = IN_PROGRESS_COLOR;
                break;
            case Filled:
                rtnVal = COMPLETED_COLOR;
                break;
            case OutOfRange:
            default:
                rtnVal = UNDEFINED_COLOR;
                break;
        }
        return rtnVal;
    }
}
