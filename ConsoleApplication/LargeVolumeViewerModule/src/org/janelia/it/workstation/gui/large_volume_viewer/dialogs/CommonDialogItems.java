package org.janelia.it.workstation.gui.large_volume_viewer.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.model.security.Subject;

/**
 *
 * @author schauderd
 */


public class CommonDialogItems {
    // these are the people we usually want to assign neuron ownership to; it's
    //  a mix of tracers, devs, and project leaders, but it's a smaller list than
    //  all the people in mouselight, so we hard-code it here
     private static Set<String> mouselightOwners = new HashSet<>(Arrays.asList(
            "ackermand",
            "arshadic",
            "base",
            "blaker",
            "chandrashekarj",
            "dossantosb",
            "elsayeda",
            "ferreirat",
            "goinac",
            "moharr",
            "mouselight",
            "olbrisd",
            "ramirezd2",
            "rokickik",
            "schauderd",
            "taylora",
            "weldonm",
            "winnubstj",
            "zafara"
    ));

    /**
     * given a combo model box, populate it with a list of all Subjects (people and groups) from
     * the db, or restrict to the custom mouselight subjects listed above; used in mouselight
     * dialogs when presenting options for ownership and other permissions
     */
    public static void updateOwnerList(DefaultComboBoxModel comboBoxModel, boolean mouselightOnly) {
        comboBoxModel.removeAllElements();

        List<Subject> subjects = new ArrayList<>();
        try {
            subjects = DomainMgr.getDomainMgr().getSubjects();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (mouselightOnly) {
            // note: in testing, there were occasionally resize problems when the
            //  checkbox changed--sometimes when the list got wider, the window didn't;
            //  currently, it's OK
            subjects = subjects.stream()
                .filter(subject -> mouselightOwners.contains(subject.getName()))
                .collect(Collectors.toList());
        }
        for (Subject subject: subjects) {
            comboBoxModel.addElement(subject);
        }
    }
}


 
