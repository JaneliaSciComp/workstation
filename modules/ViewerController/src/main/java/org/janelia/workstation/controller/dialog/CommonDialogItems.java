package org.janelia.workstation.controller.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */

public class CommonDialogItems {

    private final static Logger log = LoggerFactory.getLogger(CommonDialogItems.class);

    /**
     * given a combo model box, populate it with a list of all Subjects (people and groups) from
     * the db, or restrict to the member of the tracers or active tracers group;
     * this dialog is used in dialogs when presenting options for neuron ownership and other permissions
     */
    public static void updateOwnerList(DefaultComboBoxModel comboBoxModel, ChangeNeuronOwnerDialog.UserFilter filter) {
        comboBoxModel.removeAllElements();

        String activeTracersGroup = ConsoleProperties.getInstance().getProperty("console.LVVHorta.activetracersgroup").trim();
        String tracersGroup = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();

        List<Subject> subjects = new ArrayList<>();
        try {
            if (filter != ChangeNeuronOwnerDialog.UserFilter.NONE) {
                String group = "";
                if (filter == ChangeNeuronOwnerDialog.UserFilter.ACTIVE_TRACERS) {
                    group = activeTracersGroup;
                } else if (filter == ChangeNeuronOwnerDialog.UserFilter.TRACERS) {
                    group = tracersGroup;
                }
                List<User> users = DomainMgr.getDomainMgr().getUsersInGroup(group);
                subjects = users.stream()
                    .map(u -> (Subject) u)
                    .collect(Collectors.toList());

                // in either case, add back in the tracer group, at the top:
                Subject tracersSubject = DomainMgr.getDomainMgr().getSubjectFacade().getSubjectByNameOrKey(tracersGroup);
                if (tracersSubject != null) {
                    subjects.add(0, tracersSubject);
                }
            } else {
                // filter = NONE = all users and groups
                subjects = DomainMgr.getDomainMgr().getSubjects();
            }
        }
        catch (Exception e) {
            // I feel I need to handle this error better; it should
            //  default to nothing in the list
            log.error("Error updating owner list", e);
        }

        for (Subject subject: subjects) {
            comboBoxModel.addElement(subject);
        }
    }
}


 
