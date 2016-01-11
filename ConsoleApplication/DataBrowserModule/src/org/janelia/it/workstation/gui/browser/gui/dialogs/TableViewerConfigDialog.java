package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.gui.listview.table.TableViewerConfiguration;
import org.janelia.it.workstation.gui.browser.model.DomainConstants;
import org.janelia.it.workstation.gui.browser.model.DomainObjectAttribute;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
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
    
    private JPanel mainPanel;
    private JPanel attrsPanel;

    private TableViewerConfiguration config;
    private Preference columnsPreference;
    
    public TableViewerConfigDialog(Class<? extends DomainObject> domainClass, List<DomainObjectAttribute> attrs) {

        this.columnsPreference = DomainMgr.getDomainMgr().getPreference(
                DomainConstants.PREFERENCE_CATEGORY_TABLE_COLUMNS, domainClass.getName());
        
        if (columnsPreference==null) {
            this.columnsPreference = new Preference(SessionMgr.getSubjectKey(), DomainConstants.PREFERENCE_CATEGORY_TABLE_COLUMNS, domainClass.getName(), "");
            this.config = new TableViewerConfiguration(); 
        }
        else {
            try {
                this.config = TableViewerConfiguration.deserialize(columnsPreference.getValue());
            }
            catch (Exception e) {
                throw new IllegalStateException("Cannot deserialize column preference: "+columnsPreference.getValue());
            }
        }
        
        setTitle("Table Configuration");

        attrsPanel = new ScrollablePanel();
        attrsPanel.setLayout(new BoxLayout(attrsPanel, BoxLayout.PAGE_AXIS));
        attrsPanel.setOpaque(false);

        for (final DomainObjectAttribute attr : attrs) {
            final JCheckBox checkBox = new JCheckBox(new AbstractAction(attr.getLabel()) {
                public void actionPerformed(ActionEvent e) {
                    JCheckBox cb = (JCheckBox) e.getSource();
                    config.setAttributeVisibility(attr.getName(), cb.isSelected());
                }
            });

            checkBox.setToolTipText(attr.getLabel());
            checkBox.setSelected(config.isVisible(attr.getName()));
//            checkBox.setFont(checkboxFont);
            attrsPanel.add(checkBox);
        }

        JScrollPane attrScrollPane = new JScrollPane();
        attrScrollPane.setViewportView(attrsPanel);
        attrScrollPane.setBorder(BorderFactory.createEmptyBorder());
        attrScrollPane.setPreferredSize(new Dimension(400, 300));

        mainPanel = new JPanel(new BorderLayout());
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

    public int showDialog(Component parent) throws HeadlessException {
        packAndShow();
        return returnValue;
    }

    private void saveAndClose() {

        Utils.setWaitingCursor(TableViewerConfigDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                String value = TableViewerConfiguration.serialize(config);
                columnsPreference.setValue(value);
                DomainMgr.getDomainMgr().savePreference(columnsPreference);
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
