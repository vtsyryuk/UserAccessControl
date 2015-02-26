package uac;

import java.util.Map;

/**
 * Created by User on 26/2/2015.
 */
interface UserAccessControl<T, R> {
    Map<T, Permission<R>> getPermissionSet(String userName, T identity);
}
