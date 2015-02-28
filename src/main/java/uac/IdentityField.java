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

    public abstract IdentityType getType();
}
