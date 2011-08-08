package org.janelia.it.FlyWorkstation.gui.framework.roles;

public interface PrefEditor {

    public static final String APPLICATION_SETTINGS = "Application Settings";
    public static final String[] NO_DELAYED_CHANGES = new String[0];

    /**
     * @return String indicating while panel group this panel belongs to
     */
    public String getPanelGroup();

    /**
     * return the name that this panel should be called
     */
    public String getName();

    /**
     * These three methods are to provide hooks for the Controller in case
     * something panel-specific should happen when these buttons are pressed.
     */
    public void cancelChanges();

    /**
     * @return An array of strings indicating which changes will take effect next session
     */
    public String[] applyChanges();


    /**
     * This method helps the Pref Controller to know if the panel needs to have it's
     * changes applied or not.
     */
    public boolean hasChanged();

    /**
     * @return a Description of this panel to be used as a tool tip on the tab
     */
    public String getDescription();

    /**
     * This should be used to force the panels to de-register themelves from the
     * PrefController.
     */
    public void dispose();

}