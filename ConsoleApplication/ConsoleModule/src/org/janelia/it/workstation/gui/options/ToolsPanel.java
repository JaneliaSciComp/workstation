package org.janelia.it.workstation.gui.options;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.prefs.BackingStoreException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolInfo;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.workstation.gui.util.GroupedKeyValuePanel;
import org.janelia.it.workstation.shared.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool configuration option panel ported over from the original ToolConfigurationDialog.
 *
 * @author safford
 * @author rokickik
 */
final class ToolsPanel extends javax.swing.JPanel {

    private static final Logger log = LoggerFactory.getLogger(ToolsPanel.class);

    private final ToolsOptionsPanelController controller;
    private final GroupedKeyValuePanel mainPanel;

    private JFileChooser toolFileChooser;
    private DefaultTableModel model;
    private int selectedRow;

    ToolsPanel(ToolsOptionsPanelController controller) {
        this.controller = controller;
        initComponents();

        this.mainPanel = new GroupedKeyValuePanel();
        add(mainPanel, BorderLayout.CENTER);

        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.addColumn("Tool");
        model.addColumn("Location");
        refreshTable();
        final JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                printDebugData(table);
                selectedRow = table.getSelectedRow();
            }
        });
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        toolFileChooser = new JFileChooser();

        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                int returnVal = toolFileChooser.showOpenDialog(ToolsPanel.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = toolFileChooser.getSelectedFile();
                    log.info("Opening: " + file.getName() + ".");
                }
                else {
                    log.info("Open command cancelled by user.");
                }

                if (toolFileChooser.getSelectedFile().exists()) {
                    ToolInfo toolTest = ToolMgr.getTool(ToolInfo.TOOL_PREFIX + ToolInfo.USER + "." + toolFileChooser.getSelectedFile().getName().replaceAll("\\.", ""));
                    if (null == toolTest) {
                        try {
                            ToolMgr.getToolMgr().addTool(new ToolInfo(toolFileChooser.getSelectedFile().getName(), toolFileChooser.getSelectedFile().getAbsolutePath(), "brain.png"));
                        }
                        catch (Exception e) {
                            SessionMgr.getSessionMgr().handleException(e);
                        }
                        refreshTable();
                    }
                    else {
                        JOptionPane.showMessageDialog(ToolsPanel.this, "The tool has already been added.", "ToolInfo Already Added", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        });

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    String name = model.getValueAt(selectedRow, 0).toString();
                    String path = model.getValueAt(selectedRow, 1).toString();
                    ToolInfo tool = ToolMgr.getTool(name);
                    EditDialog editDialog = new EditDialog(name, path);
                    model.setValueAt(editDialog.getNameText(), selectedRow, 0);
                    model.setValueAt(editDialog.getPathText(), selectedRow, 1);
                    tool.setName(editDialog.getNameText());
                    tool.setPath(editDialog.getPathText());
                    ToolMgr.getToolMgr().fireToolsChanged();
                }
                catch (BackingStoreException e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });

        JButton clearTool = new JButton("Delete");
        clearTool.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeTool();
            }
        });

        mainPanel.addSeparator("External Tool Configuration");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(clearTool);
        mainPanel.addItem(buttonPanel);
        mainPanel.addItem(scrollPane, "width 100:400:2000, height 100:300:1000, grow");
    }

    private void removeTool() {
        String key = model.getValueAt(selectedRow, 0).toString();
        ToolInfo tmpTool = ToolMgr.getTool(key);
        if (!ToolInfo.SYSTEM.equals(tmpTool.getSourceFile())) {
            try {
                ToolMgr.getToolMgr().removeTool(tmpTool);
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
            model.removeRow(selectedRow);
        }
    }

    void refreshTable() {
        int tmp = model.getRowCount();
        if (0 != tmp){
            for (int i = tmp; i > 0; i--){
                model.removeRow(i - 1);
            }
        }

        for (int i = 0; i < ToolMgr.getTools().keySet().size(); i++) {
            String tmpKey = ToolMgr.getTools().keySet().toArray()[i].toString();
            ToolInfo tmpTool = ToolMgr.getTools().get(tmpKey);
            model.addRow(new Object[]{tmpTool.getName(), tmpTool.getPath()});
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    void load() {
        refreshTable();
    }

    void store() {
        // TODO: we should change the ToolMgr actions so that they save a local state which is then persisted here.
    }

    boolean valid() {
        // TODO check whether form is consistent and complete
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    private void printDebugData(JTable table) {
        int numRows = table.getRowCount();
        int numCols = table.getColumnCount();
        TableModel model = table.getModel();

        log.info("Value of data: ");
        for (int i = 0; i < numRows; i++) {
            System.out.print("    row " + i + ":");
            for (int j = 0; j < numCols; j++) {
                System.out.print("  " + model.getValueAt(i, j));
            }
            System.out.println();
        }
        System.out.println("--------------------------");
    }

    public class EditDialog extends JDialog {
        private String nameText;
        private String pathText;
        private JTextField nameTextField;
        private JTextField pathTextField;
        private JFileChooser fileChooser;

        public EditDialog(String name, String path) throws BackingStoreException {
            super((JFrame)null, "Edit", true);
            nameText = name;
            pathText = path;
            nameTextField = new JTextField(40);
            nameTextField.setText(name);
            pathTextField = new JTextField(40);
            pathTextField.setText(path);
            getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
            setSize(400, 400);

            fileChooser  = new JFileChooser();
            fileChooser.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            });

            JButton filePathButton = null;
            try {
                filePathButton = new JButton(Utils.getClasspathImage("magnifier.png"));
            }
            catch (FileNotFoundException e) {
                log.error("Could not find icon magnifier.png",e);
            }
            if (filePathButton == null) {
                filePathButton = new JButton("...");
            }

            filePathButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fileChooser.showOpenDialog(EditDialog.this);
                }
            });

            JButton _saveButton = new JButton("Save");
            _saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    if(!nameTextField.getText().isEmpty() && !pathTextField.getText().isEmpty()){
                        nameText = nameTextField.getText();
                        pathText = pathTextField.getText();
                        EditDialog.this.setVisible(false);
                    }
                    else{
                        JOptionPane.showMessageDialog(ToolsPanel.this, "Name and/or Path cannot be empty", "Edit Exception", JOptionPane.WARNING_MESSAGE);
                        EditDialog.this.setVisible(false);
                    }
                }
            });

            JButton _closeButton = new JButton("Close");
            _closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    EditDialog.this.setVisible(false);
                }
            });

            JPanel namePanel = new JPanel();
            namePanel.add(new JLabel("Name:"));
            namePanel.add(nameTextField);
            JPanel pathPanel = new JPanel();
            pathPanel.add(new JLabel("Path:"));
            pathPanel.add(pathTextField);
            pathPanel.add(filePathButton);
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(_saveButton);
            buttonPanel.add(_closeButton);
            getContentPane().add(namePanel);
            getContentPane().add(pathPanel);
            getContentPane().add(buttonPanel);
            pack();
            Toolkit tk = Toolkit.getDefaultToolkit();
            Dimension screenSize = tk.getScreenSize();
            int screenHeight = screenSize.height;
            int screenWidth = screenSize.width;
            setLocation((screenWidth - getWidth()) / 2, (screenHeight - getHeight()) / 2);
            setVisible(true);

        }

        public String getNameText(){
            return nameText;
        }

        public String getPathText(){
            return pathText;
        }
    }
}
