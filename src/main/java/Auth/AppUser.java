package Auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Single user row in {@code users.json} (plaintext password — coursework only). */
public final class AppUser {

    private String username;
    private String password;
    private Role role;

    public AppUser() {
    }

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
