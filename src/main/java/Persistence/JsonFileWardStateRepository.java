package Persistence;

import Persistence.dto.WardStateDocument;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * File-backed CAS repository: load whole snapshot; atomic compare-and-set save with monotonic persistVersion.
 */
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

    /**
     * @param clientKnownPersistVersion snapshot version observed by this JVM (0 after in-memory seed with no file)
     * @param draft document body; {@code persistVersion} is overwritten here
     * @return new persisted version
     */
    @Override
    public long saveCompareAndSwap(long clientKnownPersistVersion, WardStateDocument draft)
            throws IOException, PersistConcurrentModificationException {
        Objects.requireNonNull(draft, "draft");

        long nextVersion;
        if (Files.isRegularFile(persistencePath)) {
            WardStateDocument disk = mapper.readValue(persistencePath.toFile(), WardStateDocument.class);
            long diskVersion = disk.getPersistVersion();
            if (diskVersion != clientKnownPersistVersion) {
                throw new PersistConcurrentModificationException(
                        "Expected persistVersion " + clientKnownPersistVersion + " on disk but found " + diskVersion,
                        diskVersion);
            }
            nextVersion = diskVersion + 1;
        } else {
            if (clientKnownPersistVersion != 0L) {
                throw new PersistConcurrentModificationException(
                        "No persistence file yet but clientKnownPersistVersion was " + clientKnownPersistVersion,
                        0L);
            }
            nextVersion = 1L;
        }

        draft.setSchemaVersion(WardStateDocument.CURRENT_SCHEMA_VERSION);
        draft.setPersistVersion(nextVersion);
        AtomicJsonFiles.writeAtomically(persistencePath, mapper.writeValueAsBytes(draft));
        return nextVersion;
    }
}
