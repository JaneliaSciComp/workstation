package org.janelia.it.workstation.gui.browser.components.domain;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.model.user_data.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import java.lang.reflect.InvocationTargetException;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.components.viewer.AnnotationTablePanel;
import org.janelia.it.workstation.gui.browser.components.viewer.AnnotationView;
import org.janelia.it.workstation.gui.browser.dialogs.DomainObjectPermissionDialog;
import org.janelia.it.workstation.gui.browser.model.DomainObjectAttribute;
import org.janelia.it.workstation.gui.browser.model.DomainObjectPermission;


/**
 * A panel for displaying details about the currently selected entity.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectDetailsPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectDetailsPanel.class);

//    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private static final String ATTRIBUTES_COLUMN_KEY = "Attribute Name";
    private static final String ATTRIBUTES_COLUMN_VALUE = "Attribute Value";

    private static final String PERMISSIONS_COLUMN_SUBJECT = "Subject";
    private static final String PERMISSIONS_COLUMN_TYPE = "Type";
    private static final String PERMISSIONS_COLUMN_PERMS = "Permissions";

    public static final String TAB_NAME_ATTRIBUTES = "Attributes";
    public static final String TAB_NAME_PERMISSIONS = "Permissions";
    public static final String TAB_NAME_ANNOTATIONS = "Annotations";

    private static final String OWNER_PERMISSION = "owner";

    private JTabbedPane tabbedPane;
    private List<String> tabNames = new ArrayList<>();

    private final JLabel attributesLoadingLabel;
    private final JPanel attributesPanel;
    private final DynamicTable attributesTable;

    private final JLabel permissionsLoadingLabel;
    private final JPanel permissionsPanel;
    private final DynamicTable permissionsTable;
    private final JPanel permissionsButtonPane;
    private final JButton addPermissionButton;

    private final JLabel annotationsLoadingLabel;
    private final JPanel annotationsPanel;
    private final AnnotationView annotationsView;

    private final DomainObjectPermissionDialog dopDialog;

    private boolean firstLoad = true;
    private List<Subject> subjects;
    private DomainObject domainObject;

    private JLabel createLoadingLabel() {
        JLabel loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        return loadingLabel;
    }

    public DomainObjectDetailsPanel() {

        setLayout(new BorderLayout());

        tabNames.add(TAB_NAME_ATTRIBUTES);
        tabNames.add(TAB_NAME_PERMISSIONS);
        tabNames.add(TAB_NAME_ANNOTATIONS);

        // Child dialogs
        dopDialog = new DomainObjectPermissionDialog(this);

        // Tabbed pane
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // Attributes tab
        attributesLoadingLabel = createLoadingLabel();
        attributesPanel = new JPanel(new BorderLayout());
        attributesTable = new DynamicTable(true, false) {
            @Override
            public Object getValue(Object userObject, DynamicColumn column) {
                DomainObjectAttribute attr = (DomainObjectAttribute) userObject;
                if (null != attr) {
                    if (column.getName().equals(ATTRIBUTES_COLUMN_KEY)) {
                        return attr.getLabel();
                    }
                    else if (column.getName().equals(ATTRIBUTES_COLUMN_VALUE)) {
                        try {
                            return attr.getGetter().invoke(domainObject);
                        }
                        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                            log.error("Error getting value for attribute: " + attr.getName(), e);
                            return null;
                        }
                    }
                }
                return null;
            }
        };
        attributesTable.setAutoResizeColumns(false);
        attributesTable.addColumn(ATTRIBUTES_COLUMN_KEY, ATTRIBUTES_COLUMN_KEY, true, false, false, true);
        attributesTable.addColumn(ATTRIBUTES_COLUMN_VALUE, ATTRIBUTES_COLUMN_VALUE, true, false, false, true);

        tabbedPane.addTab(TAB_NAME_ATTRIBUTES, Icons.getIcon("table.png"), attributesPanel, "The data entity's attributes");

        // Permissions tab
        permissionsLoadingLabel = createLoadingLabel();
        permissionsTable = new DynamicTable(true, false) {
            @Override
            public Object getValue(Object userObject, DynamicColumn column) {
                DomainObjectPermission dop = (DomainObjectPermission) userObject;
                if (null != dop) {
                    if (column.getName().equals(PERMISSIONS_COLUMN_SUBJECT)) {
                        return dop.getSubjectKey().split(":")[1];
                    }
                    else if (column.getName().equals(PERMISSIONS_COLUMN_TYPE)) {
                        if (dop.isOwner()) {
                            return OWNER_PERMISSION;
                        }
                        return dop.getSubjectKey().split(":")[0];
                    }
                    else if (column.getName().equals(PERMISSIONS_COLUMN_PERMS)) {
                        return dop.getPermissions();
                    }
                }
                return null;
            }

            @Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {
                JPopupMenu menu = super.createPopupMenu(e);

                if (menu != null) {
                    JTable table = getTable();
                    ListSelectionModel lsm = table.getSelectionModel();
                    if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex()) {
                        return menu;
                    }

                    final DomainObjectPermission dop = (DomainObjectPermission) getRows().get(table.getSelectedRow()).getUserObject();

                    if (dop.isOwner()) {
                        // No menu for the permanent owner permission. In the future this might show a "gifting" option
                        // if the owner wants to transfer ownership.
                    }
                    else if (DomainUtils.isOwner(domainObject)) {

                        JMenuItem editItem = new JMenuItem("  Edit Permission");
                        editItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                dopDialog.showForPermission(dop);
                            }
                        });
                        menu.add(editItem);

                        JMenuItem deleteItem = new JMenuItem("  Delete Permission");
                        deleteItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {

                                SimpleWorker worker = new SimpleWorker() {

                                    @Override
                                    protected void doStuff() throws Exception {
//                                        ModelMgr.getModelMgr().revokePermissions(dop.getEntity().getId(), dop.getSubjectKey(), recursive);
//                                        ModelMgr.getModelMgr().invalidateCache(entity, true);
                                    }

                                    @Override
                                    protected void hadSuccess() {
                                        Utils.setDefaultCursor(DomainObjectDetailsPanel.this);
                                        refresh();
                                    }

                                    @Override
                                    protected void hadError(Throwable error) {
                                        SessionMgr.getSessionMgr().handleException(error);
                                        Utils.setDefaultCursor(DomainObjectDetailsPanel.this);
                                        refresh();
                                    }
                                };

                                Utils.setWaitingCursor(DomainObjectDetailsPanel.this);
                                worker.setProgressMonitor(new IndeterminateProgressMonitor(DomainObjectDetailsPanel.this, "Revoking permissions...", ""));
                                worker.execute();
                            }
                        });
                        menu.add(deleteItem);
                    }
                }

                return menu;
            }

            @Override
            protected void rowDoubleClicked(int row) {
                final DomainObjectPermission dop = (DomainObjectPermission) getRows().get(row).getUserObject();
                dopDialog.showForPermission(dop);
            }
        };
        permissionsTable.setAutoResizeColumns(false);
        permissionsTable.addColumn(PERMISSIONS_COLUMN_SUBJECT, PERMISSIONS_COLUMN_SUBJECT, true, false, false, true);
        permissionsTable.addColumn(PERMISSIONS_COLUMN_TYPE, PERMISSIONS_COLUMN_TYPE, true, false, false, true);
        permissionsTable.addColumn(PERMISSIONS_COLUMN_PERMS, PERMISSIONS_COLUMN_PERMS, true, false, false, true);

        addPermissionButton = new JButton("Grant permission");
        addPermissionButton.setEnabled(false);
        addPermissionButton.setToolTipText("Grant permission to a user or group");
        addPermissionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dopDialog.showForNewPermission(domainObject);
            }
        });

        permissionsButtonPane = new JPanel();
        permissionsButtonPane.setLayout(new BoxLayout(permissionsButtonPane, BoxLayout.LINE_AXIS));
        permissionsButtonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        permissionsButtonPane.add(addPermissionButton);
        permissionsButtonPane.add(Box.createHorizontalGlue());

        permissionsPanel = new JPanel(new BorderLayout());
        permissionsPanel.add(permissionsButtonPane, BorderLayout.NORTH);
        permissionsPanel.add(permissionsTable, BorderLayout.CENTER);

        tabbedPane.addTab("Permissions", Icons.getIcon("group.png"), permissionsPanel, "Who has access to the this data entity");

        // Annotations tab
        annotationsLoadingLabel = createLoadingLabel();
        annotationsPanel = new JPanel(new BorderLayout());
        annotationsView = new AnnotationTablePanel();

        tabbedPane.addTab("Annotations", Icons.getIcon("page_white_edit.png"), annotationsPanel, "The user annotations");
    }

    public void showNothing() {
        attributesPanel.removeAll();
        permissionsPanel.removeAll();
        annotationsPanel.removeAll();
        this.updateUI();
    }

    public void showLoadingIndicator() {
        showAttributesLoadingIndicator();
        showPermissionsLoadingIndicator();
        showAnnotationsLoadingIndicator();
        this.updateUI();
    }

    private void showAttributesLoadingIndicator() {
        attributesPanel.removeAll();
        attributesPanel.add(attributesLoadingLabel, BorderLayout.CENTER);
    }

    private void showPermissionsLoadingIndicator() {
        permissionsPanel.removeAll();
        permissionsPanel.add(permissionsLoadingLabel, BorderLayout.CENTER);
    }

    private void showAnnotationsLoadingIndicator() {
        annotationsPanel.removeAll();
        annotationsPanel.add(annotationsLoadingLabel, BorderLayout.CENTER);
    }

//    public void loadRootedEntity(RootedEntity rootedEntity) {
//        loadRootedEntity(rootedEntity, TAB_NAME_ATTRIBUTES);
//    }

//    public void loadRootedEntity(RootedEntity rootedEntity, String defaultTab) {
//        if (rootedEntity == null) {
//            showNothing();
//            return;
//        }
//        EntityData entityData = rootedEntity.getEntityData();
//        loadEntity(rootedEntity.getEntity(), entityData.getEntityAttrName(), defaultTab);
//    }

    public void loadDomainObject(final DomainObject domainObject) {
        loadDomainObject(domainObject, null);
    }

    public void loadDomainObject(final DomainObject domainObject, final String defaultTab) {

        this.domainObject = domainObject;

        log.info("Loading domain object: "+domainObject.getId());
        
        refresh();
                
        // Select the default tab
        if (defaultTab != null) {
            tabbedPane.setSelectedIndex(tabNames.indexOf(defaultTab));
        }
    }

    private void loadAttributes() {

        log.debug("Loading attributes for {}", domainObject.getId());
        showAttributesLoadingIndicator();

        // Update the attribute table
        attributesTable.removeAllRows();

//        attributesTable.addRow(new AttributeValue("Type", domainObject.getClass().getName()));
//        
//        attributesTable.addRow(new AttributeValue("GUID", "" + domainObject.getId()));
//        attributesTable.addRow(new AttributeValue("Name", domainObject.getName()));
//        attributesTable.addRow(new AttributeValue("Type", domainObject.getClass().getName()));
//
//        String sortCriteria = ModelMgr.getModelMgr().getSortCriteria(domainObject.getId());
//        if (sortCriteria != null) {
//            attributesTable.addRow(new AttributeValue("Sort Criteria", sortCriteria));
//        }
//
//        if (role != null) {
//            attributesTable.addRow(new AttributeValue("Role", role));
//        }
//        if (domainObject.getCreationDate() != null) {
//            attributesTable.addRow(new AttributeValue("Creation Date", df.format(domainObject.getCreationDate())));
//        }
//        if (domainObject.getUpdatedDate() != null) {
//            attributesTable.addRow(new AttributeValue("Updated Date", df.format(domainObject.getUpdatedDate())));
//        }
        
        List<DomainObjectAttribute> searchAttrs = DomainUtils.getAttributes(domainObject);
                
        Collections.sort(searchAttrs, new Comparator<DomainObjectAttribute>() {
            @Override
            public int compare(DomainObjectAttribute o1, DomainObjectAttribute o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });

        for(DomainObjectAttribute attr : searchAttrs) {
            if (attr.isDisplay()) {
                attributesTable.addRow(attr);
            }
        }

        attributesTable.updateTableModel();
        if (firstLoad) {
            attributesTable.autoResizeColWidth();
            firstLoad = false;
        }
        attributesPanel.removeAll();
        attributesPanel.add(attributesTable, BorderLayout.CENTER);
    }

    private void loadSubjects() {

        log.debug("Loading subjects for {}", domainObject.getId());

        SimpleWorker worker = new SimpleWorker() {

            private List<Subject> subjects;

            @Override
            protected void doStuff() throws Exception {
                subjects = ModelMgr.getModelMgr().getSubjects();
                Collections.sort(subjects, new Comparator<Subject>() {
                    @Override
                    public int compare(Subject o1, Subject o2) {
                        return ComparisonChain.start().compare(o1.getKey(), o2.getKey()).result();
                    }
                });
            }

            @Override
            protected void hadSuccess() {
                setSubjects(subjects);
                loadPermissions();
                addPermissionButton.setEnabled(DomainUtils.isOwner(domainObject) && !DomainUtils.isVirtual(domainObject));
                log.debug("Setting permission button state to {}", addPermissionButton.isEnabled());
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }

    private Set<DomainObjectPermission> getPermissions(DomainObject domainObject) {
        Set<DomainObjectPermission> permissions = new HashSet<>();
        Set<String> usedSubjects = new HashSet<>();
        usedSubjects.add(domainObject.getOwnerKey());
        usedSubjects.addAll(domainObject.getReaders());
        usedSubjects.addAll(domainObject.getWriters());
        for(String subjectKey : usedSubjects) {
            log.info("Adding DOP for "+subjectKey);
            DomainObjectPermission dop = new DomainObjectPermission(domainObject, subjectKey);
            dop.setRead(domainObject.getReaders().contains(subjectKey));
            dop.setWrite(domainObject.getWriters().contains(subjectKey));
            permissions.add(dop);
        }
        return permissions;
    }
    
    private void loadPermissions() {

        log.debug("Loading permissions for {}", domainObject.getId());

        showPermissionsLoadingIndicator();

        final List<DomainObjectPermission> eaps = new ArrayList<>(getPermissions(domainObject));

        Collections.sort(eaps, new Comparator<DomainObjectPermission>() {
            @Override
            public int compare(DomainObjectPermission o1, DomainObjectPermission o2) {
                return ComparisonChain.start().compare(o1.getSubjectKey(), o2.getSubjectKey()).result();
            }
        });

        permissionsTable.removeAllRows();
        for (DomainObjectPermission eap : eaps) {
            permissionsTable.addRow(eap);
        }
        permissionsTable.updateTableModel();
        permissionsPanel.removeAll();
        permissionsPanel.add(permissionsButtonPane, BorderLayout.SOUTH);
        permissionsPanel.add(permissionsTable, BorderLayout.CENTER);
        permissionsPanel.revalidate();
        permissionsPanel.repaint();
    }

    public void loadAnnotations() {

        log.debug("Loading annotations for {}", domainObject.getId());

        showAnnotationsLoadingIndicator();

        annotationsView.setAnnotations(null);

        SimpleWorker annotationLoadingWorker = new SimpleWorker() {

            private final List<Annotation> annotations = new ArrayList<>();

            @Override
            protected void doStuff() throws Exception {
                // TODO: use domain dao
                annotations.addAll(DomainExplorerTopComponent.getDao().getAnnotations(SessionMgr.getSubjectKey(), domainObject.getId()));
            }

            @Override
            protected void hadSuccess() {
                annotationsView.setAnnotations(annotations);
                annotationsPanel.removeAll();
                annotationsPanel.add((JPanel) annotationsView, BorderLayout.CENTER);
                annotationsPanel.revalidate();
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                annotationsPanel.removeAll();
                annotationsPanel.add((JPanel) annotationsView, BorderLayout.CENTER);
                annotationsPanel.revalidate();
            }
        };
        annotationLoadingWorker.execute();
    }
    
    public List<Subject> getSubjects() {
        return subjects;
    }

    /**
     * Returns the subjects which have not been granted access yet, and the 
     * currently selected subject specified by currSubjectKey. If currSubjectKey
     * is null, then only the unused subjects are returned.
     * @param currSubjectKey
     * @return 
     */
    public List<Subject> getUnusedSubjects(String currSubjectKey) {
        List<Subject> filtered = new ArrayList<>();
        for (Subject subject : subjects) {
            boolean used = false;
            if (domainObject.getReaders().contains(subject.getKey()) || domainObject.getWriters().contains(subject.getKey())) {
                    used = true;
            }
            if (!used || subject.getKey().equals(currSubjectKey)) {
                filtered.add(subject);
            }
        }
        EntityUtils.sortSubjects(filtered);
        return filtered;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }

    public void refresh() {
        loadSubjects();
        loadAttributes();
        loadAnnotations();
    }

    // TODO: we might need this later if we want to display attribute which are not DomainObjectAttributes
//    private class AttributeValue {
//
//        private final String name;
//        private final String value;
//
//        public AttributeValue(String name, String value) {
//            super();
//            this.name = name;
//            this.value = value;
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        public String getValue() {
//            return value;
//        }
//    }
}
