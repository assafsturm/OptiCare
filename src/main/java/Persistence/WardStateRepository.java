package Persistence;

import Persistence.dto.WardStateDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Load/save CAS contract for the ward JSON snapshot (Stage 6). */
public interface WardStateRepository {

    Path getPersistencePath();

    Optional<WardStateDocument> loadIfPresent() throws IOException;

    long saveCompareAndSwap(long clientKnownPersistVersion, WardStateDocument draft)
            throws IOException, PersistConcurrentModificationException;
}
