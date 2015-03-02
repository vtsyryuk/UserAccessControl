package uac;

public abstract class IdentityField {

    private String name;
    private String value;

    public IdentityField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentityField that = (IdentityField) o;

        return (name != null ? name.equals(that.name) : that.name == null) &&
                (value != null ? value.equals(that.value) : that.value == null);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public abstract IdentityType getType();

    @Override
    public String toString() {
        return "IdentityField{name='" + name + '\'' + ", value='" + value + '\'' + '}';
    }
}
