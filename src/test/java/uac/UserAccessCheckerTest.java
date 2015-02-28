package uac;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class UserAccessCheckerTest {

    private UserAccessChecker checker;
    private UserAccessControl uac;
    private HashSet<ResourcePermission> uacRepository;
    private ResourceIdentity f1wf2vf3v;
    private ResourceIdentity f1vf2wf3v;
    private ResourceIdentity f1vf2vf3w;
    private ResourceIdentity f1wf2wf3w;
    private ResourceIdentity f1wf2wf3v;
    private ResourceIdentity f1vf2wf3w;
    private ResourceIdentity f1wf2vf3w;
    private ResourceIdentity f1vf2vf3v;

    @Before
    public void setUp() {
        uac = Mockito.mock(UserAccessControl.class);
        checker = new UserAccessChecker(uac);
        uacRepository = new HashSet<>();

        f1wf2vf3v = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1wf2vf3vNone = new ResourcePermission(f1wf2vf3v, UserAccessLevel.None);

        f1vf2wf3v = new ResourceIdentity.Builder(new WildcardField("field2", "*"))
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1vf2wf3vRead = new ResourcePermission(f1vf2wf3v, UserAccessLevel.Read);

        f1vf2vf3w = new ResourceIdentity.Builder(new WildcardField("field3", "*"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field1", "value1"))
                .build();
        ResourcePermission f1vf2vf3wWrite = new ResourcePermission(f1vf2vf3w, UserAccessLevel.Write);

        f1wf2wf3w = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new WildcardField("field2", "*"))
                .field(new WildcardField("field3", "*"))
                .build();
        ResourcePermission f1wf2wf3wRead = new ResourcePermission(f1wf2wf3w, UserAccessLevel.Read);

        f1wf2wf3v = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new WildcardField("field2", "*"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1wf2wf3vRead = new ResourcePermission(f1wf2wf3v, UserAccessLevel.Read);

        f1vf2wf3w = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new WildcardField("field2", "*"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1vf2wf3wRead = new ResourcePermission(f1vf2wf3w, UserAccessLevel.Read);

        f1wf2vf3w = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new WildcardField("field2", "*"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1wf2vf3wRead = new ResourcePermission(f1wf2vf3w, UserAccessLevel.Read);

        f1vf2vf3v = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new WildcardField("field2", "*"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1vf2vf3vRead = new ResourcePermission(f1vf2vf3v, UserAccessLevel.Read);

        uacRepository.add(f1wf2vf3vNone);
        uacRepository.add(f1vf2wf3vRead);
        uacRepository.add(f1vf2vf3wWrite);
        uacRepository.add(f1wf2wf3wRead);
        uacRepository.add(f1wf2wf3vRead);
        uacRepository.add(f1vf2wf3wRead);
        uacRepository.add(f1wf2vf3wRead);
        uacRepository.add(f1vf2vf3vRead);
    }

    @Test
    public void tearDown() {
        Mockito.reset(uac);
        uacRepository.clear();
    }

    @Test
    public void testAccessLevelNoneForEmptyIdentity() {
        ValueField emptyField = new ValueField("", "");
        ResourceIdentity ri = new ResourceIdentity.Builder(emptyField).build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", ri));
    }

    @Test
    public void testAccessLevelNoneForNonExistingIdentity() {
        ResourceIdentity ri = new ResourceIdentity.Builder(new ValueField("field", "value")).build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", ri));
    }

    @Test
    public void testAccessLevelNoneForUnknownUser() {
        ResourceIdentity ri = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "value3"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", ri));
    }

    @Test
    public void testAccessLevelCorrect() {

        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);

        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1wf2vf3v));
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1vf2wf3v));
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2vf3w));
    }

    @Test
    public void testWildcardRules() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);

        ResourceIdentity ri2 = new ResourceIdentity.Builder(new ValueField("field2", "value2"))
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field3", "value3"))
                .build();

        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1wf2vf3v));

    }
}
