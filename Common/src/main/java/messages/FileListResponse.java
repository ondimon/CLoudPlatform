package messages;

import java.util.List;

public class FileListResponse extends Message {
    private List<FileHeader> fileList;
    private String dirPath;

    public String getDirPath() {
        return dirPath;
    }

    public List<FileHeader> getFileList() {
        return fileList;
    }

    public FileListResponse(String dirPath, List<FileHeader> fileList) {
        this.fileList = fileList;
        this.dirPath = dirPath;
    }
}
