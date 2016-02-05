package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.it.workstation.gui.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.framework.outline.EntityDetailsPanel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

import com.google.common.eventbus.Subscribe;

/**
 * A dialog for viewing details about an entity.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDetailsDialog extends ModalDialog {

    private final DomainInspectorPanel detailsPanel;
    private DomainObject domainObject;
    
    public DomainDetailsDialog() {

        setModalityType(ModalityType.APPLICATION_MODAL);

        this.detailsPanel = new DomainInspectorPanel();
        add(detailsPanel, BorderLayout.CENTER);
        
        // Buttons
        
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
    
    public void showForDomainObject(DomainObject domainObject) {
    	showForDomainObject(domainObject, EntityDetailsPanel.TAB_NAME_ATTRIBUTES);
    }
    
    public void showForDomainObject(DomainObject domainObject, String defaultTab) {
        
        this.domainObject = domainObject;
        
        // Register this dialog as an observer
        Events.getInstance().registerOnEventBus(this);

        detailsPanel.loadDomainObject(domainObject, defaultTab);
        setTitle("Details: "+domainObject.getName());
        Component mainFrame = SessionMgr.getMainFrame();
        setPreferredSize(new Dimension((int)(mainFrame.getWidth()*0.5),(int)(mainFrame.getHeight()*0.8)));
        // Show dialog and wait
        packAndShow();
        
        // Dialog is closing, clean up observer
        Events.getInstance().unregisterOnEventBus(this);
    }

    @Subscribe
    public void annotationsChanged(DomainObjectAnnotationChangeEvent event) {
        if (event.getDomainObject().getId().equals(domainObject.getId())) {
            detailsPanel.loadAnnotations();
        }
    }
}
