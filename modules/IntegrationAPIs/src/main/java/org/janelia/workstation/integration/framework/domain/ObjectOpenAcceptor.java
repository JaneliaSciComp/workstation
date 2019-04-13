package org.janelia.workstation.integration.framework.domain;

/**
 * Implement this to accept a object for processing.
 * 
 * @author fosterl
 */
public interface ObjectOpenAcceptor extends Compatible<Object> {
    
    public static final String LOOKUP_PATH = "Acceptors/ObjectOpenAcceptor";
    
    /**
     * The label for the menu item.
     */
    String getActionLabel();

    /**
     * Should the menu item be shown for the specified domain object?
     */
    @Override
    boolean isCompatible(Object obj);
    
    /**
     * Should the menu item be enabled for the specified domain object?
     */
    boolean isEnabled(Object obj);
    
    void acceptObject(Object obj);
    
    /**
     * Space these apart by at least 100, to leave room for injected separators
     * and for later-stage additions of menu items after the fact.
     * 
     * @return expected ascending order key for this menu item.
     */
    Integer getOrder();
    
    boolean isPrecededBySeparator();
    
    boolean isSucceededBySeparator();
    
}
