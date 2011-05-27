package org.janelia.it.FlyWorkstation.gui.framework.console;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/5/11
 * Time: 10:40 AM
 */
public class ToolConfigurationDialog extends JDialog {


    private JTextField _toolTextField;
    private JFileChooser _toolFileChooser;
    private DefaultTableModel model;

    public ToolConfigurationDialog(JFrame parentFrame) throws HeadlessException, BackingStoreException {
        super(parentFrame);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setSize(400,400);
        setModal(true);
        model = new DefaultTableModel();
        model.addColumn("Tool");
        model.addColumn("Location");
        refreshTable();
        final JTable table = new JTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(400, 100));
        table.setFillsViewportHeight(true);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                printDebugData(table);
            }
        });
         //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        _toolTextField = new JTextField(40);
        _toolFileChooser = new JFileChooser();
        JButton _addButton = new JButton("Add");
        _addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (_toolFileChooser.getSelectedFile().exists() &&
                        (null != _toolTextField.getText() && !"".equals(_toolTextField.getText()))) {
                    Preferences prefs = Preferences.userNodeForPackage(getClass());
                    String toolTest = prefs.get(_toolTextField.getText(), null);
                    if (null == toolTest) {
                        prefs.put(_toolTextField.getText(), _toolFileChooser.getSelectedFile().getAbsolutePath());
                        refreshTable();
                    } else {
                        JOptionPane.showMessageDialog(ToolConfigurationDialog.this, "The tool has already been added.",
                                "Tool Already Added", JOptionPane.WARNING_MESSAGE);
                    }
                }

            }
        });
        JButton _findFileButton = new JButton("Find Tool");
        _findFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                int returnVal = _toolFileChooser.showOpenDialog(ToolConfigurationDialog.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = _toolFileChooser.getSelectedFile();
                    System.out.println("Opening: " + file.getName() + ".");
                } else {
                    System.out.println("Open command cancelled by user.");
                }
            }
        });
        JButton _closeButton = new JButton("Close");
        _closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                ToolConfigurationDialog.this.setVisible(false);
            }
        });
        getContentPane().add(new JLabel("Current Tools"));
        getContentPane().add(scrollPane);
        JPanel mainPanel = new JPanel();
        mainPanel.add(new JLabel("Add a New Tool"));
        mainPanel.add(new JLabel("Name:"));
        mainPanel.add(_toolTextField);
        mainPanel.add(_findFileButton);
        mainPanel.add(_addButton);
        mainPanel.add(_closeButton);
        getContentPane().add(mainPanel);
        pack();
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int screenHeight = screenSize.height;
        int screenWidth = screenSize.width;
        setLocation((screenWidth-getWidth()) / 2, (screenHeight-getHeight()) / 2);
        setVisible(true);
    }


    private void refreshTable() {
        try {
            Preferences tmpPrefs = Preferences.userNodeForPackage(getClass());
//            tmpPrefs.exportNode(System.out);
            for (int i = 0; i < tmpPrefs.keys().length; i++) {
                String tmpKey = tmpPrefs.keys()[i];
                model.addRow(new Object[]{tmpKey, tmpPrefs.get(tmpKey, "Unknown")});
            }
        }
        catch (BackingStoreException e) {
            e.printStackTrace();
        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void printDebugData(JTable table) {
        int numRows = table.getRowCount();
        int numCols = table.getColumnCount();
        javax.swing.table.TableModel model = table.getModel();

        System.out.println("Value of data: ");
        for (int i=0; i < numRows; i++) {
            System.out.print("    row " + i + ":");
            for (int j=0; j < numCols; j++) {
                System.out.print("  " + model.getValueAt(i, j));
            }
            System.out.println();
        }
        System.out.println("--------------------------");
    }

}
