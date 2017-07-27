package org.janelia.it.workstation.browser.gui.listview;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.WrapLayout;


/**
 * Generic toolbar for viewer panels supporting basic navigation.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ViewerToolbar extends JPanel {

    protected JPanel toolbar;
    protected JButton refreshButton;

    public ViewerToolbar() {
        super(new BorderLayout());

        toolbar = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 2, 3));
        toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        
        refreshButton = new JButton();
        refreshButton.setIcon(Icons.getRefreshIcon());
        refreshButton.setFocusable(false);
        refreshButton.setToolTipText("Refresh the current view");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        refreshButton.addMouseListener(new MouseForwarder(this, "RefreshButton->JToolBar"));
        toolbar.add(refreshButton);

        add(toolbar, BorderLayout.CENTER);
        
        addSeparator();
    }
    
    protected abstract void refresh();

    public JButton getRefreshButton() {
        return refreshButton;
    }
    
    public void addSeparator() {
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
    }
}
