package uac;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uac.ResourceIdentity.Builder;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @BeforeEach
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

        f1wf2vf3v = new Builder()
                .field(new WildcardField("field1"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1wf2vf3vN = new ResourcePermission(f1wf2vf3v, UserAccessLevel.None);
        ResourcePermission f1wf2vf3vR = new ResourcePermission(f1wf2vf3v, UserAccessLevel.Read);
        ResourcePermission f1wf2vf3vW = new ResourcePermission(f1wf2vf3v, UserAccessLevel.Write);

        f1vf2wf3v = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new WildcardField("field2"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1vf2wf3vR = new ResourcePermission(f1vf2wf3v, UserAccessLevel.Read);
        ResourcePermission f1vf2wf3vW = new ResourcePermission(f1vf2wf3v, UserAccessLevel.Write);

        f1vf2vf3w = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "value2"))
                .field(new WildcardField("field3"))
                .build();
        ResourcePermission f1vf2vf3wW = new ResourcePermission(f1vf2vf3w, UserAccessLevel.Write);

        f1wf2wf3w = new Builder()
                .field(new WildcardField("field1"))
                .field(new WildcardField("field2"))
                .field(new WildcardField("field3"))
                .build();
        ResourcePermission f1wf2wf3wR = new ResourcePermission(f1wf2wf3w, UserAccessLevel.Read);

        f1wf2wf3v = new Builder()
                .field(new WildcardField("field1"))
                .field(new WildcardField("field2"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1wf2wf3vR = new ResourcePermission(f1wf2wf3v, UserAccessLevel.Read);

        f1vf2wf3w = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new WildcardField("field2"))
                .field(new WildcardField("field3"))
                .build();
        ResourcePermission f1vf2wf3wN = new ResourcePermission(f1vf2wf3w, UserAccessLevel.None);

        f1wf2vf3w = new Builder()
                .field(new WildcardField("field1"))
                .field(new ValueField("field2", "value2"))
                .field(new WildcardField("field3"))
                .build();
        ResourcePermission f1wf2vf3wR = new ResourcePermission(f1wf2vf3w, UserAccessLevel.Read);

        f1vf2vf3v = new Builder()
                .field(new ValueField("field1", "value1"))
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

    @AfterEach
    public void tearDown() {
        Mockito.reset(uac);
        uacRepository.clear();
    }

    @Test
    public void testAccessLevelNoneForEmptyIdentity() {
        ValueField emptyField = new ValueField("", "");
        ResourceIdentity ri = new Builder().field(emptyField).build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", ri));
    }

    @Test
    public void testAccessLevelNoneForNotRegisteredIdentity() {
        ResourceIdentity ri = new Builder()
                .field(new ValueField("field", "value"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", ri));
    }

    @Test
    public void testIncompleteIdentityAllowed() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        ResourceIdentity f1wf2vf3w = new Builder()
                .field(new ValueField("field2", "value2"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2vf3w));

        ResourceIdentity f1wf2xf3w = new Builder()
                .field(new ValueField("field2", "xyz"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2xf3w));

        ResourceIdentity f1vf2wf3v = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field3", "value3"))
                .field(new ValueField("field4", "value4"))
                .build();
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2wf3v));
    }

    @Test
    public void testIncompleteIdentityCannotBeMatched() {
        uacRepository.clear();
        ResourcePermission f1vf2vf3vR = new ResourcePermission(f1vf2vf3v, UserAccessLevel.Read);
        ResourcePermission f1vf2vf3vW = new ResourcePermission(f1vf2vf3v, UserAccessLevel.Write);
        ResourcePermission f1vf2vf3wW = new ResourcePermission(f1vf2vf3w, UserAccessLevel.Write);

        assertEquals(f1vf2vf3vR, new ResourcePermission(f1vf2vf3v, UserAccessLevel.Read));

        uacRepository.add(f1vf2vf3wW);
        uacRepository.add(f1vf2vf3vR);
        uacRepository.add(f1vf2vf3vW);

        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        ResourceIdentity f1wf2vf3w = new Builder()
                .field(new ValueField("field2", "value2"))
                .build();
        assertNotEquals(f1wf2vf3w, this.f1wf2vf3w);
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1wf2vf3w));

        ResourceIdentity f1wf2xf3w = new Builder()
                .field(new ValueField("field2", "xyz"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1wf2xf3w));

        ResourceIdentity f1vf2wf3v = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field3", "value3"))
                .field(new ValueField("field4", "value4"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1vf2wf3v));
    }

    @Test
    public void testUnknownFieldsAreIgnored() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);

        ResourceIdentity f1vf2xf3v = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "value3"))
                .field(new ValueField("field4", "value4"))
                .field(new ValueField("field5", "value5"))
                .build();
        assertEquals(UserAccessLevel.Write, checker.getLevel("user1", f1vf2xf3v));
    }

    @Test
    public void testDuplicateFieldOverridesLastDefinition() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);

        ResourceIdentity ri = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field2", "value3"))
                .build();
        assertEquals(2, ri.getFieldMap().size());
        assertEquals("value1", ri.getFieldMap().get("field1").getValue());
        assertEquals("value3", ri.getFieldMap().get("field2").getValue());
    }

    @Test
    public void testAccessLevelNoneForUnknownUser() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        ResourceIdentity ri = new Builder()
                .field(new WildcardField("field1"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "value3"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user2", ri));
    }

    @Test
    public void testAccessLevelNoneForNullPermissionSet() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(null);
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1vf2vf3v));
    }

    @Test
    public void testAccessLevelNoneForEmptyPermissionSet() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(new HashSet<>());
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1vf2vf3v));
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
    public void testExactMatchingIdentities() {
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
    public void testWildcardRules() {
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
        |   *    |   *    |  xyz   | Read  |
        |   *    |  xyz   |   *    | Read  |
        |  xyz   |   *    |   *    | Read  |
        |  xyz   |   *    |  xyz   | Read  |
        |   *    |  xyz   |  xyz   | Read  |
        |  xyz   |  xyz   |   *    | Read  |
         ----------------------------------
        |   *    |   *    | value3 | Read  |
        |  xyz   |  xyz   | value3 | Read  |
        |  xyz   |   *    | value3 | Read  |
        |   *    |  xyz   | value3 | Read  |
         ----------------------------------
        | value1 |   *    |   *    | None  |
        | value1 |  xyz   |  xyz   | None  |
        | value1 |  xyz   |   *    | None  |
        | value1 |   *    |  xyz   | None  |
         ----------------------------------
        |   *    | value2 |   *    | Read  |
        |  xyz   | value2 |  xyz   | Read  |
        |   *    | value2 |  xyz   | Read  |
        |  xyz   | value2 |   *    | Read  |
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
        ResourceIdentity f1xf2vf3v = new Builder()
                .field(new ValueField("field1", "xyz"))
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
        ResourceIdentity f1vf2xf3v = new Builder()
                .field(new ValueField("field1", "value1"))
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
        ResourceIdentity f1vf2vf3x = new Builder()
                .field(new ValueField("field1", "value1"))
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
        |   *    |   *    |  xyz   | Read  |
        |   *    |  xyz   |   *    | Read  |
        |  xyz   |   *    |   *    | Read  |
        |  xyz   |   *    |  xyz   | Read  |
        |   *    |  xyz   |  xyz   | Read  |
        |  xyz   |  xyz   |   *    | Read  |
        */
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2wf3w));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    |   *    | Read  |
        |  xyz   |  xyz   |  xyz   | Read  |
        */
        ResourceIdentity f1xf2xf3x = new Builder()
                .field(new ValueField("field1", "xyz"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2xf3x));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    |   *    | Read  |
        |   *    |   *    |  xyz   | Read  |
        */
        ResourceIdentity f1wf2wf3x = new Builder()
                .field(new WildcardField("field1"))
                .field(new WildcardField("field2"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2wf3x));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    |   *    | Read  |
        |   *    |  xyz   |   *    | Read  |
        */
        ResourceIdentity f1wf2xf3w = new Builder()
                .field(new WildcardField("field1"))
                .field(new ValueField("field2", "xyz"))
                .field(new WildcardField("field3"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2xf3w));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    |   *    | Read  |
        |  xyz   |   *    |   *    | Read  |
        */
        ResourceIdentity f1xf2wf3w = new Builder()
                .field(new ValueField("field1", "xyz"))
                .field(new WildcardField("field2"))
                .field(new WildcardField("field3"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2wf3w));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    |   *    | Read  |
        |  xyz   |   *    |  xyz   | Read  |
        */
        ResourceIdentity f1xf2wf3x = new Builder()
                .field(new ValueField("field1", "xyz"))
                .field(new WildcardField("field2"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2wf3x));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    |   *    | Read  |
        |   *    |  xyz   |  xyz   | Read  |
        */
        ResourceIdentity f1wf2xf3x = new Builder()
                .field(new WildcardField("field1"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2xf3x));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    |   *    | Read  |
        |  xyz   |  xyz   |   *    | Read  |
        */
        ResourceIdentity f1xf2xf3w = new Builder()
                .field(new ValueField("field1", "xyz"))
                .field(new ValueField("field2", "xyz"))
                .field(new WildcardField("field3"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2xf3w));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    | value3 | Read  |
        |  xyz   |  xyz   | value3 | Read  |
        |  xyz   |   *    | value3 | Read  |
        |   *    |  xyz   | value3 | Read  |
         ----------------------------------
        */
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2wf3v));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    | value3 | Read  |
        |  xyz   |  xyz   | value3 | Read  |
         ----------------------------------
        */
        ResourceIdentity f1xf2xf3v = new Builder()
                .field(new ValueField("field1", "xyz"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "value3"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2xf3v));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    | value3 | Read  |
        |  xyz   |   *    | value3 | Read  |
         ----------------------------------
        */
        ResourceIdentity f1xf2wf3v = new Builder()
                .field(new ValueField("field1", "xyz"))
                .field(new WildcardField("field2"))
                .field(new ValueField("field3", "value3"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2wf3v));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    |   *    | value3 | Read  |
        |   *    |  xyz   | value3 | Read  |
         ----------------------------------
        */
        ResourceIdentity f1wf2xf3v = new Builder()
                .field(new WildcardField("field1"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "value3"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2xf3v));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        | value1 |   *    |   *    | None  |
        | value1 |  xyz   |  xyz   | None  |
        | value1 |  xyz   |   *    | None  |
        | value1 |   *    |  xyz   | None  |
         ----------------------------------
        */
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1vf2wf3w));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        | value1 |   *    |   *    | None  |
        | value1 |  xyz   |  xyz   | None  |
         ----------------------------------
        */
        ResourceIdentity f1vf2xf3x = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1vf2xf3x));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        | value1 |   *    |   *    | None  |
        | value1 |  xyz   |   *    | None  |
         ----------------------------------
        */
        ResourceIdentity f1vf2xf3w = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "xyz"))
                .field(new WildcardField("field3"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1vf2xf3w));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        | value1 |   *    |   *    | None  |
        | value1 |   *    |  xyz   | None  |
         ----------------------------------
        */
        ResourceIdentity f1vf2wf3x = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new WildcardField("field2"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.None, checker.getLevel("user1", f1vf2wf3x));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    | value2 |   *    | Read  |
        |  xyz   | value2 |  xyz   | Read  |
        |   *    | value2 |  xyz   | Read  |
        |  xyz   | value2 |   *    | Read  |
         ----------------------------------
        */
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2vf3w));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    | value2 |   *    | Read  |
        |  xyz   | value2 |  xyz   | Read  |
         ----------------------------------
        */
        ResourceIdentity f1xf2vf3x = new Builder()
                .field(new ValueField("field1", "xyz"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2vf3x));

            /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    | value2 |   *    | Read  |
        |   *    | value2 |  xyz   | Read  |
         ----------------------------------
        */
        ResourceIdentity f1wf2vf3x = new Builder()
                .field(new WildcardField("field1"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "xyz"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1wf2vf3x));

        /*
         ----------------------------------
        | field1 | field2 | field3 | level |
         ----------------------------------
        |   *    | value2 |   *    | Read  |
        |  xyz   | value2 |   *    | Read  |
         ----------------------------------
        */
        ResourceIdentity f1xf2vf3w = new Builder()
                .field(new ValueField("field1", "xyz"))
                .field(new ValueField("field2", "value2"))
                .field(new WildcardField("field3"))
                .build();
        assertEquals(UserAccessLevel.Read, checker.getLevel("user1", f1xf2vf3w));
    }

    @Test
    public void testIdentityFieldObjectMethods() {
        ValueField field = new ValueField("field", "value");
        ValueField sameField = new ValueField("field", "value");
        ValueField nullField = new ValueField(null, null);
        ValueField sameNullField = new ValueField(null, null);

        assertEquals(field, field);
        assertEquals(field, sameField);
        assertEquals(field.hashCode(), sameField.hashCode());
        assertEquals(nullField, sameNullField);
        assertEquals(nullField.hashCode(), sameNullField.hashCode());
        assertEquals("IdentityField{name='field', value='value'}", field.toString());

        assertNotEquals(field, null);
        assertNotEquals(field, new WildcardField("field"));
        assertNotEquals(field, new ValueField("other", "value"));
        assertNotEquals(field, new ValueField("field", "other"));
        assertNotEquals(nullField, new ValueField("field", null));
        assertNotEquals(nullField, new ValueField(null, "value"));
    }

    @Test
    public void testResourceIdentityObjectMethods() {
        ResourceIdentity identity = new Builder()
                .field(new ValueField("field", "value"))
                .build();
        ResourceIdentity sameIdentity = new Builder()
                .field(new ValueField("field", "value"))
                .build();
        ResourceIdentity otherIdentity = new Builder()
                .field(new ValueField("field", "other"))
                .build();

        assertEquals(identity, identity);
        assertEquals(identity, sameIdentity);
        assertEquals(identity.hashCode(), sameIdentity.hashCode());
        assertNotEquals(identity, otherIdentity);
        assertNotEquals(identity, null);
        assertNotEquals(identity, "identity");
    }

    @Test
    public void testResourcePermissionObjectMethods() {
        ResourcePermission permission = new ResourcePermission(f1vf2vf3v, UserAccessLevel.Read);
        ResourcePermission samePermission = new ResourcePermission(f1vf2vf3v, UserAccessLevel.Read);
        ResourcePermission otherIdentity = new ResourcePermission(f1wf2vf3v, UserAccessLevel.Read);
        ResourcePermission otherAccessLevel = new ResourcePermission(f1vf2vf3v, UserAccessLevel.Write);
        ResourcePermission nullPermission = new ResourcePermission(null, null);
        ResourcePermission sameNullPermission = new ResourcePermission(null, null);
        ResourcePermission nullIdentityWithLevel = new ResourcePermission(null, UserAccessLevel.Read);
        ResourcePermission identityWithNullLevel = new ResourcePermission(f1vf2vf3v, null);

        assertEquals(permission, permission);
        assertEquals(permission, samePermission);
        assertEquals(permission.hashCode(), samePermission.hashCode());
        assertEquals(nullPermission, sameNullPermission);
        assertEquals(nullPermission.hashCode(), sameNullPermission.hashCode());
        assertEquals(f1vf2vf3v, permission.getIdentity());
        assertEquals(UserAccessLevel.Read, permission.getAccessLevel());

        assertNotEquals(permission, null);
        assertNotEquals(permission, "permission");
        assertNotEquals(permission, otherIdentity);
        assertNotEquals(permission, otherAccessLevel);
        assertNotEquals(nullPermission, nullIdentityWithLevel);
        assertNotEquals(nullPermission, identityWithNullLevel);
    }

    @Test
    public void testFieldMapIsImmutable() {
        ResourceIdentity identity = new Builder()
                .field(new ValueField("field", "value"))
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> identity.getFieldMap().put("other", new ValueField("other", "value")));
    }
}
