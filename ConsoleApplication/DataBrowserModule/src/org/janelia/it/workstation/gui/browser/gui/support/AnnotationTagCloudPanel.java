package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.EnumText;
import org.janelia.it.jacs.model.domain.ontology.Interval;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.Text;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.actions.BulkEditAnnotationKeyValueAction;
import org.janelia.it.workstation.gui.browser.actions.RemoveAnnotationKeyValueAction;
import org.janelia.it.workstation.gui.browser.actions.RemoveAnnotationTermAction;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.viewer.TagCloudPanel;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tag cloud of Entity-based annotations which support context menu operations such as deletion.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationTagCloudPanel<T,S> extends TagCloudPanel<Annotation> implements AnnotationView<T,S> {

    private static final Logger log = LoggerFactory.getLogger(AnnotationTagCloudPanel.class);

    private SelectionModel<T,S> selectionModel;
    private ImageModel<T,S> imageModel;
    
    @Override
    public void setSelectionModel(SelectionModel<T, S> selectionModel) {
        this.selectionModel = selectionModel;
    }

    @Override
    public void setImageModel(ImageModel<T, S> imageModel) {
        this.imageModel = imageModel;
    }
    
    private void deleteTag(final Annotation tag) {

        Utils.setWaitingCursor(SessionMgr.getMainFrame());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainMgr.getDomainMgr().getModel().remove(tag);
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("Error deleting annotation", error);
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
                JOptionPane.showMessageDialog(AnnotationTagCloudPanel.this, "Error deleting annotation", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        worker.execute();
    }

    @Override
    protected void showPopupMenu(final MouseEvent e, final Annotation tag) {

        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        final List<S> selectionIds = selectionModel.getSelectedIds();

        final List<T> selectedObjects = new ArrayList<>();
        for(S uniqueId : selectionIds) {
            selectedObjects.add(imageModel.getImageByUniqueId(uniqueId));
        }
        
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        if (selectionIds.size()>1) {
            JMenuItem titleItem = new JMenuItem("(Multiple Selected)");
            titleItem.setEnabled(false);
            popupMenu.add(titleItem);

            if (SessionMgr.getSubjectKey().equals(tag.getOwnerKey())) {
                final RemoveAnnotationTermAction termAction = new RemoveAnnotationTermAction(imageModel, selectedObjects, tag.getKeyTerm(), tag.getKey());
                JMenuItem deleteByTermItem = new JMenuItem(termAction.getName());
                deleteByTermItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        termAction.doAction();
                    }
                });
                popupMenu.add(deleteByTermItem);

                try {
                    
                    OntologyTerm keyTerm = model.getOntologyTermByReference(tag.getKeyTerm());
                    if (keyTerm!=null) {
                        if (keyTerm instanceof org.janelia.it.jacs.model.domain.ontology.Enum 
                                || keyTerm instanceof EnumText 
                                || keyTerm instanceof Interval 
                                || keyTerm instanceof Text) {
                            final BulkEditAnnotationKeyValueAction<T,S> bulkEditAction = new BulkEditAnnotationKeyValueAction(imageModel, selectedObjects, tag);
                            JMenuItem editByValueItem = new JMenuItem("  "+bulkEditAction.getName());
                            editByValueItem.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent actionEvent) {
                                    bulkEditAction.doAction();
                                }
                            });
                            popupMenu.add(editByValueItem);
                            final RemoveAnnotationKeyValueAction<T,S> valueAction = new RemoveAnnotationKeyValueAction(imageModel, selectedObjects, tag);
                            JMenuItem deleteByValueItem = new JMenuItem("  "+valueAction.getName());
                            deleteByValueItem.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent actionEvent) {
                                    valueAction.doAction();
                                }
                            });
                            popupMenu.add(deleteByValueItem);
                        }
                    }
                    else {
                        log.warn("Cannot create menu item because ontology term no longer exists.");
                    }
                }
                catch (Exception e1) {
                    SessionMgr.getSessionMgr().handleException(e1);
                }
            }

        }
        else {
            JMenuItem titleItem = new JMenuItem(tag.getName());
            titleItem.setEnabled(false);
            popupMenu.add(titleItem);

            if (ClientDomainUtils.hasWriteAccess(tag)) {
                JMenuItem deleteItem = new JMenuItem("  Delete Annotation");
                deleteItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        deleteTag(tag);
                    }
                });
                popupMenu.add(deleteItem);
            }

            if (tag.getValue()!=null) {
                JMenuItem editItem = new JMenuItem("  Edit Annotation");
                editItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
                        dialog.setAnnotationValue(tag.getValue());
                        dialog.setVisible(true);
                        String value = dialog.getAnnotationValue();
                        if (null==value) {
                            value = "";
                        }
                        tag.setValue(value);
                        String tmpName = tag.getName();
                        String namePrefix = tmpName.substring(0, tmpName.indexOf("=")+2);
                        tag.setName(namePrefix+value);
                        try {
                            model.save(tag);
                        }
                        catch (Exception e1) {
                            log.error("Error editing annotation", e1);
                            SessionMgr.getSessionMgr().handleException(e1);
                        }
                    }
                });
                popupMenu.add(editItem);
            }

            JMenuItem copyItem = new JMenuItem("  Copy Annotation");
            copyItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    // TODO: port this
                    //ModelMgr.getModelMgr().setCurrentSelectedOntologyAnnotation(tag);
                }
            });
            popupMenu.add(copyItem);

            JMenuItem detailsItem = new JMenuItem("  View Details");
            detailsItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    // TODO: port this
                    //OntologyOutline.viewAnnotationDetails(tag);
                }
            });
            popupMenu.add(detailsItem);
        }

        popupMenu.show(e.getComponent(), e.getX(), e.getY());
        e.consume();
    }

    @Override
    protected JLabel createTagLabel(Annotation tag) {
        JLabel label = super.createTagLabel(tag);
        label.setText(tag.getName());
        
        // TODO: port over user color mapping
        label.setBackground(ModelMgr.getModelMgr().getUserColorMapping().getColor(tag.getOwnerKey()));
        String owner = DomainUtils.getNameFromSubjectKey(tag.getOwnerKey());
        label.setToolTipText("This annotation was made by "+owner);
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
