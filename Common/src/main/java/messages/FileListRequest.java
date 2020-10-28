package messages;

public class FileListRequest extends Message {
    private String dir;

    public String getDir() {
        return dir;
    }

    public FileListRequest(String dir) {
        this.dir = dir;
    }
}
