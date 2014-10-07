package org.janelia.it.workstation.gui.large_volume_viewer.action;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.signal.Signal1;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * this action lets the user navigate to a specific location
 * in the large volume viewer
 */
public class GoToLocationAction extends AbstractAction {

    public Signal1<Vec3> gotoLocationSignal = new Signal1<>();

    private Camera3d camera;

    public GoToLocationAction(Camera3d camera) {
        this.camera = camera;

        putValue(NAME, "Go to...");
        String acc = "G";
        KeyStroke accelerator = KeyStroke.getKeyStroke(acc);
        putValue(ACCELERATOR_KEY, accelerator);
        putValue(SHORT_DESCRIPTION, "Go to x, y, z location");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String input = (String)JOptionPane.showInputDialog(
                null,
                "Enter an x, y, z or x, y location (commas optional):",
                "Go to location",
                JOptionPane.PLAIN_MESSAGE,
                null,                           // icon
                null,                           // choice list; absent = freeform
                "");                            // no initial string
        if (input == null || input.length() == 0) {
            return;
        }

        // note that this dialog closes, then we do the work of parsing;
        //  ideally, we should do a custom dialog where we parse when the
        //  user clicks "go" and only close if the input can be parse,
        //  leaving it open on an error so the user can correct the input


        // for now, do the parsing here, in-line; later when we want to
        //  get fancy and do auto-completing fuzzy matches on neuron names,
        //  we can break it out as its own class

        String [] items = input.replace(',', ' ').trim().split("\\s+");
        if (items.length < 2 || items.length > 3) {
            JOptionPane.showMessageDialog(null,
                    "Expected x, y or x ,y ,z location;\ngot: " + input,
                    "Couldn't parse input!",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Vec3 newLocation = new Vec3();
        try {
            newLocation.setX(Double.parseDouble(items[0]));
            newLocation.setY(Double.parseDouble(items[1]));

            // if we only get x, y, use current z
            if (items.length == 3) {
                newLocation.setZ(Double.parseDouble(items[2]));
            } else {
                Vec3 currentLocation = camera.getFocus();
                newLocation.setZ(currentLocation.getZ());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null,
                    "Expected x, y or x ,y ,z location;\ngot: " + input,
                    "Couldn't parse input!",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        gotoLocationSignal.emit(newLocation);
    }
}
