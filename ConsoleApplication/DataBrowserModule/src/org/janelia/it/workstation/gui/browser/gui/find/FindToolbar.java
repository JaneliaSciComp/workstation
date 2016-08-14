package org.janelia.it.workstation.gui.browser.gui.find;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Position;

import net.miginfocom.swing.MigLayout;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.util.Icons;

/**
 * A contextual find toolbar that can be added to any panel that supports being a FindContext. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FindToolbar extends JPanel {

    private final JTextField textField;
    private final FindContext findContext;

    public FindToolbar(final FindContext findContext) {

        setVisible(false);
        setLayout(new MigLayout(
                "ins 0, fillx", 
                "[grow 0, growprio 0][grow 1, growprio 0][grow 0, growprio 0][grow 0, growprio 0][grow 100, growprio 100][grow 0, growprio 0]"
                ));
        
        this.findContext = findContext;

        JLabel label = new JLabel("Find:");
        
        textField = new JTextField(); 
        textField.setColumns(10);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                nextMatch(false);
            }
            public void removeUpdate(DocumentEvent e) {
                nextMatch(false);
            }
            public void insertUpdate(DocumentEvent e) {
                nextMatch(false);
            }
          });

        JButton prevButton = new JButton();
        prevButton.setFocusable(false);
        prevButton.setIcon(Icons.getIcon("resultset_previous.png"));
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prevMatch(true);
            }
        });
        prevButton.setToolTipText("Find the previous occurrence of the search terms");

        JButton nextButton = new JButton();
        nextButton.setFocusable(false);
        nextButton.setIcon(Icons.getIcon("resultset_next.png"));
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextMatch(true);
            }
        });
        nextButton.setToolTipText("Find the next occurrence of the search terms");

        JButton hideProjectionButton = new JButton(Icons.getIcon("close.png"));
        hideProjectionButton.setBorder(null);
        hideProjectionButton.setBorderPainted(false);
        hideProjectionButton.setMargin(new Insets(0,4,0,4));
        hideProjectionButton.setFocusable(false);
        hideProjectionButton.setToolTipText("Close mapped result view");
        hideProjectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findContext.hideFindUI();
            }
        });
        
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, (Color) UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(2, 5, 3, 5)));
        add(label);
        add(textField, "width 100:250:400");
        add(prevButton);
        add(nextButton);
        add(Box.createHorizontalGlue());
        add(hideProjectionButton);

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,0,true),"prevMatch");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,0,true),"nextMatch");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,true),"selectMatch");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0,true),"closeFind");

        getActionMap().put("prevMatch",new AbstractAction("prevMatch") {
            @Override
            public void actionPerformed(ActionEvent e) {
                prevMatch(true);
            }
        });

        getActionMap().put("nextMatch",new AbstractAction("nextMatch") {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextMatch(true);
            }
        });
        
        getActionMap().put("selectMatch",new AbstractAction("selectMatch") {
            @Override
            public void actionPerformed(ActionEvent e) {
                findContext.openMatch();
                close();
            }
        });

        getActionMap().put("closeFind",new AbstractAction("closeFind") {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    private void prevMatch(boolean skipStartingNode) {
        if (textField.getText().isEmpty()) return;
        findContext.findMatch(textField.getText(), Position.Bias.Backward, skipStartingNode, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                takeFocus();
                return null;
            }
        });
    }

    private void nextMatch(boolean skipStartingNode) {
        if (textField.getText().isEmpty()) return;
        findContext.findMatch(textField.getText(), Position.Bias.Forward, skipStartingNode, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                takeFocus();
                return null;
            }
        });
    }

    public void open() {
        setVisible(true);
        nextMatch(false);
        takeFocus();
        ActivityLogHelper.logUserAction("FindToolbar.open", findContext.getClass().getSimpleName());
    }
    
    public void close() {
        setVisible(false);
    }

    private void takeFocus() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Maintain focus
                textField.grabFocus();
            }
        });
    }
}
