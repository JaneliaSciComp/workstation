/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/23/11
 * Time: 9:18 AM
 */
package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.*;

import javax.swing.*;

import org.janelia.it.jacs.model.entity.EntityData;

/**
 * The main frame for the dataviewer assembles all the subcomponents.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataviewFrame extends JFrame {

    private static final double realEstatePercent = 0.7;

    private EntityTypePane entityTypePane;
    private EntityListPane entityListPane;
    private EntityDataPane entityParentsPane;
    private EntityDataPane entityChildrenPane;
    private JPanel progressPanel;

    public DataviewFrame() {

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension((int) (screenSize.width * realEstatePercent), (int) (screenSize.height * realEstatePercent)));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(300, 20));
        progressPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        progressPanel.add(new JLabel("Loading..."), c);
        c.gridy = 1;
        progressPanel.add(progressBar, c);

        setLayout(new BorderLayout());

        setJMenuBar(new DataviewMenuBar(this));

        initUI();
        initData();
    }

    public EntityListPane getEntityListPane() {
        return entityListPane;
    }

    private void initUI() {

        entityTypePane = new EntityTypePane();

        entityParentsPane = new EntityDataPane("Entity Data: Parents", true, false) {

            @Override
            protected void doubleClick(EntityData entityData) {
                if (entityData.getParentEntity() != null) {
                    entityListPane.showEntity(entityData.getParentEntity());
                }
            }

        };

        entityChildrenPane = new EntityDataPane("Entity Data: Children", false, true) {

            @Override
            protected void doubleClick(EntityData entityData) {
                if (entityData.getChildEntity() != null) {
                    entityListPane.showEntity(entityData.getChildEntity());
                }
            }

        };

        entityListPane = new EntityListPane(entityParentsPane, entityChildrenPane);


        double frameHeight = (double) DataviewFrame.this.getPreferredSize().height - 30;

        JSplitPane splitPaneVerticalInner = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, entityParentsPane, entityListPane);
        splitPaneVerticalInner.setDividerLocation((int) (frameHeight * 1 / 4));

        JSplitPane splitPaneVerticalOuter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, splitPaneVerticalInner, entityChildrenPane);
        splitPaneVerticalOuter.setDividerLocation((int) (frameHeight * 3 / 4));

        JSplitPane splitPaneHorizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, entityTypePane, splitPaneVerticalOuter);
        splitPaneHorizontal.setDividerLocation(300);
        getContentPane().add(splitPaneHorizontal, BorderLayout.CENTER);
    }

    private void initData() {
        entityTypePane.refresh();
    }
}
