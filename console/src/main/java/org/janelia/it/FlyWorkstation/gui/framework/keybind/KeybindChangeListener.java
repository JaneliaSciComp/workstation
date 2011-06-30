package org.janelia.it.FlyWorkstation.gui.framework.keybind;

/**
 * Listener for key binding events.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface KeybindChangeListener extends java.util.EventListener {
    /**
     * This method gets called when a key binding is added, removed or when
     * its value is changed.
     * <p>
     * @param evt A KeybindChangeEvent object describing the event source 
     *   	and the binding that has changed.
     */
    void keybindChange(KeybindChangeEvent evt);
}