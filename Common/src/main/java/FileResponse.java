import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileResponse extends Message{


    private String fileName;
    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public String getFileName() {
        return fileName;
    }

    public FileResponse(Path path, byte[] data) throws IOException {
        fileName = path.getFileName().toString();
        this.data = data;
    }
}
