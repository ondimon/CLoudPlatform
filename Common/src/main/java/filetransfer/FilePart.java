package filetransfer;

import messages.FileHeader;
import messages.Message;

import java.io.IOException;

public class FilePart extends Message {


    private FileHeader fileHeader;
    private byte[] data;


    public byte[] getData() {
        return data;
    }

    public FileHeader getFileHeader() {
        return fileHeader;
    }

    public FilePart(FileHeader fileHeader, byte[] data) throws IOException {
        this.fileHeader = fileHeader;
        this.data = data;
    }
}
