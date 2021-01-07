package org.janelia.horta;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.janelia.horta.actions.LoadHortaTileAtFocusAction;
import org.janelia.horta.actors.TetVolumeActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create popup menus related to Ktx volume tile blocks.
 * @author brunsc
 */
class KtxBlockMenuBuilder {

    private boolean preferKtx = true;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    boolean isPreferKtx() {
        return preferKtx;
    }

    void setPreferKtx(boolean doPreferKtx) {
        preferKtx = doPreferKtx;
    }
    
    void populateMenus(final HortaMenuContext context) {
        JMenu tilesMenu = new JMenu("Tiles");
        context.topMenu.add(tilesMenu);
        
         tilesMenu.add(new AbstractAction("Load KTX Tile At Cursor") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.info("Load Horta Cursor Tile Action invoked");
                NeuronTracerTopComponent nttc = NeuronTracerTopComponent.getInstance();
                if (nttc == null)
                    return;
                try {
                    nttc.setPreferKtx(true);
                    nttc.loadPersistentTileAtLocation(context.mouseXyz);
                } catch (IOException ex) {
                    logger.info("Tile load failed");
                }
            }
        });       
        
       tilesMenu.add(new AbstractAction("Load KTX Tile At Focus") {
            @Override
            public void actionPerformed(ActionEvent e) {
                NeuronTracerTopComponent nttc = NeuronTracerTopComponent.getInstance();
                if (nttc == null)
                    return;
                new LoadHortaTileAtFocusAction(nttc).actionPerformed(e);
            }
        });
        
        tilesMenu.add(new JPopupMenu.Separator());
        
        /* */
        JCheckBoxMenuItem enableVolumeCacheMenu = new JCheckBoxMenuItem(
                "Prefer rendered Ktx tiles", preferKtx);
        tilesMenu.add(enableVolumeCacheMenu);
        enableVolumeCacheMenu.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
                preferKtx = item.isSelected();
                NeuronTracerTopComponent nttc = NeuronTracerTopComponent.getInstance();
                item.setSelected(preferKtx);
                nttc.reloadSampleLocation();
            }
        });
        /* */
        
        tilesMenu.add(new JMenuItem(
                new AbstractAction("Clear all Volume Blocks")
        {
            @Override
            public void actionPerformed(ActionEvent e) {
                TetVolumeActor.getInstance().clearAllBlocks();
                NeuronTracerTopComponent nttc = NeuronTracerTopComponent.getInstance();
                TetVolumeActor.getInstance().clearAllBlocks();
                nttc.getNeuronMPRenderer().clearVolumeActors();
                nttc.clearAllTiles();
            }
        }));

    }
    
}
