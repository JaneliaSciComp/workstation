package org.janelia.workstation.infopanel.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.google.common.eventbus.Subscribe;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.ViewerEvent;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageSnooperDialog extends ModalDialog  {

    private static final Logger log = LoggerFactory.getLogger(MessageSnooperDialog.class);

    private final JButton closeButton;
    private final JTable messageTable;
    private final JPanel buttonPane;
    private final JButton registerButton;
    boolean registered;

    public MessageSnooperDialog() {
        super(FrameworkAccess.getMainFrame());

        setTitle("View neuron change history");
        // set to modeless
        setModalityType(Dialog.ModalityType.MODELESS);

        registerButton = new JButton("Register");
        registerButton.setToolTipText("Start Listening to events");
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (registered)
                    unregister();
                else
                    register();
                registered = !registered;
            }
        });

        closeButton = new JButton("Close");
        closeButton.setToolTipText("Close this window");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(registerButton);
        buttonPane.add(closeButton);


        MessageTableModel tableModel = new MessageTableModel();

        messageTable = new JTable(tableModel);
        messageTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        setLayout(new BorderLayout());
        add(new JScrollPane(messageTable), BorderLayout.CENTER);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void register () {
        registerButton.setText("Unregister");
        ViewerEventBus.registerForEvents(this);
    }

    public void unregister() {
        registerButton.setText("Register");
        ViewerEventBus.unregisterForEvents(this);
    }

    @Subscribe
    private void updateTable(ViewerEvent event) {
        try {
            ((MessageTableModel)messageTable.getModel()).addRow(event);
        } catch (Exception e) {
            log.error("Problem retrieving sample and workspace information", e);
        }
    }

    class MessageTableModel extends AbstractTableModel {
        String[] columnNames = {"Event Name",
                "Source Class",
                "Source Type"};
        List<List<Object>> data = new ArrayList<List<Object>>();
        Map<String,Map<String,Object>> metaData;

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            log.info("ROW AND COL, {},{}",row,col);
            return data.get(row).get(col);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }

        public void addRow(ViewerEvent event) {
            List row = new ArrayList<Object>();
            row.add(event.getClass().getName());
            row.add(event.getSourceClass());
            row.add(event.getSourceMethod());
            row.add("");
            data.add(row);
            this.fireTableDataChanged();
        }

        public void setValueAt(Object value, int row, int col) {
            data.get(row).set(col, value);
            fireTableCellUpdated(row, col);
        }
    }
}

