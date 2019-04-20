package org.janelia.workstation.integration.spi.domain;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ContextualActionUtils {

    private static final String VISIBLE = "visible";
    private static final String ENABLED = "enabled";

    public static boolean isVisible(Action action) {
        Object visible = action.getValue(VISIBLE);
        if (visible instanceof Boolean) {
            return (Boolean)visible;
        }
        // Actions are visible by default
        return true;
    }

    public static void setVisible(Action action, boolean visible) {
        action.putValue(VISIBLE, visible);
    }

    public static boolean isEnabled(Action action) {
        Object enabled = action.getValue(ENABLED);
        if (enabled instanceof Boolean) {
            return (Boolean)enabled;
        }
        // Actions are enabled by default
        return true;
    }

    public static void setEnabled(Action action, boolean enabled) {
        action.putValue(ENABLED, enabled);
    }


    public static String getName(Action action) {
        Object name = action.getValue(Action.NAME);
        if (name instanceof String) {
            return (String)name;
        }
        return null;
    }

    public static void setName(Action action, String name) {
        action.putValue(Action.NAME, name);
    }


    public static Action getNamedAction(String name, Consumer<ActionEvent> consumer) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                consumer.accept(e);
            }
        };
    }

    public static JMenuItem getNamedActionItem(String name, Consumer<ActionEvent> consumer) {
        return new JMenuItem(getNamedAction(name, consumer));
    }

    public static JMenuItem getNamedActionItem(Action action) {
        return new JMenuItem(action);
    }

}
