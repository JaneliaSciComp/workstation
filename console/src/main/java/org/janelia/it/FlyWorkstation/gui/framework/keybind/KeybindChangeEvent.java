package org.janelia.it.FlyWorkstation.gui.framework.keybind;

import java.io.NotSerializableException;

import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;

public class KeybindChangeEvent extends java.util.EventObject {

	private KeyboardShortcut shortcut;
	private Action action;

	public KeybindChangeEvent(KeyBindings keyBindings, KeyboardShortcut shortcut, Action action) {
		super(keyBindings);
		this.shortcut = shortcut;
		this.action = action;
	}

	public KeyBindings getKeyBindings() {
		return (KeyBindings) getSource();
	}

	public KeyboardShortcut getShortcut() {
		return shortcut;
	}

	public Action getAction() {
		return action;
	}

	/**
	 * Throws NotSerializableException, since KeybindChangeEvent objects are not
	 * intended to be serializable.
	 */
	private void writeObject(java.io.ObjectOutputStream out)
			throws NotSerializableException {
		throw new NotSerializableException("Not serializable.");
	}

	/**
	 * Throws NotSerializableException, since KeybindChangeEvent objects are not
	 * intended to be serializable.
	 */
	private void readObject(java.io.ObjectInputStream in)
			throws NotSerializableException {
		throw new NotSerializableException("Not serializable.");
	}

	// Defined so that this class isn't flagged as a potential problem when
	// searches for missing serialVersionUID fields are done.
	private static final long serialVersionUID = 793724513368024975L;
}