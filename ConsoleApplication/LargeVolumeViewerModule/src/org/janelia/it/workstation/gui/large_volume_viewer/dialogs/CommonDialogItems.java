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
     private static Set<String> mouselightUsers = new HashSet<>(Arrays.asList(
            "ackermand",
            "arshadic",
            "base",
            "blaker",
            "chandrashekarj",
            "dossantosb",
            "ferreirat",
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
     
    public static void updateList(DefaultComboBoxModel comboBoxModel, boolean mouselightOnly) {
        comboBoxModel.removeAllElements();

        List<Subject> subjects = new ArrayList<>();
        try {
            subjects = DomainMgr.getDomainMgr().getSubjects();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (mouselightOnly) {
            // currently no api for getting all users in a group; hardcode for now
            // note: in testing, there were occasionally resize problems when the
            //  checkbox changed--sometimes when the list got wider, the window didn't;
            //  currently, it's OK

            // playing with Java lambdas to see if it helps clarity...not sure it does
            subjects = subjects.stream()
                .filter(subject -> mouselightUsers.contains(subject.getName()))
                .collect(Collectors.toList());
        }
        for (Subject subject: subjects) {
            comboBoxModel.addElement(subject);
        }
    }
}


 
