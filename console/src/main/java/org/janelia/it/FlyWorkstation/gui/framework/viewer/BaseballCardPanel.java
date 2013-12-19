package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.baseball_card.BaseballCard;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/5/13
 * Time: 10:30 AM
 *
 * Presents information about an Entity as a Baseball-card like configuration.  On the left side will be the default
 * 2D image (if it exists), and on right part, will be tag/value table of information.  Makes an internal JTable.
 */
public class BaseballCardPanel extends JPanel implements RootedEntityReceiver {
    public static final String IMAGE_COLUMN_HEADER = "Image";
    public static final String DETAILS_COLUMN_HEADER = "Details";
    private List<BaseballCard> cards;
    private boolean selectable;
    private int preferredWidth;
    private int rowsPerPage;
    private int nextEntityNum;
    private List<CheckboxWithData> checkboxes;
    private DynamicTable cardTable;
    private List<RootedEntity> rootedEntities;

    private Logger logger = LoggerFactory.getLogger( BaseballCardPanel.class );

    public BaseballCardPanel( int preferredWidth, int rowsPerPage ) {
        this(false, preferredWidth, rowsPerPage);
    }

    public BaseballCardPanel( boolean selectable, int preferredWidth, int rowsPerPage ) {
        this.selectable = selectable;
        this.preferredWidth = preferredWidth;
        this.rowsPerPage = rowsPerPage;
    }

    public void setRootedEntities( List<RootedEntity> rootedEntities ) {
        this.rootedEntities = rootedEntities;
        cards = new ArrayList<BaseballCard>();
        for ( nextEntityNum = 0; nextEntityNum < rowsPerPage  &&  nextEntityNum < rootedEntities.size(); nextEntityNum ++ ) {
            RootedEntity rootedEntity = rootedEntities.get( nextEntityNum );
            cards.add( new BaseballCard( rootedEntity.getEntity() ) );
        }
        establishGui();
        requestRedraw();
    }

    public void showAnotherPage() {
        int endOfPage = nextEntityNum + rowsPerPage;
        for ( ; nextEntityNum < endOfPage  &&  nextEntityNum < rootedEntities.size(); nextEntityNum++ ) {
            RootedEntity rootedEntity = rootedEntities.get( nextEntityNum );
            BaseballCard card = new BaseballCard(rootedEntity.getEntity());
            cards.add( card );
            cardTable.addRow( card );
        }
        cardTable.updateTableModel();
        updateMoreButtons();
        requestRedraw();
    }

    public void showAll() {
        for ( ; nextEntityNum < rootedEntities.size(); nextEntityNum++ ) {
            RootedEntity rootedEntity = rootedEntities.get( nextEntityNum );
            BaseballCard card = new BaseballCard(rootedEntity.getEntity());
            cards.add( card );
            cardTable.addRow( card );
        }
        cardTable.updateTableModel();
        updateMoreButtons();
        requestRedraw();
    }

    public void showLoadingIndicator() {
        removeAll();
        setLayout( new BorderLayout() );
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
        requestRedraw();
    }

    /** Everything checked can be returned from here. */
    public List<BaseballCard> getSelectedCards() {
        List<BaseballCard> returnList = new ArrayList<BaseballCard>();
        for ( CheckboxWithData data: checkboxes ) {
            if ( data.isSelected() ) {
                returnList.add( data.getCard() );
            }
        }
        return returnList;
    }

    private void establishGui() {
        removeAll();

        cardTable = new DynamicTable( true, true ) {
            @Override
            public Object getValue(Object userObject, DynamicColumn column) {
                BaseballCard card = (BaseballCard)userObject;
                if ( IMAGE_COLUMN_HEADER.equals( column.getName() )) {
                    return card.getDynamicImagePanel();
                }
                else if ( DETAILS_COLUMN_HEADER.equals( column.getName() )) {
                    return card.getEntityDetailsPanel();
                }
                else {
                    return null;
                }
            }

            @Override
            public void loadAllResults() {
                showAll();
            }

            @Override
            public void loadMoreResults(Callable<Void> success) {
                try {
                    showAnotherPage();
                    success.call();
                } catch ( Exception ex ) {
                    ex.printStackTrace();
                    logger.error( ex.getMessage() );
                }
            }
        };
        cardTable.getTable().setDefaultRenderer( Object.class, new ComponentSelfRenderer(this) );
        cardTable.getTable().setRowHeight(BaseballCard.IMAGE_HEIGHT);
        cardTable.addColumn(IMAGE_COLUMN_HEADER);
        cardTable.addColumn(DETAILS_COLUMN_HEADER);
        cardTable.getTable().setRowSelectionAllowed( selectable );

        Dimension detailsSize = new Dimension(
                preferredWidth - BaseballCard.IMAGE_WIDTH, BaseballCard.IMAGE_HEIGHT
        );
        Dimension imageSize = new Dimension(
                BaseballCard.IMAGE_WIDTH, BaseballCard.IMAGE_HEIGHT
        );
        for ( int i = 0; i < cards.size(); i++ ) {
            BaseballCard card = cards.get( i );
            addCardToTable(detailsSize, imageSize, card);
        }

        cardTable.setMoreResults( rootedEntities.size() > rowsPerPage );

        this.setLayout(new BorderLayout());
        this.add(cardTable, BorderLayout.CENTER);
        cardTable.updateTableModel();

        requestRedraw();
    }

    private void addCardToTable( Dimension detailsSize, Dimension imageSize, BaseballCard card) {
        card.getDynamicImagePanel().setPreferredSize(imageSize);
        card.getEntityDetailsPanel().setPreferredSize(detailsSize);

        cardTable.addRow( card );
    }

    private void updateMoreButtons() {
        for ( int i = 0; i < this.getComponentCount(); i++  ) {
            Component component = this.getComponent( i );
            if ( component instanceof DynamicTable) {
                DynamicTable table = (DynamicTable)component;
                table.setMoreResults( nextEntityNum < rootedEntities.size() );
            }
        }
    }

    private void requestRedraw() {
        validate();
        invalidate();
        repaint();
    }

    private static class CheckboxWithData extends JCheckBox {
        private BaseballCard card;
        public CheckboxWithData( BaseballCard card ) {
            super();
            this.card = card;
        }

        public BaseballCard getCard() { return card; }

    }

    private static class ComponentSelfRenderer extends DefaultTableCellRenderer {

        private BaseballCardPanel bbc;

        public ComponentSelfRenderer( BaseballCardPanel bbc ) {
            this.bbc = bbc;
        }

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

            // Note: need to have this feedback to redraw, since this renderer is not called again
            // once things change in the contained component.
            if ( value instanceof Container ) {
                Container container = (Container)value;
                container.addContainerListener(new ContainerAdapter() {
                    @Override
                    public void componentAdded(ContainerEvent e) {
                        super.componentAdded(e);
                        requestPanelRedraw();
                    }
                });
            }

            return rtnVal;
        }
         private void requestPanelRedraw() {
            bbc.validate();
            bbc.repaint();
        }
    }

}
