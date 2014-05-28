package org.janelia.it.workstation.gui.dataview;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The menu bar for the data viewer.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataviewMenuBar extends JMenuBar {

    protected DataviewMenuBar(final DataViewer dataview) {

        BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        add(fileMenu);

        JMenuItem menuFileExit = new JMenuItem("Exit", 'x');
        menuFileExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        fileMenu.add(menuFileExit);

        JMenu searchMenu = new JMenu("Search");
        searchMenu.setMnemonic('S');
        add(searchMenu);

        JMenuItem menuFileSearchById = new JMenuItem("Search By Entity Id...");
        menuFileSearchById.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final String entityId = (String) JOptionPane.showInputDialog(dataview, "Entity id: ", "Search by entity id", JOptionPane.PLAIN_MESSAGE, null, null, null);
                dataview.getEntityPane().performSearchById(new Long(entityId));
            }
        });
        searchMenu.add(menuFileSearchById);

        JMenuItem menuFileSearchByName = new JMenuItem("Search By Entity Name...");
        menuFileSearchByName.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final String entityName = (String) JOptionPane.showInputDialog(dataview, "Entity name: ", "Search by entity name", JOptionPane.PLAIN_MESSAGE, null, null, null);
                dataview.getEntityPane().performSearchByName(entityName);
            }
        });
        searchMenu.add(menuFileSearchByName);

    }

}
