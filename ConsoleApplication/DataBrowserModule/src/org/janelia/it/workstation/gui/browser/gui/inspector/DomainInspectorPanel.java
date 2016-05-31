package org.janelia.it.workstation.gui.browser.gui.inspector;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.enums.AlignmentScoreType;
import org.janelia.it.jacs.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.gui.dialogs.DomainObjectPermissionDialog;
import org.janelia.it.workstation.gui.browser.gui.support.AnnotationTablePanel;
import org.janelia.it.workstation.gui.browser.gui.support.AnnotationView;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicColumn;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicTable;
import org.janelia.it.workstation.gui.browser.model.DomainObjectPermission;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;


/**
 * A panel for displaying details about the currently selected domain object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainInspectorPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DomainInspectorPanel.class);

    private static final String ATTRIBUTES_COLUMN_KEY = "Attribute Name";
    private static final String ATTRIBUTES_COLUMN_VALUE = "Attribute Value";

    private static final String PERMISSIONS_COLUMN_SUBJECT = "User";
    private static final String PERMISSIONS_COLUMN_TYPE = "Type";
    private static final String PERMISSIONS_COLUMN_READ = "Read";
    private static final String PERMISSIONS_COLUMN_WRITE = "Write";

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
    private Map<String,Subject> subjectMap;
    private DomainObject domainObject;
    private PipelineResult result;
    private Set<ImmutablePair<String, Object>> propertySet;

    private JLabel createLoadingLabel() {
        JLabel loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        return loadingLabel;
    }

    public DomainInspectorPanel() {

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
                ImmutablePair<String,Object> attrPair = (ImmutablePair) userObject;
                if (null != attrPair) {
                    if (column.getName().equals(ATTRIBUTES_COLUMN_KEY)) {
                        return attrPair.getKey();
                    }
                    else if (column.getName().equals(ATTRIBUTES_COLUMN_VALUE)) {
                        return attrPair.getValue();
                    }
                }
                return null;
            }
        };
        attributesTable.setAutoResizeColumns(false);
        attributesTable.addColumn(ATTRIBUTES_COLUMN_KEY, ATTRIBUTES_COLUMN_KEY, true, false, false, true);
        attributesTable.addColumn(ATTRIBUTES_COLUMN_VALUE, ATTRIBUTES_COLUMN_VALUE, true, false, false, true);

        tabbedPane.addTab(TAB_NAME_ATTRIBUTES, Icons.getIcon("table.png"), attributesPanel, "The selected item's attributes");

        // Permissions tab
        permissionsLoadingLabel = createLoadingLabel();
        permissionsTable = new DynamicTable(true, false) {
            @Override
            public Object getValue(Object userObject, DynamicColumn column) {
                DomainObjectPermission dop = (DomainObjectPermission) userObject;
                if (dop == null) return null;
                if (column.getName().equals(PERMISSIONS_COLUMN_SUBJECT)) {
                    Subject subject = subjectMap.get(dop.getSubjectKey());
                    return subject==null ? null : subject.getFullName();
                }
                else if (column.getName().equals(PERMISSIONS_COLUMN_TYPE)) {
                    if (dop.isOwner()) {
                        return OWNER_PERMISSION;
                    }
                    return dop.getSubjectKey().split(":")[0];
                }
                else if (column.getName().equals(PERMISSIONS_COLUMN_READ)) {
                    return dop.getPermissions().contains("r");
                }
                else if (column.getName().equals(PERMISSIONS_COLUMN_WRITE)) {
                    return dop.getPermissions().contains("w");
                }
                return null;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                DynamicColumn dc = getColumns().get(column);
                if (dc.getName().equals(PERMISSIONS_COLUMN_READ) || dc.getName().equals(PERMISSIONS_COLUMN_WRITE)) {
                    return Boolean.class;
                }
                return super.getColumnClass(column);
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
                    else if (ClientDomainUtils.isOwner(domainObject)) {

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
                                        DomainModel model = DomainMgr.getDomainMgr().getModel();
                                        model.changePermissions(domainObject, dop.getSubjectKey(), "rw", false);
                                    }

                                    @Override
                                    protected void hadSuccess() {
                                        Utils.setDefaultCursor(DomainInspectorPanel.this);
                                        refresh();
                                    }

                                    @Override
                                    protected void hadError(Throwable error) {
                                        SessionMgr.getSessionMgr().handleException(error);
                                        Utils.setDefaultCursor(DomainInspectorPanel.this);
                                        refresh();
                                    }
                                };

                                Utils.setWaitingCursor(DomainInspectorPanel.this);
                                worker.setProgressMonitor(new IndeterminateProgressMonitor(DomainInspectorPanel.this, "Revoking permissions...", ""));
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
                if (!dop.isOwner()) {
                    dopDialog.showForPermission(dop);
                }
            }
        };
        permissionsTable.setAutoResizeColumns(false);
        permissionsTable.addColumn(PERMISSIONS_COLUMN_SUBJECT, PERMISSIONS_COLUMN_SUBJECT, true, false, false, true);
        permissionsTable.addColumn(PERMISSIONS_COLUMN_TYPE, PERMISSIONS_COLUMN_TYPE, true, false, false, true);
        permissionsTable.addColumn(PERMISSIONS_COLUMN_READ, PERMISSIONS_COLUMN_READ, true, false, false, true);
        permissionsTable.addColumn(PERMISSIONS_COLUMN_WRITE, PERMISSIONS_COLUMN_WRITE, true, false, false, true);

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

        tabbedPane.addTab("Permissions", Icons.getIcon("group.png"), permissionsPanel, "Who has access to the selected item");

        // Annotations tab
        annotationsLoadingLabel = createLoadingLabel();
        annotationsPanel = new JPanel(new BorderLayout());
        annotationsView = new AnnotationTablePanel();

        tabbedPane.addTab("Annotations", Icons.getIcon("page_white_edit.png"), annotationsPanel, "Annotations on the selected item");
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

    public void loadDomainObject(final DomainObject domainObject) {
        loadDomainObject(domainObject, null);
    }

    public void loadDomainObject(final DomainObject domainObject, final String defaultTab) {

        this.domainObject = domainObject;

        log.debug("Loading domain object: "+domainObject.getId());
        
        refresh();
                
        // Select the default tab
        if (defaultTab != null) {
            tabbedPane.setSelectedIndex(tabNames.indexOf(defaultTab));
        }
        tabbedPane.setEnabledAt(1, true);
        tabbedPane.setEnabledAt(2, true);
    }

    public void loadPipelineResult(PipelineResult result) {

        log.debug("Loading properties for pipeline result {}", result.getId());
        showAttributesLoadingIndicator();

        this.result = result;
        this.propertySet = new TreeSet<>();

        addProperty("Creation Date", result.getCreationDate());
        addProperty("Filepath", result.getFilepath());
        addProperty("GUID", result.getId());
        addProperty("Name", result.getName());

        if (result instanceof HasAnatomicalArea) {
            HasAnatomicalArea hasAA = (HasAnatomicalArea) result;
            addProperty("Anatomical Area", hasAA.getAnatomicalArea());
        }

        if (result instanceof SampleAlignmentResult) {
            SampleAlignmentResult alignment = (SampleAlignmentResult) result;
            addProperty("Alignment Space", alignment.getAlignmentSpace());
            addProperty("Bounding Box", alignment.getBoundingBox());
            addProperty("Channel Colors", alignment.getChannelColors());
            addProperty("Channel Spec", alignment.getChannelSpec());
            addProperty("Image Size", alignment.getImageSize());
            addProperty("Objective", alignment.getObjective());
            addProperty("Optical Resolution", alignment.getOpticalResolution());
            for (AlignmentScoreType scoretype : alignment.getScores().keySet()) {
                String score = alignment.getScores().get(scoretype);
                addProperty(scoretype.getLabel(), score);
            }
        }

        if (result instanceof SampleProcessingResult) {
            SampleProcessingResult spr = (SampleProcessingResult) result;
            addProperty("Channel Colors", spr.getChannelColors());
            addProperty("Channel Spec", spr.getChannelSpec());
            addProperty("Image Size", spr.getImageSize());
            addProperty("Optical Resolution", spr.getOpticalResolution());
        }

        addPropertiesToTable();
        attributesPanel.removeAll();
        attributesPanel.add(attributesTable, BorderLayout.CENTER);

        tabbedPane.setSelectedIndex(0);
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setEnabledAt(2, false);
    }

    private void loadAttributes() {

        log.debug("Loading properties for domain object {}", domainObject.getId());
        showAttributesLoadingIndicator();

        List<DomainObjectAttribute> searchAttrs = DomainUtils.getSearchAttributes(domainObject.getClass());

        this.propertySet = new TreeSet<>();
        for(DomainObjectAttribute attr : searchAttrs) {
            if (attr.isDisplay()) {
                Object value = null;
                try {
                    value = attr.getGetter().invoke(domainObject);
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    log.error("Error getting value for attribute: " + attr.getName(), e);
                }
                addProperty(attr.getLabel(), value);
            }
        }

        addPropertiesToTable();
        attributesPanel.removeAll();
        attributesPanel.add(attributesTable, BorderLayout.CENTER);
    }

    private void addProperty(String key, Object value) {
        ImmutablePair<String,Object> attrPair = ImmutablePair.of(key, value);
        propertySet.add(attrPair);
    }

    private void addPropertiesToTable() {
        attributesTable.removeAllRows();
        for(ImmutablePair<String,Object> attrPair : propertySet) {
            attributesTable.addRow(attrPair);
        }
        attributesTable.updateTableModel();
        if (firstLoad) {
            attributesTable.autoResizeColWidth();
            firstLoad = false;
        }
    }

    private void loadSubjects() {

        log.debug("Loading subjects for {}", domainObject.getId());

        SimpleWorker worker = new SimpleWorker() {

            private List<Subject> subjects;

            @Override
            protected void doStuff() throws Exception {
                subjects = DomainMgr.getDomainMgr().getSubjects();
            }

            @Override
            protected void hadSuccess() {
                setSubjects(subjects);
                loadPermissions();
                addPermissionButton.setEnabled(ClientDomainUtils.isOwner(domainObject));
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
                ComparisonChain chain = ComparisonChain.start()
                        .compare(o2.isOwner(), o1.isOwner(), Ordering.natural())
                        .compare(o1.getSubjectKey(), o2.getSubjectKey(), Ordering.natural().nullsFirst());
                return chain.result();
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
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                annotations.addAll(model.getAnnotations(Reference.createFor(domainObject)));
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
        DomainUtils.sortSubjects(filtered);
        return filtered;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
        this.subjectMap = new HashMap<>();
        for(Subject subject : subjects) {
            subjectMap.put(subject.getKey(), subject);
        }
    }

    public void refresh() {
        this.domainObject = DomainMgr.getDomainMgr().getModel().getDomainObject(domainObject);
        if (domainObject!=null) {
            loadSubjects();
            loadAttributes();
            loadAnnotations();
        }
        else {
            showNothing();
        }
    }
}
