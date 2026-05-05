package Auth;

/** Central place for minimal role checks (UI + handler guards). */
public final class RolePermissions {

    private RolePermissions() {
    }

    public static boolean mayViewWard(Role role) {
        return role != null;
    }

    public static boolean mayRunOptimization(Role role) {
        return role == Role.NURSE || role == Role.ADMIN;
    }

    public static boolean mayApproveOrReject(Role role) {
        return role == Role.NURSE || role == Role.ADMIN;
    }

    public static boolean mayManualOverride(Role role) {
        return role == Role.NURSE || role == Role.ADMIN;
    }

    public static boolean mayAdmitOrDischarge(Role role) {
        return role == Role.NURSE || role == Role.ADMIN;
    }

    public static boolean mayEditWardStructure(Role role) {
        return role == Role.ADMIN;
    }

    public static boolean mayManageUsers(Role role) {
        return role == Role.ADMIN;
    }
}
