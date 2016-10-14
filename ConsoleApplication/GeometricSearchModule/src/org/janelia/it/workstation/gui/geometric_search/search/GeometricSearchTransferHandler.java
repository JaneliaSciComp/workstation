package org.janelia.it.workstation.gui.geometric_search.search;

import javax.swing.JComponent;

import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.DomainObjectTransferHandler;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by murphys on 7/31/2015.
 */
// TODO: this needs to be ported to use domain objects
public class GeometricSearchTransferHandler extends DomainObjectTransferHandler {

    private final Logger logger = LoggerFactory.getLogger(GeometricSearchTransferHandler.class);

    private JComponent dropTarget;
    private VoxelViewerController controller;

    public GeometricSearchTransferHandler(JComponent dropTarget, VoxelViewerController controller) {
        super(null, null); // TODO: need these models from somewhere
        this.dropTarget = dropTarget;
        this.controller = controller;
    }

//    @Override
//    public JComponent getDropTargetComponent() {
//        return dropTarget;
//    }
//
//    @Override
//    public boolean canImport(TransferHandler.TransferSupport support) {
//
//        Transferable transferable = support.getTransferable();
//
//        Class importClass=getImportType(transferable);
//
//        if (importClass!=null) {
//            return true;
//        }
//
//        return false;
//
//    }
//
//    @Override
//    public boolean importData(final TransferHandler.TransferSupport support) {
//
//        logger.info("importData() called");
//
//        Transferable transferable = support.getTransferable();
//
//        Class importClass=getImportType(transferable);
//
//        Dataset dataset=null;
//        if (importClass.equals(ScreenDataset.class)) {
//            dataset=ScreenDataset.createDataset(transferable);
//        } else if (importClass.equals(MCFODataset.class)) {
//            dataset=MCFODataset.createDataset(transferable);
//        }
//
//        if (dataset!=null) {
//            controller.addDataset(dataset);
//        }
//        return true;
//    }
//
//    private Class getImportType(Transferable transferable) {
//        if (ScreenDataset.canImport(transferable)) {
//            return ScreenDataset.class;
//        } else if (MCFODataset.canImport(transferable)) {
//            return MCFODataset.class;
//        } else {
//            return null;
//        }
//    }

}
