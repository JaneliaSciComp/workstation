package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityDetailsPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.baseball_card.BaseballCard;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private JTable baseballCardTable;
    private BaseballCardTableModel baseballCardModel;
    private boolean includeSelectorCheckbox;

    public BaseballCardPanel() {
        this( false );
    }

    public BaseballCardPanel( boolean includeSelectorCheckbox ) {
        this.includeSelectorCheckbox = includeSelectorCheckbox;
        establishGui();
    }

    public void setRootedEntities( Collection<RootedEntity> rootedEntities ) {
        cards = new ArrayList<BaseballCard>();
        for ( RootedEntity rootedEntity: rootedEntities ) {
            cards.add( new BaseballCard( rootedEntity.getEntity() ) );
        }
        baseballCardModel.setCards( cards );
    }

    private void establishGui() {
        baseballCardModel = new BaseballCardTableModel( includeSelectorCheckbox );
        JTable baseBallCardTable = new JTable( baseballCardModel );
        baseBallCardTable.setDefaultRenderer( Object.class, new ComponentSelfRenderer() );
        this.setLayout( new BorderLayout() );
        add(baseBallCardTable, BorderLayout.CENTER);
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

//    private static class BaseballCardColumnModel extends DefaultTableColumnModel {
//        public enum ColumnType {
//            Checkbox, Image, Details
//        };
//
//        private ColumnType colType;
//
//        public BaseballCardColumnModel( ColumnType colType ) {
//            this.colType = colType;
//        }
//
//        @Override
//        public Component getTableCellRendererComponent() {
//            Component rtnVal = null;
//
//            return rtnVal;
//        }
//    }

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
            columnClasses.add( EntityDetailsPanel.class );
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
         * No editable cells.
         */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
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

    }

    private static class CheckboxWithData extends JCheckBox {
        private BaseballCard card;
        public CheckboxWithData( BaseballCard card ) {
            super();
            this.card = card;
        }

        public BaseballCard getCard() { return card; }

    }
}
