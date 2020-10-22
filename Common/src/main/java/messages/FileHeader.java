package messages;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.UUID;

public class FileHeader implements Serializable {


    private UUID uuid;
    private String fileName;
    private String clientPath;
    private String serverPath;
    private long length;

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    private boolean isFolder;

    public FileHeader() {
        uuid = UUID.randomUUID();
    }

    public FileHeader(String fileName, boolean isFolder, long length) {
        this();
        this.fileName = fileName;
        this.length = length;
        this.isFolder = isFolder;
    }

    public String getClientPath() {
        return clientPath;
    }

    public void setClientPath(String clientPath) {

        this.clientPath = clientPath;
        setFileName(clientPath);
    }

    public String getServerPath() {
        return serverPath;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
        setFileName(serverPath);
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public void setFileName(String path) {
        //if(fileName != null) return;
        fileName = Paths.get(path).getFileName().toString();
    }

    public String getFileName() {
        return fileName;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "FileHeader{" +
                "uuid=" + uuid +
                ", fileName='" + fileName + '\'' +
                ", clientPath='" + clientPath + '\'' +
                ", serverPath='" + serverPath + '\'' +
                ", length=" + length +
                '}';
    }

}
