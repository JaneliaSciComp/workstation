package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.dialogs.ModalDialog;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persistent heads-up display for a synchronized image. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Hud extends ModalDialog {

	private static final Logger log = LoggerFactory.getLogger(Hud.class);
	
    private Entity entity;
    private boolean dirtyEntityFor3D;
	private JLabel previewLabel;
    private Mip3d mip3d;
    private JCheckBox render3DCheckbox;
    private Hud3DController hud3DController;
    
    public Hud() {
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

    public void setEntity( Entity entity ) {
        this.entity = entity;
        if ( entity == null ) {
            dirtyEntityFor3D = false;
        }
        else {
            dirtyEntityFor3D = true;
            if (render3DCheckbox != null ) {
                render3DCheckbox.setSelected(false);
                hud3DController.entityUpdate();
            }
        }
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

    /**
     * This convenience method lets a previously-known buffered image be handed to this widget, without the
     * need to read it from here.
     *
     * @param bufferedImage will be used for 2D.
     */
	public void setImage(BufferedImage bufferedImage) {
		previewLabel.setIcon(bufferedImage == null ? null : new ImageIcon(bufferedImage));
	}

    public void handleRenderSelection() {
        if ( shouldRender3D() ) {
            renderIn3D();
        }
        else {
            this.remove(mip3d);
            this.add(previewLabel, BorderLayout.CENTER);
        }
        this.validate();
        this.repaint();
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
            render3DCheckbox = new JCheckBox( "3D" );
            render3DCheckbox.setSelected( false ); // Always startup as false.
            render3DCheckbox.addActionListener(hud3DController);
            render3DCheckbox.setFont( render3DCheckbox.getFont().deriveFont( 9.0f ));
            render3DCheckbox.setBorderPainted(false);

            JPanel menuLikePanel = new JPanel();
            menuLikePanel.setLayout( new BorderLayout() );
            menuLikePanel.add( render3DCheckbox, BorderLayout.EAST );
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
            if ( hud3DController != null &&  hud3DController.is3DReady()  &&  (this.previewLabel.getIcon() == null) ) {
                rtnVal = true;
            }
        }
        return rtnVal;
    }

}
