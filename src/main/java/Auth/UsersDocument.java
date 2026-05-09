package Auth;

import java.util.ArrayList;
import java.util.List;



// root JSON for users json file (equivalent to the ward state document)
public final class UsersDocument {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private List<AppUser> users = new ArrayList<>();

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<AppUser> getUsers() {
        return users;
    }

    public void setUsers(List<AppUser> users) {
        this.users = users != null ? new ArrayList<>(users) : new ArrayList<>();
    }
}
