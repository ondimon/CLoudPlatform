import filetransfer.FileLoader;
import filetransfer.FilePart;
import filetransfer.FileSender;
import messages.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class MessageServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LogManager.getLogger(MessageServerHandler.class.getName());

    private final Server server;
    private UUID token;
    private User user;
    private final ConcurrentHashMap<UUID, FileLoader> fileLoaders = new ConcurrentHashMap<>();
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
        LOGGER.info("user disconnect");
        if(token != null) {
            server.unregisterUser(token);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("user connect");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Object response = null;
        LOGGER.debug("get message {}", msg);

        if(msg instanceof LoginRequest ) {
            response = loginRequestHandler((LoginRequest) msg);
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
        }else if(msg instanceof CreateDirectoryRequest) {
            response = createDirectoryRequestHandler((CreateDirectoryRequest) msg);
        }else if(msg instanceof FileDeleteRequest) {
            response = fileDeleteRequestHandler((FileDeleteRequest) msg);
        }else if(msg instanceof FileRenameRequest) {
            response = fileRenameRequestHandler((FileRenameRequest) msg);
        }

        if (response != null) {
            LOGGER.debug("send message {}", response);
            ctx.channel().write(response);
        }

    }



    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error(cause.getMessage(), cause);
        if(token != null) {
            server.unregisterUser(token);
        }
        ctx.close();
    }

    private Message loginRequestHandler(LoginRequest msg) throws IOException {
        LOGGER.info("user login: {}", msg.getLogin());
        AuthService authService = server.getAuthService();
        String login = msg.getLogin();
        String pass = msg.getPassword();
        boolean userChecked = authService.checkUser(login, pass);

        LOGGER.info("user login success: {}", userChecked);
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
        String newDir = msg.getDir();
        Path path;
        if (newDir.equals("...")) {
            if(server.getUserCurrentDir(user).equals(server.getUserHomeDir(user))) {
                path = server.getUserCurrentDir(user);
            }else{
                path = server.getUserCurrentDir(user).getParent();
            }
        }else{
            path = server.getUserCurrentDir(user).resolve(newDir);
        }
        server.setUserCurrentDir(user, path);
        List<FileHeader> listFiles = FileUtility.getListFilesHeader(path);
        return new FileListResponse(user.getCurrentDir(), listFiles);
    }

    private Message fileUploadRequestHandler(FileUploadRequest msg) throws IOException {
        FileHeader fileHeader = msg.getFileHeader();
        Path path = server.getUserCurrentDir(user).resolve(fileHeader.getFileName());
        fileHeader.setServerPath(path.toString());

        FileLoader fileLoader = new FileLoader(path, fileHeader);
        fileLoader.setCallback(message ->{
            FileLoad fileLoad = (FileLoad) message;
            FileHeader fileHeaderLoad  = fileLoad.getFileHeader();
            unRegisterFileLoader(fileHeaderLoad);
            channel.writeAndFlush(message);

            try {
                List<FileHeader> listFiles = FileUtility.getListFilesHeader(server.getUserCurrentDir(user));
                channel.writeAndFlush(new FileListResponse(user.getCurrentDir(), listFiles));
            } catch (IOException e) {
                LOGGER.error(e);
            }

        });
        new Thread(fileLoader).start();
        registerFileLoader(fileLoader);

        return new FileUploadResponse(fileHeader);
    }

    private Message fileDownloadRequestHandler(FileDownloadRequest msg) {
        FileHeader fileHeader = msg.getFileHeader();
        Path path = server.getUserCurrentDir(user).resolve(fileHeader.getFileName());
        fileHeader.setServerPath(path.toString());
        fileHeader.setLength(path.toFile().length());

        FileSender fileSender = new FileSender(path, fileHeader, channel);
        new Thread(fileSender::sendFile).start();
        return new FileDownloadResponse(fileHeader);
    }

    private Message createDirectoryRequestHandler(CreateDirectoryRequest msg) throws IOException {
        Path currentDir = server.getUserCurrentDir(user);
        Files.createDirectory(currentDir.resolve(msg.getDirName()));
        return new FileListResponse(user.getCurrentDir(), FileUtility.getListFilesHeader(currentDir));
    }

    private Message fileDeleteRequestHandler(FileDeleteRequest msg) throws IOException {
        Path currentDir = server.getUserCurrentDir(user);
        Files.deleteIfExists(currentDir.resolve(msg.getFileName()));
        return new FileListResponse(user.getCurrentDir(), FileUtility.getListFilesHeader(currentDir));
    }

    private Object fileRenameRequestHandler(FileRenameRequest msg) throws IOException {
        Path currentDir = server.getUserCurrentDir(user);
        File oldFile = currentDir.resolve(msg.getOldName()).toFile();
        File newFile = currentDir.resolve(msg.getNewName()).toFile();
        if(!oldFile.renameTo(newFile)) {
            LOGGER.error("Cannot rename file {}", oldFile);
        }
        return new FileListResponse(user.getCurrentDir(), FileUtility.getListFilesHeader(currentDir));
    }

    private boolean checkToken(Message msg) {
        return msg.getToken().equals(token);
    }

    private void registerFileLoader(FileLoader fileLoader) {
        fileLoaders.put(fileLoader.getFileHeader().getUuid(), fileLoader);
    }

    public void unRegisterFileLoader(FileHeader fileHeader) {
        fileLoaders.remove(fileHeader.getUuid());
    }
}
