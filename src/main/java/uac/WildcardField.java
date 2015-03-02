package uac;

public class WildcardField extends IdentityField {

    private static final String wildcard = "*";

    public WildcardField(String name) {
        super(name, wildcard);
    }

    @Override
    public IdentityType getType() {
        return IdentityType.Wildcard;
    }
}
