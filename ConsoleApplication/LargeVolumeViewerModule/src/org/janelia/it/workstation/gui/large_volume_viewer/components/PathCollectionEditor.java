package org.janelia.it.workstation.gui.large_volume_viewer.components;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Widget to help with editing the common paths used by all of LVV userdom.
 * @author Leslie L Foster
 */
public class PathCollectionEditor extends JPanel {
	public static final int PCE_HEIGHT = 500;
	public static final int PCE_WIDTH = 800;

	private Entity entity;
	
	private static Logger log = LoggerFactory.getLogger(PathCollectionEditor.class);
	
	public PathCollectionEditor(Entity entity, String propName) {
		super();
		this.setSize(PCE_WIDTH, PCE_HEIGHT);
		JTable table = new JTable();
	    table.setModel(new EntityDataModel(propName, entity));
				
	}
	
	
	private static class EntityDataModel extends DefaultTableModel {
		private Entity entity;
		public EntityDataModel(String propName, Entity entity) {
			this.entity = entity;
			Set<Entity> children = entity.getChildren();
			Set<String> values = new HashSet<>();
			for (Entity child: children) {
				if (child.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)  &&
					child.getName().equals("settings") ) {
					// Got the one, which might contain our setting.
					for (EntityData ed: child.getEntityData()) {
						if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
							String nameValue = ed.getValue();
							String[] nameValueArr = nameValue.split("=");
							if (nameValueArr.length == 2  &&  nameValueArr[0].trim() == propName) {
								String[] allValues = nameValueArr[1].split("\n");
								for (String aValue: allValues) {
									values.add(aValue);
								}
							}
							else {
								log.warn("Invalid name/value string in property attribute. " + nameValue);								
							}
						}
					}
				}
			}
			EntityData propertyData = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_PROPERTY);
			if (propertyData != null) {
				
			}
		}
	}
}
