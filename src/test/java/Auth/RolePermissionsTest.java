package Auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionsTest {

    @Test
    void guest_viewOnly() {
        assertTrue(RolePermissions.mayViewWard(Role.GUEST));
        assertFalse(RolePermissions.mayRunOptimization(Role.GUEST));
        assertFalse(RolePermissions.mayApproveOrReject(Role.GUEST));
        assertFalse(RolePermissions.mayManualOverride(Role.GUEST));
        assertFalse(RolePermissions.mayAdmitOrDischarge(Role.GUEST));
        assertFalse(RolePermissions.mayEditWardStructure(Role.GUEST));
        assertFalse(RolePermissions.mayManageUsers(Role.GUEST));
    }

    @Test
    void nurse_clinicalWorkflow() {
        assertTrue(RolePermissions.mayRunOptimization(Role.NURSE));
        assertTrue(RolePermissions.mayAdmitOrDischarge(Role.NURSE));
        assertFalse(RolePermissions.mayEditWardStructure(Role.NURSE));
        assertFalse(RolePermissions.mayManageUsers(Role.NURSE));
    }

    @Test
    void admin_fullUi() {
        assertTrue(RolePermissions.mayRunOptimization(Role.ADMIN));
        assertTrue(RolePermissions.mayEditWardStructure(Role.ADMIN));
        assertTrue(RolePermissions.mayManageUsers(Role.ADMIN));
    }
}
