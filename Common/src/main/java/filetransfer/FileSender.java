package filetransfer;

import messages.FileHeader;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Arrays;

public class FileSender {
    private static final Logger LOGGER = LogManager.getLogger(FileSender.class.getName());

    private final Path path;
    private final Channel channel;
    private final FileHeader fileHeader;

    public FileSender(Path path, FileHeader fileHeader, Channel channel) {
        this.path = path;
        this.channel = channel;
        this.fileHeader  = fileHeader;
    }

    public void sendFile() {
        LOGGER.debug("start send file {}", fileHeader.getFileName());
        LOGGER.debug("file length {}", fileHeader.getLength());

        try (FileChannel fileChannel = new FileInputStream(path.toFile()).getChannel()) {
              ByteBuffer buffer = ByteBuffer.allocate(1024 * 100);
              int byteRead;
              while ((byteRead = fileChannel.read(buffer)) != -1) {
                  buffer.flip();
                  byte[] data = buffer.array();
                  channel.writeAndFlush(new FilePart(fileHeader,  Arrays.copyOf(data, byteRead))).sync();
                  LOGGER.debug("send {}", byteRead);
                  buffer.clear();
               }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
    }

}
