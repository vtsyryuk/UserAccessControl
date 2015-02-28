package uac;

import java.util.Set;

public final class UserAccessChecker {

    private final UserAccessControl uac;

    public UserAccessChecker(UserAccessControl uac) {
        this.uac = uac;
    }

    public UserAccessLevel getLevel(String userName, final ResourceIdentity identity) {

        final Set<ResourcePermission> permissionSet = uac.getPermissionSet(userName);
        if (permissionSet.isEmpty()) return UserAccessLevel.None;

        UserAccessLevel ual = UserAccessLevel.None;
        for (ResourcePermission p : permissionSet) {
            if (p.getIdentity().equals(identity)) {
                UserAccessLevel level = p.getAccessLevel();
                if (level == UserAccessLevel.Write) {
                    ual = level;
                } else if (level == UserAccessLevel.Read && ual != UserAccessLevel.Write) {
                    ual = level;
                } else if (level == UserAccessLevel.None) {
                    return UserAccessLevel.None;
                }
            }
        }
        return ual;
    }
}
