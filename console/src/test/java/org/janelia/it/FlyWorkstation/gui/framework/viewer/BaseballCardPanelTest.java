package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.baseball_card.BaseballCard;
import org.janelia.it.FlyWorkstation.gui.util.panels.DataSourceSettingsPanel;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.jacs.model.entity.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

        panel = new BaseballCardPanel( true, WIDTH );
        this.add( panel, BorderLayout.CENTER );
        panel.setPreferredSize( this.getSize() );
        this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        // Need to mock the browser environment.
        // Prime the tool-specific properties before the Session is invoked
        ConsoleProperties.load();

        // Protocol Registration - Adding more than one type should automatically switch over to the Aggregate Facade
        FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");

        // Assuming that the user has entered the login/password information, now validate
        String username = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME);
        String email = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL);

        if (username==null || email==null) {
            Object[] options = {"Enter Login", "Exit Program"};
            final int answer = JOptionPane.showOptionDialog(null, "Please enter your login and email information.", "Information Required",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                PrefController.getPrefController().getPrefInterface(DataSourceSettingsPanel.class, null);
            }
            else {
                SessionMgr.getSessionMgr().systemExit();
            }
        }

        SessionMgr.getSessionMgr().loginSubject();
        SessionMgr.getSessionMgr().newBrowser();

        JButton checkDump = new JButton( "List Selection" );
        checkDump.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for ( BaseballCard card: panel.getSelectedCards() ) {
                    System.out.println("Entity is " + card.getEntity().getId() + "/" + card.getEntity().getName() );
                }
            }
        });

        this.add( checkDump, BorderLayout.SOUTH );

    }

    private void initCardPanel() {
        // Adding mock data to the panel.
        EntityType type = new EntityType();
        type.setName( EntityConstants.TYPE_NEURON_FRAGMENT );
        Collection<RootedEntity> rEntities = new ArrayList<RootedEntity>();
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
//        for ( long i = 0; i < 50; i++ ) {
        int i = 0;
        for ( Long guid: guids ) {
            EntityData entityData = getEntityData( "aName", "fosterl", "Something");
            Set<EntityData> dataSet = new HashSet<EntityData>();
            dataSet.add( entityData );
            Entity entity = new Entity( guid, "Neuron " + i, "fosterl", null, type, new Date(), new Date(), dataSet );
            RootedEntity re = new RootedEntity( entity );
            rEntities.add( re );

            i++;
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
