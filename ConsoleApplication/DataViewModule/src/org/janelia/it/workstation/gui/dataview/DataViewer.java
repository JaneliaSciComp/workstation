package org.janelia.it.workstation.gui.dataview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;

import org.janelia.it.jacs.model.entity.EntityData;

/**
 * The main frame for the data viewer assembles all the subcomponents.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataViewer extends JPanel {

    private static final double realEstatePercent = 1.0;

    private SearchPane searchPane;
    private EntityPane entityPane;
    private EntityDataPane entityParentsPane;
    private EntityDataPane entityChildrenPane;
    private JPanel progressPanel;

    public DataViewer() {

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension((int) (screenSize.width * realEstatePercent), (int) (screenSize.height * realEstatePercent) - 50));

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

        initUI();
        initData();
    }

    private void initUI() {

        entityParentsPane = new EntityDataPane("Entity Data: Parents", true, false) {
            @Override
            protected void doubleClick(EntityData entityData) {
                if (entityData.getParentEntity() != null) {
                    entityPane.showEntity(entityData.getParentEntity());
                }
            }
        };

        entityChildrenPane = new EntityDataPane("Entity Data: Children", false, true) {
            @Override
            protected void doubleClick(EntityData entityData) {
                if (entityData.getChildEntity() != null) {
                    entityPane.showEntity(entityData.getChildEntity());
                }
            }
        };

        searchPane = new SearchPane() {
            @Override
            public void performHibernateSearch(String searchString) {
                if (searchString.matches("\\d{19}")) {
                    entityPane.performSearchById(new Long(searchString));
                }
                else {
                    entityPane.performSearchByName(searchString);
                }
            }
        };

        entityPane = new EntityPane(searchPane, entityParentsPane, entityChildrenPane);

        double frameHeight = (double) DataViewer.this.getPreferredSize().height - 30;
        double frameWidth = (double) DataViewer.this.getPreferredSize().width - 30;

        JSplitPane splitPaneVerticalInner = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, entityParentsPane, entityPane);
        splitPaneVerticalInner.setDividerLocation((int) (frameHeight * 0.15));
        splitPaneVerticalInner.setResizeWeight(0.5);

        JSplitPane splitPaneVerticalOuter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, splitPaneVerticalInner, entityChildrenPane);
        splitPaneVerticalOuter.setDividerLocation((int) (frameHeight * 0.5));
        splitPaneVerticalOuter.setResizeWeight(0.5);

        add(searchPane, BorderLayout.NORTH);
        add(splitPaneVerticalOuter, BorderLayout.CENTER);
    }

    private void initData() {
    }

    public EntityPane getEntityPane() {
        return entityPane;
    }

    public SearchPane getSearchPane() {
        return searchPane;
    }

    public EntityDataPane getEntityParentsPane() {
        return entityParentsPane;
    }

    public EntityDataPane getEntityChildrenPane() {
        return entityChildrenPane;
    }

    public JPanel getProgressPanel() {
        return progressPanel;
    }

}
