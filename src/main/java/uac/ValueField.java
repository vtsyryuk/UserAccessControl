package uac;

public class ValueField extends IdentityField {

    public ValueField(String name, String value) {
        super(name, value);
    }

    @Override
    public IdentityType getType() {
        return IdentityType.Value;
    }
}
