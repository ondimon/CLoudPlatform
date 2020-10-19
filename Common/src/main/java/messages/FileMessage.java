package messages;

public abstract class FileMessage extends Message {
    private FileHeader fileHeader;

    public FileMessage(FileHeader fileHeader) {
        this.fileHeader = fileHeader;
    }

    public FileHeader getFileHeader() {
        return fileHeader;
    }
}
