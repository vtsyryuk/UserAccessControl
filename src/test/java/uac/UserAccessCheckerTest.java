package uac;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uac.ResourceIdentity.Builder;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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

    @Test
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
}
