import filetransfer.FileLoader;
import filetransfer.FilePart;
import filetransfer.FileSender;
import messages.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class MessageServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(MessageServerHandler.class.getName());

    private Server server;
    private UUID token;
    private User user;
    private ConcurrentHashMap<UUID, FileLoader> fileLoaders = new ConcurrentHashMap<>();
    private Channel channel;

    public MessageServerHandler(Server server) {
        this.server = server;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        channel = ctx.channel();
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
        Object response = null;
        logger.debug("get message " + msg.toString());

        if(msg instanceof LoginRequest ) {
            response = userLogin(ctx, (LoginRequest) msg);
        }else if (msg instanceof FileListRequest ) {
            response  = getFileListHandler((FileListRequest) msg);
        }else if(msg instanceof FileUploadRequest) {
            response = fileUploadRequestHandler((FileUploadRequest) msg);
        }else if(msg instanceof FilePart ) {
            FilePart filePart = (FilePart) msg;
            FileLoader fileLoader = fileLoaders.get(filePart.getFileHeader().getUuid());
            fileLoader.setData(filePart.getData());
        }else if(msg instanceof FileDownloadRequest ) {
            response = fileDownloadRequestHandler((FileDownloadRequest) msg);
        }
        if (response != null) {
            logger.debug("send message" + response.toString());
            ctx.channel().write(response);
        }

    }



    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        logger.error(cause.getMessage());
        if(token != null) {
            server.unregisterUser(token);
        }
        ctx.close();
    }

    private Message userLogin(ChannelHandlerContext ctx, LoginRequest msg) throws IOException {
        logger.info(String.format("user login: %s", msg.getLogin()));
        AuthService authService = server.getAuthService();
        String login = msg.getLogin();
        String pass = msg.getPassword();
        boolean userChecked = authService.checkUser(login, pass);

        logger.info(String.format("user login success: %b", userChecked));
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setLoginSuccess(userChecked);
        if (userChecked) {
            user = new User(login);
            UUID uuid = server.registerUser(user);
            loginResponse.setToken(uuid);
            token = uuid;
        }
        return loginResponse;
    }

    private Message getFileListHandler(FileListRequest msg) throws IOException {
        if(!checkToken(msg)) {
            return null;
        }
        ArrayList<String> listFiles = FileUtility.getListFiles(server.getUserDir(user));
        return new FileListResponse(listFiles);
    }

    private Message fileUploadRequestHandler(FileUploadRequest msg) throws IOException {
        FileHeader fileHeader = msg.getFileHeader();
        Path path = Paths.get(server.getUserDir(user).toString(), fileHeader.getFileName());
        fileHeader.setServerPath(path.toString());

        FileLoader fileLoader = new FileLoader(path, fileHeader);
        fileLoader.setCallback((message) ->{
            channel.writeAndFlush(message);
        });
        new Thread(fileLoader).start();
        registerFileLoader(fileLoader);

        return new FileUploadResponse(fileHeader);
    }

    private Message fileDownloadRequestHandler(FileDownloadRequest msg) {
        FileHeader fileHeader = msg.getFileHeader();
        Path path = Paths.get(server.getUserDir(user).toString(), fileHeader.getFileName());
        fileHeader.setServerPath(path.toString());
        fileHeader.setLength(path.toFile().length());

        FileSender fileSender = new FileSender(path, fileHeader, channel);
        new Thread(fileSender::sendFile).start();
        return new FileDownloadResponse(fileHeader);
    }

//    private Messages.Message getFileResponseHandler(ChannelHandlerContext ctx, FileResponse msg) {
//        try {
//            Path path = Paths.get(server.getUserDir(user).toString(), msg.getFileName());
//            logger.debug(path.toString());
//            byte[] data = msg.getData();
//            ByteBuffer byteBuffer= ByteBuffer.wrap(data);
//
//            logger.debug(data.length);
//
////            Set<StandardOpenOption> options = new HashSet<>();
////            options.add(StandardOpenOption.CREATE);
////            options.add(StandardOpenOption.APPEND);
////            FileChannel fileChannel = FileChannel.open(path, options);
////            fileChannel.write(byteBuffer);
////            fileChannel.close();
//
//            if(Files.notExists(path)) {
//                logger.debug("create file");
//                Files.createFile(path);
//           }
//            RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), "rw");
//            FileChannel fileChannel = randomAccessFile.getChannel();
//
//            while (byteBuffer.hasRemaining()){;
//                fileChannel.position(path.toFile().length());
//                fileChannel.write(byteBuffer);
//            }
//
//            fileChannel.close();
//            randomAccessFile.close();
//
////            try (FileChannel channel = new RandomAccessFile(path.toFile(), "rw").getChannel()) {
////                logger.debug("write file");
////                channel.write(ByteBuffer.wrap(msg.getData()), channel.size());
////            }
////            Files.write(path,
////                        msg.getData(),
////                        StandardOpenOption.APPEND);
//        } catch (Exception e) {
//           logger.error(e);
//        }
//        return null;
//    }

    private boolean checkToken(Message msg) {
        return msg.getToken().equals(token);
    }

    private void registerFileLoader(FileLoader fileLoader) {
        fileLoaders.put(fileLoader.getFileHeader().getUuid(), fileLoader);
    }
}
