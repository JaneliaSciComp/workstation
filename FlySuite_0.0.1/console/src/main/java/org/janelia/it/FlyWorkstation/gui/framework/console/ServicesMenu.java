package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.FlyWorkstation.gui.dialogs.DataGroupDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.DataSetListDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.ScreenEvaluationDialog;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class ServicesMenu extends JMenu {

    public ServicesMenu(final Browser browser) {
        super("Services");

        JMenuItem dataCircleMenuItem = new JMenuItem("Data Groups Manager...");
        dataCircleMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new DataGroupDialog();
                dialog.setMaximumSize(new Dimension(600,500));
                dialog.setVisible(true);
            }
        });
//        add(dataCircleMenuItem);
        
        JMenuItem neuronSeparationMenuItem = new JMenuItem("Neuron Separation Service...");
        neuronSeparationMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                SessionMgr.getSessionMgr().getActiveBrowser().getRunNeuronSeparationDialog().showDialog();
            }
        });
        add(neuronSeparationMenuItem);

        final ScreenEvaluationDialog screenEvaluationDialog = browser.getScreenEvaluationDialog();
        if (screenEvaluationDialog.isAccessible()) {
        	JMenuItem menuItem = new JMenuItem("Screen Evaluation...");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	screenEvaluationDialog.showDialog();
                }
            });
            add(menuItem);
        }

        final DataSetListDialog dataSetListDialog = browser.getDataSetListDialog();
        if (dataSetListDialog.isAccessible()) {
        	JMenuItem menuItem = new JMenuItem("Data Sets...");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	dataSetListDialog.showDialog();
                }
            });
            add(menuItem);
        }
    }
}
