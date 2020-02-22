package org.janelia.workstation.browser.gui.dialogs.identifiers;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public final class IdentifiersVisualPanel1 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(IdentifiersVisualPanel1.class);
    
    // Controller
    private IdentifiersWizardPanel1 wizardPanel;
    
    // GUI
    private JTextArea textArea;

    // State
    private Class<? extends DomainObject> currSearchClass = Sample.class;
    
    @Override
    public String getName() {
        return "Search Parameters";
    }

    /**
     * Creates new form DownloadVisualPanel1
     */
    public IdentifiersVisualPanel1(IdentifiersWizardPanel1 wizardPanel) {
        this.wizardPanel = wizardPanel;
        setLayout(new BorderLayout());

        DropDownButton typeCriteriaButton = new DropDownButton();
        ButtonGroup typeGroup = new ButtonGroup();
        for (final Class<? extends DomainObject> searchClass : DomainUtils.getSearchClasses()) {
            final String type = DomainUtils.getTypeName(searchClass);
            JMenuItem menuItem = new JRadioButtonMenuItem(type, searchClass.equals(currSearchClass));
            menuItem.addActionListener(e -> {
                currSearchClass = searchClass;
                typeCriteriaButton.setText("Result Type: " + DomainUtils.getTypeName(searchClass));
            });
            typeGroup.add(menuItem);
            typeCriteriaButton.addMenuItem(menuItem);
        }
        // Set default label
        typeCriteriaButton.setText("Result Type: " + DomainUtils.getTypeName(currSearchClass));

        JLabel instructions = new JLabel("Paste in the identifiers of the items you'd like to search for (line names, slide codes, GUIDs...), one identifier per line.");

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

        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new BoxLayout(upperPanel, BoxLayout.PAGE_AXIS));
        upperPanel.add(typeCriteriaButton);
        upperPanel.add(Box.createRigidArea(new Dimension(0,10)));
        upperPanel.add(instructions);

        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.add(upperPanel, BorderLayout.NORTH);
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

    public Class<? extends DomainObject> getCurrSearchClass() {
        return currSearchClass;
    }

    private void triggerValidation() {
        wizardPanel.fireChangeEvent();
    }
}
