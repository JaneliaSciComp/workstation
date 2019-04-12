package org.janelia.workstation.browser.gui.dialogs.identifiers;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IdentifiersVisualPanel1 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(IdentifiersVisualPanel1.class);
    
    // Controller
    private IdentifiersWizardPanel1 wizardPanel;
    
    // GUI
    private JTextArea textArea;
    
    @Override
    public String getName() {
        return "Paste Identifiers";
    }
    
    /**
     * Creates new form DownloadVisualPanel1
     */
    public IdentifiersVisualPanel1(IdentifiersWizardPanel1 wizardPanel) {
        this.wizardPanel = wizardPanel;
        setLayout(new BorderLayout());

        JLabel instructions = new JLabel("Paste in the identifiers of the items you'd like to search for (line names, slide codes, GUIDs...), one identifier per line.");
        
        JPanel outerPanel = new JPanel(new BorderLayout());

        this.textArea = new JTextArea();
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                triggerValidation();
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                triggerValidation();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                triggerValidation();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(textArea);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Increase scroll speed
                
        outerPanel.add(instructions, BorderLayout.NORTH);
        outerPanel.add(scrollPane, BorderLayout.CENTER);
        
        removeAll();
        add(outerPanel, BorderLayout.CENTER);
    }
    
    public void init(IdentifiersWizardState state) {
        
        if (state.getText() != null) {
            textArea.setText(state.getText());
        }
        triggerValidation();
    }

    public String getText() {
        return textArea.getText();
    }

    private void triggerValidation() {
        wizardPanel.fireChangeEvent();
    }
}
