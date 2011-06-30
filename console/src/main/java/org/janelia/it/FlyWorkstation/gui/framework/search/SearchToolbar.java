package org.janelia.it.FlyWorkstation.gui.framework.search;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

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
    static final private String UP = "up";
    static final private String NEXT = "next";
    static final private String SOMETHING_ELSE = "other";
    static final private String TEXT_ENTERED = "text";

    public SearchToolbar() {
        super(new BorderLayout());

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

    protected void addButtons(JToolBar toolBar) {
        JButton button = null;

        //first button
        button = makeNavigationButton("Back24", PREVIOUS,
                                      "Back to previous something-or-other",
                                      "Previous");
        toolBar.add(button);

        //second button
        button = makeNavigationButton("Up24", UP,
                                      "Up to something-or-other",
                                      "Up");
        toolBar.add(button);

        //third button
        button = makeNavigationButton("Forward24", NEXT,
                                      "Forward to something-or-other",
                                      "Next");
        toolBar.add(button);

        //separator
        toolBar.addSeparator();

        //fourth button
        button = new JButton("Another button");
        button.setActionCommand(SOMETHING_ELSE);
        button.setToolTipText("Something else");
        button.addActionListener(this);
        toolBar.add(button);

        //fifth component is NOT a button!
        JTextField textField = new JTextField("A text field");
        textField.setColumns(10);
        textField.addActionListener(this);
        textField.setActionCommand(TEXT_ENTERED);
        toolBar.add(textField);
    }

    protected JButton makeNavigationButton(String imageName,
                                           String actionCommand,
                                           String toolTipText,
                                           String altText) {
        //Look for the image.
        String imgLocation = "images/"
                             + imageName
                             + ".gif";
        URL imageURL = SearchToolbar.class.getResource(imgLocation);

        //Create and initialize the button.
        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(this);

        if (imageURL != null) {                      //image found
            button.setIcon(new ImageIcon(imageURL, altText));
        } else {                                     //no image found
            button.setText(altText);
            System.err.println("Resource not found: "
                               + imgLocation);
        }

        return button;
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        String description = null;

        // Handle each button.
        if (PREVIOUS.equals(cmd)) { //first button clicked
            description = "taken you to the previous <something>.";
        } else if (UP.equals(cmd)) { // second button clicked
            description = "taken you up one level to <something>.";
        } else if (NEXT.equals(cmd)) { // third button clicked
            description = "taken you to the next <something>.";
        } else if (SOMETHING_ELSE.equals(cmd)) { // fourth button clicked
            description = "done something else.";
        } else if (TEXT_ENTERED.equals(cmd)) { // text field
            JTextField tf = (JTextField)e.getSource();
            String text = tf.getText();
            tf.setText("");
            description = "done something with this text: "
                          + newline + "  \""
                          + text + "\"";
        }

        displayResult("If this were a real app, it would have "
                        + description);
    }

    protected void displayResult(String actionDescription) {
        textArea.append(actionDescription + newline);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}
