package org.janelia.it.workstation.gui.dataview;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

/**
 * The top search pane with a tab for every search type.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SearchPane extends JPanel {

    private JPanel hibernatePanel;
    private JTextField hibernateInput;

    public SearchPane() {

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

        add(hibernatePanel, BorderLayout.CENTER);

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "enterAction");
        getActionMap().put("enterAction", new AbstractAction("enterAction") {
            @Override
            public void actionPerformed(ActionEvent e) {
                hibernateButton.doClick(100);
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
            }
        });
    }

    public abstract void performHibernateSearch(String searchString);

}
