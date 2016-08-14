package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.gui.listview.table.TableViewerConfiguration;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.panels.ScrollablePanel;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * A dialog for configuring a TableViewerPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TableViewerConfigDialog extends ModalDialog {

    public static final int ERROR_OPTION = -1;
    public static final int CANCEL_OPTION = 0;
    public static final int CHOOSE_OPTION = 1;
    
    private int returnValue = ERROR_OPTION;
    
    private final JPanel mainPanel;
    private final JPanel attrsPanel;

    private TableViewerConfiguration config;
    private List<DomainObjectAttribute> sortedAttrs;
    private Map<DomainObjectAttribute, JCheckBox> checkboxes = new HashMap<>();

    public TableViewerConfigDialog(List<DomainObjectAttribute> attrs) {

        this.config = TableViewerConfiguration.loadConfig();
        setTitle("Table View Configuration");

        JButton checkAllButton = new JButton("Check all");
        checkAllButton.setFocusable(false);
        checkAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setAll(true);
            }
        });

        JButton uncheckAllButton = new JButton("Uncheck all");
        uncheckAllButton.setFocusable(false);
        uncheckAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setAll(false);
            }
        });

        JPanel topPane = new JPanel();
        topPane.setLayout(new BoxLayout(topPane, BoxLayout.LINE_AXIS));
        topPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        topPane.add(checkAllButton);
        topPane.add(uncheckAllButton);
        topPane.add(Box.createHorizontalGlue());

        attrsPanel = new ScrollablePanel();
        attrsPanel.setLayout(new BoxLayout(attrsPanel, BoxLayout.PAGE_AXIS));
        attrsPanel.setOpaque(false);

        this.sortedAttrs = new ArrayList<>(new HashSet<>(attrs));
        Collections.sort(sortedAttrs, new Comparator<DomainObjectAttribute>() {
            @Override
            public int compare(DomainObjectAttribute o1, DomainObjectAttribute o2) {
                return ComparisonChain.start()
                        .compare(o1.getLabel(), o2.getLabel(), Ordering.natural().nullsFirst())
                        .result();
            }
        });

        for (final DomainObjectAttribute attr : sortedAttrs) {

            String label = attr.getLabel();
            final JCheckBox checkBox = new JCheckBox(new AbstractAction(label) {
                public void actionPerformed(ActionEvent e) {
                    JCheckBox cb = (JCheckBox) e.getSource();
                    config.setColumnVisibility(attr.getName(), cb.isSelected());
                }
            });

            checkboxes.put(attr, checkBox);

            checkBox.setToolTipText(attr.getLabel());
            checkBox.setSelected(config.isColumnVisible(attr.getName()));
            attrsPanel.add(checkBox);
        }

        JScrollPane attrScrollPane = new JScrollPane();
        attrScrollPane.setViewportView(attrsPanel);
        attrScrollPane.setBorder(BorderFactory.createEmptyBorder());
        attrScrollPane.setPreferredSize(new Dimension(400, 300));

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPane, BorderLayout.NORTH);
        mainPanel.add(attrScrollPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                returnValue = CANCEL_OPTION;
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

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                returnValue = CANCEL_OPTION;
            }
        });
    }

    private void setAll(boolean state) {
        for (final DomainObjectAttribute attr : sortedAttrs) {
            JCheckBox cb = checkboxes.get(attr);
            cb.setSelected(state);
            config.setColumnVisibility(attr.getName(), state);
        }
    }

    public int showDialog(Component parent) throws HeadlessException {
        ActivityLogHelper.logUserAction("TableViewerConfigDialog.showDialog");
        packAndShow();
        return returnValue;
    }

    private void saveAndClose() {

        Utils.setWaitingCursor(TableViewerConfigDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                config.save();
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(TableViewerConfigDialog.this);
                returnValue = CHOOSE_OPTION;
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(TableViewerConfigDialog.this);
                returnValue = ERROR_OPTION;
                setVisible(false);
            }
        };

        worker.execute();
    }

    public TableViewerConfiguration getConfig() {
        return config;
    }
}
