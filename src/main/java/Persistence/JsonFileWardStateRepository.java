package Persistence;

import Persistence.dto.WardStateDocument;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** File-backed repository: load whole snapshot and save atomically. */
public final class JsonFileWardStateRepository implements WardStateRepository {

    private final Path persistencePath;
    private final ObjectMapper mapper = PersistenceJson.createObjectMapper();

    public JsonFileWardStateRepository(Path persistencePath) {
        this.persistencePath = persistencePath;
    }

    @Override
    public Path getPersistencePath() {
        return persistencePath;
    }

    @Override
    public Optional<WardStateDocument> loadIfPresent() throws IOException {
        if (!Files.isRegularFile(persistencePath)) {
            return Optional.empty();
        }
        WardStateDocument doc = mapper.readValue(persistencePath.toFile(), WardStateDocument.class);
        return Optional.of(doc);
    }

    @Override
    public long save(WardStateDocument draft) throws IOException {
        Objects.requireNonNull(draft, "draft");
        long nextVersion = 1L;
        if (Files.isRegularFile(persistencePath)) {
            WardStateDocument disk = mapper.readValue(persistencePath.toFile(), WardStateDocument.class);
            nextVersion = Math.max(0L, disk.getPersistVersion()) + 1L;
        }

        draft.setSchemaVersion(WardStateDocument.CURRENT_SCHEMA_VERSION);
        draft.setPersistVersion(nextVersion);
        AtomicJsonFiles.writeAtomically(persistencePath, mapper.writeValueAsBytes(draft));
        return nextVersion;
    }
}
