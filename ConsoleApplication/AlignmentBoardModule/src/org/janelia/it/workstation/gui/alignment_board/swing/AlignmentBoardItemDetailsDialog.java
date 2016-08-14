/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.alignment_board.swing;

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

import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

import java.awt.Dialog;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.workstation.gui.browser.gui.dialogs.ModalDialog;

/**
 * A dialog for viewing details about an entity.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentBoardItemDetailsDialog extends ModalDialog {

    private AlignmentBoardItem alignmentBoardItem;
    private AlignmentBoardItemPanel alignmentBoardItemPanel;
    
    public AlignmentBoardItemDetailsDialog() {

        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        
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
    
    public void show(AlignmentBoardItem item) {
        show(item, null);
    }

    /** Unsure if defaultTab will ever be used. */
    public void show(AlignmentBoardItem item, String defaultTab) {
        
        this.alignmentBoardItem = item;
        
        // Register this dialog as an observer
        Events.getInstance().registerOnEventBus(this);

        alignmentBoardItemPanel = new AlignmentBoardItemPanel(item);
        setTitle("Details: "+alignmentBoardItem);
        Component mainFrame = SessionMgr.getMainFrame();        
        setPreferredSize(new Dimension((int)(mainFrame.getWidth()*0.4),(int)(mainFrame.getHeight()*0.6)));
        this.add(alignmentBoardItemPanel, BorderLayout.CENTER);
        // Show dialog and wait
        packAndShow();
        
        // Dialog is closing, clean up observer
        Events.getInstance().unregisterOnEventBus(this);
    }
    
}

