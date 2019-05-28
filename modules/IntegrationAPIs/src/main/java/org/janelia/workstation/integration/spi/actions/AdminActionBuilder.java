package org.janelia.workstation.integration.spi.actions;

import javax.swing.Action;

/**
 * Implement this to build an action that can only be executed by administrators. The action is shown under the
 * Tools/Admin submenu, which is disabled for non-admins.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AdminActionBuilder {

    /**
     * Should there be a separator before this action when it appears in a menu?
     * @return false by default
     */
    default boolean isPrecededBySeparator() {
        return false;
    }

    /**
     * Should there be a separator after this action when it appears in a menu?
     * @return false by default
     */
    default boolean isSucceededBySeparator() {
        return false;
    }

    /**
     * Builds and returns the action.
     * @return action
     */
    Action getAction();

}
