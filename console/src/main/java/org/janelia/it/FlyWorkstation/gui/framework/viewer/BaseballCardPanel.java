package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.baseball_card.BaseballCard;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;

import javax.swing.*;
import java.awt.*;
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
public class BaseballCardPanel extends JPanel implements RootedEntityReceiver {
    private List<BaseballCard> cards;
    private boolean selectable;
    private int preferredWidth;
    private List<CheckboxWithData> checkboxes;

    public BaseballCardPanel( int preferredWidth ) {
        this(false, preferredWidth);
    }

    public BaseballCardPanel( boolean selectable, int preferredWidth ) {
        this.selectable = selectable;
        this.preferredWidth = preferredWidth;
    }

    public void setRootedEntities( Collection<RootedEntity> rootedEntities ) {
        cards = new ArrayList<BaseballCard>();
        for ( RootedEntity rootedEntity: rootedEntities ) {
            cards.add( new BaseballCard( rootedEntity.getEntity() ) );
        }
        establishGui();
        validate();
        invalidate();
        repaint();
    }

    public void showLoadingIndicator() {
        removeAll();
        setLayout( new BorderLayout() );
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
        validate();
        invalidate();
        repaint();
    }

    private void establishGui() {
        removeAll();
        JPanel innerPanel = new JPanel();
        JScrollPane cardScroller = new JScrollPane( innerPanel );
        innerPanel.setLayout( new GridBagLayout() );
        int nextCol = 0;
        int gridHeight = 3;
        Insets flushInsets = new Insets(0,0,0,0);
        Insets borderInsets = new Insets(1,1,1,1);
        GridBagConstraints checkBoxConstraints = null;
        if (selectable) {
            checkBoxConstraints = new GridBagConstraints(
                    nextCol, 0,
                    1, gridHeight,
                    0.0, 1.0,
                    GridBagConstraints.WEST,
                    GridBagConstraints.NONE,
                    flushInsets,
                    0,0
            );
            nextCol ++;
            checkboxes = new ArrayList<CheckboxWithData>();
        }
        GridBagConstraints imageConstraints = new GridBagConstraints(
                nextCol, 0,
                2, gridHeight,
                0.0, 1.0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE,
                borderInsets,
                0,0
        );
        nextCol += 2;
        GridBagConstraints detailsConstraints = new GridBagConstraints(
                nextCol, 0,
                4, gridHeight,
                10.0, 1.0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                flushInsets,
                0,0
        );

        Dimension detailsSize = new Dimension(
                preferredWidth - BaseballCard.IMAGE_WIDTH, BaseballCard.IMAGE_HEIGHT
        );
        Dimension imageSize = new Dimension(
                BaseballCard.IMAGE_WIDTH, BaseballCard.IMAGE_HEIGHT
        );
        for ( int i = 0; i < cards.size(); i++ ) {
            BaseballCard card = cards.get( i );
            if (selectable) {
                checkBoxConstraints.gridy = i * gridHeight;
                CheckboxWithData checkboxWithData = new CheckboxWithData( card );
                innerPanel.add( checkboxWithData, checkBoxConstraints );
                checkboxes.add( checkboxWithData );
            }

            imageConstraints.gridy = i * gridHeight;
            card.getDynamicImagePanel().setPreferredSize( imageSize );
            innerPanel.add( card.getDynamicImagePanel(), imageConstraints );

            detailsConstraints.gridy = i * gridHeight;
            card.getEntityDetailsPanel().setPreferredSize( detailsSize );
            innerPanel.add( card.getEntityDetailsPanel(), detailsConstraints );
        }

        cardScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cardScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        this.setLayout(new BorderLayout());
        add(cardScroller, BorderLayout.CENTER);
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

    private static class CheckboxWithData extends JCheckBox {
        private BaseballCard card;
        public CheckboxWithData( BaseballCard card ) {
            super();
            this.card = card;
        }

        public BaseballCard getCard() { return card; }

    }

}
