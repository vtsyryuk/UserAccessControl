package uac;

import org.junit.Before;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;

public class UserAccessCheckerTest {

    @Before
    public void setUp() {

        UserAccessControl<String, List<String>> uac = Mockito.mock(UserAccessControl.class);
        HashMap<String, Permission<String>> uacRepository = new HashMap<>();
        uacRepository.put("id1", new Permission<>("id1", UserAccessLevel.None));
        uacRepository.put("id1", new Permission<>("id1", UserAccessLevel.Write));
        uacRepository.put("id1", new Permission<>("id1", UserAccessLevel.Read));
        Mockito.when(uac.getPermissionSet("user1", "id1")).thenReturn(uacRepository);

        UserAccessChecker checker = new UserAccessChecker();
    }
}
