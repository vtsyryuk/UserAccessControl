package uac;

public class WildcardField extends IdentityField {

    private static final String WILDCARD = "*";

    public WildcardField(String name) {
        super(name, WILDCARD);
    }

    @Override
    public IdentityType getType() {
        return IdentityType.Wildcard;
    }
}
