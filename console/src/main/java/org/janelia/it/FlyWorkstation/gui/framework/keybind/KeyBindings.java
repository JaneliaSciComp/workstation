/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/15/11
 * Time: 12:32 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.keybind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OntologyElementAction;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.ontology.OntologyRoot;

/**
 * Maintains a set of key bindings for the user. Maps KeyboardShortcuts to Actions. Enforces a one-to-one mapping,
 * i.e. a single shortcut for each action, and a single action for each shortcut.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class KeyBindings {

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
        bindings.put(shortcut, action);
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
    public void loadOntologyKeybinds(OntologyRoot root, Map<Long, Action> entityActionMap) {

        System.out.println("Loading key bindings for ontology "+root.getId());

        ontologyBindings.clear();

        try {
        	OntologyKeyBindings ontologyKeyBindings = ModelMgr.getModelMgr().getKeyBindings(root.getId());
        	Set<OntologyKeyBind> keybinds = ontologyKeyBindings.getKeybinds();
        	
        	for (OntologyKeyBind bind : keybinds) {
                KeyboardShortcut shortcut = KeyboardShortcut.fromString(bind.getKey());

                try {
                    long entityId = bind.getOntologyTermId();
                    Action action = entityActionMap.get(entityId);
                    if (action == null) {
                        System.out.println("Ontology does not have an action for element " + entityId);
                    }
                    else {
                        ontologyBindings.put(shortcut, action);
                    }
                }
                catch (Exception e) {
                    System.out.println("Could not load key binding from user preference '" + bind.getKey() + "'.");
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
            System.out.println("Could not load user's key binding preferences");
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    /**
     * Save the key binding preferences for a given ontology.
     */
    public void saveOntologyKeybinds(OntologyRoot root) {

        System.out.println("Saving key bindings for ontology "+root.getId());

        OntologyKeyBindings ontologyKeyBindings = new OntologyKeyBindings(SessionMgr.getUsername(), root.getId());
        try {
            for (Map.Entry<KeyboardShortcut, Action> entry : ontologyBindings.entrySet()) {
                if (entry.getValue() instanceof OntologyElementAction) {
                    KeyboardShortcut shortcut = entry.getKey();
                    OntologyElementAction action = (OntologyElementAction) entry.getValue();
                    ontologyKeyBindings.addBinding(shortcut.toString(), action.getOntologyElement().getId());
                }
            }

            ModelMgr.getModelMgr().saveOntologyKeyBindings(ontologyKeyBindings);
        }
        catch (Exception e) {
            System.out.println("Could not save user's key binding preferences");
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    /**
     * Remove all key binds referencing the given ontology.
     */
    public void removeOntologyKeybinds(OntologyRoot root) {

        try {
            ModelMgr.getModelMgr().removeOntologyKeyBindings(root.getId());
        }
        catch (Exception e) {
            System.out.println("Could not delete key binding preferences for defunct ontology " + root.getName());
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
}
