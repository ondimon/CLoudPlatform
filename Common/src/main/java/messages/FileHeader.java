package messages;

import java.nio.file.Paths;
import java.util.UUID;

public class FileHeader extends Message {


    private UUID uuid;
    private String fileName;
    private String clientPath;
    private String serverPath;
    private long length;

    public FileHeader() {
        uuid = UUID.randomUUID();
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

    private void setFileName(String path) {
        if(fileName != null) return;
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
