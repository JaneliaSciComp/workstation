package org.janelia.it.workstation.gui.framework.viewer;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
import org.janelia.it.workstation.gui.framework.viewer.baseball_card.BaseballCard;
import org.janelia.it.workstation.gui.framework.viewer.search.SolrResultsMetaData;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
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

    private static final String STATUS_TEXT_FMT = "%d results found for '%s' in this alignment context, %d results loaded.";
    private static final String STATUS_TOOLTIP_FMT = "Query took %d milliseconds";

    private List<BaseballCard> cards;
    private boolean selectable;
    private int preferredWidth;
    private int rowsPerPage;
    private int nextEntityNum;
    private DynamicTable cardTable;
    private ControlCallback controlCallback;
    private List<RootedEntity> rootedEntities;
    private JLabel statusLabel;
    private SolrResultsMetaData solrResultsMetaData;

    private Logger logger = LoggerFactory.getLogger( BaseballCardPanel.class );

    public BaseballCardPanel( boolean selectable, int preferredWidth, int rowsPerPage, ControlCallback callback ) {
        this.selectable = selectable;
        this.preferredWidth = preferredWidth;
        this.rowsPerPage = rowsPerPage;
        this.controlCallback = callback;
    }

    public BaseballCardPanel( int preferredWidth, int rowsPerPage ) {
        this(false, preferredWidth, rowsPerPage);
    }

    public BaseballCardPanel( boolean selectable, int preferredWidth, int rowsPerPage ) {
        this( selectable, preferredWidth, rowsPerPage, null );
    }

    @Override
    public void setRootedEntities(
            List<RootedEntity> rootedEntities, SolrResultsMetaData solrResultsMetaData
    ) {
        this.rootedEntities = rootedEntities;
        this.solrResultsMetaData = solrResultsMetaData;
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
        updateMoreButtonsAndStatus();
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
        updateMoreButtonsAndStatus();
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
        List<Object> selectedObjects = cardTable.getSelectedObjects();
        for ( Object o: selectedObjects ) {
            if (o instanceof BaseballCard) {
                returnList.add( (BaseballCard) o );
            }
        }
        return returnList;
    }

    public List<Object> getSelectedObjects() {
        return cardTable.getSelectedObjects();
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

            @Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {

                JTable target = (JTable) e.getSource();
                if (target.getSelectedRow() <0 || target.getSelectedColumn()<0) return null;

                final JPopupMenu popupMenu = new JPopupMenu();
                popupMenu.setLightWeightPopupEnabled(true);

                ListSelectionModel lsm = getTable().getSelectionModel();

                if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) {

                    final BaseballCard value = cards.get(target.getSelectedRow());

                    JMenuItem titleMenuItem = new JMenuItem(value.getEntity().getName());
                    titleMenuItem.setEnabled(false);
                    popupMenu.add(titleMenuItem);

                    // Items which are  only available when selecting a single cell
                    JMenuItem copyMenuItem = new JMenuItem("  Show in Lightbox");
                    copyMenuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // Signal caller we are taking the screen.
                            controlCallback.callerRequiresFocus();
                            // Show in HUD/Light Box
                            Entity entity = value.getEntity();
                            Hud.getSingletonInstance().hideDialog(); // Ensure: one-way trip
                            Hud.getSingletonInstance().setEntityAndToggleDialog(entity);
                        }
                    });
                    popupMenu.add(copyMenuItem);
                }
                else {
                    JMenuItem titleMenuItem = new JMenuItem("(Multiple Items Selected)");
                    titleMenuItem.setEnabled(false);
                    popupMenu.add(titleMenuItem);
                }

                return popupMenu;
            }



        };
        ComponentSelfRenderer componentSelfRenderer = new ComponentSelfRenderer(this);
        cardTable.getTable().setRowHeight(BaseballCard.IMAGE_HEIGHT);

        DynamicColumn imageColumn =  cardTable.addColumn(IMAGE_COLUMN_HEADER);
        cardTable.setColumnRenderer( imageColumn, componentSelfRenderer );
        DynamicColumn detailsColumn = cardTable.addColumn(DETAILS_COLUMN_HEADER);
        cardTable.setColumnRenderer( detailsColumn, componentSelfRenderer );

        cardTable.getTable().setRowSelectionAllowed(selectable);
        cardTable.getTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        Dimension detailsSize = new Dimension(
                preferredWidth - BaseballCard.IMAGE_WIDTH - 50, BaseballCard.IMAGE_HEIGHT
        );
        Dimension imageSize = new Dimension(
                BaseballCard.IMAGE_WIDTH, BaseballCard.IMAGE_HEIGHT
        );
        for ( int i = 0; i < cards.size(); i++ ) {
            BaseballCard card = cards.get( i );
            addCardToTable(detailsSize, imageSize, card);
        }
        cardTable.setMaxColWidth( Math.max( detailsSize.width, imageSize.width ) );
        cardTable.setMoreResults( rootedEntities.size() > rowsPerPage );

        this.setLayout(new BorderLayout());
        this.add(cardTable, BorderLayout.CENTER);
        cardTable.updateTableModel();

        cardTable.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateSelectionAppearance();
            }
        });         cardTable.autoResizeColWidth();

        statusLabel = new JLabel();
        updateStatus();
        this.add( statusLabel, BorderLayout.NORTH );

        requestRedraw();
    }

    private void updateStatus() {
        statusLabel.setText(
                String.format(
                        STATUS_TEXT_FMT,
                        solrResultsMetaData.getNumHits(),
                        solrResultsMetaData.getQueryStr(),
                        cardTable.getRows().size()
                )
        );
        statusLabel.setToolTipText(
                String.format( STATUS_TOOLTIP_FMT, solrResultsMetaData.getSearchDuration() )
        );
    }

    /** Clear the GUI representation of the selection, and replace it with whatever is selected at call time. */
    private void updateSelectionAppearance() {
        Color unselectedBackground = (Color)UIManager.get( "Panel.background" );
        Color selectionBackground = ( unselectedBackground.equals( Color.white ) ) ?
                unselectedBackground.darker() :
                unselectedBackground.brighter();

        for (BaseballCard card : cards) {
            card.setBackground( unselectedBackground );
        }

        List<BaseballCard> selection = getSelectedCards();
        for (BaseballCard selected : selection) {
            selected.setBackground( selectionBackground );
        }
    }

    private void addCardToTable( Dimension detailsSize, Dimension imageSize, BaseballCard card) {
        card.getDynamicImagePanel().setPreferredSize(imageSize);
        card.getEntityDetailsPanel().setPreferredSize(detailsSize);

        cardTable.addRow(card);
    }

    private void updateMoreButtonsAndStatus() {
        for ( int i = 0; i < this.getComponentCount(); i++  ) {
            Component component = this.getComponent( i );
            if ( component instanceof DynamicTable) {
                DynamicTable table = (DynamicTable)component;
                table.setMoreResults( nextEntityNum < rootedEntities.size() );
            }
        }
        updateStatus();
    }

    private void requestRedraw() {
        validate();
        invalidate();
        repaint();
    }

    /**
     * Implement this to allow this panel to manipulate some external caller.
     */
    public static interface ControlCallback {
        void callerRequiresFocus();
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
