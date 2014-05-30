package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.ForbiddenEntity;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.events.EntityChangeEvent;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.access.Accessibility;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog changing the Sort Criteria for an entity or set of entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetSortCriteriaDialog extends ModalDialog implements Accessibility {

    private static final Logger log = LoggerFactory.getLogger(SetSortCriteriaDialog.class);

    public static final String SORT_CRITERIA_PROP_PREFIX = "SortCriteria";

    /**
     * An attribute must be present on this percentage of child entities in order to be considered a sortable field
     */
    private static final float PERCENT_PRESENT = 0.8f;

    private static final String[] intrinsicFields = {EntityConstants.VALUE_SC_GUID, EntityConstants.VALUE_SC_NAME, EntityConstants.VALUE_SC_DATE_CREATED, EntityConstants.VALUE_SC_DATE_UPDATED};
    private static final String DEFAULT_SORT_VALUE = "Default";

    private JPanel attrPanel;
    private JComboBox sortingFieldCombobox;
    private JComboBox sortingOrderCombobox;
    private DefaultComboBoxModel sortingFieldModel;
    private DefaultComboBoxModel sortingOrderModel;
    private Entity entity;

    public SetSortCriteriaDialog() {

        setTitle("Set Sorting Criteria");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        sortingFieldCombobox = new JComboBox();
        sortingFieldCombobox.setEditable(false);
        sortingFieldCombobox.setToolTipText("Choose sorting field");

        sortingFieldModel = (DefaultComboBoxModel) sortingFieldCombobox.getModel();
        sortingFieldModel.addElement(DEFAULT_SORT_VALUE);
        for (String field : intrinsicFields) {
            sortingFieldModel.addElement(field);
        }

        sortingOrderCombobox = new JComboBox();
        sortingOrderCombobox.setEditable(false);
        sortingOrderCombobox.setToolTipText("Choose sort order");

        sortingOrderModel = (DefaultComboBoxModel) sortingOrderCombobox.getModel();
        sortingOrderModel.addElement(EntityConstants.VALUE_SC_SORT_ORDER_ASC);
        sortingOrderModel.addElement(EntityConstants.VALUE_SC_SORT_ORDER_DESC);
        sortingOrderModel.setSelectedItem(EntityConstants.VALUE_SC_SORT_ORDER_ASC);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showForEntity(final Entity entity) {

        this.entity = entity;

        if (!EntityUtils.areLoaded(entity.getEntityData())) {
            Utils.setWaitingCursor(SessionMgr.getMainFrame());
            try {
                ModelMgr.getModelMgr().loadLazyEntity(entity, false);
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
            Utils.setDefaultCursor(SessionMgr.getMainFrame());
        }

        // Find common attributes in child entities that the user can sort by
        Map<String, Integer> attrCounts = new HashMap<String, Integer>();
        for (Entity child : entity.getChildren()) {
            if (child instanceof ForbiddenEntity) {
                continue;
            }
            for (EntityData ed : child.getEntityData()) {
                if (ed.getChildEntity() == null) {
                    Integer count = attrCounts.get(ed.getEntityAttrName());
                    if (count == null) {
                        count = 1;
                    }
                    attrCounts.put(ed.getEntityAttrName(), count + 1);
                }
            }
        }

        List<String> attrKeys = new ArrayList<String>(attrCounts.keySet());
        Collections.sort(attrKeys);

        int total = entity.getChildren().size();
        for (String attr : attrKeys) {
            int count = attrCounts.get(attr);
            if ((float) count / (float) total > PERCENT_PRESENT) {
                sortingFieldModel.addElement(attr);
            }
        }

        attrPanel.removeAll();

        attrPanel.add(new JLabel("Sort by: "), "");
        attrPanel.add(sortingFieldCombobox, "wrap");
        attrPanel.add(new JLabel("Sort order: "), "");
        attrPanel.add(sortingOrderCombobox, "wrap");

        String currCriteria = ModelMgr.getModelMgr().getSortCriteria(entity.getId());
        if (!StringUtils.isEmpty(currCriteria)) {
            String sortField = currCriteria.substring(1);
            String sortOrder = currCriteria.startsWith("-") ? EntityConstants.VALUE_SC_SORT_ORDER_DESC : EntityConstants.VALUE_SC_SORT_ORDER_ASC;
            sortingFieldModel.setSelectedItem(sortField);
            sortingOrderModel.setSelectedItem(sortOrder);
        }

        packAndShow();
    }

    private void saveAndClose() {

        Utils.setWaitingCursor(SessionMgr.getMainFrame());

        final String sortField = (String) sortingFieldCombobox.getSelectedItem();
        final String sortOrder = (String) sortingOrderCombobox.getSelectedItem();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (StringUtils.isEmpty(sortField) || DEFAULT_SORT_VALUE.equals(sortField)) {
                    String currCriteria = ModelMgr.getModelMgr().getSortCriteria(entity.getId());
                    if (currCriteria != null) {
                        ModelMgr.getModelMgr().saveSortCriteria(entity.getId(), null);
                    }
                }
                else {
                    String order = EntityConstants.VALUE_SC_SORT_ORDER_DESC.equals(sortOrder) ? "-" : "+";
                    String sortCriteria = order + sortField;
                    ModelMgr.getModelMgr().saveSortCriteria(entity.getId(), sortCriteria);
                }
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
                ModelMgr.getModelMgr().postOnEventBus(new EntityChangeEvent(entity));
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
            }
        };
        worker.execute();

        setVisible(false);
    }

    public boolean isAccessible() {
        return true;
    }
}
