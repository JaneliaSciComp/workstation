package org.janelia.it.workstation.gui.large_volume_viewer.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;

/**
 *
 * @author schauderd
 */

public class CommonDialogItems {
    private static final String ACTIVE_TRACERS_GROUP = ConsoleProperties.getInstance().getProperty("console.LVVHorta.activetracersgroup").trim();
    private static final String TRACERS_GROUP = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();

    /**
     * given a combo model box, populate it with a list of all Subjects (people and groups) from
     * the db, or restrict to the member of the tracers or active tracers group;
     * this dialog is used in dialogs when presenting options for neuron ownership and other permissions
     */
    public static void updateOwnerList(DefaultComboBoxModel comboBoxModel, ChangeNeuronOwnerDialog.UserFilter filter) {
        comboBoxModel.removeAllElements();

        List<Subject> subjects = new ArrayList<>();
        try {
            if (filter != ChangeNeuronOwnerDialog.UserFilter.NONE) {
                String group = "";
                if (filter == ChangeNeuronOwnerDialog.UserFilter.ACTIVE_TRACERS) {
                    group = ACTIVE_TRACERS_GROUP;
                } else if (filter == ChangeNeuronOwnerDialog.UserFilter.TRACERS) {
                    group = TRACERS_GROUP;
                }
                List<User> users = DomainMgr.getDomainMgr().getUsersInGroup(group);
                subjects = users.stream()
                    .map(u -> (Subject) u)
                    .collect(Collectors.toList());

                // in either case, add back in the tracer group, at the top:
                Subject tracersGroup = DomainMgr.getDomainMgr().getSubjectFacade().getSubjectByNameOrKey(TRACERS_GROUP);
                if (tracersGroup != null) {
                    subjects.add(0, tracersGroup);
                }
            } else {
                // filter = NONE = all users and groups
                subjects = DomainMgr.getDomainMgr().getSubjects();
            }
        }
        catch (Exception e) {
            // I feel I need to handle this error better; it should
            //  default to nothing in the list
            e.printStackTrace();
        }

        for (Subject subject: subjects) {
            comboBoxModel.addElement(subject);
        }
    }
}


 
