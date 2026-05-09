package Persistence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import Persistence.dto.WardStateDocument; // present or not (no null)


// load save contract for the ward json snapshot
public interface WardStateRepository {

    Path getPersistencePath();

    Optional<WardStateDocument> loadIfPresent() throws IOException; // load the ward state if it exists

    long save(WardStateDocument draft) throws IOException; // save the ward state
}
