package org.janelia.it.workstation.browser.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.janelia.it.workstation.browser.actions.Action;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.prefs.KeyBindChangedEvent;
import org.janelia.it.workstation.browser.gui.keybind.KeyboardShortcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains a set of key bindings for the user. Maps KeyboardShortcuts to Actions. Enforces a one-to-one mapping,
 * i.e. a single shortcut for each action, and a single action for each shortcut.
 *
 * Emits a KeyBindChangedEvent whenever a key binding is updated.
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

    private final Map<KeyboardShortcut, Action> bindings = new HashMap<>();

    public Map<KeyboardShortcut, Action> getBindings() {
        return bindings;
    }

    public KeyboardShortcut getBinding(Action action) {
        for (Map.Entry<KeyboardShortcut, Action> entry : bindings.entrySet()) {
            if (action.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Action getConflict(KeyboardShortcut shortcut) {
        return bindings.get(shortcut);
    }
    
    public void setBinding(KeyboardShortcut shortcut, Action action) {
        setBinding(shortcut, action, true);
    }
    
    public void setBinding(KeyboardShortcut shortcut, Action action, boolean notify) {
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

        // Now remove any existing binding
        Action existingAction = bindings.remove(shortcut);
        
        // Now we can add the new shortcut
        if (shortcut != null) {
            log.trace("Setting binding {}={}", shortcut, action);
            bindings.put(shortcut, action);
        }
        
        if (notify) {
            Events.getInstance().postOnEventBus(new KeyBindChangedEvent(shortcut, existingAction, action));
        }
    }

    public boolean executeBinding(KeyboardShortcut shortcut) {
        Action action = bindings.get(shortcut);
        if (action == null) {
            return false;
        }
        log.info("Executing key binding for "+shortcut);
        action.doAction();
        return true;
    }
}
