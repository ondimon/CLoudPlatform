import java.util.List;

public class FileListResponse extends Message {
    private List<String> fileList;

    public List<String> getFileList() {
        return fileList;
    }

    public FileListResponse(List<String> fileList) {
        this.fileList = fileList;
    }
}
