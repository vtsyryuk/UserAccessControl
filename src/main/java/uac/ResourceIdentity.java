package uac;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ResourceIdentity {

    private final Map<String, IdentityField> fieldMap;

    private ResourceIdentity(List<IdentityField> fields) {
        Map<String, IdentityField> _fieldMap = new LinkedHashMap<>(fields.size());
        for (IdentityField f : fields) {
            _fieldMap.put(f.getName(), f);
        }
        this.fieldMap = Map.copyOf(_fieldMap);
    }

    public Map<String, IdentityField> getFieldMap() {
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
