package uac;

public class WildcardField extends IdentityField {

    public WildcardField(String name, String value) {
        super(name, value);
    }

    @Override
    public IdentityType getType() {
        return IdentityType.Wildcard;
    }
}
