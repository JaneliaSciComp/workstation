/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.session_mgr;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import static org.janelia.it.workstation.gui.framework.session_mgr.ServerModel.ACTIVITY_LOG_GRANULARITY_PROP;
import static org.janelia.it.workstation.gui.framework.session_mgr.ServerModel.OWNER_OF_SERVER_MODEL;
import static org.janelia.it.workstation.gui.framework.session_mgr.ServerModel.SETTINGS_ENTITY_NAME;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The server model is a collection of settings implemented as Entity/ED
 * hierarchy.  This is the place to make changes to it.  It belongs to the
 * entire application.
 *
 * @author Leslie L Foster
 */
public class ServerModelModifier {
	private static Logger logger = LoggerFactory.getLogger(ServerModel.class);
	
	/**
	 * This main-runner will create settings on the server.  It should be called
	 * before any attempt at reading the settings, in code above.
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			// Attempt to get the settings.
			String newSettingsStr =
					ACTIVITY_LOG_GRANULARITY_PROP + "=1";
			Hashtable environment = new Hashtable();
			environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
			environment.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
			environment.put(Context.PROVIDER_URL, "jnp://foster-ws:1199");
			InitialContext context = new InitialContext(environment);
			EntityBeanRemote eobj = (EntityBeanRemote) context.lookup("compute/EntityEJB/remote");
			Set<Entity> entities = eobj.getEntitiesByName(null, SETTINGS_ENTITY_NAME);
			if (entities == null || entities.isEmpty()) {
				// Need to create.
				Entity entity = new Entity();
				entity.setOwnerKey(OWNER_OF_SERVER_MODEL);
				entity.setCreationDate(new Date());
				entity.setEntityTypeName(EntityConstants.TYPE_PROPERTY_SET);
				entity.setName(SETTINGS_ENTITY_NAME);
				entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_PROPERTY, newSettingsStr);
				
				eobj.saveOrUpdateEntity(OWNER_OF_SERVER_MODEL, entity);
			}
			else {
				final Iterator<Entity> iterator = entities.iterator();
				Entity entity = iterator.next();
				// Need to update.
				for (EntityData ed: entity.getEntityData()) {
					if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
						// Had to find the right entity-data.
						ed.setValue(newSettingsStr);
						eobj.saveOrUpdateEntityData(OWNER_OF_SERVER_MODEL, ed);
						break;
					}
				}
				if (iterator.hasNext()) {
					logger.warn("Multiple " + SETTINGS_ENTITY_NAME + " entities found.  Using only first.");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.warn("Failed to update settings.");
		}
				
	}
}
