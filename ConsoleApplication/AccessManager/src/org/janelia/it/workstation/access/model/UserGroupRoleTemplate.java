package org.janelia.it.workstation.access.model;

import org.janelia.it.jacs.model.domain.subjects.Group;
import org.janelia.it.jacs.model.domain.subjects.GroupRole;
import org.janelia.it.jacs.model.domain.subjects.User;

public class UserGroupRoleTemplate {

    private User user;
    private Group group;
    private GroupRole role;
    
    public UserGroupRoleTemplate(User user, Group group, GroupRole role) {
        this.user = user;
        this.group = group;
        this.role = role;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public GroupRole getRole() {
        return role;
    }

    public void setRole(GroupRole role) {
        this.role = role;
    }
    
}