package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.dialogs.ModalDialog;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * A persistent heads-up display for a synchronized image. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Hud extends ModalDialog {

    private Entity entity;
    private boolean dirtyEntityFor3D;
	private JLabel previewLabel;
    private Mip3d mip3d;
    private JCheckBox render3DCheckbox;
    private Hud3DController hud3DController;
    
    public Hud() {
        dirtyEntityFor3D = true;
		setModalityType(ModalityType.MODELESS);
        setLayout( new BorderLayout() );
		previewLabel = new JLabel(new ImageIcon());
        //init3dCapability();

        add(previewLabel, BorderLayout.CENTER);
	}

    public void set2D() {
        if ( render3DCheckbox != null ) {
            render3DCheckbox.setSelected( false );
        }
        handleRenderSelection();
    }

    public void toggleDialog() {
		if (isVisible()) {
			hideDialog();
		}
		else {
            render();
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
        dirtyEntityFor3D = true;
        if (render3DCheckbox != null ) {
            render3DCheckbox.setSelected(false);
            hud3DController.entityUpdate();
        }
    }
    
    public void set3DEnabled( boolean flag ) {
        if ( render3DCheckbox != null ) {
            render3DCheckbox.setEnabled( flag );
        }
    }

    public Entity getEntity() {
        return entity;
    }

//	public void setEntityId(Long entityId) {
//		this.entityId = entityId;
//        this.dirtyEntityFor3D = true;
//	}

	public void setImage(BufferedImage bufferedImage) {
		previewLabel.setIcon(bufferedImage == null ? null : new ImageIcon(bufferedImage));
	}

    public void handleRenderSelection() {
        if ( mayRender3D() ) {
            setup3D();
        }
        else {
            this.remove(mip3d);
            this.add(previewLabel, BorderLayout.CENTER);
        }
        this.validate();
        this.repaint();
    }

    private void init3dCapability() {
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
    }

    private void render() {
        setup3D();
        packAndShow();
    }

    private void setup3D() {
        if ( mayRender3D() ) {
            this.remove(previewLabel);
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
                    set2D();

                }
            }
            else {
                if ( hud3DController != null ) {
                    hud3DController.set3dWidget();
                }
            }
        }
    }

    private boolean mayRender3D() {
        return render3DCheckbox != null  &&  render3DCheckbox.isEnabled() && render3DCheckbox.isSelected();
    }

}
