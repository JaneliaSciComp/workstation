package org.janelia.it.FlyWorkstation.gui.framework.search;

import org.janelia.it.FlyWorkstation.shared.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 4:57 PM
 */
public class SearchToolbar extends JPanel implements ActionListener {
    protected JTextArea textArea;
    protected String newline = "\n";
    static final private String PREVIOUS = "previous";
    static final private String SAVE = "save";
    static final private String NEXT = "next";
    static final private String SEARCH = "search";
    static final private String TEXT_ENTERED = "text";

    public SearchToolbar() {
        super(new BorderLayout());

        try {
            //Create the toolbar.
            JToolBar toolBar = new JToolBar("Still draggable");
            addButtons(toolBar);
            toolBar.setFloatable(false);
            toolBar.setRollover(true);

            //Create the text area used for output.  Request
            //enough space for 5 rows and 30 columns.
            textArea = new JTextArea(5, 30);
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);

            //Lay out the main panel.
            setPreferredSize(new Dimension(450, 130));
            add(toolBar, BorderLayout.PAGE_START);
            add(scrollPane, BorderLayout.CENTER);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void addButtons(JToolBar toolBar) throws FileNotFoundException {
        JButton button = null;

        //first button
        button = makeNavigationButton("arrow_left", PREVIOUS, "Back to previous Search", "Previous");
        toolBar.add(button);

        //second button
        button = makeNavigationButton("arrow_right", NEXT, "Forward to following Search", "Next");
        toolBar.add(button);

        //third button
        button = makeNavigationButton("table_save", SAVE, "Save the Search and results", "Save");
        toolBar.add(button);

        //separator
        toolBar.addSeparator();

        //fourth button
        button = new JButton("Search");
        button.setActionCommand(SEARCH);
        button.setToolTipText("Search the Database");
        button.addActionListener(this);
        toolBar.add(button);

        //fifth component is NOT a button!
        JTextField textField = new JTextField("<Enter Search Terms>");
        textField.setColumns(10);
        textField.addActionListener(this);
        textField.setActionCommand(TEXT_ENTERED);
        toolBar.add(textField);
    }

    protected JButton makeNavigationButton(String imageName, String actionCommand, String toolTipText, String altText) throws FileNotFoundException {
        //Create and initialize the button.
        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(this);
        button.setIcon(Utils.getClasspathImage(imageName + ".png"));
        ((ImageIcon) button.getIcon()).setDescription(altText);

        return button;
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        String description = null;

        // Handle each button.
        if (PREVIOUS.equals(cmd)) { //first button clicked
            description = "taken you to the previous Search terms.";
        }
        else if (SAVE.equals(cmd)) { // second button clicked
            description = "saved the Search result set.";
        }
        else if (NEXT.equals(cmd)) { // third button clicked
            description = "taken you to the next Search terms.";
        }
        else if (SEARCH.equals(cmd)) { // fourth button clicked
            description = "Searches the database for entites that match your Search terms.";
        }
        else if (TEXT_ENTERED.equals(cmd)) { // text field
            JTextField tf = (JTextField) e.getSource();
            String text = tf.getText();
            tf.setText("");
            description = "done something with this text: " + newline + "  \"" + text + "\"";
        }

        displayResult("If this were fully functional, it would have " + description);
    }

    protected void displayResult(String actionDescription) {
        textArea.append(actionDescription + newline);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}
