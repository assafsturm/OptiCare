package Auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFileUsersRepositoryTest {

    @Test
    void loadOrInitialize_createsDefaultUsersWhenMissing(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("users.json");
        JsonFileUsersRepository repo = new JsonFileUsersRepository(file);
        UsersDocument doc = repo.loadOrInitialize();
        assertEquals(2, doc.getUsers().size());
        assertTrue(doc.getUsers().stream().anyMatch(u -> "admin".equals(u.getUsername())));
        assertTrue(doc.getUsers().stream().anyMatch(u -> "nurse".equals(u.getUsername())));
    }

    @Test
    void authenticate_nurseValid(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("users.json");
        JsonFileUsersRepository repo = new JsonFileUsersRepository(file);
        repo.loadOrInitialize();
        Optional<LoginResult> r = repo.authenticate("nurse", "nurse");
        assertTrue(r.isPresent());
        assertEquals(Role.NURSE, r.get().role());
        assertEquals("nurse", r.get().username());
    }

    @Test
    void authenticate_wrongPassword(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("users.json");
        JsonFileUsersRepository repo = new JsonFileUsersRepository(file);
        repo.loadOrInitialize();
        assertTrue(repo.authenticate("nurse", "bad").isEmpty());
    }

    @Test
    void addUser_thenAuthenticate(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("users.json");
        JsonFileUsersRepository repo = new JsonFileUsersRepository(file);
        repo.loadOrInitialize();
        repo.addUser(new AppUser("u2", "p2", Role.NURSE));
        Optional<LoginResult> r = repo.authenticate("u2", "p2");
        assertTrue(r.isPresent());
        assertEquals(Role.NURSE, r.get().role());
    }

    @Test
    void addUser_duplicateRejected(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("users.json");
        JsonFileUsersRepository repo = new JsonFileUsersRepository(file);
        repo.loadOrInitialize();
        assertThrows(IllegalArgumentException.class, () -> repo.addUser(new AppUser("nurse", "x", Role.ADMIN)));
    }
}
