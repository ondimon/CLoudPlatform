import filetransfer.FileLoader;
import filetransfer.FilePart;
import filetransfer.FileSender;
import messages.FileDownloadResponse;
import messages.FileHeader;
import messages.FileUploadResponse;
import messages.Message;
import callback.Callback;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MessageClientHandler.class.getName());

    private Channel channel;
    private Callback callback;
    private ConcurrentHashMap<UUID, FileLoader> fileLoaders = new ConcurrentHashMap<>();

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void registerFileLoader(FileLoader fileLoader) {
        fileLoaders.put(fileLoader.getFileHeader().getUuid(), fileLoader);
    }

    public void unRegisterFileLoader(FileHeader fileHeader) {
        fileLoaders.remove(fileHeader.getUuid());
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws IOException {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws URISyntaxException, IOException {
        logger.debug("get message" + msg.toString());

        if (msg instanceof FileUploadResponse ) {
            FileUploadResponse fileUploadResponse = (FileUploadResponse) msg;
            FileHeader fileHeader = fileUploadResponse.getFileHeader();
            FileSender fileSender = new FileSender(Paths.get(fileHeader.getClientPath()), fileHeader, channel);
            new Thread(fileSender::sendFile).start();
        }else if(msg instanceof FilePart ) {
            FilePart filePart = (FilePart) msg;
            FileLoader fileLoader = fileLoaders.get(filePart.getFileHeader().getUuid());
            fileLoader.setData(filePart.getData());
        }else if(msg instanceof FileDownloadResponse ) {
            FileDownloadResponse fileDownloadResponse = (FileDownloadResponse) msg;
            FileHeader fileHeader = fileDownloadResponse.getFileHeader();
            FileLoader fileLoader = fileLoaders.get(fileHeader.getUuid());
            fileLoader.setFileHeader(fileHeader);
            new Thread(fileLoader).start();
        }else {
            if((callback != null)) {
                callback.setMessage((Message) msg);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
