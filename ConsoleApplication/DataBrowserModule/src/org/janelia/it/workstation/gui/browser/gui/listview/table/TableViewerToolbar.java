package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.janelia.it.workstation.gui.browser.gui.listview.ViewerToolbar;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.MouseForwarder;

/**
 * Tool bar for table viewer panels.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class TableViewerToolbar extends ViewerToolbar {

    protected JButton chooseColumnsButton;
    protected JButton exportButton;

    public TableViewerToolbar() {
        super();

        chooseColumnsButton = new JButton();
        chooseColumnsButton.setIcon(Icons.getIcon("table.png"));
        chooseColumnsButton.setFocusable(false);
        chooseColumnsButton.setToolTipText("Select columns to display.");
        chooseColumnsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseColumnsButtonPressed();
            }
        });
        chooseColumnsButton.addMouseListener(new MouseForwarder(toolbar, "ChooseColumnsButton->JToolBar"));
        toolbar.add(chooseColumnsButton);

        exportButton = new JButton();
        exportButton.setIcon(Icons.getIcon("table_save.png"));
        exportButton.setFocusable(false);
        exportButton.setToolTipText("Export results to a tab-delimited text file.");
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportButtonPressed();
            }
        });
        exportButton.addMouseListener(new MouseForwarder(toolbar, "ExportButton->JToolBar"));
        toolbar.add(exportButton);

        toolbar.addSeparator();
    }

    public JButton getChooseColumnsButton() {
        return chooseColumnsButton;
    }

    public JButton getExportButton() {
        return exportButton;
    }

    public abstract void chooseColumnsButtonPressed();
    
    public abstract void exportButtonPressed();
}
