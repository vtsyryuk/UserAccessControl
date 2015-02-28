package uac;

import java.util.Set;

public interface UserAccessControl {
    Set<ResourcePermission> getPermissionSet(String userName);
}
