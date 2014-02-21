package org.janelia.it.FlyWorkstation.gui.framework.bookmark;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.swing.table.SortButtonRenderer;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;


public class BookmarkTableDialog extends JDialog {
    private static final String CLOSE_DIALOG_AFTER_NAVIGATION = "CloseDialogUponNavigation";
    private int sortCol = 0;
    private boolean sortAsc = true;
    private final int ROW_HEIGHT = 15;
    private static final String SPECIES       = "Species";
    private static final String TYPE          = "Type";
    private static final String SEARCH_VALUE  = "Search Value";
    private static final String OID_COLUMN    = "OID";
    private static final String COMMENTS      = "Comments";
    private static final String URL_COLUMN    = "URL";

    private final String[] names={SPECIES, TYPE, SEARCH_VALUE, OID_COLUMN,
            COMMENTS};
    private Vector data;
    private TreeMap bookmarkMap = BookmarkMgr.getBookmarkMgr().getBookmarks();
    JFrame parentFrame = new JFrame();
    JPanel panel1 = new JPanel();
    JCheckBox closeCheckBox = new JCheckBox("Close Dialog Upon Navigation");
    JTable table;
    JScrollPane jsp;
    JPanel jPanel2 = new JPanel();
    JButton deleteButton = new JButton();
    JButton editButton = new JButton();
    JButton closeButton = new JButton();
    JButton navigateButton = new JButton();
    BookmarkInfo selectedBookmark;

    public BookmarkTableDialog(JFrame parentFrame, TreeMap bookmarkMap) {
        super(parentFrame, "Bookmarks", false);
        this.parentFrame = parentFrame;
        this.bookmarkMap=bookmarkMap;
        updateData();

        // Create the table.
        jsp = createTable();

        try  {
            jbInit();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

        if (SessionMgr.getSessionMgr().getModelProperty(CLOSE_DIALOG_AFTER_NAVIGATION)==null) {
            SessionMgr.getSessionMgr().setModelProperty(CLOSE_DIALOG_AFTER_NAVIGATION,
                    Boolean.TRUE);
            closeCheckBox.setSelected(true);
        }
        else {
            boolean tmpBoolean = ((Boolean)SessionMgr.getSessionMgr().
                    getModelProperty(CLOSE_DIALOG_AFTER_NAVIGATION)).booleanValue();
            closeCheckBox.setSelected(tmpBoolean);
        }

        closeCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                SessionMgr.getSessionMgr().setModelProperty(CLOSE_DIALOG_AFTER_NAVIGATION,
                        new Boolean(closeCheckBox.isSelected()));
            }
        });
        table.setAutoResizeMode(2);
        table.setSelectionMode(0);
        table.setRowSelectionAllowed(true); ;
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(ROW_HEIGHT);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        MyComparator comp = new MyComparator(sortCol,sortAsc);
        Collections.sort(data,comp);
        ((MyTableModel)table.getModel()).fireTableDataChanged();
        table.repaint();

        // Select the initial item.
        if (table.getRowCount()>0) table.setRowSelectionInterval(0,0);

        this.pack();
        this.setSize(600, 200);
        this.setLocationRelativeTo(parentFrame);
        this.setVisible(true);
    }

    private JScrollPane createTable() {
        // Create a model of the data.
        TableModel dataModel = new MyTableModel();
        // Create the table
        table = new JTable(dataModel);
        table.getTableHeader().addMouseListener(new ColumnListener(table));
        TableColumnModel columnModel = table.getColumnModel();
        final SortButtonRenderer headerRenderer = new SortButtonRenderer();
        int i = dataModel.getColumnCount();
        for (int j = 0; j < i; j++) {
            columnModel.getColumn(j).setHeaderRenderer(headerRenderer);
        }

        // Listen to mouse press on the headers
        final JTableHeader header = table.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int col = header.columnAtPoint(e.getPoint());
                headerRenderer.setPressedColumn(col);
                headerRenderer.setSelectedColumn(col);
                header.repaint();

                if (header.getTable().isEditing()) {
                    header.getTable().getCellEditor().stopCellEditing();
                }
            }

            public void mouseReleased(MouseEvent e) {
                // Clear the selection.  Otherwise the header column will be
                // toggled down
                headerRenderer.setPressedColumn(-1);
                header.repaint();
            }
        });


        // Turn off the default action for Enter.  Do not want to toggle down list.
        // Navigate to the item selected.
        InputMap tmpMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tmpMap = tmpMap.getParent();
        tmpMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        table.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e){
                if (e.getKeyChar()=='\n') {
                    navigateToRowItem();
                }
            }
            public void keyPressed(KeyEvent e){}
            public void keyReleased(KeyEvent e){}
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    navigateToRowItem();
                }
            }
        });

        JScrollPane scrollpane = new JScrollPane(table);
        return scrollpane;
    }

    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            dispose();
        }
    }

    void jbInit() throws Exception {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        deleteButton.setText("Delete");
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteButton_actionPerformed(e);
            }
        });
        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeButton_actionPerformed(e);
            }
        });

        editButton.setText("Edit");
        editButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editButton_actionPerformed(e);
            }
        });

        navigateButton.setText("Navigate");
        navigateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navigateToRowItem();
            }
        });

        panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
        JPanel closePanel = new JPanel();
        closePanel.setLayout(new BoxLayout(closePanel, BoxLayout.X_AXIS));
        closePanel.add(Box.createHorizontalStrut(10));
        closePanel.add(closeCheckBox);
        closePanel.add(Box.createHorizontalGlue());

        jPanel2.setLayout(new BoxLayout(jPanel2, BoxLayout.X_AXIS));
        jPanel2.add(Box.createHorizontalGlue());
        jPanel2.add(navigateButton);
        jPanel2.add(Box.createHorizontalStrut(10));
        jPanel2.add(editButton);
        jPanel2.add(Box.createHorizontalStrut(10));
        jPanel2.add(deleteButton);
        jPanel2.add(Box.createHorizontalStrut(10));
        jPanel2.add(closeButton);
        jPanel2.add(Box.createHorizontalGlue());

        jsp.getViewport().add(table, null);
        panel1.add(Box.createVerticalStrut(5));
        panel1.add(jsp);
        panel1.add(Box.createVerticalStrut(5));
        panel1.add(closePanel);
        panel1.add(Box.createVerticalStrut(5));
        panel1.add(jPanel2);
        panel1.add(Box.createVerticalStrut(10));
        panel1.add(Box.createVerticalGlue());

        getContentPane().add(panel1);
        this.pack();
        this.repaint();
    }


    private void closeButton_actionPerformed(ActionEvent evt) {
        this.dispose();
    }


    private void deleteButton_actionPerformed(ActionEvent evt){
        if (table.getSelectedRow()<0) return;
        BookmarkInfo tmpInfo = (BookmarkInfo)data.get(table.getSelectedRow());
        BookmarkMgr.getBookmarkMgr().deleteBookmark(tmpInfo);
        BookmarkMgr.getBookmarkMgr().fireBookmarksChanged();
        updateData();
        ((MyTableModel)table.getModel()).fireTableDataChanged();
        ((MyTableModel)table.getModel()).sortAndScrollToItem("");
    }


    private void editButton_actionPerformed(ActionEvent evt){
        int row = table.getSelectedRow();
        if (row<0) return;
        BookmarkInfo tmpInfo = (BookmarkInfo)data.get(row);
        new BookmarkPropertyDialog(parentFrame, "Bookmarks", true, tmpInfo);
        ((MyTableModel)table.getModel()).fireTableDataChanged();
    }


    private void navigateToRowItem(){
        int row = table.getSelectedRow();
        if (row<0) return;
        BookmarkInfo tmpInfo = (BookmarkInfo)data.get(row);
        BookmarkMgr.getBookmarkMgr().selectBookmark(tmpInfo);
        if (closeCheckBox.isSelected()) BookmarkTableDialog.this.dispose();
    }


    private void updateData() {
        data=new Vector();
        TreeMap tmpMap = BookmarkMgr.getBookmarkMgr().getBookmarks();
        for (Iterator it=tmpMap.keySet().iterator();it.hasNext();) {
            data.add(tmpMap.get(it.next()));
        }
        this.repaint();
    }

    private class MyTableModel extends AbstractTableModel {
        public int getColumnCount() { return names.length; }
        public int getRowCount() { return data.size();}
        public String getColumnName(int column) {
            return names[column];
        }
        public Class getColumnClass(int c) {return getValueAt(0, c).getClass();}
        public boolean isCellEditable(int row, int col) { return false; }

        public Object getValueAt(int row, int col) {
            BookmarkInfo tmpInfo = (BookmarkInfo)data.get(row);
            switch (col) {
                // Species Name
                case 0: { return tmpInfo.getSpecies(); }
                case 1: { return tmpInfo.getBookmarkType(); }
                case 2: { return tmpInfo.getSearchValue(); }
                case 3: { return tmpInfo.getId().toString(); }
                case 4: { return tmpInfo.getComments(); }
            }
            return null;
        }

        public void setValueAt(Object aValue, int row, int column) {}

        public void sortAndScrollToItem(String rowName) {
            MyComparator comp = new MyComparator(sortCol,sortAsc);
            Collections.sort(data,comp);
            ((MyTableModel)table.getModel()).fireTableDataChanged();
            if (!rowName.equals("")) {
                for (int x=0; x<table.getRowCount();x++) {
                    if (((String)table.getModel().getValueAt(x,0)).equals(rowName)) {
                        table.setRowSelectionInterval(x,x);
                        Rectangle rect = table.getCellRect(x, 0, true);
                        table.scrollRectToVisible(rect);
                        table.repaint();
                        return;
                    }
                }
            }
            table.repaint();
        }
    }


    /**
     * MouseListener for mouse clicks on column headers to sort the table
     */
    class ColumnListener extends MouseAdapter {
        protected JTable table;

        public ColumnListener(JTable table) {
            this.table = table;
        }

        public void mouseClicked(MouseEvent e) {
            // Figure out which column header was clicked and sort on that column
            String rowName = new String("");
            int targetRow = table.getSelectedRow();
            if (targetRow >= 0 && targetRow < table.getRowCount())
                rowName = ((BookmarkInfo)data.get(targetRow)).getName();

            TableColumnModel colModel = table.getColumnModel();
            int colModelIndex = colModel.getColumnIndexAtX(e.getX());
            int modelIndex = colModel.getColumn(colModelIndex).getModelIndex();
            if (modelIndex < 0)
                return;
            if (sortCol==modelIndex)
                sortAsc = !sortAsc;
            else
                sortCol = modelIndex;
            // Redraw Header
            for (int i=0; i<names.length; i++) {
                TableColumn column = colModel.getColumn(i);
                column.setHeaderValue(table.getModel().getColumnName(column.getModelIndex()));
            }
            table.getTableHeader().repaint();
            ((MyTableModel)table.getModel()).sortAndScrollToItem(rowName);
        }
    }

    /**
     * Comparator to sort columns
     */
    class MyComparator implements Comparator {
        protected int sortCol;
        protected boolean sortAsc;

        public MyComparator(int sortCol, boolean sortAsc) {
            this.sortCol = sortCol;
            this.sortAsc = sortAsc;
        }

        public int compare(Object o1, Object o2) {
            Object compObj1, compObj2;
            int retVal;
            try {
                compObj1 = getValueAt((BookmarkInfo)o1,sortCol);
                compObj2 = getValueAt((BookmarkInfo)o2,sortCol);
                if (compObj1==null || compObj2==null) return 0;
                retVal = 0;
                if (compObj1 instanceof Comparable && compObj2 instanceof Comparable) {
                    Comparable c1 = (Comparable)compObj1;
                    Comparable c2 = (Comparable)compObj2;
                    if (c1 instanceof String && c2 instanceof String) {
                        String s1 = (String)c1;
                        String s2 = (String)c2;
                        retVal = sortAsc ? s1.compareToIgnoreCase(s2) : s2.compareToIgnoreCase(s1);
                    }
                    else retVal = sortAsc ? c1.compareTo(c2) : c2.compareTo(c1);
                }
                else if (compObj1 == null && compObj2 != null)
                    retVal = sortAsc ? -1 : 1;
                else if (compObj2 == null && compObj1 != null)
                    retVal = sortAsc ? 1 : -1;
            }
            catch (Exception ex) { return 0; }
            return retVal;
        }

        protected Object getValueAt(BookmarkInfo bi, int col) {
            switch(col) {
                case 0: return bi.getSpecies();
                case 1: return bi.getBookmarkType();
                case 2: return bi.getSearchValue();
                case 3: return bi.getId().toString();
                case 4: return bi.getComments();
                case 5: return bi.getBookmarkURLText();
                default: return "No Column Defined";
            }
        }
    }

}
