package uac;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static uac.ResourceIdentity.Builder;

public final class UserAccessChecker {

    private final UserAccessControl uac;

    public UserAccessChecker(UserAccessControl uac) {
        this.uac = uac;
    }

    private static ResourceIdentity getCompleteIdentity(final ResourceIdentity input, final ResourceIdentity pattern) {
        final Set<String> fieldSetDiff = new HashSet<>(input.getFieldMap().keySet());
        for (String fieldName : pattern.getFieldMap().keySet()) {
            if (!fieldSetDiff.add(fieldName)) {
                fieldSetDiff.remove(fieldName);
            }
        }

        if (fieldSetDiff.isEmpty()) {
            return input;
        }

        Builder riBuilder = new Builder();
        for (String fieldName : fieldSetDiff) {
            riBuilder = riBuilder.field(new WildcardField(fieldName));
        }
        for (Map.Entry<String, IdentityField> entry : input.getFieldMap().entrySet()) {
            riBuilder = riBuilder.field(entry.getValue());
        }
        return riBuilder.build();
    }

    private static UserAccessLevel getResultedLevel(Set<ResourcePermission> permissionSet) {
        UserAccessLevel ual = UserAccessLevel.NONE;
        for (ResourcePermission p : permissionSet) {
            UserAccessLevel level = p.getAccessLevel();
            if (level == UserAccessLevel.WRITE) {
                ual = level;
            } else if (level == UserAccessLevel.READ && ual != UserAccessLevel.WRITE) {
                ual = level;
            } else if (level == UserAccessLevel.NONE) {
                return UserAccessLevel.NONE;
            }
        }
        return ual;
    }

    public UserAccessLevel getLevel(String userName, final ResourceIdentity identity) {

        final Set<ResourcePermission> permissionSet = uac.getPermissionSet(userName);
        if (permissionSet == null || permissionSet.isEmpty()) {
            return UserAccessLevel.NONE;
        }

        final ResourceIdentity patternIdentity = permissionSet.iterator().next().getIdentity();
        final ResourceIdentity lookupIdentity = getCompleteIdentity(identity, patternIdentity);

        final Map<ResourcePermission, Integer> freqMap = new HashMap<>();
        for (ResourcePermission permission : permissionSet) {
            freqMap.put(permission, getFrequency(permission.getIdentity().getFieldMap(), lookupIdentity));
        }

        final int maxFreq = freqMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (maxFreq == 0) {
            return UserAccessLevel.NONE;
        }

        final Set<ResourcePermission> bestMatch = new HashSet<>();
        for (Map.Entry<ResourcePermission, Integer> entry : freqMap.entrySet()) {
            if (Objects.equals(entry.getValue(), maxFreq)) {
                bestMatch.add(entry.getKey());
            }
        }

        return getResultedLevel(bestMatch);
    }

    private static int getFrequency(Map<String, IdentityField> fieldMap, ResourceIdentity lookupIdentity) {
        int count = 0;
        for (final IdentityField f : lookupIdentity.getFieldMap().values()) {
            final IdentityField lookupField = fieldMap.get(f.getName());
            if (lookupField == null) {
                continue; // Unknown fields are ignored.
            }
            if (lookupField.equals(f)) {
                count += 2;
            } else if (lookupField.getType() == IdentityType.WILDCARD) {
                count++;
            } else {
                return 0;
            }
        }
        return count;
    }
}
