package org.janelia.it.FlyWorkstation.gui.framework.table;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseForwarder;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A reusable table component with configurable columns.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DynamicTable extends JPanel {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicTable.class);
    
	private static final int DEFAULT_MIN_COLUMN_WIDTH = 100;
    private static final int DEFAULT_MAX_COLUMN_WIDTH = 500;
    
	private final JTable table;
	private final JButton loadMoreButton;
	private final JButton loadAllButton;
    private final JScrollPane scrollPane;
    private final JPanel mainPane;
    
    private final boolean allowRightClickCellSelection;
    private TableModel tableModel;
    
//    private ButtonHeaderRenderer headerRenderer;
//    private JPopupMenu headerPopupMenu;
    
    private List<DynamicColumn> columns = new ArrayList<DynamicColumn>();
    private List<DynamicColumn> displayedColumns = new ArrayList<DynamicColumn>();
    private List<DynamicRow> rows = new ArrayList<DynamicRow>();
    private List<Object> userObjects = new ArrayList<Object>();
    
    private Map<DynamicColumn,TableCellRenderer> renderers = new HashMap<DynamicColumn,TableCellRenderer>();

    private Rectangle currViewRect;
    private boolean hasMoreResults = false;
    private boolean autoLoadResults = false;
    private int minColWidth = DEFAULT_MIN_COLUMN_WIDTH;
    private int maxColWidth = DEFAULT_MAX_COLUMN_WIDTH;
    
    public DynamicTable() {
    	this(true, false);
    }
    
    protected void loadAllResults() {
    	if (!hasMoreResults) {
			return;
    	}
    	loadMoreResults(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				loadAllResults();
				return null;
			}
		});
    }
    
    public DynamicTable(final boolean allowRightClickCellSelection, final boolean sortableByColumn) {
    	
    	this.allowRightClickCellSelection = allowRightClickCellSelection;
    	
    	loadMoreButton = new JButton("Load More");
    	loadMoreButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Utils.setWaitingCursor(DynamicTable.this);
				loadMoreButton.setEnabled(false);
				loadMoreResults(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						Utils.setDefaultCursor(DynamicTable.this);
						loadMoreButton.setEnabled(true);
						return null;
					}
					
				});
			}
		});
    	loadMoreButton.setVisible(false);

    	loadAllButton = new JButton("Load All");
    	loadAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadAllButton.setEnabled(false);
				loadAllResults();
			}
		});
    	loadAllButton.setVisible(false);

    	
        table = new LargeFontTable(UIManager.getDefaults().getFont("Menu.font")) {
    		@Override
    		public TableCellEditor getCellEditor(int row, int col) {
    			TableCellEditor editor = DynamicTable.this.getCellEditor(row, col);
    			return editor==null ? super.getCellEditor(row, col) : editor;
    		}
    		@Override
    		public Class<?> getColumnClass(int column) {
                Class clazz = DynamicTable.this.getColumnClass(column);
                return clazz==null ? super.getColumnClass(column) : clazz;
    		}
    	};
        table.setFillsViewportHeight(true);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);
        table.setAutoCreateRowSorter(sortableByColumn);
        
        table.addMouseListener(new MouseHandler() {	
			@Override
			protected void popupTriggered(MouseEvent e) {
				if (e.isConsumed()) return;
				ListSelectionModel lsm = table.getSelectionModel();
				if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) { 
					// User is not selecting multiple rows, so we can select the cell they right clicked on
					if (allowRightClickCellSelection) {
		                table.setColumnSelectionAllowed(true);
		                int col = table.columnAtPoint(e.getPoint());
		                table.getColumnModel().getSelectionModel().setSelectionInterval(col, col);
					}
					// Select the row being clicked
	                int row = table.rowAtPoint(e.getPoint());
	                table.getSelectionModel().setSelectionInterval(row, row);
				}
				showPopupMenu(e);
				e.consume();
			}
			
			@Override
			protected void singleLeftClicked(MouseEvent e) {
				if (e.isConsumed()) return;
				if (allowRightClickCellSelection) {
	                table.setColumnSelectionAllowed(false);
	                table.getColumnModel().getSelectionModel().setSelectionInterval(0, table.getColumnCount());
				}
                int row = table.rowAtPoint(e.getPoint());
                if (row>=0) {
                    int col = table.columnAtPoint(e.getPoint());
                    // Don't process clicking on editable columns
                    if (getColumns().get(col).isEditable()) return;
                	rowClicked(row);
                }
                else {
                	backgroundClicked();
                }
				e.consume();
			}
			
			@Override
			protected void doubleLeftClicked(MouseEvent e) {
				if (e.isConsumed()) return;
				int col = table.columnAtPoint(e.getPoint());
				// Don't process clicking on editable columns
			    if (getColumns().get(col).isEditable()) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row>=0) {
                	rowDoubleClicked(row);
                }
                else {
                	backgroundClicked();
                }
				e.consume();
			}
        });

        table.addMouseListener(new MouseForwarder(this, "JTable->DynamicTable"));

        JTableHeader header = table.getTableHeader();
        
        header.setReorderingAllowed(false);
//        header.addMouseListener(new HeaderListener(header));
        
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);
//        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
//        scrollPane.getVerticalScrollBar().setBlockIncrement(200);
        
        scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
	        @Override
	        public void adjustmentValueChanged(final AdjustmentEvent e) {
	            SwingUtilities.invokeLater(new Runnable() {
	    			@Override
	    			public void run() {
	    				if (!autoLoadResults) return;
	    		    	final JViewport viewPort = scrollPane.getViewport();
	    		    	Rectangle viewRect = viewPort.getViewRect();
	    		    	if (viewRect.equals(currViewRect)) {
	    		    		return;
	    		    	}
	    		    	currViewRect = viewRect;
	    		    	
	    		    	if (isAtBottom() && hasMoreResults) {
	    		    		loadMoreResults(null);
	    		    	}
	    			}
	    		});
	        }
	    });
        
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.add(loadMoreButton);
        buttonPane.add(loadAllButton);
        buttonPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
        mainPane.add(scrollPane);
        mainPane.add(buttonPane);
        
        setLayout(new BorderLayout());
        add(mainPane, BorderLayout.CENTER);
    }

    private boolean isAtBottom() {
        Adjustable sb = scrollPane.getVerticalScrollBar();
        return sb.getMaximum() == (sb.getValue() + sb.getVisibleAmount());
    }

    /**
     * Implement this to load more results when the user scrolls to the bottom of the table.
     */
    protected void loadMoreResults(Callable<Void> success) {
    }
    
    public JScrollPane getScrollPane() {
    	return scrollPane;
    }

	public void showNothing() {
        removeAll();
        revalidate();
        repaint();
    }
    
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showTable() {
    	removeAll();
        add(mainPane, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
    
    /**
     * Override this to know when a value was edited. 
     * @param dc
     * @param row
     * @param data
     */
    protected void valueChanged(DynamicColumn dc, int row, Object data) {
    }
    
//	private class HeaderListener extends MouseAdapter {
//
//		public void mousePressed(MouseEvent e) {
//			int col = table.getTableHeader().columnAtPoint(e.getPoint());
//			
//			headerPopupMenu = new JPopupMenu();
//			headerPopupMenu.setLightWeightPopupEnabled(true);
//	        
//	        for(DynamicColumn column : columns) {
//	        	//JCheckBoxMenuItem
//	            JMenuItem menuItem = new JMenuItem(column.getName());
//	            headerPopupMenu.add(menuItem);
//	        }
//	        
//	        // Calculate the position of the popup.. Swing does not make this easy
//	        TableColumnModel colModel = table.getTableHeader().getColumnModel();
//	        
//	        int x = 0;
//	        for(int i=0; i<col; i++) {
//	        	if (i>0) x += colModel.getColumnMargin();
//	        	x += colModel.getColumn(i).getWidth();
//	        }
//	        
//	        headerPopupMenu.show((JComponent) e.getSource(), x, table.getTableHeader().getHeight());
//		}
//
//		public void mouseReleased(MouseEvent e) {
//			headerPopupMenu.setVisible(false);
//		}
//	}

    /**
     * Override this method to return a custom cell editor for a given cell.
     */
    public TableCellEditor getCellEditor(int row, int col) {
    	return null;
    }
    
    /**
     * Override this method to return a custom class for a given column.
     */
    public Class<?> getColumnClass(int column) {
        return null;
    }
    
    /**
     * Override this method and call super.createPopupMenu(e) to create the base menu. Then add your custom items
     * to the menu. Item names should begin with two spaces. 
     * @param e
     * @return
     */
    protected JPopupMenu createPopupMenu(MouseEvent e) {

        JTable target = (JTable) e.getSource();
        if (target.getSelectedRow() <0 || target.getSelectedColumn()<0) return null;

        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

		ListSelectionModel lsm = table.getSelectionModel();
		
		if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) { 

	        final String value = target.getValueAt(target.getSelectedRow(), target.getSelectedColumn()).toString();
	        
	        JMenuItem titleMenuItem = new JMenuItem(value);
	        titleMenuItem.setEnabled(false);
	        popupMenu.add(titleMenuItem);
	        
			// Items which are  only available when selecting a single cell
			if (allowRightClickCellSelection) {
		        JMenuItem copyMenuItem = new JMenuItem("  Copy To Clipboard");
		        copyMenuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
			            Transferable t = new StringSelection(value);
			            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
					}
				});
		        popupMenu.add(copyMenuItem);
			}
		}
		else {
	        JMenuItem titleMenuItem = new JMenuItem("(Multiple Items Selected)");
	        titleMenuItem.setEnabled(false);
	        popupMenu.add(titleMenuItem);
		}
		
		return popupMenu;
    }
    
    protected void showPopupMenu(MouseEvent e) {
    	JPopupMenu popupMenu = createPopupMenu(e);
        if (popupMenu!=null) popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }   

    /**
     * Override this method to provide custom functionality for row left clicking.
     * @param row
     */
    protected void rowClicked(int row) {
    }

	/**
	 * Override this method to provide custom functionality for row double left clicking. 
	 * @param row
	 */
	protected void rowDoubleClicked(int row) {
	}
	
    /**
     * Override this method to provide custom functionality for background clicking.
     */
    protected void backgroundClicked() {
    }
    
    public DynamicColumn addColumn(String label) {
    	return addColumn(label, label, true, false, false, false);
    }
    
    /**
     * Add a column to the table.
     * @param name
     * @param visible
     * @param editable
     * @param switchable
     * @return
     */
    public DynamicColumn addColumn(String name, String label, boolean visible, boolean editable, boolean switchable, boolean sortable) {
    	DynamicColumn col = new DynamicColumn(name, label, visible, editable, switchable, sortable);
    	columns.add(col);
    	return col;
    }
    
    /**
     * Clear the column configuration.
     */
    public void clearColumns() {
    	columns.clear();
    }

    /**
     * Get a list of all columns.
     * @param name
     * @return
     */
    public List<DynamicColumn> getColumns() {
    	return columns;
    }

    /**
     * Get a list of currently displayed columns.
     * @param name
     * @return
     */
    public List<DynamicColumn> getDisplayedColumns() {
    	return displayedColumns;
    }
    
    /**
     * Get the column with the given name.
     * @param name
     * @return
     */
    public DynamicColumn getColumn(String name) {
    	for(DynamicColumn col : columns) {
    		if (col.getName().equals(name)) {
    			return col;
    		}
    	}
    	return null;
    }
    
    public TableColumn getTableColumn(DynamicColumn column) {
    	int index = columns.indexOf(column);
    	if (index<0) return null;
    	return table.getColumnModel().getColumn(index);
    }
    
    /**
     * Add a new row to the table. Each row represents a user object.
     * @param userObject
     * @return
     */
    public DynamicRow addRow(Object userObject) {
    	DynamicRow row = new DynamicRow(userObject);
    	rows.add(row);
    	userObjects.add(userObject);
    	return row;
    }
    
    /**
     * Returns a list of the rows in display order.
     * @return
     */
    public List<DynamicRow> getRows() {
    	return rows;
    }
    
    public List<Object> getUserObjects() {
    	return userObjects;
    }

    public void removeRow(DynamicRow row) {
    	userObjects.remove(rows.indexOf(row));
    	rows.remove(row);
    	updateTableModel();
    }
    
    public void removeAllRows() {
    	userObjects.clear();
    	rows.clear();
    	updateTableModel();
    }
    
    /**
     * Returns the first currently selected row.
     * @return
     */
    public DynamicRow getCurrentRow() {
    	for (int i : table.getSelectedRows()) {
    		return rows.get(table.convertRowIndexToModel(i));
    	}
    	return null;
    }

    /**
     * Returns all of the selected rows.
     * @return
     */
    public List<DynamicRow> getSelectedRows() {
    	List<DynamicRow> selected = new ArrayList<DynamicRow>();
        for (int i : table.getSelectedRows()) {
            int mi = table.convertRowIndexToModel(i);
        	selected.add(rows.get(mi));
        }	
        return selected;
    }

    /**
     * Returns all of the selected user objects.
     * @return
     */
    public List<Object> getSelectedObjects() {
    	List<Object> selected = new ArrayList<Object>();
        for (int i : table.getSelectedRows()) {
            int mi = table.convertRowIndexToModel(i);
        	selected.add(userObjects.get(mi));
        }	
        return selected;
    }
    
    public boolean navigateToRowWithObject(Object userObject) {
    	int i = 0;
    	for(DynamicRow row : rows) {
    		if (row.getUserObject().equals(userObject)) {
    			int vi = table.convertRowIndexToView(i);
    			table.getSelectionModel().setSelectionInterval(vi, vi);
    			return true;
    		}
    		i++;
    	}
    	table.getSelectionModel().clearSelection();
    	return false;
    }

    public DynamicRow getRowForUserObject(Object userObject) {
    	return rows.get(userObjects.indexOf(userObject));
    }
    
    /**
     * Override this method to extract the correct value from the user object for the given column.
     * @param userObject
     * @param column
     * @return
     */
    public abstract Object getValue(Object userObject, DynamicColumn column);
    
    /**
     * Synchronous method for updating the JTable model. Should be called from the EDT.
     */
    public synchronized void updateTableModel() {

    	displayedColumns.clear();
    	
        // Data formatted for the JTable
        Vector<String> columnNames = new Vector<String>();
        
        for(DynamicColumn column : columns) {
        	if (column.isVisible()) {
        		columnNames.add(column.getLabel());
        		displayedColumns.add(column);
        	}
        }
        
        Vector<Vector<Object>> data = new Vector<Vector<Object>>();

        // Build the data in column order
        for (DynamicRow row : rows) {
            Vector<Object> rowData = new Vector<Object>();

            for(DynamicColumn column : displayedColumns) {
        		Object value = getValue(row.getUserObject(), column);
        		rowData.add(value == null ? "" : value);
            }

            data.add(rowData);
        }
        
        tableModel = new DefaultTableModel(data, columnNames) {
            public boolean isCellEditable(int rowIndex, int mColIndex) {
            	try {
            		return columns.get(mColIndex).isEditable();
            	}
            	catch (ArrayIndexOutOfBoundsException e) {
            		log.error("Error getting column",e);
            		return false;
            	}
            }
        };

        tableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                TableModel model = (TableModel)e.getSource();
                DynamicColumn dc = getColumns().get(column);
                Object data = model.getValueAt(row, column);
                valueChanged(dc, row, data);
            } 
        });
        
        table.setModel(tableModel);
        
        TableColumnModel colModel = table.getTableHeader().getColumnModel();
        for(DynamicColumn column : displayedColumns) {
        	TableCellRenderer renderer = renderers.get(column);
        	if (renderer == null) continue;
        	int c = displayedColumns.indexOf(column);
    		if (c>=0) {
    	  		colModel.getColumn(c).setCellRenderer(renderer);
    		}
        }

        autoResizeColWidth();
    }
    
    public void setColumnRenderer(DynamicColumn column, TableCellRenderer renderer) {
    	renderers.put(column, renderer);
    }
    
    /**
     * Returns the underlying JTable.
     *
     * @return
     */
    public JTable getTable() {    	
        return table;
    }

    /**
     * Returns the underlying table model.
     *
     * @return
     */
    public TableModel getTableModel() {
        return tableModel;
    }
    
    public void setMoreResults(boolean moreResults) {
    	this.hasMoreResults = moreResults;
    	loadMoreButton.setVisible(hasMoreResults);
    	loadMoreButton.setEnabled(hasMoreResults);
    	loadAllButton.setVisible(hasMoreResults);
    	loadAllButton.setEnabled(hasMoreResults);
    	revalidate();
    	repaint();
    }
    
    public int getMinColWidth() {
		return minColWidth;
	}

	public void setMinColWidth(int minColWidth) {
		this.minColWidth = minColWidth;
	}

	public int getMaxColWidth() {
		return maxColWidth;
	}

	public void setMaxColWidth(int maxColWidth) {
		this.maxColWidth = maxColWidth;
	}

	public boolean isAutoLoadResults() {
		return autoLoadResults;
	}

	public void setAutoLoadResults(boolean autoLoadResults) {
		this.autoLoadResults = autoLoadResults;
		loadMoreButton.setVisible(!autoLoadResults);
    	loadAllButton.setVisible(!autoLoadResults);
	}

	/**
     * Borrowed from http://www.pikopong.com/blog/2008/08/13/auto-resize-jtable-column-width/
     *
     * @param table table to work against
     */
    public void autoResizeColWidth() {

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        DefaultTableCellRenderer defaultRenderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();

        int margin = 5;

        for (int c = 0; c < table.getColumnCount(); c++) {
            DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn col = colModel.getColumn(c);
            int width;

            // Get width of column header
            TableCellRenderer renderer = col.getHeaderRenderer();
            if (renderer == null) renderer = defaultRenderer;

            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
            width = comp.getPreferredSize().width;

            // Get maximum width of column data
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, c);
                comp = table.prepareRenderer(renderer, r, c);
                
                width = Math.max(width, (int)comp.getPreferredSize().getWidth());
            }

            width += 2 * margin;
            if (width>maxColWidth) width=maxColWidth;
            if (width<minColWidth) width=minColWidth;
            col.setPreferredWidth(width);
        }

        defaultRenderer.setHorizontalAlignment(SwingConstants.LEFT);
    }

}
