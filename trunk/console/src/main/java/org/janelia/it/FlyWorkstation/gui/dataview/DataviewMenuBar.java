package org.janelia.it.FlyWorkstation.gui.dataview;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * The menu bar for the dataviewer.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataviewMenuBar extends JMenuBar {

    protected DataviewMenuBar(final DataviewFrame dataview) {

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

        JMenuItem menuFileSearchById = new JMenuItem("Search by entity id...");
        menuFileSearchById.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final String entityId = (String) JOptionPane.showInputDialog(dataview, "Entity id: ", "Search by entity id", JOptionPane.PLAIN_MESSAGE, null, null, null);

                SimpleWorker searchWorker = new SimpleWorker() {

                    private Entity entity;

                    protected void doStuff() throws Exception {
                        entity = ModelMgr.getModelMgr().getEntityById(entityId);
                    }

                    protected void hadSuccess() {
                        dataview.getEntityPane().showEntity(entity);
                    }

                    protected void hadError(Throwable error) {
                        error.printStackTrace();
                        JOptionPane.showMessageDialog(dataview, "Error finding entity", "Entity Search Error", JOptionPane.ERROR_MESSAGE);
                    }

                };

                searchWorker.execute();

            }
        });
        searchMenu.add(menuFileSearchById);

        JMenuItem menuFileSearchByName = new JMenuItem("Search by entity name...");
        menuFileSearchByName.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                final String entityName = (String) JOptionPane.showInputDialog(dataview, "Entity name: ", "Search by entity name", JOptionPane.PLAIN_MESSAGE, null, null, null);

                SimpleWorker searchWorker = new SimpleWorker() {

                    private List<Entity> entities;

                    protected void doStuff() throws Exception {
                        entities = ModelMgr.getModelMgr().getEntitiesByName(entityName);
                    }

                    protected void hadSuccess() {
                        dataview.getEntityPane().showEntities(entities);
                    }

                    protected void hadError(Throwable error) {
                        error.printStackTrace();
                        JOptionPane.showMessageDialog(dataview, "Error finding entity", "Entity Search Error", JOptionPane.ERROR_MESSAGE);
                    }

                };

                searchWorker.execute();
            }
        });
        searchMenu.add(menuFileSearchByName);

    }

}
