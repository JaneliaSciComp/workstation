package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.*;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * A dialog for viewing details about an entity.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDetailsDialog extends ModalDialog {
    
    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    
    private JPanel attrPanel;
    private JLabel roleLabel;
    private JLabel nameLabel;
    private JLabel typeLabel;
    private JLabel ownerLabel;
    private JLabel creationDateLabel;
    private JLabel updatedDateLabel;

    private JLabel addAttribute(String name) {
        JLabel nameLabel = new JLabel(name);
        JLabel valueLabel = new JLabel();
        nameLabel.setLabelFor(valueLabel);
        attrPanel.add(nameLabel);
        attrPanel.add(valueLabel);
        return valueLabel;
    }
    
    public EntityDetailsDialog() {

        setTitle("Entity Details");

        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Annotation Properties")));

        nameLabel = addAttribute("Name: ");
        typeLabel = addAttribute("Type: ");
        roleLabel = addAttribute("Role: ");
        ownerLabel = addAttribute("Annotation Owner: ");
        creationDateLabel = addAttribute("Creation Date: ");
        updatedDateLabel = addAttribute("Updated Date: ");

        add(attrPanel, BorderLayout.CENTER);
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        
        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showForEntityData(EntityData entityData) {
    	
    	roleLabel.setText(entityData.getEntityAttribute()==null?"":entityData.getEntityAttribute().getName());
    	Entity entity = entityData.getChildEntity();
    	showForEntity(entity);
    }
    
    public void showForEntity(Entity entity) {

    	nameLabel.setText(entity.getName());
    	typeLabel.setText(entity.getEntityType().getName());
        ownerLabel.setText(entity.getUser().getUserLogin());
        creationDateLabel.setText(df.format(entity.getCreationDate()));
        updatedDateLabel.setText(df.format(entity.getUpdatedDate()));
        
        packAndShow();
    }
}
