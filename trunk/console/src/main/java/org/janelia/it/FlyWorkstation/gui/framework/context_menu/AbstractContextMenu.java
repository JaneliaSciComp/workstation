package org.janelia.it.FlyWorkstation.gui.framework.context_menu;

import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.jacs.shared.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * A common base class for supporting single and multiple-selection context menus.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractContextMenu<T> extends JPopupMenu {

	protected final List<T> selectedElements;
	protected final String label;
	
	protected boolean nextAddRequiresSeparator = false;

	public AbstractContextMenu(List<T> selectedElements) {
		this(selectedElements, null);
	}

	public AbstractContextMenu(List<T> selectedElements, String label) {
		this.selectedElements = selectedElements;
		this.label = label;
		addMenuItems();
	}
	
	public void addMenuItems() {
		if (isMultipleSelection()) {
			add(getTitleItem("(Multiple items selected)"));
			addMultipleSelectionItems();
		}
		else {
			if (!StringUtils.isEmpty(label)) {
				add(getTitleItem(label));
		        add(getCopyToClipboardItem());
			}
			addSingleSelectionItems();
		}
	}

	public boolean isMultipleSelection() {
		return selectedElements.size()>1;
	}
	
	public List<T> getSelectedElements() {
		return selectedElements;
	}
	
	public T getSelectedElement() {
		if (isMultipleSelection()) throw new IllegalStateException("AbstractContextMenu.getSelectedElement() called when more than 1 item was selected");
		return selectedElements.get(0);
	}
	
	/**
	 * Override this to add any items that are displayed when a single item is selected.
	 */
	protected abstract void addSingleSelectionItems();
	
	/**
	 * Override this method to add any items that are displayed when multiple items are selected. 
	 */
	protected abstract void addMultipleSelectionItems();
	
	
	protected JMenuItem getTitleItem(String title) {
        JMenuItem titleMenuItem = new JMenuItem(title);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
	}
	
	protected JMenuItem getCopyToClipboardItem() {
		if (selectedElements.size()>1) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Copy To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(label);
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        return copyMenuItem;
	}
	
	protected JMenuItem getActionItem(final Action action) {
        JMenuItem actionMenuItem = new JMenuItem("  "+action.getName());
        actionMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.doAction();
			}
		});
        return actionMenuItem;
	}

	@Override
	public JMenuItem add(JMenuItem menuItem) {
		
		if (menuItem == null) return null;
		
		if (nextAddRequiresSeparator) {
			addSeparator();
			nextAddRequiresSeparator = false;
		}
		
		return super.add(menuItem);
	}

	public void setNextAddRequiresSeparator(boolean nextAddRequiresSeparator) {
		this.nextAddRequiresSeparator = nextAddRequiresSeparator;
	}

}