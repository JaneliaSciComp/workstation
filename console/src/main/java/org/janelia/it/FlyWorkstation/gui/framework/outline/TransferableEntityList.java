package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * List of entities being transferred, supporting multiple output flavors.  
 */
public class TransferableEntityList implements Transferable {
	
    private static final Logger log = LoggerFactory.getLogger(TransferableEntityList.class);
    
    private static final DataFlavor rootedEntityFlavor = getDataFlavor(RootedEntity.class);
    private static final DataFlavor entityFlavor = getDataFlavor(Entity.class);
    private static final DataFlavor stringFlavor = getDataFlavor(String.class);
    
    private final Set<DataFlavor> flavors = new HashSet<DataFlavor>();

    protected EntityTree entityTree;
    protected List<DefaultMutableTreeNode> nodes;
    protected List<RootedEntity> rootedEntities;
    
    public TransferableEntityList(List<RootedEntity> rootedEntities) {
        this(null, rootedEntities);
    }
    
	public TransferableEntityList(EntityTree entityTree, List<RootedEntity> rootedEntities) {
	    this.entityTree = entityTree;
		this.rootedEntities = rootedEntities;
		initFlavors(rootedEntityFlavor, entityFlavor, stringFlavor);
	}
	
	protected void initFlavors(DataFlavor... flavors) {
	    for (DataFlavor flavor : flavors) {
	        this.flavors.add(flavor);
	    }
	}
	
	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
		if (flavor==rootedEntityFlavor) {
		    return rootedEntities;
		}
		else if (flavor==entityFlavor) {
		    List<Entity> entities = new ArrayList<Entity>();
		    for(RootedEntity rootedEntity : rootedEntities) {
		        entities.add(rootedEntity.getEntity());
		    }
            return entities;
        }
		else if (flavor==stringFlavor) {
		    StringBuilder sb = new StringBuilder();
            for(RootedEntity rootedEntity : rootedEntities) {
                if (sb.length()>0) sb.append(", ");
                sb.append(rootedEntity.getName());
            }
            return sb.toString();
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
	
    public EntityTree getEntityTree() {
        return entityTree;
    }

    public static DataFlavor getDataFlavor(Class clazz) {
        try {
            return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\""+ clazz.getName() + "\"");    
        }
        catch (ClassNotFoundException e) {
            log.error("Error getting data flavor for class "+clazz.getName());
            return null;
        }
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