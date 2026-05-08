package Persistence;

import Persistence.dto.WardStateDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;


// load save contract for the ward json snapshot
public interface WardStateRepository {

    Path getPersistencePath();

    Optional<WardStateDocument> loadIfPresent() throws IOException; // load the ward state if it exists

    long save(WardStateDocument draft) throws IOException; // save the ward state
}
