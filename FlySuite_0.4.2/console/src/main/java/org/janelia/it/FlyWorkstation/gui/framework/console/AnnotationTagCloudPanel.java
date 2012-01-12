package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.border.Border;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * A tag cloud of Entity-based annotations which support context menu operations such as deletion.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationTagCloudPanel extends TagCloudPanel<OntologyAnnotation> {
    
    private void deleteTag(final OntologyAnnotation tag) {
    	
        Utils.setWaitingCursor(SessionMgr.getSessionMgr().getActiveBrowser());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
            	ModelMgr.getModelMgr().removeAnnotation(tag.getId());
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getSessionMgr().getActiveBrowser());
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
                Utils.setDefaultCursor(SessionMgr.getSessionMgr().getActiveBrowser());
                JOptionPane.showMessageDialog(AnnotationTagCloudPanel.this, "Error deleting annotation", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        worker.execute();
    }

    @Override
    protected void showPopupMenu(final MouseEvent e, final OntologyAnnotation tag) {

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);
        
        JMenuItem titleItem = new JMenuItem(tag.getEntity().getName());
        titleItem.setEnabled(false);
        popupMenu.add(titleItem);
        
        
    	if (SessionMgr.getUsername().equals(tag.getOwner())) {
            JMenuItem deleteItem = new JMenuItem("  Delete annotation");
            deleteItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    deleteTag(tag);
                }
            });
            popupMenu.add(deleteItem);
    	}

        JMenuItem detailsItem = new JMenuItem("  View details");
        detailsItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().viewAnnotationDetails(tag);
            }
        });
        popupMenu.add(detailsItem);

        popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
    }

    
	@Override
	protected JLabel createTagLabel(OntologyAnnotation tag) {
		JLabel label = super.createTagLabel(tag);
		
		label.setBackground(ModelMgr.getModelMgr().getUserColorMapping().getColor(tag.getOwner()));
		
		AnnotationSession currentSession = ModelMgr.getModelMgr().getCurrentAnnotationSession();
		if (currentSession != null) {
			if (tag.getSessionId() != null && tag.getSessionId().equals(currentSession.getId())) {
				// This annotation is in the current session, so display it normally.	
			}
			else {
				// Dim the annotations from other sessions.
				Color dimFgColor = label.getBackground().darker();
			    Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
			    Border lineBorder = BorderFactory.createLineBorder(dimFgColor, 1);
			    Border border = BorderFactory.createCompoundBorder(lineBorder, paddingBorder);
				label.setBorder(border);
				label.setForeground(dimFgColor);
			}
		}
		else if (currentSession == null) {
			// We're not even in a session, just display everything normally.
		}
		
		return label;
	}

}
