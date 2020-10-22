import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class.getName());
    private static final boolean SSL = System.getProperty("ssl") != null;
    private static final int PORT = Integer.parseInt(System.getProperty("port", "8189"));
    private static final String ROOT_DIR = System.getProperty("root_dir", "./data/storage");

    private ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();
    private final AuthService authService;
    private final int port;
    private final boolean ssl;
    private final String root_dir;

    public static void main(String[] args) {
        new Server(PORT, SSL, ROOT_DIR);
    }

    Server(int port, boolean ssl, String root_dir) {

        this.port = port;
        this.ssl = ssl;
        this.root_dir = root_dir;

        final SslContext sslCtx;
        SslContext sslCtx1;
        if (ssl) {
            SelfSignedCertificate ssc = null;
            try {
                ssc = new SelfSignedCertificate();
            } catch (CertificateException e) {
                e.printStackTrace();
            }

            try {
                sslCtx1 = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } catch (SSLException e) {
                sslCtx1 = null;
                e.printStackTrace();

            }
        } else {
            sslCtx1 = null;
        }

        authService = new DBAuthService();
        authService.start();

        sslCtx = sslCtx1;
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Server server = this;
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
//                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ServerInitializer(sslCtx, this));

            logger.info("Server started");
            bootstrap.bind(port).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            if(authService != null) {
                authService.stop();
            }
            logger.info("Server stopped");
        }

    }

    public UUID registerUser(User user) throws IOException {
        UUID uuid = UUID.randomUUID();
        users.put(uuid, user);
        Path userDir = getUserDir(user);
        if(Files.notExists(userDir)) {
            Files.createDirectories(userDir);
        }
        return uuid;
    }

    public void unregisterUser(UUID token){
        users.remove(token);
    }


    public Path getUserDir(User user) {
        return Paths.get(root_dir, user.getHomeDir());
    }

    public AuthService getAuthService() {
        return authService;
    }
}
