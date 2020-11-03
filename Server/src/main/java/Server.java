import exception.AuthServiceNotStart;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final Logger LOGGER = LogManager.getLogger(Server.class.getName());
    private static final boolean SSL = System.getProperty("ssl") != null;
    private static final int PORT = Integer.parseInt(System.getProperty("port", "8189"));
    private static final String ROOT_DIR = System.getProperty("root_dir", "./data/storage");

    private final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();
    private final AuthService authService;
    private final String rootDir;

    public static void main(String[] args) {
        new Server(PORT, SSL, ROOT_DIR);
    }

    Server(int port, boolean ssl, String rootDir) {
        this.rootDir = rootDir;
        final SslContext sslCtx = getSslContext(ssl);

        authService = new DBAuthService();
        if(!authService.start()) {
            throw new AuthServiceNotStart("Server not started. Auth service not started.");
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerInitializer(sslCtx, this));

            LOGGER.info("Server started");
            bootstrap.bind(port).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            authService.stop();
            LOGGER.info("Server stopped");
        }

    }

    private SslContext getSslContext(boolean ssl) {
        SslContext sslCtx1 = null;
        if ( ssl ) {
            SelfSignedCertificate ssc;
            try {
                ssc = new SelfSignedCertificate();
                sslCtx1 = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } catch (CertificateException | SSLException e) {
                LOGGER.error(e.getMessage(), e);
                sslCtx1 = null;
            }
        }
        return sslCtx1;
    }

    public UUID registerUser(User user) throws IOException {
        UUID uuid = UUID.randomUUID();
        users.put(uuid, user);
        Path userDir = getUserHomeDir(user);
        if(Files.notExists(userDir)) {
            Files.createDirectories(userDir);
        }
        return uuid;
    }

    public void unregisterUser(UUID token){
        users.remove(token);
    }

    public Path getUserHomeDir(User user) {
        return Paths.get(rootDir, user.getHomeDir());
    }

    public Path getUserCurrentDir(User user) {
        return Paths.get(rootDir, user.getCurrentDir());
    }

    public void setUserCurrentDir(User user, Path dir) {
        String currentDir = Paths.get(rootDir).relativize(dir).toString();
        LOGGER.debug("set current dir {}", currentDir);
        user.setCurrentDir(currentDir);
    }

    public AuthService getAuthService() {
        return authService;
    }
}
