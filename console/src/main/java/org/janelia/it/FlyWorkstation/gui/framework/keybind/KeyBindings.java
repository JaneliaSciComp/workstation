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

/**
 * Maintains a set of key bindings for the user. Maps KeyboardShortcuts to Actions. Enforces a one-to-one mapping,
 * i.e. a single shortcut for each action, and a single action for each shortcut.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class KeyBindings {

    private HashMap<KeyboardShortcut, Action>bindings;

    public KeyBindings() {
        this.bindings = new HashMap<KeyboardShortcut, Action>();
    }

    public void load() {

        // TODO: load from config file

    }

    public void save() {

        // TODO: save to config file

    }

    public Action getConflict(KeyboardShortcut shortcut) {
        return bindings.get(shortcut);
    }

    public KeyboardShortcut getBinding(Action action) {
        for(Map.Entry<KeyboardShortcut, Action> entry : bindings.entrySet()) {
            if (action.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void setBinding(KeyboardShortcut shortcut, Action action) {
        // First remove all existing shortcuts for the action
        ArrayList<KeyboardShortcut> binds = new ArrayList<KeyboardShortcut>();
        for(Map.Entry<KeyboardShortcut, Action> entry : bindings.entrySet()) {
            if (action.equals(entry.getValue())) {
                binds.add(entry.getKey());
            }
        }
        for(KeyboardShortcut bind : binds) {
            bindings.remove(bind);
        }
        // Now we can add the new shortcut
        bindings.put(shortcut, action);
    }

    public void executeBinding(KeyboardShortcut shortcut) {
        Action action = bindings.get(shortcut);
        if (action == null) return;
        action.doAction();
    }

}
