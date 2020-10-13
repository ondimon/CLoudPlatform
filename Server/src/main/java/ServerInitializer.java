import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {


    private final SslContext sslCtx;
    private final Server server;

    public ServerInitializer(SslContext sslCtx, Server server) {
        this.sslCtx = sslCtx;
        this.server = server;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }
        p.addLast(
                new ObjectEncoder(),
                new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                new ChunkedWriteHandler(),
                new MessageLoginHandler(server));
    }
}
