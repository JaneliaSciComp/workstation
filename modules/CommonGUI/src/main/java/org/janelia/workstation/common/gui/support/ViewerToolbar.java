package org.janelia.workstation.common.gui.support;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;


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
        refreshButton.addActionListener(e -> refresh());
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
