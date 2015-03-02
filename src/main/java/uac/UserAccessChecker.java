package uac;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static uac.ResourceIdentity.Builder;

public final class UserAccessChecker {

    private final UserAccessControl uac;

    public UserAccessChecker(UserAccessControl uac) {
        this.uac = uac;
    }

    private static ResourceIdentity getCompleteIdentity(final ResourceIdentity input, final ResourceIdentity pattern) {
        final Sets.SetView<String> fieldSetDiff = Sets.symmetricDifference(
                input.getFieldMap().keySet(),
                pattern.getFieldMap().keySet());

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
        UserAccessLevel ual = UserAccessLevel.None;
        for (ResourcePermission p : permissionSet) {
            UserAccessLevel level = p.getAccessLevel();
            if (level == UserAccessLevel.Write) {
                ual = level;
            } else if (level == UserAccessLevel.Read && ual != UserAccessLevel.Write) {
                ual = level;
            } else if (level == UserAccessLevel.None) {
                return UserAccessLevel.None;
            }
        }
        return ual;
    }

    public UserAccessLevel getLevel(String userName, final ResourceIdentity identity) {

        final Set<ResourcePermission> permissionSet = uac.getPermissionSet(userName);
        if (permissionSet.isEmpty()) {
            return UserAccessLevel.None;
        }

        final ResourceIdentity patternIdentity = Iterables.get(permissionSet, 0).getIdentity();
        final ResourceIdentity lookupIdentity = getCompleteIdentity(identity, patternIdentity);

        final Map<ResourcePermission, Integer> freqMap = Maps.asMap(permissionSet, new Function<ResourcePermission, Integer>() {
            @Override
            public Integer apply(ResourcePermission permission) {
                final ImmutableMap<String, IdentityField> fieldMap = permission.getIdentity().getFieldMap();
                return getFrequency(fieldMap);
            }

            private Integer getFrequency(ImmutableMap<String, IdentityField> fieldMap) {
                int count = 0;
                for (final IdentityField f : lookupIdentity.getFieldMap().values()) {
                    final IdentityField lookupField = fieldMap.get(f.getName());
                    if (lookupField == null) {
                        continue; //NOTE: unknown fields are ignored
                    }
                    if (lookupField.equals(f)) {
                        count += 2;
                    } else if (lookupField.getType() == IdentityType.Wildcard) {
                        count++;
                    } else {
                        return 0; //NOTE: field is not matching to any
                    }
                }
                return count;
            }
        });

        final int maxFreq = Collections.max(freqMap.values());
        if (maxFreq == 0) {
            return UserAccessLevel.None;
        }
        final Set<ResourcePermission> bestMatch = Maps.filterEntries(freqMap, new Predicate<Map.Entry<ResourcePermission, Integer>>() {
            @Override
            public boolean apply(Map.Entry<ResourcePermission, Integer> input) {
                return input.getValue() == maxFreq;
            }
        }).keySet();

        return getResultedLevel(bestMatch);
    }
}
