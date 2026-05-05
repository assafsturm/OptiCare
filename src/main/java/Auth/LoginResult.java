package Auth;

/** Successful login outcome (plaintext demo auth). */
public record LoginResult(String username, Role role) {
}
