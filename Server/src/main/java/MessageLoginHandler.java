import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class MessageLoginHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(Server.class.getName());

    private Server server;
    private UUID token;
    private User user;

    public MessageLoginHandler(Server server) {
        this.server = server;
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.info("user disconnect");
        if(token != null) {
            server.unregisterUser(token);
        }
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("user connect");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message response = null;
        logger.debug(msg.toString());

        if(msg instanceof MessageLogin) {
            response = userLogin(ctx, (MessageLogin) msg);
        }else if (msg instanceof FileListRequest) {
            response  = getFileListHandler((FileListRequest) msg);
        }else if(msg instanceof  FileResponse) {
            getFileResponseHandler(ctx, (FileResponse) msg);
        }
        if (response != null) {
            ctx.channel().write(response);
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause.getMessage());
        if(token != null) {
            server.unregisterUser(token);
        }
        ctx.close();
    }

    private Message userLogin(ChannelHandlerContext ctx, MessageLogin msg) throws IOException {
        logger.info(String.format("user login: %s", msg.getLogin()));
        AuthService authService = server.getAuthService();
        String login = msg.getLogin();
        String pass = msg.getPassword();
        boolean userChecked = authService.checkUser(login, pass);

        logger.info(String.format("user login success: %b", userChecked));
        msg.setLoginSuccess(userChecked);
        if (userChecked) {
            user = new User(login);
            UUID uuid = server.registerUser(user);
            msg.setToken(uuid);
            token = uuid;
        }
        return msg;
    }

    private Message getFileListHandler(FileListRequest msg) throws IOException {
        if(!checkToken(msg)) {
            return null;
        }
        ArrayList<String> listFiles = FileUtility.getListFiles(server.getUserDir(user));
        return new FileListResponse(listFiles);
    }

    private Message getFileResponseHandler(ChannelHandlerContext ctx, FileResponse msg) {
        try {
            Path path = Paths.get(server.getUserDir(user).toString(), msg.getFileName());
            logger.debug(path.toString());
            byte[] data = msg.getData();
            ByteBuffer byteBuffer= ByteBuffer.wrap(data);
            logger.debug(data.length);

            //Set<StandardOpenOption> options = new HashSet<>();
            //options.add(StandardOpenOption.CREATE);
            //options.add(StandardOpenOption.APPEND);
            //FileChannel fileChannel = FileChannel.open(path, options);
            //fileChannel.write(byteBuffer);
            //fileChannel.close();

            if(Files.notExists(path)) {
                logger.debug("create file");
                Files.createFile(path);
           }
            RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), "rw");
            FileChannel fileChannel = randomAccessFile.getChannel();

            while (byteBuffer.hasRemaining()){;
                fileChannel.position(path.toFile().length());
                fileChannel.write(byteBuffer);
            }
;
            fileChannel.close();
            randomAccessFile.close();

//            try (FileChannel channel = new RandomAccessFile(path.toFile(), "rw").getChannel()) {
//                logger.debug("write file");
//                channel.write(ByteBuffer.wrap(msg.getData()), channel.size());
//            }
//            Files.write(path,
//                        msg.getData(),
//                        StandardOpenOption.APPEND);
        } catch (Exception e) {
           logger.error(e);
        }
        return null;
    }

    private boolean checkToken(Message msg) {
        return msg.getToken().equals(token);
    }
}
