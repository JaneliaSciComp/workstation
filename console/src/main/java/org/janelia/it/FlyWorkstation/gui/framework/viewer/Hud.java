package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.dialogs.ModalDialog;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persistent heads-up display for a synchronized image. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Hud extends ModalDialog {

	private static final Logger log = LoggerFactory.getLogger(Hud.class);
    public static final String RED_GREEN_BLUE_CONTROL = "rgb";
    public static final String THREE_D_CONTROL = "3D";

    private Entity entity;
    private boolean dirtyEntityFor3D;
	private JLabel previewLabel;
    private Mip3d mip3d;
    private JCheckBox render3DCheckbox;
    private JButton rgbButton;
    private Hud3DController hud3DController;
    private Logger logger = LoggerFactory.getLogger( Hud.class );

    private static Hud instance;

    public static Hud getSingletonInstance() {
        if ( instance == null ) {
            instance = new Hud();
        }
        return instance;
    }
    
    private Hud() {
        dirtyEntityFor3D = true;
		setModalityType(ModalityType.MODELESS);
        setLayout(new BorderLayout());
		previewLabel = new JLabel(new ImageIcon());
        init3dGui();

        add(previewLabel, BorderLayout.CENTER);
	}

    public void toggleDialog() {
		if (isVisible()) {
			hideDialog();
		}
		else {
            if ( entity != null ) {
                render();
            }
		}
	}
	
	public void hideDialog() {
		setVisible(false);
	}
	
	public Long getEntityId() {
        if ( entity == null ) {
            return 0L;
        }
		return entity.getId();
	}

    public void setEntityAndToggleDialog(Entity entity) {
        setEntityAndToggleDialog( entity, true );
    }

    public void setEntity(Entity entity) {
        setEntityAndToggleDialog( entity, false );
    }

    /** Filters RGB characteristics of rendering in 3D. */
    public void setRgbValues() {
        mip3d.setRgbValues();
    }

    /**
     * This is a controller-callback method to allow or disallow the user from attempting to switch
     * to 3D mode.
     *
     * @param flag T=allow; F=disallow
     */
    public void set3dModeEnabled(boolean flag) {
        if ( render3DCheckbox != null ) {
            render3DCheckbox.setEnabled( flag );
            if ( previewLabel.getIcon() == null ) {
                // In this case, no 2D image exists, and the checkbox for 3D is locked at true.
                render3DCheckbox.setEnabled( false );
                render3DCheckbox.setSelected( true );
            }
            else {
                // In this case, 2D exists.  Need to reset the checkbox for 2D use, for now.
                render3DCheckbox.setSelected( false );
            }

        }
    }

    public Entity getEntity() {
        return entity;
    }

    public void handleRenderSelection() {
        if ( shouldRender3D() ) {
            renderIn3D();
            if ( this.rgbButton != null ) {
                rgbButton.setEnabled( true );
            }
        }
        else {
            this.remove(mip3d);
            this.add(previewLabel, BorderLayout.CENTER);
            if ( this.rgbButton != null ) {
                rgbButton.setEnabled( false );
            }
        }
        this.validate();
        this.repaint();
    }

    public void setEntityAndToggleDialog(Entity entity, boolean toggle) {
        logger.info( "HUD: entity type is {}", entity.getEntityType().getName() );
        this.entity = entity;
        if ( entity == null ) {
            dirtyEntityFor3D = false;
        }
        else {
            boolean imageEstablished = false;
            try {
                imageEstablished = establishImage();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }

            if ( imageEstablished ) {
                dirtyEntityFor3D = true;
                if (render3DCheckbox != null ) {
                    render3DCheckbox.setSelected(false);
                    hud3DController.entityUpdate();
                }
                setTitle(entity.getName());
                pack();

                if ( toggle ) {
                    toggleDialog();
                }
            }
            else {
                JOptionPane.showMessageDialog( SessionMgr.getBrowser(), "Sorry, no image to display." );
                logger.info( "No image established for {}:{}", entity.getName(), entity.getEntityType() );
            }
        }

    }

    private boolean establishImage() throws Exception {
        boolean rtnVal = true;
        String imagePath = EntityUtils.getImageFilePath( entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE );
        if ( imagePath == null ) {
            logger.info("No image path for {}:{}" , entity.getName(), entity.getId());
            rtnVal = false;
        }
        else {

            BufferedImage image = null;
            SessionMgr sessionMgr = SessionMgr.getSessionMgr();
            Boolean invertImage =
                    (Boolean) sessionMgr.getModelProperty(ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY);
            ImageCache ic = SessionMgr.getBrowser().getImageCache();
            if ( ic != null ) {
                image = ic.get( imagePath );
            }

            // Ensure we have an image and that it is cached.
            if ( image == null ) {
                logger.info("In HUD: must load image.");
                image = Utils.readImage( imagePath );

                if ( ic != null ) {
                    ic.put( imagePath, image );
                }

            }

            // No image loaded or cached.  Do nada.
            if ( image == null ) {
                logger.info("No image read for {}:{}", entity.getId(), imagePath);
                rtnVal = false;
            }
            else {
                // May need to invert the colors to conform to the current settings.
                if ( invertImage ) {
                    image = Utils.invertImage( image );
                }

                ImageIcon imageIcon = new ImageIcon(image);
                previewLabel.setIcon( image == null ? null : imageIcon);

            }

        }
        return  rtnVal;
    }

    private void render() {
        if ( shouldRender3D() ) {
            renderIn3D();
        }
        packAndShow();
    }

    private void renderIn3D() {
        this.remove( previewLabel );
        if ( dirtyEntityFor3D ) {
            try {
                if ( hud3DController != null ) {
                    hud3DController.setUiBusyMode();
                    hud3DController.load3d();
                    dirtyEntityFor3D = hud3DController.isDirty();
                }

            } catch ( Exception ex ) {
                JOptionPane.showMessageDialog( this, "Failed to load 3D image." );
                ex.printStackTrace();
                set3dModeEnabled(false);
                handleRenderSelection();

            }
        }
        else {
            if ( hud3DController != null ) {
                hud3DController.set3dWidget();
            }
        }
    }

    private void init3dGui() {
    	if (!Mip3d.isAvailable()) {
    		log.error("Cannot initialize 3D HUD because 3D MIP viewer is not available");
    		return;
    	}
        try {
            mip3d = new Mip3d();
            hud3DController = new Hud3DController(this, mip3d);
            render3DCheckbox = new JCheckBox( THREE_D_CONTROL );
            render3DCheckbox.setSelected( false ); // Always startup as false.
            render3DCheckbox.addActionListener(hud3DController);
            render3DCheckbox.setFont( render3DCheckbox.getFont().deriveFont( 9.0f ));
            render3DCheckbox.setBorderPainted(false);
            render3DCheckbox.setActionCommand( THREE_D_CONTROL );

            rgbButton = new JButton( RED_GREEN_BLUE_CONTROL );
            rgbButton.setFont( rgbButton.getFont().deriveFont( 9.0f ) );
            rgbButton.addActionListener( hud3DController );
            rgbButton.setBorderPainted( true );
            rgbButton.setActionCommand( RED_GREEN_BLUE_CONTROL );
            rgbButton.setEnabled( false );

            JPanel rightSidePanel = new JPanel();
            rightSidePanel.setLayout( new FlowLayout() );
            rightSidePanel.add( rgbButton );
            rightSidePanel.add( render3DCheckbox );

            JPanel menuLikePanel = new JPanel();
            menuLikePanel.setLayout( new BorderLayout() );
            menuLikePanel.add( rightSidePanel, BorderLayout.EAST );
            add(menuLikePanel, BorderLayout.NORTH);
        } catch ( Exception ex ) {
            // Turn off the 3d capability if exception.
            render3DCheckbox = null;
            log.error("Error initializing 3D HUD",ex);
        }
    }

    private boolean shouldRender3D() {
        boolean rtnVal = render3DCheckbox != null  &&  render3DCheckbox.isEnabled() && render3DCheckbox.isSelected();
        if ( !rtnVal ) {
            if ( hud3DController != null  &&  hud3DController.is3DReady()  &&  (this.previewLabel.getIcon() == null) ) {
                rtnVal = true;
            }
        }
        return rtnVal;
    }

}
