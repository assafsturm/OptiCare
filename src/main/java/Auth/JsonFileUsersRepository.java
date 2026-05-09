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

//very simple users file plaintext passwords, default seed on first run
// manages the users json file
public final class JsonFileUsersRepository {

    private final Path filePath;
    private final ObjectMapper mapper = PersistenceJson.createObjectMapper();

    public JsonFileUsersRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
    }

    public Path getFilePath() {
        return filePath;
    }

    // load file or create it with default admin/nurse users
    public UsersDocument loadOrInitialize() throws IOException {
        if (!Files.isRegularFile(filePath)) {// if file does not exist, create it with default admin/nurse users
            UsersDocument seeded = defaultDocument();
            save(seeded);
            return seeded;
        }
        UsersDocument doc = mapper.readValue(filePath.toFile(), UsersDocument.class);
        if (doc.getUsers() == null || doc.getUsers().isEmpty()) {// if file is empty, create it with default admin/nurse users
            UsersDocument seeded = defaultDocument();
            save(seeded);
            return seeded;
        }
        return doc;// otherwise return the document
    }

    // authenticate the user
    public Optional<LoginResult> authenticate(String username, String password) throws IOException {
        UsersDocument doc = loadOrInitialize();// load the document
        String u = username == null ? "" : username.trim();
        String p = password == null ? "" : password;
        for (AppUser usr : doc.getUsers()) {// iterate over the users
            if (usr != null && u.equals(usr.getUsername()) && p.equals(usr.getPassword())) {
                Role r = usr.getRole();// get the role
                if (r == Role.NURSE || r == Role.ADMIN) {
                    return Optional.of(new LoginResult(usr.getUsername(), r));
                }
            }
        }
        return Optional.empty();
    }
    // add a user to the file
    public void addUser(AppUser user) throws IOException {
        Objects.requireNonNull(user, "user");
        String u = user.getUsername() == null ? "" : user.getUsername().trim();
        if (u.isEmpty()) {// if username is empty, throw an exception
            throw new IllegalArgumentException("Username required");
        }
        if (user.getRole() != Role.NURSE && user.getRole() != Role.ADMIN) {// if role is not nurse or admin, throw an exception
            throw new IllegalArgumentException("Role must be NURSE or ADMIN");
        }
        UsersDocument doc = loadOrInitialize();// load the document
        for (AppUser existing : doc.getUsers()) {
            if (existing != null && u.equalsIgnoreCase(existing.getUsername())) {// if username already exists, throw an exception
                throw new IllegalArgumentException("Username already exists: " + u);
            }
        }
        user.setUsername(u);// set the username with the trimmed username
        doc.getUsers().add(user);// add the user to the document
        save(doc);// save the document
    }

    // save the document
    public void save(UsersDocument doc) throws IOException {
        Objects.requireNonNull(doc, "doc");
        doc.setSchemaVersion(UsersDocument.CURRENT_SCHEMA_VERSION);
        byte[] bytes = mapper.writeValueAsBytes(doc);// convert the document to bytes
        AtomicJsonFiles.writeAtomically(filePath, bytes);// write the bytes to the file atomically
    }

    // create the default document with admin and nurse users
    //seed data for the first run
    private static UsersDocument defaultDocument() {
        UsersDocument d = new UsersDocument();
        List<AppUser> list = new ArrayList<>();
        list.add(new AppUser("admin", "admin", Role.ADMIN));
        list.add(new AppUser("nurse", "nurse", Role.NURSE));
        d.setUsers(list);
        return d;
    }
}
