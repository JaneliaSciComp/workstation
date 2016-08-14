package org.janelia.it.workstation.gui.browser.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.janelia.it.workstation.gui.browser.actions.OntologyElementAction;
import org.janelia.it.workstation.gui.browser.gui.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.browser.model.keybind.OntologyKeyBind;
import org.janelia.it.workstation.gui.browser.model.keybind.OntologyKeyBindings;
import org.janelia.it.workstation.gui.framework.actions.Action;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
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

    // Singleton
    private static final KeyBindings instance = new KeyBindings();
    public static KeyBindings getKeyBindings() {
        return instance;
    }

    private final Map<KeyboardShortcut, Action> ontologyBindings;

    public KeyBindings() {
        this.ontologyBindings = new HashMap<>();
    }

    public Action getConflict(KeyboardShortcut shortcut) {
        return ontologyBindings.get(shortcut);
    }

    public KeyboardShortcut getBinding(Action action) {
        for (Map.Entry<KeyboardShortcut, Action> entry : ontologyBindings.entrySet()) {
            if (action.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void setBinding(Map<KeyboardShortcut, Action> bindings, KeyboardShortcut shortcut, Action action) {
        // First remove all existing shortcuts for the action
        ArrayList<KeyboardShortcut> binds = new ArrayList<>();
        for (Map.Entry<KeyboardShortcut, Action> entry : bindings.entrySet()) {
            if (action.equals(entry.getValue())) {
                binds.add(entry.getKey());
            }
        }
        for (KeyboardShortcut bind : binds) {
            bindings.remove(bind);
        }
        // Now we can add the new shortcut
        if (shortcut != null) {
            log.debug("Setting binding {}={}", shortcut, action);
            bindings.put(shortcut, action);
        }
    }

    public void setBinding(KeyboardShortcut shortcut, Action action) {
        setBinding(ontologyBindings, shortcut, action);

    }

    public boolean executeBinding(KeyboardShortcut shortcut) {
        Action action = ontologyBindings.get(shortcut);
        if (action == null) {
            return false;
        }
        action.doAction();
        return true;
    }

    /**
     * Load the key binding preferences for a given ontology.
     */
    public void loadOntologyKeybinds(Long rootId, Map<String, Action> entityActionMap) {

        log.info("Loading key bindings for ontology: " + rootId);

        ontologyBindings.clear();

        try {
            OntologyKeyBindings ontologyKeyBindings = StateMgr.getStateMgr().loadOntologyKeyBindings(rootId);
            if (ontologyKeyBindings!=null) {
                Set<OntologyKeyBind> keybinds = ontologyKeyBindings.getKeybinds();

                for (OntologyKeyBind bind : keybinds) {
                    KeyboardShortcut shortcut = KeyboardShortcut.fromString(bind.getKey());

                    try {
                        long entityId = bind.getOntologyTermId();

                        for (String uniqueId : entityActionMap.keySet()) {
                            if (uniqueId.endsWith("" + entityId)) {
                                Action action = entityActionMap.get(uniqueId);
                                ontologyBindings.put(shortcut, action);
                            }
                        }

                    }
                    catch (Exception e) {
                        log.error("Could not load key binding from user preference '" + bind.getKey() + "'.", e);
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("Could not load user's key binding preferences", e);
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    /**
     * Save the key binding preferences for a given ontology.
     */
    public void saveOntologyKeybinds(Long rootId) {

        if (rootId == null) {
            return;
        }

        log.info("Saving key bindings for ontology " + rootId);

        OntologyKeyBindings ontologyKeyBindings = new OntologyKeyBindings(AccessManager.getSubjectKey(), rootId);
        try {
            for (Map.Entry<KeyboardShortcut, Action> entry : ontologyBindings.entrySet()) {
                if (entry.getValue() instanceof OntologyElementAction) {
                    KeyboardShortcut shortcut = entry.getKey();
                    OntologyElementAction action = (OntologyElementAction) entry.getValue();
                    log.info("  Adding binding {}={}", shortcut, action);
                    ontologyKeyBindings.addBinding(shortcut.toString(), action.getElementId());
                }
            }

            StateMgr.getStateMgr().saveOntologyKeyBindings(ontologyKeyBindings);
        }
        catch (Exception e) {
            log.error("Could not save user's key binding preferences", e);
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
}
