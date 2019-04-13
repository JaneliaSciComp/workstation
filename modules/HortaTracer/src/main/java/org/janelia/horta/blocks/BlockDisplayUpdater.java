package org.janelia.horta.blocks;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;

/**
 * BlockDisplayUpdater listens to the camera location, and signals an updated
 * set of blocks to display.
 *
 * @author brunsc
 */
public class BlockDisplayUpdater<BTK extends BlockTileKey, BTS extends BlockTileSource<BTK>> {

    private final CameraObserver cameraObserver = new CameraObserver();
    private final ObservableInterface displayChangeObservable = new ComposableObservable();
    private Vantage vantage;
    private BTS blockTileSource;
    private ConstVector3 cachedFocus;
    private final BlockChooser blockChooser;
    private List<BTK> cachedDesiredBlocks;
    private boolean doAutoUpdate = true;

    public BlockDisplayUpdater(BlockChooser<BTK, BTS> blockChooser) {
        this.blockChooser = blockChooser;
    }

    public ObservableInterface getDisplayChangeObservable() {
        return displayChangeObservable;
    }

    public List<BTK> getDesiredBlocks() {
        return cachedDesiredBlocks;
    }

    public void setVantage(Vantage vantage) {
        if (this.vantage == vantage) {
            return;
        }
        if (this.vantage != null) {
            this.vantage.deleteObserver(cameraObserver);
        }
        this.vantage = vantage;
        vantage.addObserver(cameraObserver);
    }

    public void setBlockTileSource(BTS blockTileSource) {
        if (this.blockTileSource == blockTileSource) {
            return; // no change
        }
        this.blockTileSource = blockTileSource;
    }

    public void refreshBlocks(ConstVector3 focus) {
        if (!doAutoUpdate) {
            return;
        }
        if (blockTileSource == null) {
            return;
        }
        if (focus == null) {
            return;
        }
        if (focus.equals(cachedFocus)) {
            return; // short circuit when nothing has changed...
        }
        ConstVector3 previousFocus = cachedFocus;
        cachedFocus = new Vector3(focus);
        List<BTK> desiredBlocks = blockChooser.chooseBlocks(blockTileSource, focus, previousFocus);
        if (desiredBlocks.equals(cachedDesiredBlocks)) {
            return; // no change in desired set
        }
        cachedDesiredBlocks = desiredBlocks;
        displayChangeObservable.setChanged();
        displayChangeObservable.notifyObservers();
    }

    public void setAutoUpdate(boolean updateCache) {
        if (doAutoUpdate == updateCache) {
            return; // no change
        }
        this.doAutoUpdate = updateCache;
        if (doAutoUpdate) {
            refreshBlocks(cachedFocus);
        }
    }

    private class CameraObserver implements Observer {

        @Override
        public void update(Observable o, Object arg) {
            if (!doAutoUpdate) {
                return;
            }
            if (blockTileSource == null) {
                return;
            }
            ConstVector3 focus = vantage.getFocusPosition();
            refreshBlocks(focus);
        }
    }

}
