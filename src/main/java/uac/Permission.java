package uac;

public class Permission<T> {
    private final T identity;
    private final UserAccessLevel accessLevel;

    Permission(T identity, UserAccessLevel accessLevel) {
        this.identity = identity;
        this.accessLevel = accessLevel;
    }

    public T getIdentity() {
        return identity;
    }

    public UserAccessLevel getAccessLevel() {
        return accessLevel;
    }
}
