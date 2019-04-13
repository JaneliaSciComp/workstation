package org.janelia.workstation.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.janelia.workstation.integration.FrameworkImplProvider;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.model.domain.DomainObject;

import com.google.common.eventbus.Subscribe;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;

/**
 * A dialog for viewing details about an entity.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDetailsDialog extends ModalDialog {

    private DomainInspectorPanel detailsPanel;
    private DomainObject domainObject;

    public DomainDetailsDialog() {
        this(null);
    }
    
    public DomainDetailsDialog(Dialog parent) {
        super(parent);
        
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
    	showForDomainObject(domainObject, DomainInspectorPanel.TAB_NAME_ATTRIBUTES);
    }
    
    public void showForDomainObject(DomainObject domainObject, String defaultTab) {
        
        this.domainObject = domainObject;
        
        // Register this dialog as an observer
        Events.getInstance().registerOnEventBus(this);

        detailsPanel.loadDomainObject(domainObject, defaultTab);
        setTitle("Details: "+domainObject.getName());
        Component mainFrame = FrameworkImplProvider.getMainFrame();
        setPreferredSize(new Dimension((int)(mainFrame.getWidth()*0.4),(int)(mainFrame.getHeight()*0.6)));

        ActivityLogHelper.logUserAction("DomainDetailsDialog.showForDomainObject", domainObject);

        // Show dialog and wait
        packAndShow();
        
        // Dialog is closing, clean up observer
        Events.getInstance().unregisterOnEventBus(this);
    }

    @Subscribe
    public void annotationsChanged(DomainObjectAnnotationChangeEvent event) {
        if (domainObject==null) return;
        if (event.getDomainObject().getId().equals(domainObject.getId())) {
            detailsPanel.loadAnnotations();
        }
    }
}
