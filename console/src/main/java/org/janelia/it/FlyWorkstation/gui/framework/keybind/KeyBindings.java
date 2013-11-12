package org.janelia.it.FlyWorkstation.gui.framework.keybind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OntologyElementAction;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.utils.OntologyKeyBind;
import org.janelia.it.FlyWorkstation.model.utils.OntologyKeyBindings;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains a set of key bindings for the user. Maps KeyboardShortcuts to Actions. Enforces a one-to-one mapping,
 * i.e. a single shortcut for each action, and a single action for each shortcut.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class KeyBindings {
	
	private static final Logger log = LoggerFactory.getLogger(KeyBindings.class);
	
    private Map<KeyboardShortcut, Action> generalBindings;
    private Map<KeyboardShortcut, Action> ontologyBindings;

    public KeyBindings() {
        this.generalBindings = new HashMap<KeyboardShortcut, Action>();
        this.ontologyBindings = new HashMap<KeyboardShortcut, Action>();
    }

    public Action getConflict(KeyboardShortcut shortcut) {
        return ontologyBindings.get(shortcut);
    }

    private KeyboardShortcut getBinding(Map<KeyboardShortcut, Action> bindings, Action action) {
        for (Map.Entry<KeyboardShortcut, Action> entry : bindings.entrySet()) {
            if (action.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public KeyboardShortcut getBinding(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null for KeyBindings.getBinding(Action)");
        }
        KeyboardShortcut ontologyAction = getBinding(ontologyBindings, action);
        if (ontologyAction != null) {
            return ontologyAction;
        }
        return getBinding(generalBindings, action);
    }

    private void setBinding(Map<KeyboardShortcut, Action> bindings, KeyboardShortcut shortcut, Action action) {
        // First remove all existing shortcuts for the action
        ArrayList<KeyboardShortcut> binds = new ArrayList<KeyboardShortcut>();
        for (Map.Entry<KeyboardShortcut, Action> entry : bindings.entrySet()) {
            if (action.equals(entry.getValue())) {
                binds.add(entry.getKey());
            }
        }
        for (KeyboardShortcut bind : binds) {
            bindings.remove(bind);
        }
        // Now we can add the new shortcut
        if (shortcut!=null) {
        	bindings.put(shortcut, action);
        }
    }

    public void setBinding(KeyboardShortcut shortcut, Action action) {
        if (action instanceof OntologyElementAction) {
            setBinding(ontologyBindings, shortcut, action);
        }
        else {
            setBinding(generalBindings, shortcut, action);
        }

    }

    public boolean executeBinding(KeyboardShortcut shortcut) {
        Action action = ontologyBindings.get(shortcut);
        if (action == null) {
            action = generalBindings.get(shortcut);
        }
        if (action == null) {
            return false;
        }
        action.doAction();
        return true;
    }

    public void loadGeneralKeybinds() {
        // TODO: complete implementation of general keybinds
        throw new UnsupportedOperationException();
    }

    /**
     * Load the key binding preferences for a given ontology.
     *
     * @param root
     */
    public void loadOntologyKeybinds(Entity root, Map<String, Action> entityActionMap) {

        log.info("Loading key bindings for ontology: "+root.getId());

        ontologyBindings.clear();

        try {
        	OntologyKeyBindings ontologyKeyBindings = ModelMgr.getModelMgr().getKeyBindings(root.getId());
        	Set<OntologyKeyBind> keybinds = ontologyKeyBindings.getKeybinds();
        	
        	for (OntologyKeyBind bind : keybinds) {
                KeyboardShortcut shortcut = KeyboardShortcut.fromString(bind.getKey());

                try {
                    long entityId = bind.getOntologyTermId();
                    
                    for(String uniqueId : entityActionMap.keySet()) {
                        if (uniqueId.endsWith("e_"+entityId)) {
                            Action action = entityActionMap.get(uniqueId);
                            ontologyBindings.put(shortcut, action);
                        }
                    }
                    
                }
                catch (Exception e) {
                	log.error("Could not load key binding from user preference '" + bind.getKey() + "'.",e);
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
        	log.error("Could not load user's key binding preferences",e);
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    /**
     * Save the key binding preferences for a given ontology.
     */
    public void saveOntologyKeybinds(Entity root) {

        if (root==null) return;
        
    	log.info("Saving key bindings for ontology "+root.getId());

        OntologyKeyBindings ontologyKeyBindings = new OntologyKeyBindings(SessionMgr.getSubjectKey(), root.getId());
        try {
            for (Map.Entry<KeyboardShortcut, Action> entry : ontologyBindings.entrySet()) {
                if (entry.getValue() instanceof OntologyElementAction) {
                    KeyboardShortcut shortcut = entry.getKey();
                    OntologyElementAction action = (OntologyElementAction) entry.getValue();
                    ontologyKeyBindings.addBinding(shortcut.toString(), EntityUtils.getEntityIdFromUniqueId(action.getUniqueId()));
                }
            }

            ModelMgr.getModelMgr().saveOntologyKeyBindings(ontologyKeyBindings);
        }
        catch (Exception e) {
        	log.error("Could not save user's key binding preferences",e);
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    /**
     * Remove all key binds referencing the given ontology.
     */
    public void removeOntologyKeybinds(Entity root) {

        try {
            ModelMgr.getModelMgr().removeOntologyKeyBindings(root.getId());
        }
        catch (Exception e) {
            log.error("Could not delete key binding preferences for defunct ontology " + root.getName(),e);
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
}
