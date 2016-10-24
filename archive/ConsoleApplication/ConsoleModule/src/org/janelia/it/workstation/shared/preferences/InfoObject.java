package org.janelia.it.workstation.shared.preferences;

import java.util.Properties;


public abstract class InfoObject {
    protected String name;
    protected boolean isDirty = false;
    protected String keyBase = "";
    protected String sourceFile = "Unknown";

    /**
     * The client's InfoObjects are cloned from the default and user
     * InfoObjects that are loaded from the files.
     */
    public abstract Object clone();

    public abstract String getKeyName();

    /**
     * This method is so the object will provide the formatted properties
     * for the writeback mechanism.
     */
    public abstract Properties getPropertyOutput();

    /**
     * Provides the unique name of the object itself.
     * This name is not in <filename>.properties format but a readable string.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        isDirty = true;
        this.name = name;
        this.keyBase = PreferenceManager.getKeyForName(name, true);
    }

    /**
     * As Info objects need to track their own modified ("dirty") state for writeback,
     * this method is a check for that state.
     */
    public boolean hasChanged() {
        return isDirty;
    }

    /**
     * This method is to figure out which file this object's information should
     * go to.
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * This method determines the file that changes will go to.
     */
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * Default toString will return the Info name.
     */
    public String toString() {
        return name;
    }
}