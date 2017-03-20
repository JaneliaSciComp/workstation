package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.workstation.browser.nb_action.NodePresenterAction;
import org.janelia.it.workstation.gui.large_volume_viewer.nodes.TmSampleNode;
import org.openide.nodes.Node;

/**
 * This action allows the user to create new objects annotating an TM Sample by right-clicking on it
 * and choosing New from the context menu.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class NewSampleAnnotationsAction extends NodePresenterAction {

    private final static NewSampleAnnotationsAction singleton = new NewSampleAnnotationsAction();
    public static NewSampleAnnotationsAction get() {
        return singleton;
    }

    private NewSampleAnnotationsAction() {
    }

    @Override 
    public JMenuItem getPopupPresenter() {

        List<Node> selected = getSelectedNodes();
        TmSampleNode node = (TmSampleNode)selected.get(0);
        TmSample sample = node.getSample();
        
        JMenu newMenu = new JMenu("New");
        
        JMenuItem menuItem = new JMenuItem("Workspace");
        menuItem.addActionListener(new NewWorkspaceActionListener(sample));
        newMenu.add(menuItem);

        menuItem = new JMenuItem("Workspace from SWC Path");
        menuItem.addActionListener(new NewWorkspaceFromSWCActionListener(sample));
        newMenu.add(menuItem);
        
        menuItem = new JMenuItem("Semi-Automated Tracing Session");
        menuItem.addActionListener(new NewSataSessionActionListener(sample));
        newMenu.add(menuItem);
        
        if (selected.size()!=1) {
            newMenu.setEnabled(false);
        }
        
        return newMenu;
    }  
}
