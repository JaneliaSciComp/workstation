package org.janelia.it.workstation.gui.dataview;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.workstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.workstation.gui.dialogs.search.SearchParametersPanel;

/**
 * The top search pane with a tab for every search type.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SearchPane extends JPanel {

    private JPanel hibernatePanel;
    private JTextField hibernateInput;
    private SearchParametersPanel solrPanel; // haha, get it? 
    private JPanel groovyPanel;
    private JTextArea groovyArea;
    private JTextArea groovyExamples;
    private JTabbedPane tabbedPane;

    public SearchPane(final SearchConfiguration searchConfig) {

        setLayout(new BorderLayout());

        hibernatePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Search by GUID or name: ");

        hibernateInput = new JTextField(40);

        final JButton hibernateButton = new JButton("Search");
        hibernateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performHibernateSearch(hibernateInput.getText());
            }
        });

        final JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hibernateInput.setText("");
            }
        });

        JPanel hibernateSearchPanel = new JPanel();
        hibernateSearchPanel.setLayout(new BoxLayout(hibernateSearchPanel, BoxLayout.LINE_AXIS));
        hibernateSearchPanel.add(titleLabel);
        hibernateSearchPanel.add(hibernateInput);
        hibernateSearchPanel.add(Box.createHorizontalStrut(5));
        hibernateSearchPanel.add(hibernateButton);
        hibernateSearchPanel.add(Box.createHorizontalStrut(5));
        hibernateSearchPanel.add(clearButton);

        hibernatePanel = new JPanel();
        hibernatePanel.setLayout(new GridBagLayout());
        hibernatePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.weightx = c.weighty = 1.0;
        hibernatePanel.add(hibernateSearchPanel, c);

        solrPanel = new SearchParametersPanel() {
            @Override
            public void performSearch(boolean clear) {
                super.performSearch(clear);
                performSolrSearch(clear);
            }
        };
        searchConfig.addConfigurationChangeListener(solrPanel);

        groovyPanel = new JPanel(new BorderLayout());
        groovyArea = new JTextArea(5, 10);
        groovyExamples = new JTextArea(5, 10);
        groovyExamples.setEditable(false);

        StringBuffer examples = new StringBuffer();
        examples.append("Ex1: a.getEntity(id)\n");
        examples.append("Ex1: a.getEntityTree(id)\n");

        groovyExamples.setText(examples.toString());

        JPanel groovyBody = new JPanel(new FlowLayout());
        groovyBody.setLayout(new BoxLayout(groovyBody, BoxLayout.LINE_AXIS));
        groovyBody.add(groovyArea);
        groovyBody.add(Box.createRigidArea(new Dimension(10, 1)));
        groovyBody.add(groovyExamples);

        final JButton groovyButton = new JButton("Execute Groovy Code");
        groovyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performGroovySearch(groovyArea.getText());
            }
        });

        JPanel groovyButtons = new JPanel();
        groovyButtons.setLayout(new BoxLayout(groovyButtons, BoxLayout.LINE_AXIS));
        groovyButtons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        groovyButtons.add(groovyButton);
        groovyButtons.add(Box.createHorizontalGlue());

        groovyPanel.add(groovyBody, BorderLayout.CENTER);
        groovyPanel.add(groovyButtons, BorderLayout.SOUTH);
        groovyPanel.setVisible(false);

        JPanel hibernateTab = new JPanel(new BorderLayout());
        hibernateTab.add(hibernatePanel, BorderLayout.CENTER);
        JPanel solrTab = new JPanel(new BorderLayout());
        solrTab.add(solrPanel, BorderLayout.CENTER);
        JPanel groovyTab = new JPanel(new BorderLayout());
        groovyTab.add(groovyPanel, BorderLayout.CENTER);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Hibernate Search", hibernateTab);
        tabbedPane.addTab("Solr Search", solrTab);
	    // TODO: complete Groovy implementation and unhide this
        //tabbedPane.addTab("Groovy Search", groovyTab);
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                switch (tabbedPane.getSelectedIndex()) {
                    case 0:
                        hibernatePanel.setVisible(true);
                        solrPanel.setVisible(false);
                        groovyPanel.setVisible(false);
                        break;
                    case 1:
                        hibernatePanel.setVisible(false);
                        solrPanel.setVisible(true);
                        groovyPanel.setVisible(false);
                        break;
                    case 2:
                        hibernatePanel.setVisible(false);
                        solrPanel.setVisible(false);
                        groovyPanel.setVisible(true);
                        break;
                }

            }
        });
        add(tabbedPane, BorderLayout.CENTER);

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "enterAction");
        getActionMap().put("enterAction", new AbstractAction("enterAction") {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (tabbedPane.getSelectedIndex()) {
                    case 0:
                        hibernateButton.doClick(100);
                        break;
                    case 1:
                        solrPanel.getSearchButton().doClick(100);
                        break;
                    case 2:
                        groovyButton.doClick(100);
                        break;
                }
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
            }
        });
    }

    public void setTabIndex(int index) {
        tabbedPane.setSelectedIndex(index);
    }

    public abstract void performHibernateSearch(String searchString);

    public abstract void performSolrSearch(boolean clear);

    public abstract void performGroovySearch(String code);

    public SearchParametersPanel getSolrPanel() {
        return solrPanel;
    }
}
