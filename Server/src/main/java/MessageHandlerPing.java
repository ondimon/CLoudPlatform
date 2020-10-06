import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


public class MessageHandlerPing extends SimpleChannelInboundHandler<MessagePing> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessagePing msg) throws Exception {
        System.out.println("ping");
        ctx.write("Pong");
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
