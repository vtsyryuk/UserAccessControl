package uac;

public class ResourcePermission {
    private final ResourceIdentity identity;
    private final UserAccessLevel accessLevel;

    ResourcePermission(ResourceIdentity identity, UserAccessLevel accessLevel) {
        this.identity = identity;
        this.accessLevel = accessLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourcePermission that = (ResourcePermission) o;
        return accessLevel == that.accessLevel &&
                (identity != null ? identity.equals(that.identity) : that.identity == null);
    }

    @Override
    public int hashCode() {
        int result = identity != null ? identity.hashCode() : 0;
        result = 31 * result + (accessLevel != null ? accessLevel.hashCode() : 0);
        return result;
    }

    public ResourceIdentity getIdentity() {
        return identity;
    }

    public UserAccessLevel getAccessLevel() {
        return accessLevel;
    }
}
