package Persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import Persistence.dto.WardStateDocument;// present or not (no null)


// file backed repository load whole snapshot and save atomically
public final class JsonFileWardStateRepository implements WardStateRepository {

    private final Path persistencePath;
    private final ObjectMapper mapper = PersistenceJson.createObjectMapper();// json mapper

    public JsonFileWardStateRepository(Path persistencePath) {
        this.persistencePath = persistencePath;
    }

    @Override
    public Path getPersistencePath() {
        return persistencePath;
    }

    @Override
    public Optional<WardStateDocument> loadIfPresent() throws IOException {// laod the ward state if it exists
        if (!Files.isRegularFile(persistencePath)) {// if not found
            return Optional.empty();
        }
        if (Files.size(persistencePath) == 0L) {// if empty
            return Optional.empty();
        }
        WardStateDocument doc = mapper.readValue(persistencePath.toFile(), WardStateDocument.class);// read the file
        return Optional.of(doc);// return the ward state
    }

    @Override
    public long save(WardStateDocument draft) throws IOException {// save the ward state
        Objects.requireNonNull(draft, "draft");
        long nextVersion = 1L;
        if (Files.isRegularFile(persistencePath) && Files.size(persistencePath) > 0L) {
            WardStateDocument disk = mapper.readValue(persistencePath.toFile(), WardStateDocument.class);
            nextVersion = Math.max(0L, disk.getPersistVersion()) + 1L;
        }

        draft.setSchemaVersion(WardStateDocument.CURRENT_SCHEMA_VERSION);
        draft.setPersistVersion(nextVersion);
        AtomicJsonFiles.writeAtomically(persistencePath, mapper.writeValueAsBytes(draft));// write the ward state to the file atomically
        return nextVersion;// saned as virsion *number*
    }
}
