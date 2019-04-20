package org.janelia.workstation.integration.spi.domain;

import javax.swing.Action;

/**
 * Implement this to build an action for a given object. The action is shown in right-click context menus,
 * and invoked in certain cases when the user navigates to an object.
 *
 * @author fosterl
 * @author rokickik
 */
public interface ContextualActionBuilder extends Compatible<Object> {

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
     * Should the menu item be shown for the specified object?
     */
    @Override
    boolean isCompatible(Object obj);

    /**
     * Should the menu item be enabled for the specified object?
     *
     * Note that the action itself can also be enabled/disabled, but that has a different meaning in
     * NetBeans nodes (i.e. show or not show), so we don't use that.
     */
    default boolean isEnabled(Object obj) {
        return true;
    }

    /**
     * Builds and returns the action for the given context object.
     * @param obj context object that was selected by the user
     * @return action
     */
    Action getAction(Object obj);

    /**
     * Builds and returns the node action for the given context object. The NodeAction will
     * have the context injected by the NetBeans Lookup mechanism. By default, this method returns null, meaning
     * the action will not appear on nodes.
     * @param obj context object that was selected by the user
     * @return NodeAction
     */
    default Action getNodeAction(Object obj) {
        return null;
    }
}
