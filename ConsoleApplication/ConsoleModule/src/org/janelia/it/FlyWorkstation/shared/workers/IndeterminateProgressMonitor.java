package org.janelia.it.FlyWorkstation.shared.workers;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import javax.accessibility.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;

/**
 * As Java's ProgressMonitor provides no way to set the type of JProgressBar to indeterminate, we copy and pasted 
 * their code in frustration, and this is the heretical result. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IndeterminateProgressMonitor extends ProgressMonitor implements Accessible
{
    protected JDialog         dialog;
    protected JOptionPane     pane;
    protected JProgressBar    myBar;
    protected JLabel          noteLabel;
    protected Component       parentComponent;
    protected String          note;
    protected Object[]        cancelOption = null;
    protected Object          message;
    protected int             min;
    protected int             max;

    public IndeterminateProgressMonitor(Component parentComponent,
                            Object message,
                            String note) {
    	// only extend ProgressMonitor so that we can pass this to methods which expect a ProgressMonitor
    	super(parentComponent, message, note, 0, 100); 
        this.parentComponent = parentComponent;

        cancelOption = new Object[1];
        cancelOption[0] = UIManager.getString("OptionPane.cancelButtonText");
        this.min = 0;
        this.max = 100;
        this.message = message;
        this.note = note;
    }


    protected class ProgressOptionPane extends JOptionPane
    {
        ProgressOptionPane(Object messageList) {
            super(messageList,
                  JOptionPane.INFORMATION_MESSAGE,
                  JOptionPane.DEFAULT_OPTION,
                  null,
                  IndeterminateProgressMonitor.this.cancelOption,
                  null);
        }


        public int getMaxCharactersPerLineCount() {
            return 60;
        }


        /**
         * Returns the specified component's toplevel <code>Frame</code> or
         * <code>Dialog</code>.
         * 
         * @param parentComponent the <code>Component</code> to check for a 
         *		<code>Frame</code> or <code>Dialog</code>
         * @return the <code>Frame</code> or <code>Dialog</code> that
         *		contains the component, or the default
         *         	frame if the component is <code>null</code>,
         *		or does not have a valid 
         *         	<code>Frame</code> or <code>Dialog</code> parent
         * @exception HeadlessException if
         *   <code>GraphicsEnvironment.isHeadless</code> returns
         *   <code>true</code>
         * @see java.awt.GraphicsEnvironment#isHeadless
         */
        private Window getWindowForComponent(Component parentComponent) 
            throws HeadlessException {
            if (parentComponent == null)
                return getRootFrame();
            if (parentComponent instanceof Frame || parentComponent instanceof Dialog)
                return (Window)parentComponent;
            return getWindowForComponent(parentComponent.getParent());
        }
        
        // Equivalent to JOptionPane.createDialog,
        // but create a modeless dialog.
        // This is necessary because the Solaris implementation doesn't
        // support Dialog.setModal yet.
        public JDialog createDialog(Component parentComponent, String title) {
            final JDialog dialog;
	    
	    Window window = getWindowForComponent(parentComponent);
	    if (window instanceof Frame) {
		dialog = new JDialog((Frame)window, title, false);	
	    } else {
		dialog = new JDialog((Dialog)window, title, false);
	    }
//  	    if (window instanceof SwingUtilities.SharedOwnerFrame) {
//  		WindowListener ownerShutdownListener =
//  		    (WindowListener)SwingUtilities.getSharedOwnerFrameShutdownListener();
//  		dialog.addWindowListener(ownerShutdownListener);
//  	    }
            Container contentPane = dialog.getContentPane();

            contentPane.setLayout(new BorderLayout());
            contentPane.add(this, BorderLayout.CENTER);
            dialog.pack();
            dialog.setLocationRelativeTo(parentComponent);
            dialog.addWindowListener(new WindowAdapter() {
                boolean gotFocus = false;

                public void windowClosing(WindowEvent we) {
                    setValue(cancelOption[0]);
                }

                public void windowActivated(WindowEvent we) {
                    // Once window gets focus, set initial focus
                    if (!gotFocus) {
                        selectInitialValue();
                        gotFocus = true;
                    }
                }
            });

            addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent event) {
                    if(dialog.isVisible() && 
                       event.getSource() == ProgressOptionPane.this &&
                       (event.getPropertyName().equals(VALUE_PROPERTY) ||
                        event.getPropertyName().equals(INPUT_VALUE_PROPERTY))){
                        dialog.setVisible(false);
                        dialog.dispose();
                    }
                }
            });

            return dialog;
        }
        
	/////////////////
	// Accessibility support for ProgressOptionPane
	////////////////
	    
	/**
	 * Gets the AccessibleContext for the ProgressOptionPane
	 *
	 * @return the AccessibleContext for the ProgressOptionPane
	 * @since 1.5
	 */
	public AccessibleContext getAccessibleContext() {
	    return IndeterminateProgressMonitor.this.getAccessibleContext();
	}

	/*
	 * Returns the AccessibleJOptionPane
	 */
	private AccessibleContext getAccessibleJOptionPane() {
	    return super.getAccessibleContext();
	}
    }


    /** 
     * Indicate the progress of the operation being monitored.
     * If the specified value is >= the maximum, the progress
     * monitor is closed. 
     * @param nv an int specifying the current value, between the
     *        maximum and minimum specified for this component
     * @see #setMinimum
     * @see #setMaximum
     * @see #close
     */
    public void setProgress(int nv) {
        if (nv >= max) {
            close();
        }
        else {
            myBar = new JProgressBar();
            myBar.setIndeterminate(true);
            if (note != null) noteLabel = new JLabel(note);
            pane = new ProgressOptionPane(new Object[] {message,
                                                        null,
                                                        myBar});
            dialog = pane.createDialog(parentComponent, "Processing");
            dialog.show();
        }
    }


    /** 
     * Indicate that the operation is complete.  This happens automatically
     * when the value set by setProgress is >= max, but it may be called
     * earlier if the operation ends early.
     */
    public void close() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
            pane = null;
            myBar = null;
        }
    }


    /** 
     * Returns true if the user hits the Cancel button in the progress dialog.
     */
    public boolean isCanceled() {
        if (pane == null) return false;
        Object v = pane.getValue();
        return ((v != null) &&
                (cancelOption.length == 1) &&
                (v.equals(cancelOption[0])));
    }
    
    /**
     * Specifies the additional note that is displayed along with the
     * progress message. Used, for example, to show which file the
     * is currently being copied during a multiple-file copy.
     *
     * @param note  a String specifying the note to display
     * @see #getNote
     */
    public void setNote(String note) {
        this.note = note;
        if (noteLabel != null) {
            noteLabel.setText(note);
        }
    }


    /**
     * Specifies the additional note that is displayed along with the
     * progress message.
     *
     * @return a String specifying the note to display
     * @see #setNote
     */
    public String getNote() {
        return note;
    }

    /////////////////
    // Accessibility support
    ////////////////
	
    /**
     * The <code>AccessibleContext</code> for the <code>ProgressMonitor</code> 
     * @since 1.5
     */
    protected AccessibleContext accessibleContext = null;

    private AccessibleContext accessibleJOptionPane = null;

    /**
     * Gets the <code>AccessibleContext</code> for the 
     * <code>ProgressMonitor</code> 
     *
     * @return the <code>AccessibleContext</code> for the
     * <code>ProgressMonitor</code>
     * @since 1.5
     */
    public AccessibleContext getAccessibleContext() {
	if (accessibleContext == null) {
	    accessibleContext = new AccessibleProgressMonitor();
	}
	if (pane != null && accessibleJOptionPane == null) {
	    // Notify the AccessibleProgressMonitor that the
	    // ProgressOptionPane was created. It is necessary
	    // to poll for ProgressOptionPane creation because
	    // the ProgressMonitor does not have a Component 
	    // to add a listener to until the ProgressOptionPane
	    // is created.
	    if (accessibleContext instanceof AccessibleProgressMonitor) {
		((AccessibleProgressMonitor)accessibleContext).optionPaneCreated();
	    }
	}
	return accessibleContext;
    }
    
    /**
     * <code>AccessibleProgressMonitor</code> implements accessibility 
     * support for the <code>ProgressMonitor</code> class. 
     * @since 1.5 
     */
    protected class AccessibleProgressMonitor extends AccessibleContext
        implements AccessibleText, ChangeListener, PropertyChangeListener {

	/*
	 * The accessibility hierarchy for ProgressMonitor is a flattened 
	 * version of the ProgressOptionPane component hierarchy. 
	 *
	 * The ProgressOptionPane component hierarchy is:
	 *   JDialog
	 *     ProgressOptionPane
	 *       JPanel
	 *         JPanel
	 *           JLabel
	 *           JLabel
	 *           JProgressBar
	 *
	 * The AccessibleProgessMonitor accessibility hierarchy is:
	 *   AccessibleJDialog
	 *     AccessibleProgressMonitor
	 *       AccessibleJLabel
	 *       AccessibleJLabel
	 *       AccessibleJProgressBar
	 *
	 * The abstraction presented to assitive technologies by
	 * the AccessibleProgressMonitor is that a dialog contains a 
	 * progress monitor with three children: a message, a note
	 * label and a progress bar.
	 */

	private Object oldModelValue;

	/**
	 * AccessibleProgressMonitor constructor
	 */
	protected AccessibleProgressMonitor() {
	}

	/*
	 * Initializes the AccessibleContext now that the ProgressOptionPane 
	 * has been created. Because the ProgressMonitor is not a Component
	 * implementing the Accessible interface, an AccessibleContext
	 * must be synthesized from the ProgressOptionPane and its children.
	 *
	 * For other AWT and Swing classes, the inner class that implements
	 * accessibility for the class extends the inner class that implements
	 * implements accessibility for the super class. AccessibleProgressMonitor 
	 * cannot extend AccessibleJOptionPane and must therefore delegate calls 
	 * to the AccessibleJOptionPane.
	 */
	private void optionPaneCreated() {
	    accessibleJOptionPane = 
		((ProgressOptionPane)pane).getAccessibleJOptionPane();

	    // add a listener for progress bar ChangeEvents
	    if (myBar != null) {
		myBar.addChangeListener(this);
	    }

	    // add a listener for note label PropertyChangeEvents
	    if (noteLabel != null) {
		noteLabel.addPropertyChangeListener(this);
	    }
	}

        /**
         * Invoked when the target of the listener has changed its state.
         *
         * @param e  a <code>ChangeEvent</code> object. Must not be null.
	 * @throws NullPointerException if the parameter is null.
	 */  
	public void stateChanged(ChangeEvent e) {
            if (e == null) {
                return;
            }
	    if (myBar != null) {
		// the progress bar value changed
		Object newModelValue = myBar.getValue();
		firePropertyChange(ACCESSIBLE_VALUE_PROPERTY, 
				   oldModelValue, 
				   newModelValue);
		oldModelValue = newModelValue;
	    }
	}

	/**
	 * This method gets called when a bound property is changed.
	 *
	 * @param e A <code>PropertyChangeEvent</code> object describing
	 * the event source and the property that has changed. Must not be null.
	 * @throws NullPointerException if the parameter is null.
	 */ 
	public void propertyChange(PropertyChangeEvent e) {
	    if (e.getSource() == noteLabel && e.getPropertyName() == "text") {
		// the note label text changed
		firePropertyChange(ACCESSIBLE_TEXT_PROPERTY, null, 0);
	    } 
	}

	/* ===== Begin AccessileContext ===== */

	/**
	 * Gets the accessibleName property of this object.  The accessibleName
	 * property of an object is a localized String that designates the purpose
	 * of the object.  For example, the accessibleName property of a label
	 * or button might be the text of the label or button itself.  In the
	 * case of an object that doesn't display its name, the accessibleName
	 * should still be set.  For example, in the case of a text field used
	 * to enter the name of a city, the accessibleName for the en_US locale
	 * could be 'city.'
	 *
	 * @return the localized name of the object; null if this 
	 * object does not have a name
	 *
	 * @see #setAccessibleName
	 */
	public String getAccessibleName() {
	    if (accessibleName != null) { // defined in AccessibleContext
		return accessibleName;
	    } else if (accessibleJOptionPane != null) {
		// delegate to the AccessibleJOptionPane
		return accessibleJOptionPane.getAccessibleName();
	    } 
	    return null;
	}
	
	/**
	 * Gets the accessibleDescription property of this object.  The
	 * accessibleDescription property of this object is a short localized
	 * phrase describing the purpose of the object.  For example, in the 
	 * case of a 'Cancel' button, the accessibleDescription could be
	 * 'Ignore changes and close dialog box.'
	 *
	 * @return the localized description of the object; null if 
	 * this object does not have a description
	 *
	 * @see #setAccessibleDescription
	 */
	public String getAccessibleDescription() {
	    if (accessibleDescription != null) { // defined in AccessibleContext
		return accessibleDescription; 
	    } else if (accessibleJOptionPane != null) {
		// delegate to the AccessibleJOptionPane
		return accessibleJOptionPane.getAccessibleDescription();
	    } 
	    return null;
	}

	/**
	 * Gets the role of this object.  The role of the object is the generic
	 * purpose or use of the class of this object.  For example, the role
	 * of a push button is AccessibleRole.PUSH_BUTTON.  The roles in 
	 * AccessibleRole are provided so component developers can pick from
	 * a set of predefined roles.  This enables assistive technologies to
	 * provide a consistent interface to various tweaked subclasses of 
	 * components (e.g., use AccessibleRole.PUSH_BUTTON for all components
	 * that act like a push button) as well as distinguish between sublasses
	 * that behave differently (e.g., AccessibleRole.CHECK_BOX for check boxes
	 * and AccessibleRole.RADIO_BUTTON for radio buttons).
	 * <p>Note that the AccessibleRole class is also extensible, so 
	 * custom component developers can define their own AccessibleRole's
	 * if the set of predefined roles is inadequate.
	 *
	 * @return an instance of AccessibleRole describing the role of the object
	 * @see AccessibleRole
	 */
	public AccessibleRole getAccessibleRole() {
	    return AccessibleRole.PROGRESS_MONITOR;
	}
    
	/**
	 * Gets the state set of this object.  The AccessibleStateSet of an object
	 * is composed of a set of unique AccessibleStates.  A change in the 
	 * AccessibleStateSet of an object will cause a PropertyChangeEvent to 
	 * be fired for the ACCESSIBLE_STATE_PROPERTY property.
	 *
	 * @return an instance of AccessibleStateSet containing the 
	 * current state set of the object
	 * @see AccessibleStateSet
	 * @see AccessibleState
	 * @see #addPropertyChangeListener
	 */
	public AccessibleStateSet getAccessibleStateSet() {
	    if (accessibleJOptionPane != null) {
		// delegate to the AccessibleJOptionPane
		return accessibleJOptionPane.getAccessibleStateSet();
	    } 
	    return null;
	}

	/**
	 * Gets the Accessible parent of this object.
	 *
	 * @return the Accessible parent of this object; null if this
	 * object does not have an Accessible parent
	 */
	public Accessible getAccessibleParent() {
	    if (dialog != null) {
		return (Accessible)dialog;
	    }
	    return null;
	}

	/*
	 * Returns the parent AccessibleContext
	 */
	private AccessibleContext getParentAccessibleContext() {
	    if (dialog != null) {
		return dialog.getAccessibleContext();
	    }
	    return null;
	}

	/**
	 * Gets the 0-based index of this object in its accessible parent.
	 *
	 * @return the 0-based index of this object in its parent; -1 if this 
	 * object does not have an accessible parent.
	 *
	 * @see #getAccessibleParent 
	 * @see #getAccessibleChildrenCount
	 * @see #getAccessibleChild
	 */
	public int getAccessibleIndexInParent() {
	    if (accessibleJOptionPane != null) {
		// delegate to the AccessibleJOptionPane
		return accessibleJOptionPane.getAccessibleIndexInParent();
	    }
	    return -1;
	}

	/**
	 * Returns the number of accessible children of the object.
	 *
	 * @return the number of accessible children of the object.
	 */
	public int getAccessibleChildrenCount() {
	    // return the number of children in the JPanel containing
	    // the message, note label and progress bar
	    AccessibleContext ac = getPanelAccessibleContext();
	    if (ac != null) {
		return ac.getAccessibleChildrenCount();
	    }
	    return 0;
	}

	/**
	 * Returns the specified Accessible child of the object.  The Accessible
	 * children of an Accessible object are zero-based, so the first child 
	 * of an Accessible child is at index 0, the second child is at index 1,
	 * and so on.
	 *
	 * @param i zero-based index of child
	 * @return the Accessible child of the object
	 * @see #getAccessibleChildrenCount
	 */
	public Accessible getAccessibleChild(int i) {
	    // return a child in the JPanel containing the message, note label 
	    // and progress bar
	    AccessibleContext ac = getPanelAccessibleContext();
	    if (ac != null) {
		return ac.getAccessibleChild(i);
	    }
	    return null;
	}
	
	/*
	 * Returns the AccessibleContext for the JPanel containing the
	 * message, note label and progress bar
	 */
	private AccessibleContext getPanelAccessibleContext() {
	    if (myBar != null) {
		Component c = myBar.getParent();
		if (c instanceof Accessible) {
		    return ((Accessible)c).getAccessibleContext();
		}
	    }
	    return null;
	}

	/** 
	 * Gets the locale of the component. If the component does not have a 
	 * locale, then the locale of its parent is returned.  
	 *
	 * @return this component's locale.  If this component does not have 
	 * a locale, the locale of its parent is returned.
	 *
	 * @exception IllegalComponentStateException 
	 * If the Component does not have its own locale and has not yet been 
	 * added to a containment hierarchy such that the locale can be
	 * determined from the containing parent. 
	 */
	public Locale getLocale() throws IllegalComponentStateException {
	    if (accessibleJOptionPane != null) {
		// delegate to the AccessibleJOptionPane
		return accessibleJOptionPane.getLocale();
	    }
	    return null;
	}

	/* ===== end AccessibleContext ===== */

	/**
	 * Gets the AccessibleComponent associated with this object that has a 
	 * graphical representation.
	 *
	 * @return AccessibleComponent if supported by object; else return null
	 * @see AccessibleComponent
	 */
	public AccessibleComponent getAccessibleComponent() {
	    if (accessibleJOptionPane != null) {
		// delegate to the AccessibleJOptionPane
		return accessibleJOptionPane.getAccessibleComponent();
	    }
	    return null;
	}

	/**
	 * Gets the AccessibleValue associated with this object that supports a 
	 * Numerical value. 
	 * 
	 * @return AccessibleValue if supported by object; else return null 
	 * @see AccessibleValue
	 */
        public AccessibleValue getAccessibleValue() {
	    if (myBar != null) {
		// delegate to the AccessibleJProgressBar
		return myBar.getAccessibleContext().getAccessibleValue();
	    }
	    return null;
        }

	/**
	 * Gets the AccessibleText associated with this object presenting 
	 * text on the display.
	 *
	 * @return AccessibleText if supported by object; else return null
	 * @see AccessibleText
	 */
	public AccessibleText getAccessibleText() {
	    if (getNoteLabelAccessibleText() != null) {
		return this;
	    }
	    return null;
	}

	/*
	 * Returns the note label AccessibleText
	 */
	private AccessibleText getNoteLabelAccessibleText() {
	    if (noteLabel != null) {
		// AccessibleJLabel implements AccessibleText if the
		// JLabel contains HTML text
		return noteLabel.getAccessibleContext().getAccessibleText();
	    }
	    return null;
	}

	/* ===== Begin AccessibleText impl ===== */

	/**
	 * Given a point in local coordinates, return the zero-based index
	 * of the character under that Point.  If the point is invalid,
	 * this method returns -1.
	 *
	 * @param p the Point in local coordinates
	 * @return the zero-based index of the character under Point p; if 
	 * Point is invalid return -1.
	 */
	public int getIndexAtPoint(Point p) {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null && sameWindowAncestor(pane, noteLabel)) {
		// convert point from the option pane bounds
		// to the note label bounds.
		Point noteLabelPoint = SwingUtilities.convertPoint(pane,
								   p,
								   noteLabel);
		if (noteLabelPoint != null) {
		    return at.getIndexAtPoint(noteLabelPoint);
		}
	    }
	    return -1;
	}

	/**
	 * Determines the bounding box of the character at the given 
	 * index into the string.  The bounds are returned in local
	 * coordinates.  If the index is invalid an empty rectangle is returned.
	 *
	 * @param i the index into the String
	 * @return the screen coordinates of the character's bounding box,
	 * if index is invalid return an empty rectangle.
	 */
	public Rectangle getCharacterBounds(int i) {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null && sameWindowAncestor(pane, noteLabel)) {
		// return rectangle in the option pane bounds
		Rectangle noteLabelRect = at.getCharacterBounds(i);
		if (noteLabelRect != null) {
		    return SwingUtilities.convertRectangle(noteLabel,
							   noteLabelRect,
							   pane);
		}
	    }
	    return null;
	}

	/*
	 * Returns whether source and destination components have the
	 * same window ancestor
	 */
	private boolean sameWindowAncestor(Component src, Component dest) {
	    if (src == null || dest == null) {
		return false;
	    }
	    return SwingUtilities.getWindowAncestor(src) ==
		SwingUtilities.getWindowAncestor(dest);
	}

	/**
	 * Returns the number of characters (valid indicies) 
	 *
	 * @return the number of characters
	 */
	public int getCharCount() {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null) {	// JLabel contains HTML text
		return at.getCharCount();
	    } 
	    return -1;
	}

	/**
	 * Returns the zero-based offset of the caret.
	 *
	 * Note: That to the right of the caret will have the same index
	 * value as the offset (the caret is between two characters).
	 * @return the zero-based offset of the caret.
	 */
	public int getCaretPosition() {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null) {	// JLabel contains HTML text
		return at.getCaretPosition();
	    }
	    return -1;
	}

	/**
	 * Returns the String at a given index. 
	 *
	 * @param part the CHARACTER, WORD, or SENTENCE to retrieve
	 * @param index an index within the text
	 * @return the letter, word, or sentence
	 */
	public String getAtIndex(int part, int index) {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null) {	// JLabel contains HTML text
		return at.getAtIndex(part, index);
	    }
	    return null;
	}

	/**
	 * Returns the String after a given index.
	 *
	 * @param part the CHARACTER, WORD, or SENTENCE to retrieve
	 * @param index an index within the text
	 * @return the letter, word, or sentence
	 */
	public String getAfterIndex(int part, int index) {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null) {	// JLabel contains HTML text
		return at.getAfterIndex(part, index);
	    }
	    return null;
	}

	/**
	 * Returns the String before a given index.
	 *
	 * @param part the CHARACTER, WORD, or SENTENCE to retrieve
	 * @param index an index within the text
	 * @return the letter, word, or sentence
	 */
	public String getBeforeIndex(int part, int index) {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null) {	// JLabel contains HTML text
		return at.getBeforeIndex(part, index);
	    }
	    return null;
	}

	/**
	 * Returns the AttributeSet for a given character at a given index
	 *
	 * @param i the zero-based index into the text 
	 * @return the AttributeSet of the character
	 */
	public AttributeSet getCharacterAttribute(int i) {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null) {	// JLabel contains HTML text
		return at.getCharacterAttribute(i);
	    }
	    return null;
	}

	/**
	 * Returns the start offset within the selected text.
	 * If there is no selection, but there is
	 * a caret, the start and end offsets will be the same.
	 *
	 * @return the index into the text of the start of the selection
	 */
	public int getSelectionStart() {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null) {	// JLabel contains HTML text
		return at.getSelectionStart();
	    }
	    return -1;
	}

	/**
	 * Returns the end offset within the selected text.
	 * If there is no selection, but there is
	 * a caret, the start and end offsets will be the same.
	 *
	 * @return the index into teh text of the end of the selection
	 */
	public int getSelectionEnd() {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null) {	// JLabel contains HTML text
		return at.getSelectionEnd();
	    }
	    return -1;
	}

	/**
	 * Returns the portion of the text that is selected. 
	 *
	 * @return the String portion of the text that is selected
	 */
	public String getSelectedText() {
	    AccessibleText at = getNoteLabelAccessibleText();
	    if (at != null) {	// JLabel contains HTML text
		return at.getSelectedText();
	    }
	    return null;
	}
	/* ===== End AccessibleText impl ===== */
    } 
    // inner class AccessibleProgressMonitor

}
