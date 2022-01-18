package org.janelia.workstation.controller.action;

import Jama.Matrix;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.ViewEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.geom.Vec3;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Horta",
        id = "GoToLocation"
)
@ActionRegistration(
        displayName = "#CTL_GoToLocation",
        lazy = false
)
@NbBundle.Messages("CTL_GoToLocation=Go to...")
public class GoToLocationAction extends AbstractAction {

    public GoToLocationAction() {
        super("Go to...");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String input = (String)JOptionPane.showInputDialog(
                null,
                "Enter an x, y, z or x, y location:\n(commas optional; brackets allowed)",
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

        // remove optional commas because they are annoying to require
        // remove square brackets so we can round-trip from "copy
        //  location to clipboard" command
        String [] items = input.replace(',', ' ').replaceAll("\\[", "").replaceAll("]", "").trim().split("\\s+");
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
            newLocation.setZ(Double.parseDouble(items[2]));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null,
                    "Expected x, y or x ,y ,z location;\ngot: " + input,
                    "Couldn't parse input!",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        ViewEvent syncViewEvent = new ViewEvent(this,newLocation.getX(),
                newLocation.getY(),
                newLocation.getZ(),
                500, null, false);

        Matrix m2v = TmModelManager.getInstance().getMicronToVoxMatrix();
        Jama.Matrix micLoc = new Jama.Matrix(new double[][]{
                {newLocation.getX(),},
                {newLocation.getY(),},
                {newLocation.getZ(),},
                {1.0,},});
        // NeuronVertex API requires coordinates in micrometers
        Jama.Matrix voxLoc = m2v.times(micLoc);
        Vec3 voxelXyz = new Vec3(
                (float) voxLoc.get(0, 0),
                (float) voxLoc.get(1, 0),
                (float) voxLoc.get(2, 0));
        TmViewState currView = TmModelManager.getInstance().getCurrentView();
        currView.setCameraFocusX(voxelXyz.getX());
        currView.setCameraFocusY(voxelXyz.getY());
        currView.setCameraFocusZ(voxelXyz.getZ());
        currView.setZoomLevel(500);
        ViewerEventBus.postEvent(syncViewEvent);
    }

}
