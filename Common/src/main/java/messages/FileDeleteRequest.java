package messages;

public class FileDeleteRequest extends Message {
    public String getFileName() {
        return fileName;
    }

    private String fileName;

    public FileDeleteRequest(String fileName) {
        this.fileName = fileName;
    }
}
