package Persistence;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;// if cant do atomic move
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;// options for move
import java.nio.file.StandardOpenOption;// options for write
// implement atomic write/ write fully to temp file and then rename to the real path
// if somthing happens , the file is or the old version or missing, no corrupted files
public final class AtomicJsonFiles {

    private AtomicJsonFiles() {
    }
    // write the payload to the temp file and then rename to the real path
    public static void writeAtomically(Path target, byte[] utf8Payload) throws IOException { 
        Path parent = target.toAbsolutePath().getParent(); // get the parent directory
        if (parent != null) {
            Files.createDirectories(parent); // if not exists, create it
        }
        Path temp = Files.createTempFile(parent != null ? parent : Path.of("."), "opticare-", ".tmp.json"); // create a temp file
        try {
            Files.write(temp, utf8Payload, // write the payload to the temp file
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,// clear if allredy exists
                    StandardOpenOption.SYNC); // rushes os to flush to storge and not stay in memory
            renameOntoExisting(temp, target); // rename the temp file to the real path
        }
        finally {
            Files.deleteIfExists(temp); // delete the temp file
        }
    }

    // rename the temp file to the real path
    private static void renameOntoExisting(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, // move the temp file to the real path
                    StandardCopyOption.ATOMIC_MOVE,// single step
                    StandardCopyOption.REPLACE_EXISTING);
        }

        catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); // if cant do atomic move, replace the existing file
        }
    }

}
