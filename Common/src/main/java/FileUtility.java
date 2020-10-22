import messages.FileHeader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.Files.*;

public class FileUtility {
    public static ArrayList<String> getListFiles(Path path) throws IOException {
        ArrayList<String> fileList;
        fileList = (ArrayList<String>) Files.list(path)
                .filter(p -> !Files.isDirectory(p))
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        return fileList;
    }

    public static ArrayList<FileHeader> getListFilesHeader(Path path) throws IOException {
        ArrayList<FileHeader> fileList;
        fileList = (ArrayList<FileHeader>) Files.list(path)
                .map(p -> new FileHeader(p.getFileName().toString(), Files.isDirectory(p), p.toFile().length()))
                .collect(Collectors.toList());
        return fileList;
    }
}
