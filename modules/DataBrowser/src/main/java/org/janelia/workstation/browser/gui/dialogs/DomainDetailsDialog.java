package org.janelia.workstation.browser.gui.dialogs;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.*;
import java.awt.*;

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

        this.detailsPanel = new DomainInspectorPanel(this);
        add(detailsPanel, BorderLayout.CENTER);
        
        // Buttons
        
        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(e -> setVisible(false));

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
        Component mainFrame = FrameworkAccess.getMainFrame();
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
