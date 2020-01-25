package org.janelia.workstation.browser.gui.listview.table;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.common.gui.support.ViewerToolbar;


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

        chooseColumnsButton = new JButton("Columns");
        chooseColumnsButton.setIcon(Icons.getIcon("cog.png"));
        chooseColumnsButton.setFocusable(false);
        chooseColumnsButton.setToolTipText("Select columns to display.");
        chooseColumnsButton.addActionListener(e -> {
            ActivityLogHelper.logUserAction("TableViewerToolbar.chooseColumnsButtonPressed");
            chooseColumnsButtonPressed();
        });
        chooseColumnsButton.addMouseListener(new MouseForwarder(toolbar, "ChooseColumnsButton->JToolBar"));
        toolbar.add(chooseColumnsButton);

        exportButton = new JButton("Export");
        exportButton.setIcon(Icons.getIcon("table_save.png"));
        exportButton.setFocusable(false);
        exportButton.setToolTipText("Export results to a tab-delimited text file.");
        exportButton.addActionListener(e -> {
            ActivityLogHelper.logUserAction("TableViewerToolbar.exportButtonPressed");
            exportButtonPressed();
        });
        exportButton.addMouseListener(new MouseForwarder(toolbar, "ExportButton->JToolBar"));
        toolbar.add(exportButton);

        addSeparator();
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
