package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.apache.log4j.Logger;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityDetailsPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.baseball_card.BaseballCard;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/5/13
 * Time: 10:30 AM
 *
 * Presents information about an Entity as a Baseball-card like configuration.  On the left side will be the default
 * 2D image (if it exists), and on right part, will be tag/value table of information.  Makes an internal JTable.
 */
public class BaseballCardPanel extends JPanel {
    private List<BaseballCard> cards;
    private BaseballCardTableModel baseballCardModel;
    private boolean includeSelectorCheckbox;
    private int preferredWidth;

    public BaseballCardPanel( int preferredWidth ) {
        this(false, preferredWidth);
    }

    public BaseballCardPanel( boolean includeSelectorCheckbox, int preferredWidth ) {
        this.includeSelectorCheckbox = includeSelectorCheckbox;
        this.preferredWidth = preferredWidth;
        establishGui();
    }

    public void setRootedEntities( Collection<RootedEntity> rootedEntities ) {
        cards = new ArrayList<BaseballCard>();
        for ( RootedEntity rootedEntity: rootedEntities ) {
            cards.add( new BaseballCard( rootedEntity.getEntity() ) );
        }
        baseballCardModel.setCards(cards);
    }

    private void establishGui() {
        baseballCardModel = new BaseballCardTableModel( includeSelectorCheckbox );
        final JTable baseBallCardTable = new JTable( baseballCardModel );
        baseBallCardTable.getColumnModel().getColumn( baseballCardModel.getImageColNo() ).setPreferredWidth( BaseballCard.IMAGE_WIDTH );
        baseBallCardTable.getColumnModel().getColumn( baseballCardModel.getDetailsColNo() ).setPreferredWidth(preferredWidth - BaseballCard.IMAGE_WIDTH);
        JScrollPane cardScroller = new JScrollPane( baseBallCardTable );
        cardScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cardScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        baseBallCardTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        baseBallCardTable.setTableHeader(null);
        baseBallCardTable.setDefaultRenderer(Object.class, new ComponentSelfRenderer());
        baseBallCardTable.setCellSelectionEnabled( true );

        baseBallCardTable.setDefaultEditor(EntityDetailsPanel.class, new ComponentSelfEditor());
        baseBallCardTable.setDefaultEditor(DynamicImagePanel.class, new ComponentSelfEditor());
        baseBallCardTable.setDefaultEditor( AbstractButton.class, new ComponentSelfEditor() );

        baseBallCardTable.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int row = baseBallCardTable.rowAtPoint(event.getPoint());
                int col = baseBallCardTable.columnAtPoint(event.getPoint());

                Component dispatchComponent = SwingUtilities.getDeepestComponentAt(event.getComponent(), event.getX(), event.getY());
                if ( dispatchComponent != baseBallCardTable ) {
                    MouseEvent recursionStoppingEvent = new MouseEvent( dispatchComponent, MouseEvent.MOUSE_CLICKED,
                            new Date().getTime() + 100000,
                            event.getModifiers(), event.getX(), event.getY(), 1, event.isPopupTrigger() );
                    dispatchComponent.dispatchEvent( recursionStoppingEvent );
                }
                Object value = baseballCardModel.getValueAt( row, col );
                if ( value instanceof Component ) {
                    ((Component) value).requestFocus();
                    ((Component) value).dispatchEvent( event );
                }
                super.mouseClicked(event);
            }
        });

        this.setLayout(new BorderLayout());
        add(cardScroller, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                baseBallCardTable.getColumnModel().getColumn(baseballCardModel.getDetailsColNo()).setPreferredWidth(e.getComponent().getWidth() - BaseballCard.IMAGE_WIDTH);
            }
        });
        if ( includeSelectorCheckbox ) {
            baseBallCardTable.getColumnModel().getColumn( baseballCardModel.getCheckboxColNo() ).setPreferredWidth(50);
        }
        baseBallCardTable.setRowHeight( BaseballCard.IMAGE_HEIGHT );
    }

    private static class ComponentSelfRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component rtnVal;
            if ( value instanceof Component ) {
                rtnVal = (Component)value;
            }
            else {
                rtnVal = null;
            }
            return rtnVal;
        }
    }

    private static class ComponentSelfEditor implements TableCellEditor {
        private Component value;

        public ComponentSelfEditor() {
            super();
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                              boolean isSelected,
                                              int row, int column) {
            Component rtnVal;
            if ( value instanceof Component ) {
                rtnVal = (Component)value;
            }
            else {
                rtnVal = null;
            }
            this.value = rtnVal;
            return rtnVal;
        }

        @Override
        public Object getCellEditorValue() {
            return value;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) anEvent;
                int id = me.getID();
                int x = me.getX();
                int y = me.getY();
                Component source = (Component)me.getSource();
                Component dispatchComponent = SwingUtilities.getDeepestComponentAt(source, x, y);
                MouseEvent e2 = new MouseEvent(dispatchComponent, id,
                        me.getWhen() + 100000, me.getModifiers(), x, y, me.getClickCount(),
                        me.isPopupTrigger());
                dispatchComponent.dispatchEvent(e2);
            }
            return false;
        }
        @Override
        public boolean stopCellEditing() {
            return false;
        }

        @Override
        public void cancelCellEditing() {
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
        }
    }

    /**
     * This table model leverages the list of cards to populate the table.
     */
    private static class BaseballCardTableModel extends AbstractTableModel {
        private List<BaseballCard> cards;
        private List<AbstractButton> checkBoxes;

        private List<Integer> columns;
        private List<Class> columnClasses;

        private int checkboxColNo = Integer.MIN_VALUE;
        private int imageColNo = 0;
        private int detailsColNo = 1;

        public BaseballCardTableModel( boolean includeSelectorCheckbox ) {
            columns = new ArrayList<Integer>();
            columnClasses = new ArrayList<Class>();
            int nextCol = 0;

            if ( includeSelectorCheckbox ) {
                columns.add( checkboxColNo = nextCol++ );
                columnClasses.add( AbstractButton.class );
                checkBoxes = new ArrayList<AbstractButton>();

            }
            columns.add( imageColNo = nextCol++ );
            columnClasses.add( DynamicImagePanel.class );

            columns.add( detailsColNo = nextCol++ );
            columnClasses.add(EntityDetailsPanel.class);
        }

        public void setCards( List<BaseballCard> cards ) {
            this.cards = cards;
            if (checkBoxes != null) {
                checkBoxes.clear();
                for ( BaseballCard card: cards ) {
                    JCheckBox checkBox = new CheckboxWithData( card );
                    checkBoxes.add( checkBox );
                }
            }
            this.fireTableDataChanged();
        }

        /** Tells all cards selected. */
        public List<BaseballCard> getSelectedCards() {
            List<BaseballCard> rtnVal = Collections.EMPTY_LIST;
            if ( checkBoxes != null ) {
                rtnVal = new ArrayList<BaseballCard>();
                for ( AbstractButton button: checkBoxes ) {
                    CheckboxWithData cbdata = (CheckboxWithData)button;
                    if ( cbdata.isSelected() ) {
                        rtnVal.add( cbdata.getCard() );
                    }
                }
            }
            else {
                Logger.getLogger( BaseballCardPanel.class )
                        .warn( "Attempted to get selected cards, when no checkboxes are present." );
            }
            return rtnVal;
        }

        @Override
        public int getRowCount() {
            int rowCt = 0;
            if ( cards != null ) {
                rowCt = cards.size();
            }
            return rowCt;
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            Class colClass = String.class;

            if ( columnIndex > columns.size() ) {
                return columnClasses.get( columnIndex );
            }

            return colClass;
        }

        /**
         * Editable cells?
         */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object rtnVal = null;
            if ( cards != null  &&  cards.size() > rowIndex ) {
                BaseballCard card = cards.get( rowIndex );
                if ( columnIndex == checkboxColNo ) {
                    rtnVal = checkBoxes.get( rowIndex );
                }
                else if ( columnIndex == imageColNo) {
                    rtnVal = card.getDynamicImagePanel();
                }
                else if (columnIndex == detailsColNo) {
                    rtnVal = card.getEntityDetailsPanel();
                }
            }
            return rtnVal;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        public int getImageColNo() {
            return imageColNo;
        }

        public int getDetailsColNo() {
            return detailsColNo;
        }

        public int getCheckboxColNo() {
            return checkboxColNo;
        }
    }

    private static class CheckboxWithData extends JCheckBox {
        private BaseballCard card;
        public CheckboxWithData( BaseballCard card ) {
            super();
            this.card = card;
        }

        public BaseballCard getCard() { return card; }

    }

    /*

    public void setImageSize(int width, int height) {
        super.setImageSize(width, height);
        dynamicImagePanel.rescaleImage(width);
        dynamicImagePanel.setPreferredSize(new Dimension(width, height));
    }

    public void setViewable(boolean viewable) {
        super.setViewable(viewable);
        dynamicImagePanel.setViewable(viewable, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Register our image height
                if (dynamicImagePanel.getMaxSizeImage()!=null && dynamicImagePanel.getImage()!=null) {
                    double w = dynamicImagePanel.getImage().getIconWidth();
                    double h = dynamicImagePanel.getImage().getIconHeight();
                    registerAspectRatio(w, h);
                }
                return null;
            }

        });
    }

     */
}
