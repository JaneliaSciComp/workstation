package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.dialogs.DataSetListDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.ScreenEvaluationDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class ServicesMenu extends JMenu {

    private Browser currentBrowser;

    public ServicesMenu(final Browser browser) {
        super("Services");
        currentBrowser = browser;

        JMenuItem cellCountingMenuItem = new JMenuItem("Cell Counting Service");
        cellCountingMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                currentBrowser.getRunCellCounterDialog().showDialog();
            }
        });
        add(cellCountingMenuItem);

        JMenuItem neuronSeparationMenuItem = new JMenuItem("Neuron Separation Service");
        neuronSeparationMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                currentBrowser.getRunNeuronSeparationDialog().showDialog();
            }
        });
        add(neuronSeparationMenuItem);

        final ScreenEvaluationDialog screenEvaluationDialog = browser.getScreenEvaluationDialog();
        if (screenEvaluationDialog.isAccessible()) {
        	JMenuItem menuItem = new JMenuItem("Screen Evaluation");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	screenEvaluationDialog.showDialog();
                }
            });
            add(menuItem);
        }
        
        final DataSetListDialog dataSetListDialog = browser.getDataSetListDialog();
        if (dataSetListDialog.isAccessible()) {
        	JMenuItem menuItem = new JMenuItem("Data Sets");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	dataSetListDialog.showDialog();
                }
            });
            add(menuItem);
        }
    }
}
