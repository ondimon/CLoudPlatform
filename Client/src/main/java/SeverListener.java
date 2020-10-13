import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;

import javax.net.ssl.SSLException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.UUID;

public class SeverListener implements Runnable  {
    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8189"));

    private static SeverListener severListener;

    private MessageHandler messageHandler;
    private EventLoopGroup group;
    private Channel ch;
    private boolean isConnect = false;
    private UUID token;

    static SeverListener getInstance() {
        if(severListener == null) {
            severListener = new SeverListener();
        }
        return severListener;
    }
    private SeverListener(){

    }
    public void setToken(UUID token) {
        this.token = token;
    }

    public boolean isConnect() {
        return isConnect;
    }

    public void setCallback(Callback callback) {
        messageHandler.setCallback(callback);
    }

    public void sendMessage(Message message) {
        message.setToken(token);
            if(ch.isWritable()) {
                ch.writeAndFlush(message);
            }
    }

    public void sendFile(Path path) {
        try(RandomAccessFile raf = new RandomAccessFile(path.toString(), "r")) {
            long length = raf.length();
            ch.write("send file");
            //ch.write(new DefaultFileRegion(raf.getChannel(), 0, length));
            ch.write(new ChunkedFile(raf));
            ch.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        group.shutdownGracefully();
    }

    @Override
    public void run() {
        // Configure SSL.
        SslContext sslCtx = null;
        if (SSL) {
            try {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                e.printStackTrace();
            }
        } else {
            sslCtx = null;
        }
        final SslContext sslCtx1 = sslCtx;
        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx1 != null) {
                                p.addLast(sslCtx1.newHandler(ch.alloc(), HOST, PORT));
                            }
                            p.addLast(
                                    new ObjectEncoder(),
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    new ChunkedWriteHandler(),
                                    new MessageHandler());
                        }
                    });

            // Start the connection attempt.
            ch = b.connect(HOST, PORT).sync().channel();
            messageHandler = ch.pipeline().get(MessageHandler.class);
            isConnect = true;
            ch.closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public void stop() {
        if(group != null) {
            group.shutdownGracefully();
        }
    }
}
