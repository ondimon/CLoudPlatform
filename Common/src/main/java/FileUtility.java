import messages.FileHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtility {
    private static final Logger LOGGER = LogManager.getLogger(FileUtility.class.getName());

    private FileUtility() {
        throw new IllegalStateException("Utility class");
    }

    public static List<FileHeader> getListFilesHeader(Path path) throws IOException {
        try (Stream<Path> pathStream = Files.list(path)){
            return   pathStream
                    .map(p -> new FileHeader(p.getFileName().toString(), Files.isDirectory(p), p.toFile().length()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(e.getMessage());
        }
    }
}
