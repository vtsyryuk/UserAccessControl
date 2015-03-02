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

        /* setting up permission table
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    | value2 | value3 | None  |
        |   *    | value2 | value3 | Read  |
        |   *    | value2 | value3 | Write |
         ----------------------------------
        | value1 |   *    | value3 | Read  |
        | value1 |   *    | value3 | Write |
         ----------------------------------
        | value1 | value2 |   *    | Write |
        |   *    |   *    |   *    | Read  |
        |   *    |   *    | value3 | Read  |
        | value1 |   *    |   *    | None  |
        |   *    | value2 |   *    | Read  |
         ----------------------------------
        | value1 | value2 | value3 | Read  |
        | value1 | value2 | value3 | Write |
        */

        f1wf2vf3v = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1wf2vf3vN = new ResourcePermission(f1wf2vf3v, UserAccessLevel.None);
        ResourcePermission f1wf2vf3vR = new ResourcePermission(f1wf2vf3v, UserAccessLevel.Read);
        ResourcePermission f1wf2vf3vW = new ResourcePermission(f1wf2vf3v, UserAccessLevel.Write);

        f1vf2wf3v = new ResourceIdentity.Builder(new ValueField("field1", "value1"))
                .field(new WildcardField("field2", "*"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1vf2wf3vR = new ResourcePermission(f1vf2wf3v, UserAccessLevel.Read);
        ResourcePermission f1vf2wf3vW = new ResourcePermission(f1vf2wf3v, UserAccessLevel.Write);

        f1vf2vf3w = new ResourceIdentity.Builder(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "value2"))
                .field(new WildcardField("field3", "*"))
                .build();
        ResourcePermission f1vf2vf3wW = new ResourcePermission(f1vf2vf3w, UserAccessLevel.Write);

        f1wf2wf3w = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new WildcardField("field2", "*"))
                .field(new WildcardField("field3", "*"))
                .build();
        ResourcePermission f1wf2wf3wR = new ResourcePermission(f1wf2wf3w, UserAccessLevel.Read);

        f1wf2wf3v = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new WildcardField("field2", "*"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1wf2wf3vR = new ResourcePermission(f1wf2wf3v, UserAccessLevel.Read);

        f1vf2wf3w = new ResourceIdentity.Builder(new ValueField("field1", "value1"))
                .field(new WildcardField("field2", "*"))
                .field(new WildcardField("field3", "*"))
                .build();
        ResourcePermission f1vf2wf3wN = new ResourcePermission(f1vf2wf3w, UserAccessLevel.None);

        f1wf2vf3w = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new ValueField("field2", "value2"))
                .field(new WildcardField("field3", "*"))
                .build();
        ResourcePermission f1wf2vf3wR = new ResourcePermission(f1wf2vf3w, UserAccessLevel.Read);

        f1vf2vf3v = new ResourceIdentity.Builder(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1vf2vf3vR = new ResourcePermission(f1vf2vf3v, UserAccessLevel.Read);
        ResourcePermission f1vf2vf3vW = new ResourcePermission(f1vf2vf3v, UserAccessLevel.Write);

        uacRepository.add(f1wf2vf3vN);
        uacRepository.add(f1wf2vf3vR);
        uacRepository.add(f1wf2vf3vW);

        uacRepository.add(f1vf2wf3vR);
        uacRepository.add(f1vf2wf3vW);

        uacRepository.add(f1vf2vf3wW);

        uacRepository.add(f1wf2wf3wR);

        uacRepository.add(f1wf2wf3vR);

        uacRepository.add(f1vf2wf3wN);

        uacRepository.add(f1wf2vf3wR);

        uacRepository.add(f1vf2vf3vR);
        uacRepository.add(f1vf2vf3vW);
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
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        ResourceIdentity ri = new ResourceIdentity.Builder(new WildcardField("field1", "*"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "value3"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user2", ri));
    }

    @Test
    public void testResultedPermissionIsUnionOfReadAndWrite() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2vf3v));
    }

    @Test
    public void testNoneRejectsReadWrite() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1wf2vf3v));
    }

    @Test
    public void testMatchingIdentitiesHaveConfiguredAccessLevel() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);

        /*
         -------------------------------------------
        | field1 | field2 | field3 | level | result |
         -------------------------------------------
        |   *    | value2 | value3 | None  |        |
        |   *    | value2 | value3 | Read  | None   |
        |   *    | value2 | value3 | Write |        |
         -------------------------------------------
        | value1 |   *    | value3 | Read  |        |
        | value1 |   *    | value3 | Write | Write  |
         -------------------------------------------
        | value1 | value2 |   *    | Write |        |
        |   *    |   *    |   *    | Read  |        |
        |   *    |   *    | value3 | Read  |        |
        | value1 |   *    |   *    | None  |        |
        |   *    | value2 |   *    | Read  |        |
         -------------------------------------------
        | value1 | value2 | value3 | Read  |        |
        | value1 | value2 | value3 | Write | Write  |
        */

        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1wf2vf3v));
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2wf3v));
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2vf3w));
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2wf3w));
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2wf3v));
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1vf2wf3w));
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2vf3w));
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2vf3v));
    }

    @Test
    public void testClosestMatchingWildcardIdentityTakesPlace() {
        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    | value2 | value3 | None  |
        |  xyz   | value2 | value3 | None  |
         ----------------------------------
        | value1 |   *    | value3 | Write |
        | value1 |  xyz   | value3 | Write |
         ----------------------------------
        | value1 | value2 |   *    | Write |
        | value1 | value2 |  xyz   | Write |
         ----------------------------------
        |   *    |   *    |   *    | Read  |
        |  xyz   |  xyz   |  xyz   | Read  |
         ----------------------------------
        |   *    |   *    | value3 | Read  |
        |  xyz   |  xyz   | value3 | Read  |
         ----------------------------------
        | value1 |   *    |   *    | None  |
        | value1 |  xyz   |  xyz   | None  |
         ----------------------------------
        |   *    | value2 |   *    | Read  |
        |  xyz   | value2 |  xyz   | Read  |
         ----------------------------------
        | value1 | value2 | value3 | Write |
        */
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    | value2 | value3 | None  |
        |  xyz   | value2 | value3 | None  |
         ----------------------------------
         */
        ResourceIdentity f1xf2vf3v =  new ResourceIdentity.Builder(new ValueField("field1", "xyz"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "value3"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1wf2vf3v));
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1xf2vf3v));


        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        | value1 |   *    | value3 | Write |
        | value1 |  xyz   | value3 | Write |
         ----------------------------------
        */
        ResourceIdentity f1vf2xf3v = new ResourceIdentity.Builder(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "value3"))
                .build();
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2wf3v));
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2xf3v));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        | value1 | value2 |   *    | Write |
        | value1 | value2 |  xyz   | Write |
         ----------------------------------
        */
        ResourceIdentity f1vf2vf3x = new ResourceIdentity.Builder(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2vf3w));
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2vf3x));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    |   *    | Read  |
        |  xyz   |  xyz   |  xyz   | Read  |
         ----------------------------------
        */
        ResourceIdentity f1xf2xf3x = new ResourceIdentity.Builder(new ValueField("field1", "xyz"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2wf3w));
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2xf3x));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    | value3 | Read  |
        |  xyz   |  xyz   | value3 | Read  |
         ----------------------------------
        */
        ResourceIdentity f1xf2xf3v = new ResourceIdentity.Builder(new ValueField("field1", "xyz"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "value3"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2wf3v));
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2xf3v));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        | value1 |   *    |   *    | None  |
        | value1 |  xyz   |  xyz   | None  |
         ----------------------------------
        */
        ResourceIdentity f1vf2xf3x = new ResourceIdentity.Builder(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1vf2wf3w));
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1vf2xf3x));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    | value2 |   *    | Read  |
        |  xyz   | value2 |  xyz   | Read  |
         ----------------------------------
        */
        ResourceIdentity f1xf2vf3x = new ResourceIdentity.Builder(new ValueField("field1", "xyz"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2vf3w));
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2vf3x));
    }
}
