package org.janelia.it.workstation.gui.framework.viewer;

import org.janelia.it.workstation.gui.browser.baseball_card.BaseballCardPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.gui.browser.WorkstationEnvironment;
import org.janelia.it.workstation.gui.browser.baseball_card.BaseballCard;
import org.janelia.it.workstation.gui.framework.viewer.search.SolrResultsMetaData;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/5/13
 * Time: 3:45 PM
 *
 * Short cut test of baseball card panel.  Application throws up a frame to look at.
 */
@Category(TestCategories.InteractiveTests.class)
public class BaseballCardPanelTest extends JFrame {

    public static final int HEIGHT = 800;
    public static final int WIDTH = 800;

    private BaseballCardPanel panel;
    private Logger logger = LoggerFactory.getLogger( BaseballCardPanelTest.class );

    public static void main( String[] args ) throws Exception {
        BaseballCardPanelTest test = new BaseballCardPanelTest();
        test.setVisible( true );
    }

    public BaseballCardPanelTest() throws Exception {
        super("Test Baseball Card Display");
        logger.info(
                "The purpose of this test is to show what the baseball card panel looks like, and see its display\n" +
                        "  When this test runs, expect to see a popup window containing one page worth of mock \n" +
                        "  Neuron Fragments, as if it had been searched. You may then hit the load buttons for more or all.\n" +
                        "  Finally, select things (using typical java-type multi-select/multiple groups). \n" +
                        "  There should be a button for spitting out what you have selected, as log-issues.\n"
        );
        initGui();
        initCardPanel();
    }

    private void initGui() throws Exception {
        this.setLayout( new BorderLayout() );
        this.setSize(WIDTH, HEIGHT);
        this.setLocation( 0, 0 );

        panel = new BaseballCardPanel( true, WIDTH, 10 );
        this.add(panel, BorderLayout.CENTER);
        panel.setPreferredSize(this.getSize());
        this.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
        new WorkstationEnvironment().invoke();

        JButton checkDump = new JButton( "List Selection" );
        checkDump.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for ( BaseballCard card: panel.getSelectedCards() ) {
                    logger.info("Entity is " + card.getDomainObject().getId() + "/" + card.getDomainObject().getName());
                }
            }
        });

        this.add( checkDump, BorderLayout.SOUTH );

    }

    private void initCardPanel() {
        // Adding mock data to the panel.
        List<RootedEntity> rEntities = new ArrayList<>();
        Long[] guids = {
                1870583260875063394L,
                1930003161519489192L,
                1874576934948569186L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
                1870583260875063394L,
        };
        int i = 0;
        for ( Long guid: guids ) {
            EntityData entityData = getEntityData( "aName", "fosterl", "Something");
            Set<EntityData> dataSet = new HashSet<>();
            dataSet.add( entityData );
            Entity entity = new Entity( guid, "Neuron " + i, "fosterl", EntityConstants.TYPE_NEURON_FRAGMENT, new Date(), new Date(), dataSet );
            RootedEntity re = new RootedEntity( entity );
            rEntities.add( re );

            i++;
        }
        //long elapsedTime, int resultCount, String queryString
        SolrResultsMetaData srmd = new SolrResultsMetaData();
        srmd.setRawNumHits( 2000 );
        srmd.setNumHits( guids.length );
        srmd.setQueryStr( "Something" );
        srmd.setSearchDuration( 1000 );
        panel.setRootedEntities( rEntities, srmd );
    }

    private EntityData getEntityData(String name,
                                     String ownerKey,
                                     String value) {
        return new EntityData(null,
                name,
                null,
                null,
                ownerKey,
                value,
                null,
                null,
                null);
    }

}
