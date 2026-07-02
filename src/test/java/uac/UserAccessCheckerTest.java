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

class UserAccessCheckerTest {

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
    void setUp() {
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
        ResourcePermission f1wf2vf3vN = new ResourcePermission(f1wf2vf3v, UserAccessLevel.NONE);
        ResourcePermission f1wf2vf3vR = new ResourcePermission(f1wf2vf3v, UserAccessLevel.READ);
        ResourcePermission f1wf2vf3vW = new ResourcePermission(f1wf2vf3v, UserAccessLevel.WRITE);

        f1vf2wf3v = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new WildcardField("field2"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1vf2wf3vR = new ResourcePermission(f1vf2wf3v, UserAccessLevel.READ);
        ResourcePermission f1vf2wf3vW = new ResourcePermission(f1vf2wf3v, UserAccessLevel.WRITE);

        f1vf2vf3w = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "value2"))
                .field(new WildcardField("field3"))
                .build();
        ResourcePermission f1vf2vf3wW = new ResourcePermission(f1vf2vf3w, UserAccessLevel.WRITE);

        f1wf2wf3w = new Builder()
                .field(new WildcardField("field1"))
                .field(new WildcardField("field2"))
                .field(new WildcardField("field3"))
                .build();
        ResourcePermission f1wf2wf3wR = new ResourcePermission(f1wf2wf3w, UserAccessLevel.READ);

        f1wf2wf3v = new Builder()
                .field(new WildcardField("field1"))
                .field(new WildcardField("field2"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1wf2wf3vR = new ResourcePermission(f1wf2wf3v, UserAccessLevel.READ);

        f1vf2wf3w = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new WildcardField("field2"))
                .field(new WildcardField("field3"))
                .build();
        ResourcePermission f1vf2wf3wN = new ResourcePermission(f1vf2wf3w, UserAccessLevel.NONE);

        f1wf2vf3w = new Builder()
                .field(new WildcardField("field1"))
                .field(new ValueField("field2", "value2"))
                .field(new WildcardField("field3"))
                .build();
        ResourcePermission f1wf2vf3wR = new ResourcePermission(f1wf2vf3w, UserAccessLevel.READ);

        f1vf2vf3v = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "value3"))
                .build();
        ResourcePermission f1vf2vf3vR = new ResourcePermission(f1vf2vf3v, UserAccessLevel.READ);
        ResourcePermission f1vf2vf3vW = new ResourcePermission(f1vf2vf3v, UserAccessLevel.WRITE);

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
    void tearDown() {
        Mockito.reset(uac);
        uacRepository.clear();
    }

    private void assertAccess(UserAccessLevel expected, ResourceIdentity identity) {
        assertEquals(expected, checker.getLevel("user1", identity));
    }

    @Test
    void testAccessLevelNoneForEmptyIdentity() {
        ValueField emptyField = new ValueField("", "");
        ResourceIdentity ri = new Builder().field(emptyField).build();
        assertAccess(UserAccessLevel.NONE, ri);
    }

    @Test
    void testAccessLevelNoneForNotRegisteredIdentity() {
        ResourceIdentity ri = new Builder()
                .field(new ValueField("field", "value"))
                .build();
        assertAccess(UserAccessLevel.NONE, ri);
    }

    @Test
    void testIncompleteIdentityAllowed() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        var f1wf2vf3wLookup = new Builder()
                .field(new ValueField("field2", "value2"))
                .build();
        assertAccess(UserAccessLevel.READ, f1wf2vf3wLookup);

        ResourceIdentity f1wf2xf3wLookup = new Builder()
                .field(new ValueField("field2", "xyz"))
                .build();
        assertAccess(UserAccessLevel.READ, f1wf2xf3wLookup);

        ResourceIdentity f1vf2wf3vLookup = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field3", "value3"))
                .field(new ValueField("field4", "value4"))
                .build();
        assertAccess(UserAccessLevel.WRITE, f1vf2wf3vLookup);
    }

    @Test
    void testIncompleteIdentityCannotBeMatched() {
        uacRepository.clear();
        ResourcePermission f1vf2vf3vR = new ResourcePermission(f1vf2vf3v, UserAccessLevel.READ);
        ResourcePermission f1vf2vf3vW = new ResourcePermission(f1vf2vf3v, UserAccessLevel.WRITE);
        ResourcePermission f1vf2vf3wW = new ResourcePermission(f1vf2vf3w, UserAccessLevel.WRITE);

        assertEquals(f1vf2vf3vR, new ResourcePermission(f1vf2vf3v, UserAccessLevel.READ));

        uacRepository.add(f1vf2vf3wW);
        uacRepository.add(f1vf2vf3vR);
        uacRepository.add(f1vf2vf3vW);

        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        ResourceIdentity f1wf2vf3wLookup = new Builder()
                .field(new ValueField("field2", "value2"))
                .build();
        assertNotEquals(f1wf2vf3wLookup, this.f1wf2vf3w);
        assertAccess(UserAccessLevel.NONE, f1wf2vf3wLookup);

        ResourceIdentity f1wf2xf3wLookup = new Builder()
                .field(new ValueField("field2", "xyz"))
                .build();
        assertAccess(UserAccessLevel.NONE, f1wf2xf3wLookup);

        ResourceIdentity f1vf2wf3vLookup = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field3", "value3"))
                .field(new ValueField("field4", "value4"))
                .build();
        assertAccess(UserAccessLevel.NONE, f1vf2wf3vLookup);
    }

    @Test
    void testUnknownFieldsAreIgnored() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);

        ResourceIdentity f1vf2xf3v = new Builder()
                .field(new ValueField("field1", "value1"))
                .field(new ValueField("field2", "xyz"))
                .field(new ValueField("field3", "value3"))
                .field(new ValueField("field4", "value4"))
                .field(new ValueField("field5", "value5"))
                .build();
        assertAccess(UserAccessLevel.WRITE, f1vf2xf3v);
    }

    @Test
    void testDuplicateFieldOverridesLastDefinition() {
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
    void testAccessLevelNoneForUnknownUser() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        ResourceIdentity ri = new Builder()
                .field(new WildcardField("field1"))
                .field(new ValueField("field2", "value2"))
                .field(new ValueField("field3", "value3"))
                .build();
        assertEquals(UserAccessLevel.NONE, checker.getLevel("user2", ri));
    }

    @Test
    void testAccessLevelNoneForNullPermissionSet() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(null);
        assertAccess(UserAccessLevel.NONE, f1vf2vf3v);
    }

    @Test
    void testAccessLevelNoneForEmptyPermissionSet() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(new HashSet<>());
        assertAccess(UserAccessLevel.NONE, f1vf2vf3v);
    }

    @Test
    void testResultedPermissionIsUnionOfReadAndWrite() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        assertAccess(UserAccessLevel.WRITE, f1vf2vf3v);
    }

    @Test
    void testNoneRejectsReadWrite() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);
        assertAccess(UserAccessLevel.NONE, f1wf2vf3v);
    }

    @Test
    void testExactMatchingIdentities() {
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

        assertAccess(UserAccessLevel.NONE, f1wf2vf3v);
        assertAccess(UserAccessLevel.WRITE, f1vf2wf3v);
        assertAccess(UserAccessLevel.WRITE, f1vf2vf3w);
        assertAccess(UserAccessLevel.READ, f1wf2wf3w);
        assertAccess(UserAccessLevel.READ, f1wf2wf3v);
        assertAccess(UserAccessLevel.NONE, f1vf2wf3w);
        assertAccess(UserAccessLevel.READ, f1wf2vf3w);
        assertAccess(UserAccessLevel.WRITE, f1vf2vf3v);
    }

    @Test
    void testWildcardRulesForExactAndAllWildcardMatches() {
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
        assertAccess(UserAccessLevel.NONE, f1wf2vf3v);
        assertAccess(UserAccessLevel.NONE, f1xf2vf3v);


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
        assertAccess(UserAccessLevel.WRITE, f1vf2wf3v);
        assertAccess(UserAccessLevel.WRITE, f1vf2xf3v);

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
        assertAccess(UserAccessLevel.WRITE, f1vf2vf3w);
        assertAccess(UserAccessLevel.WRITE, f1vf2vf3x);

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
        assertAccess(UserAccessLevel.READ, f1wf2wf3w);

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
        assertAccess(UserAccessLevel.READ, f1xf2xf3x);

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
        assertAccess(UserAccessLevel.READ, f1wf2wf3x);

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
        assertAccess(UserAccessLevel.READ, f1wf2xf3w);
    }

    @Test
    void testWildcardRulesForReadFallbacks() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);

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
        assertAccess(UserAccessLevel.READ, f1xf2wf3w);

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
        assertAccess(UserAccessLevel.READ, f1xf2wf3x);

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
        assertAccess(UserAccessLevel.READ, f1wf2xf3x);

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
        assertAccess(UserAccessLevel.READ, f1xf2xf3w);

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
        assertAccess(UserAccessLevel.READ, f1wf2wf3v);

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
        assertAccess(UserAccessLevel.READ, f1xf2xf3v);

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
        assertAccess(UserAccessLevel.READ, f1xf2wf3v);

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
        assertAccess(UserAccessLevel.READ, f1wf2xf3v);
    }

    @Test
    void testWildcardRulesForNoneAndField2Matches() {
        Mockito.when(uac.getPermissionSet("user1")).thenReturn(uacRepository);

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
        assertAccess(UserAccessLevel.NONE, f1vf2wf3w);

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
        assertAccess(UserAccessLevel.NONE, f1vf2xf3x);

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
        assertAccess(UserAccessLevel.NONE, f1vf2xf3w);

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
        assertAccess(UserAccessLevel.NONE, f1vf2wf3x);

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
        assertAccess(UserAccessLevel.READ, f1wf2vf3w);

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
        assertAccess(UserAccessLevel.READ, f1xf2vf3x);

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
        assertAccess(UserAccessLevel.READ, f1wf2vf3x);

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
        assertAccess(UserAccessLevel.READ, f1xf2vf3w);
    }

    @Test
    void testIdentityFieldObjectMethods() {
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

        assertNotEquals(null, field);
        assertNotEquals(field, new WildcardField("field"));
        assertNotEquals(field, new ValueField("other", "value"));
        assertNotEquals(field, new ValueField("field", "other"));
        assertNotEquals(nullField, new ValueField("field", null));
        assertNotEquals(nullField, new ValueField(null, "value"));
    }

    @Test
    void testResourceIdentityObjectMethods() {
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
        assertNotEquals(null, identity);
        assertNotEquals("identity", identity);
    }

    @Test
    void testResourcePermissionObjectMethods() {
        ResourcePermission permission = new ResourcePermission(f1vf2vf3v, UserAccessLevel.READ);
        ResourcePermission samePermission = new ResourcePermission(f1vf2vf3v, UserAccessLevel.READ);
        ResourcePermission otherIdentity = new ResourcePermission(f1wf2vf3v, UserAccessLevel.READ);
        ResourcePermission otherAccessLevel = new ResourcePermission(f1vf2vf3v, UserAccessLevel.WRITE);
        ResourcePermission nullPermission = new ResourcePermission(null, null);
        ResourcePermission sameNullPermission = new ResourcePermission(null, null);
        ResourcePermission nullIdentityWithLevel = new ResourcePermission(null, UserAccessLevel.READ);
        ResourcePermission identityWithNullLevel = new ResourcePermission(f1vf2vf3v, null);

        assertEquals(permission, permission);
        assertEquals(permission, samePermission);
        assertEquals(permission.hashCode(), samePermission.hashCode());
        assertEquals(nullPermission, sameNullPermission);
        assertEquals(nullPermission.hashCode(), sameNullPermission.hashCode());
        assertEquals(f1vf2vf3v, permission.getIdentity());
        assertEquals(UserAccessLevel.READ, permission.getAccessLevel());

        assertNotEquals(null, permission);
        assertNotEquals("permission", permission);
        assertNotEquals(permission, otherIdentity);
        assertNotEquals(permission, otherAccessLevel);
        assertNotEquals(nullPermission, nullIdentityWithLevel);
        assertNotEquals(nullPermission, identityWithNullLevel);
    }

    @Test
    void testFieldMapIsImmutable() {
        ResourceIdentity identity = new Builder()
                .field(new ValueField("field", "value"))
                .build();

        var fieldMap = identity.getFieldMap();
        var otherField = new ValueField("other", "value");
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> fieldMap.put("other", otherField));
        assertEquals(UnsupportedOperationException.class, exception.getClass());
    }
}
