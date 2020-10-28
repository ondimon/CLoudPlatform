package messages;

public class CreateDirectoryRequest extends Message {
    public String getDirName() {
        return dirName;
    }

    private String dirName;

    public CreateDirectoryRequest(String dirName) {
        this.dirName = dirName;
    }
}
