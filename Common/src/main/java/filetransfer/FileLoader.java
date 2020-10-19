package filetransfer;

import messages.FileHeader;
import messages.FileLoad;
import callback.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;

public class FileLoader implements Runnable {
    private static final Logger logger = LogManager.getLogger(FileLoader.class.getName());

    private Path path;



    private FileHeader fileHeader;



    private long fileLength;
    private long byteLoad;
    private ArrayBlockingQueue<byte[]> queue;
    FileChannel fileChannel;

    public void setData(byte[] data) {
       logger.debug("set in queue bytes " + data.length);
       while(!queue.offer(data)) {
           logger.debug("queue is full");
       }
    }

    public FileHeader getFileHeader() {
        return fileHeader;
    }

    public void setFileHeader(FileHeader fileHeader) {
        this.fileHeader = fileHeader;
        this.fileLength = fileHeader.getLength();
    }

    private Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public FileLoader(Path path, FileHeader fileHeader) throws IOException {
        this.path = path;
        setFileHeader(fileHeader);
        if( Files.exists(path)) {
            Files.delete(path);
        }
        Files.createFile(path);
        queue = new ArrayBlockingQueue<>(10);
        this.fileChannel = new RandomAccessFile(path.toFile(), "rw").getChannel();
    }

    @Override
    public void run() {
        logger.debug("write file " + fileHeader.toString());

        while (fileLength != byteLoad) {
            byte[] data = queue.poll();
            if(data == null) {
                continue;
            }
            logger.debug("get bytes " + data.length + " " + fileHeader.toString());

            try {
                fileChannel.write(ByteBuffer.wrap(data), fileChannel.size());
                byteLoad += data.length;
                logger.debug("byte load " + byteLoad + " in  " + fileLength);

            } catch (IOException e) {
              logger.error(e.getMessage());
            }
        }
        try {
            fileChannel.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        if((callback != null)) {
            callback.setMessage(new FileLoad(fileHeader));
        }
    }
}
