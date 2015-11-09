package org.janelia.it.workstation.gui.large_volume_viewer.components;

/**
 * Created by murphys on 11/6/2015.
 */

import org.janelia.it.workstation.gui.large_volume_viewer.VolumeCache;
import org.janelia.it.workstation.gui.large_volume_viewer.cache.TileStackCacheController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Created by murphys on 11/4/2015.
 */
public class TileStackCacheStatusPanel extends JPanel implements ActionListener {

    private static final int PANEL_HEIGHT=210;
    private static final int PANEL_WIDTH=90;
    private static final int Z_OFFSET=60;
    private static final int BOX_XSIZE=7;
    private static final int BOX_YSIZE=7;
    private static final int BOX_XBORDER=2;
    private static final int BOX_YBORDER=2;

    private final Logger log = LoggerFactory.getLogger(TileStackCacheStatusPanel.class);

    private int completeCount = 0;
    private long combinedLoadTime = 0;

    private Timer timer=new Timer(500 /*ms*/, this);
    Map<File, int[]> cacheStatusMap;
    Color[] statusColors=new Color[] { Color.RED, Color.YELLOW, Color.GREEN };
    int zLevelCenter=3;

    public TileStackCacheStatusPanel() {
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (VolumeCache.useVolumeCache()) {
            if (!isVisible()) {
                setVisible(true);
            }
            cacheStatusMap = TileStackCacheController.getInstance().getCacheStatusMap();
            repaint();
        } else {
            if (isVisible()) {
                setVisible(false);
            }
        }
    }

    private int[] getStatusBoxCoordinates(int[] statusArr) {
        int x=statusArr[0];
        int y=statusArr[1];
        int z=statusArr[2];

        z*=-1; // reverses up and down for display

        int y1=zLevelCenter-y;
        int x1=zLevelCenter+x;
        y = (PANEL_HEIGHT - (Z_OFFSET*3))/2 + (z+1)*Z_OFFSET + y1*(BOX_YSIZE+BOX_YBORDER);
        x = (PANEL_WIDTH - (BOX_YSIZE+BOX_XBORDER)*7)/2 + x1*(BOX_XSIZE+BOX_XBORDER);

        return new int[] { x, y };
    }

    @Override
    public void paint(Graphics graphics) {
        if (cacheStatusMap==null)
            return;
        BufferedImage image=new BufferedImage(PANEL_WIDTH, PANEL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setBackground(Color.BLACK);

        Collection<int[]> statusList = cacheStatusMap.values();

        for (int[] statusArr : statusList) {
            int[] xy=getStatusBoxCoordinates(statusArr);
            g.setColor(statusColors[statusArr[3]]);
            g.fillRect(xy[0], xy[1], BOX_XSIZE, BOX_YSIZE);
        }

        graphics.drawImage(image, 0, 0, this);
    }

}

