package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * A tag cloud of Entity-based annotations which support context menu operations such as deletion.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AnnotationTagCloudPanel extends TagCloudPanel<Annotation> implements AnnotationView {

    @Override
    protected void showPopupMenu(final MouseEvent e, final Annotation tag) {
        try {
            JPopupMenu popupMenu = getPopupMenu(tag);
            if (popupMenu!=null) {
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
                e.consume();
            }
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }
    
    protected abstract JPopupMenu getPopupMenu(Annotation tag);

    @Override
    protected String getTagTitle(Annotation tag) {
        return StringUtils.abbreviate(tag.getName(), 40);
    }
    
    @Override
    protected JLabel createTagLabel(Annotation tag) {
        JLabel label = super.createTagLabel(tag);
        label.setBackground(StateMgr.getStateMgr().getUserColorMapping().getColor(tag.getOwnerKey()));
        String owner = DomainUtils.getNameFromSubjectKey(tag.getOwnerKey());
        label.setToolTipText(tag.getName()+" ("+owner+")");
        return label;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return getTags();
    }

    @Override
    public void setAnnotations(List<Annotation> annotations) {
        setTags(annotations);
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
        removeTag(annotation);
    }

    @Override
    public void addAnnotation(Annotation annotation) {
        addTag(annotation);
    }
}
