package org.janelia.horta;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.*;

import org.janelia.horta.actions.LoadHortaTileAtFocusAction;
import org.janelia.horta.actors.TetVolumeActor;
import org.janelia.horta.util.FILE_FORMAT;
import org.janelia.horta.volume.VolumeMipMaterial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create popup menus related to Ktx volume tile blocks.
 * @author brunsc
 */
class KtxBlockMenuBuilder {
    private FILE_FORMAT fileFormat = FILE_FORMAT.KTX;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    FILE_FORMAT getFileFormat() {
        return fileFormat;
    }

    void setFileFormat(FILE_FORMAT preference) {
        fileFormat = preference;
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
                    nttc.setFileFormat(FILE_FORMAT.KTX);
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

        JMenu fileFormatMenu = new JMenu("Tile Imagery Format");
        tilesMenu.add(fileFormatMenu);
        fileFormatMenu.add(new JRadioButtonMenuItem(
                new AbstractAction("N5 Format") {
                    {
                        putValue(Action.SELECTED_KEY,
                                fileFormat == fileFormat.N5);
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fileFormat = FILE_FORMAT.N5;
                        NeuronTracerTopComponent nttc = NeuronTracerTopComponent.getInstance();
                        nttc.reloadSampleLocation();
                    }
                }));
        fileFormatMenu.add(new JRadioButtonMenuItem(
                new AbstractAction("KTX Format") {
                    {
                        putValue(Action.SELECTED_KEY,
                                fileFormat == FILE_FORMAT.KTX);
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fileFormat = FILE_FORMAT.KTX;
                        NeuronTracerTopComponent nttc = NeuronTracerTopComponent.getInstance();
                        nttc.reloadSampleLocation();
                    }
                }));

        fileFormatMenu.add(new JRadioButtonMenuItem(
                new AbstractAction("RAW (TIFF/MJ2) Format") {
                    {
                        putValue(Action.SELECTED_KEY,
                                fileFormat == FILE_FORMAT.RAW);
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fileFormat = FILE_FORMAT.RAW;
                        NeuronTracerTopComponent nttc = NeuronTracerTopComponent.getInstance();
                        nttc.reloadSampleLocation();
                    }
                }));

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
