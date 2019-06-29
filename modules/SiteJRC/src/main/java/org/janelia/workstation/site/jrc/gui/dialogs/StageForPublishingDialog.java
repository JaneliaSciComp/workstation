package org.janelia.workstation.site.jrc.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * A dialog for staging samples for publishing.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StageForPublishingDialog extends ModalDialog {

    public StageForPublishingDialog() {
        this(null);
    }

    public StageForPublishingDialog(Dialog parent) {
        super(parent);





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
    
    public void showForSamples(Collection<Sample> samples) {

        setTitle("Stage "+samples.size()+" Samples for Publishing");
        Component mainFrame = FrameworkAccess.getMainFrame();
        setPreferredSize(new Dimension((int)(mainFrame.getWidth()*0.4),(int)(mainFrame.getHeight()*0.6)));

        ActivityLogHelper.logUserAction("StageForPublishingDialog.showForSamples");

        // Show dialog and wait
        packAndShow();
        
        // Dialog is closing, clean up observer
        Events.getInstance().unregisterOnEventBus(this);
    }
}
