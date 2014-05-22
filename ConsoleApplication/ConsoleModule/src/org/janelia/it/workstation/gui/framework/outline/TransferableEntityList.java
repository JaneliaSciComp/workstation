package org.janelia.it.workstation.gui.framework.outline;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * List of entities being transferred, supporting multiple output flavors.
 */
public class TransferableEntityList implements Transferable {

    private static final Logger log = LoggerFactory.getLogger(TransferableEntityList.class);

    private static final DataFlavor sourceFlavor = getDataFlavor(EntityTree.class);
    private static final DataFlavor rootedEntityFlavor = getDataFlavor(org.janelia.it.workstation.model.entity.RootedEntity.class);
    private static final DataFlavor entityFlavor = getDataFlavor(Entity.class);
    private static final DataFlavor stringFlavor = getDataFlavor(String.class);

    private final Set<DataFlavor> flavors = new HashSet<DataFlavor>();

    protected JComponent sourceComponent;
    protected List<org.janelia.it.workstation.model.entity.RootedEntity> rootedEntities;

    public TransferableEntityList(JComponent sourceComponent, List<org.janelia.it.workstation.model.entity.RootedEntity> rootedEntities) {
        this.sourceComponent = sourceComponent;
        this.rootedEntities = rootedEntities;
        initFlavors(sourceFlavor, rootedEntityFlavor, entityFlavor, stringFlavor);
    }

    protected void initFlavors(DataFlavor... flavors) {
        for (DataFlavor flavor : flavors) {
            this.flavors.add(flavor);
        }
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        if (flavor == rootedEntityFlavor) {
            return rootedEntities;
        }
        else if (flavor == entityFlavor) {
            List<Entity> entities = new ArrayList<Entity>();
            for (org.janelia.it.workstation.model.entity.RootedEntity rootedEntity : rootedEntities) {
                entities.add(rootedEntity.getEntity());
            }
            return entities;
        }
        else if (flavor == stringFlavor) {
            StringBuilder sb = new StringBuilder();
            for (org.janelia.it.workstation.model.entity.RootedEntity rootedEntity : rootedEntities) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(rootedEntity.getName());
            }
            return sb.toString();
        }
        else if (flavor == sourceFlavor) {
		    // This is a hack in order to get the true source entity tree. For some reason, the TransferSupport 
            // does not provide the correct component, or maybe I'm going something wrong. Either way, this
            // hack works pretty well to prevent transfer among various incompatible entity trees for now, 
            // but it will need to be revisted in the future.
            return sourceComponent;
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

    public JComponent getSourceComponent() {
        return sourceComponent;
    }

    public static DataFlavor getDataFlavor(Class clazz) {
        try {
            return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + clazz.getName() + "\"");
        }
        catch (ClassNotFoundException e) {
            log.error("Error getting data flavor for class " + clazz.getName());
            return null;
        }
    }

    public static DataFlavor getSourceFlavor() {
        return sourceFlavor;
    }

    public static DataFlavor getRootedEntityFlavor() {
        return rootedEntityFlavor;
    }

    public static DataFlavor getEntityFlavor() {
        return entityFlavor;
    }

    public static DataFlavor getStringFlavor() {
        return stringFlavor;
    }
}
