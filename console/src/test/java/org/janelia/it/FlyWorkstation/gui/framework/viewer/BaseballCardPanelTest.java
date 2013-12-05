package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/5/13
 * Time: 3:45 PM
 *
 * Short cut test of baseball card panel.  Application throws up a frame to look at.
 */
public class BaseballCardPanelTest extends JFrame {

    public static final int HEIGHT = 800;
    public static final int WIDTH = 800;

    private BaseballCardPanel panel;

    public static final void main( String[] args ) throws Exception {
        BaseballCardPanelTest test = new BaseballCardPanelTest();
        test.setVisible( true );
    }

    public BaseballCardPanelTest() throws Exception {
        super("Test Baseball Card Display");
        initGui();
        initCardPanel();
    }

    private void initGui() throws Exception {
        this.setLayout( new BorderLayout() );
        this.setSize(WIDTH, HEIGHT);
        this.setLocation( 0, 0 );

        panel = new BaseballCardPanel( false );
        this.add( panel, BorderLayout.CENTER );
        this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    }

    private void initCardPanel() {
        // Adding mock data to the panel.
        EntityType type = new EntityType();
        type.setName( EntityConstants.TYPE_NEURON_FRAGMENT );
        Collection<RootedEntity> rEntities = new ArrayList<RootedEntity>();
        for ( long i = 0; i < 50; i++ ) {
            EntityData entityData = getEntityData( "aName", "fosterl", "Something");
            Set<EntityData> dataSet = new HashSet<EntityData>();
            dataSet.add( entityData );
            Entity entity = new Entity( new Long(60000 + i), "Neuron " + i, "fosterl", null, type, new Date(), new Date(), dataSet );
            RootedEntity re = new RootedEntity( entity );
            rEntities.add( re );
        }
        panel.setRootedEntities( rEntities );
    }

    private EntityData getEntityData(String name,
                                     String ownerKey,
                                     String value) {
        return new EntityData(null,
                getEntityAttribute(name),
                null,
                null,
                ownerKey,
                value,
                null,
                null,
                null);
    }

    private EntityAttribute getEntityAttribute(String name) {
        return new EntityAttribute(null, name, null, null, null);
    }

}
