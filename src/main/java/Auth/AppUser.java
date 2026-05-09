package Auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


// single user row in users json file
// annotated with JsonCreator and JsonProperty to be serialized and deserialized by Jackson
public final class AppUser {

    private String username;
    private String password;
    private Role role;

    public AppUser() {
    }
    // json can build from this constructor
    @JsonCreator
    public AppUser(
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("role") Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
