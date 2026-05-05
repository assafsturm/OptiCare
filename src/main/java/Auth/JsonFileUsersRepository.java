package Auth;

import Persistence.AtomicJsonFiles;
import Persistence.PersistenceJson;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Minimal users file: plaintext passwords, default seed on first run.
 */
public final class JsonFileUsersRepository {

    private final Path filePath;
    private final ObjectMapper mapper = PersistenceJson.createObjectMapper();

    public JsonFileUsersRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
    }

    public Path getFilePath() {
        return filePath;
    }

    /** Load file or create it with default admin/nurse users. */
    public UsersDocument loadOrInitialize() throws IOException {
        if (!Files.isRegularFile(filePath)) {
            UsersDocument seeded = defaultDocument();
            save(seeded);
            return seeded;
        }
        UsersDocument doc = mapper.readValue(filePath.toFile(), UsersDocument.class);
        if (doc.getUsers() == null || doc.getUsers().isEmpty()) {
            UsersDocument seeded = defaultDocument();
            save(seeded);
            return seeded;
        }
        return doc;
    }

    public Optional<LoginResult> authenticate(String username, String password) throws IOException {
        UsersDocument doc = loadOrInitialize();
        String u = username == null ? "" : username.trim();
        String p = password == null ? "" : password;
        for (AppUser usr : doc.getUsers()) {
            if (usr != null && u.equals(usr.getUsername()) && p.equals(usr.getPassword())) {
                Role r = usr.getRole();
                if (r == Role.NURSE || r == Role.ADMIN) {
                    return Optional.of(new LoginResult(usr.getUsername(), r));
                }
            }
        }
        return Optional.empty();
    }

    public void addUser(AppUser user) throws IOException {
        Objects.requireNonNull(user, "user");
        String u = user.getUsername() == null ? "" : user.getUsername().trim();
        if (u.isEmpty()) {
            throw new IllegalArgumentException("Username required");
        }
        if (user.getRole() != Role.NURSE && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Role must be NURSE or ADMIN");
        }
        UsersDocument doc = loadOrInitialize();
        for (AppUser existing : doc.getUsers()) {
            if (existing != null && u.equalsIgnoreCase(existing.getUsername())) {
                throw new IllegalArgumentException("Username already exists: " + u);
            }
        }
        user.setUsername(u);
        doc.getUsers().add(user);
        save(doc);
    }

    public void save(UsersDocument doc) throws IOException {
        Objects.requireNonNull(doc, "doc");
        doc.setSchemaVersion(UsersDocument.CURRENT_SCHEMA_VERSION);
        byte[] bytes = mapper.writeValueAsBytes(doc);
        AtomicJsonFiles.writeAtomically(filePath, bytes);
    }

    private static UsersDocument defaultDocument() {
        UsersDocument d = new UsersDocument();
        List<AppUser> list = new ArrayList<>();
        list.add(new AppUser("admin", "admin", Role.ADMIN));
        list.add(new AppUser("nurse", "nurse", Role.NURSE));
        d.setUsers(list);
        return d;
    }
}
