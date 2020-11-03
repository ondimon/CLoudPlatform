package messages;

public class FileRenameRequest extends Message {
    private String oldName;
    private String newName;

    public String getOldName() {
        return oldName;
    }

    public String getNewName() {
        return newName;
    }

    public FileRenameRequest(String oldName, String newName) {
        this.oldName = oldName;
        this.newName = newName;
    }
}
