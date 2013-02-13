package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.jacs.model.entity.Entity;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/13/13
 * Time: 3:26 PM
 *
 * A DnD transfer handler for the alignment board viewer.
 */
public class ABTransferHandler extends TransferHandler {
    private Entity alignmentBoard;

    public ABTransferHandler( Entity viewer ) {
        this.alignmentBoard = viewer;
    }
    @Override
    public boolean canImport(TransferSupport supp) {
        // Check for String flavor
        for ( DataFlavor flavor: supp.getDataFlavors() ) {
            if (!supp.isDataFlavorSupported( flavor )) {
                return false;
            }
        }

        // Fetch the drop location
        DropLocation loc = supp.getDropLocation();

        // Return whether we accept the location
        return true;
    }

    @Override
    public boolean importData(TransferSupport supp) {
        if (!canImport(supp)) {
            return false;
        }

        // Fetch the Transferable and its data
        Transferable t = supp.getTransferable();
        for ( DataFlavor flavor: supp.getDataFlavors() ) {
            try {
                Object data = t.getTransferData( flavor );

                // Fetch the drop location
                DropLocation loc = supp.getDropLocation();

                // Do something.
                if ( data instanceof ArrayList) {
                    ArrayList list = (ArrayList)data;
                    if ( list.size() > 0 ) {
                        Object firstItem = list.get( 0 );
                        if ( firstItem instanceof Entity) {
                            Entity draggedEntity = (Entity)firstItem;
                            if ( alignmentBoard != null ) {
                                alignmentBoard.addChildEntity( draggedEntity );
                            }
                        }
                    }
                }
                System.out.println("Accepting import data " + loc);
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }

        return true;
    }
}
