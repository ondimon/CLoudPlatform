package filetransfer;

import messages.FileHeader;
import messages.FileLoad;
import callback.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;

public class FileLoader implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(FileLoader.class.getName());

    private final Path path;
    private FileHeader fileHeader;
    private long fileLength;
    private long byteLoad;
    private final ArrayBlockingQueue<byte[]> queue;
    private Callback callback;
    private final FileChannel fileChannel;
    private final RandomAccessFile randomAccessFile;

    public void setData(byte[] data) {
       LOGGER.debug("set in queue bytes {}", data.length);
       while(!queue.offer(data)) {
           LOGGER.debug("queue is full");
       }
    }

    public FileHeader getFileHeader() {
        return fileHeader;
    }

    public void setFileHeader(FileHeader fileHeader) {
        this.fileHeader = fileHeader;
        this.fileLength = fileHeader.getLength();
    }

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

        try {
            this.randomAccessFile = new RandomAccessFile(path.toFile(), "rw");
            this.fileChannel = randomAccessFile.getChannel();
        } catch (FileNotFoundException e) {
            closeFile();
            throw new FileNotFoundException(path.toString());
        }
    }

    @Override
    public void run() {
        LOGGER.debug("write file {}", fileHeader);

        while (fileLength != byteLoad) {
            byte[] data = queue.poll();
            if(data == null) {
                continue;
            }
            LOGGER.debug("get bytes {} {}", data.length, fileHeader);

            try {
                fileChannel.write(ByteBuffer.wrap(data), fileChannel.size());
                byteLoad += data.length;
                LOGGER.debug("byte load {} in {}", byteLoad, fileLength);

            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                try {
                    Files.delete(path);
                } catch (IOException ioException) {
                    LOGGER.error(ioException.getMessage(), ioException);
                }
            } finally {
                closeFile();
            }
        }

        closeFile();
        if((callback != null)) {
            callback.setMessage(new FileLoad(fileHeader));
        }
    }

    private void closeFile() {
        try {
            if(fileChannel != null) {
                fileChannel.close();
            }
            if(randomAccessFile != null) {
                randomAccessFile.close();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
