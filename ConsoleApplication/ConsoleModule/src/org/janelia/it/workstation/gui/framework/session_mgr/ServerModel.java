/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.session_mgr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * These are server-side settings.  Their occurrence is once.  They
 * are singleton, applying to all users/all clients of Workstation.
 * This is intended to be a fail-easy class.  If it fails to return what
 * the caller needs, the caller should supply the default value.
 *
 * @author Leslie L Foster
 */
public class ServerModel {
	public static final String OWNER_OF_SERVER_MODEL = "group:workstation_users";
	public static final String ACTIVITY_LOG_GRANULARITY_PROP = "Activity Log Granularity";

	private static final int UPDATE_INTERVAL_MS = 1000 * 60 * 10; // Ten Min.
	// TODO: migrate this down into EntityConstants' Singleton Entity Names section, on master branch.
	static final String SETTINGS_ENTITY_NAME = "Universal Workstation Settings";
	// In order for this server model to reflect the latest settings on the
	// server side, it must periodically update itself.
	private Map<String,String> serverSettings = null;
	private String settingsStr = null;
	
	private static Logger logger = LoggerFactory.getLogger(ServerModel.class);
	
	// Singleton support.
    private static ServerModel serverModel = new ServerModel();	
	public static ServerModel getServerModel() {
		return serverModel;
	}
	
	private ServerModel() {
		this(true);
	}
	
	private ServerModel(boolean continuallyUpdate) {
		if (continuallyUpdate) {
			serverSettings = updateSettings();
			// Perpetually update at regular intervals.
			new Thread(new SettingsUpdater()).start();
		}
	}

	/**
	 * Must supply a default value, to ensure caller is not at the mercy of
	 * this method.
	 * 
	 * @param name what to lookup.
	 * @param defaultVal alternative to finding it here.  May be null.
	 * @return what was found.  May be the default.
	 */
	public String getSetting(String name, String defaultVal) {
		try {
			String value = serverSettings.get(name);
			if (value == null) {
				value = defaultVal;
			}
			return value;
		} catch (Exception ex) {
			logger.error("Failed to get setting " + name);
			return defaultVal;
		}
	}
	
	/**
	 * This is a convenience int-returning version of
	 * @see #getSetting(java.lang.String, java.lang.String) 
	 * 
	 * @param name look this up.
	 * @param defaultVal pass this back if problems occur
	 * @return the found value.
	 */
	public int getIntSetting(String name, int defaultVal) {
		String value = getSetting(name, null);
		if (value == null) {
			return defaultVal;
		}
		else {
			try {
				return Integer.parseInt(value);
			} catch (Exception ex) {
				logger.error("Failed to parse " + name + "'s value /" + value + "/ as integer");
				return defaultVal;
			}
		}
	}

	private synchronized Map<String,String> updateSettings() {
		ModelMgr modelMgr = ModelMgr.getModelMgr();
		Map<String,String> localSettings = new HashMap<>();
		try {
			Entity settingsEntity = getSettingsEntity(modelMgr);
			if (settingsEntity == null) {
				logger.warn("No settings found.  All defaults are in effect.");
				return localSettings;
			}
			
			// Now get the settings from there.
			for (EntityData ed: settingsEntity.getEntityData()) {
				if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
					String unparsedValue = ed.getValue();
					if (unparsedValue != null  &&  !unparsedValue.equals(settingsStr)) {
						parseNV(unparsedValue, localSettings);
					}
				}
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();			
		}
		// Now get the actual data value.
		return localSettings;
	}

	private static Entity getSettingsEntity(ModelMgr modelMgr) throws Exception {
		List<Entity> settingsEntities = modelMgr.getEntitiesByName(SETTINGS_ENTITY_NAME);		
		if (settingsEntities.size() > 1) {
			logger.warn("More than one " + SETTINGS_ENTITY_NAME + " exists.  Only first will be examined.");
		}
		else if (settingsEntities.isEmpty()) {
			return null;
		}
		Entity settingsEntity = settingsEntities.get(0);
		settingsEntity = modelMgr.loadLazyEntity(settingsEntity, true);
		return settingsEntity;
	}
	
	private synchronized void refreshSettings() {
		serverSettings = updateSettings();
	}
	
	private static class SettingsUpdater implements Runnable {
		public void run() {
			while ( true ) {
				try {
					ServerModel.getServerModel().refreshSettings();
					
					Thread.currentThread().sleep(UPDATE_INTERVAL_MS);
				} catch (Exception threadEx) {
					logger.warn(threadEx.getMessage() + " while updating settings.");
				}
			}
		}
	}
	
	private void parseNV(String unparsedValue, Map<String,String> localSettings) {
		if (unparsedValue == null) {
			return;
		}
		String[] nvPairs = unparsedValue.split("\n");
		for (String nvPair: nvPairs) {
			String[] nameValue = nvPair.split("=");
			if (nameValue.length == 2) {
				localSettings.put(nameValue[0].trim(), nameValue[1].trim());
			}
		}
	}
	
	private String formNV(Map<String,String> newValues) {
		StringBuilder bldr = new StringBuilder();
		for (String name: newValues.keySet()) {			
			if (bldr.length() > 0) {
				bldr.append("\n");
			}
			bldr.append(name).append('=').append(newValues.get(name));
		}
		
		return bldr.toString();
	}
	
}
