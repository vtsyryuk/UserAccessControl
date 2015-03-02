package uac;

import java.util.LinkedList;
import java.util.List;

public class ResourceIdentity {

    private final List<IdentityField> fields;

    private ResourceIdentity(List<IdentityField> fields) {
        this.fields = fields;
    }

    public List<IdentityField> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceIdentity that = (ResourceIdentity) o;
        return fields.equals(that.fields);

    }

    @Override
    public int hashCode() {
        return fields.hashCode();
    }

    public static class Builder {

        private final List<IdentityField> fields = new LinkedList<>();

        public Builder(IdentityField field) {
            this.fields.add(field);
        }

        public Builder field(IdentityField field) {
            this.fields.add(field);
            return this;
        }
        public ResourceIdentity build() {
            return new ResourceIdentity(fields);
        }
    }

    @Override
    public String toString() {
        return "ResourceIdentity{" +
                "fields=" + fields +
                '}';
    }
}
