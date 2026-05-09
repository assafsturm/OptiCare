package Auth;


// get the permissions for the role
public final class RolePermissions {

    private RolePermissions() {
    }

    public static boolean mayViewWard(Role role) { // any role can view the ward
        return role != null;
    }

    public static boolean mayRunOptimization(Role role) { // only nurse and admin can run optimization
        return role == Role.NURSE || role == Role.ADMIN;
    }

    public static boolean mayApproveOrReject(Role role) { // only nurse and admin can approve or reject
        return role == Role.NURSE || role == Role.ADMIN;
    }

    public static boolean mayManualOverride(Role role) { // only nurse and admin can manually override
        return role == Role.NURSE || role == Role.ADMIN;
    }

    public static boolean mayAdmitOrDischarge(Role role) { // only nurse and admin can admit or discharge
        return role == Role.NURSE || role == Role.ADMIN;
    }

    public static boolean mayEditWardStructure(Role role) { // only admin can edit the ward structure
        return role == Role.ADMIN;
    }

    public static boolean mayManageUsers(Role role) { // only admin can manage users
        return role == Role.ADMIN;
    }
}
