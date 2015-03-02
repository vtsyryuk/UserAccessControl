package uac;

import com.google.common.collect.ImmutableMap;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ResourceIdentity {

    private final ImmutableMap<String, IdentityField> fieldMap;

    private ResourceIdentity(List<IdentityField> fields) {
        Map<String, IdentityField> fieldMap = new Hashtable<>(fields.size());
        for (IdentityField f : fields) {
            fieldMap.put(f.getName(), f);
        }
        this.fieldMap = ImmutableMap.<String, IdentityField>builder().putAll(fieldMap).build();
    }

    public ImmutableMap<String, IdentityField> getFieldMap() {
        return fieldMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceIdentity that = (ResourceIdentity) o;
        return fieldMap.equals(that.fieldMap);
    }

    @Override
    public int hashCode() {
        return fieldMap.hashCode();
    }

    public static class Builder {

        private final List<IdentityField> fields = new LinkedList<>();

        public Builder field(IdentityField field) {
            this.fields.add(field);
            return this;
        }

        public ResourceIdentity build() {
            return new ResourceIdentity(fields);
        }
    }
}
