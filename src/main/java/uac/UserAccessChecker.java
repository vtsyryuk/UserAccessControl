package uac;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UserAccessChecker {

    private final UserAccessControl uac;

    public UserAccessChecker(UserAccessControl uac) {
        this.uac = uac;
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

        final Function<IdentityField, String> getName = new Function<IdentityField, String>() {
            @Override
            public String apply(IdentityField input) {
                return input.getName();
            }
        };

        List<IdentityField> uacFields = Iterables.get(permissionSet, 0).getIdentity().getFields();
        final Set<String> uacFieldNames = Sets.newHashSet(Iterables.transform(uacFields, getName));
        final Set<String> inputFieldNames = Sets.newHashSet(Iterables.transform(identity.getFields(), getName));

        if (!Sets.symmetricDifference(inputFieldNames, uacFieldNames).isEmpty()) {
            throw new IllegalArgumentException(identity + " - number of fields do not match permission repository configuration");
        }

        Map<ResourcePermission, Integer> matchFreq = Maps.asMap(permissionSet, new Function<ResourcePermission, Integer>() {
            @Override
            public Integer apply(ResourcePermission permission) {
                int count = 0;
                final ImmutableMap<String, IdentityField> fieldMap = Maps.uniqueIndex(permission.getIdentity().getFields(), getName);
                for (final IdentityField f : identity.getFields()) {
                    IdentityField soughtField = fieldMap.get(f.getName());
                    if (soughtField.equals(f)) {
                        count += 2;
                    } else if (soughtField.getType() == IdentityType.Wildcard) {
                        count++;
                    }
                }
                return count;
            }
        });

        final int maxFreq = Collections.max(matchFreq.values());
        Set<ResourcePermission> bestMatch = Maps.filterEntries(matchFreq, new Predicate<Map.Entry<ResourcePermission, Integer>>() {
            @Override
            public boolean apply(Map.Entry<ResourcePermission, Integer> input) {
                return input.getValue() == maxFreq;
            }
        }).keySet();

        return getResultedLevel(bestMatch);
    }
}
