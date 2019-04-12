package org.janelia.workstation.browser.gui.listview.icongrid;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import org.janelia.workstation.browser.flavors.DomainObjectFlavor;
import org.janelia.model.domain.DomainObject;

/**
 * List of entities being transferred, supporting multiple output flavors.
 */
public class TransferableDomainObjectList implements Transferable {

    private final Set<DataFlavor> flavors = new HashSet<>();

    protected List<DomainObject> domainObjects;

    public TransferableDomainObjectList(JComponent sourceComponent, List<DomainObject> domainObjects) {
        this.domainObjects = domainObjects;
        initFlavors(DomainObjectFlavor.SINGLE_FLAVOR, DomainObjectFlavor.LIST_FLAVOR);
    }

    protected final void initFlavors(DataFlavor... flavors) {
        for (DataFlavor flavor : flavors) {
            this.flavors.add(flavor);
        }
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        if (flavor == DomainObjectFlavor.LIST_FLAVOR) {
            return domainObjects;
        }
        else if (flavor == DomainObjectFlavor.SINGLE_FLAVOR) {
            if (domainObjects.size()==1) {
                return domainObjects.get(0);
            }
            throw new UnsupportedFlavorException(flavor);
        }
        throw new UnsupportedFlavorException(flavor);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.toArray(new DataFlavor[flavors.size()]);
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavors.contains(flavor);
    }
}
