package org.janelia.workstation.browser.gui.listview.table;

import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.common.gui.support.ViewerToolbar;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;

import javax.swing.*;

/**
 * Tool bar for table viewer panels.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class TableViewerToolbar extends ViewerToolbar {

    protected JButton chooseColumnsButton;
    protected JButton exportButton;

    protected int firstComponentIndex;
    protected int customComponentIndex;

    public TableViewerToolbar() {
        super();

        firstComponentIndex = toolbar.getComponentCount();

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

        customComponentIndex = toolbar.getComponentCount();
    }

    public JButton getChooseColumnsButton() {
        return chooseColumnsButton;
    }

    public JButton getExportButton() {
        return exportButton;
    }

    /**
     * Add a component after the refresh button.
     * @param component
     */
    public void addComponentInFront(JComponent component) {
        toolbar.add(component, null, firstComponentIndex);
    }

    public void addCustomComponent(JComponent component) {
        toolbar.add(component, null, customComponentIndex++);
    }

    public abstract void chooseColumnsButtonPressed();
    
    public abstract void exportButtonPressed();
}
