import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;


public class MessageHandler extends SimpleChannelInboundHandler<MessageLogin> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client connect");
    }



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageLogin msg) throws Exception {
        System.out.println("login");
        System.out.println(msg.getLogin());
        System.out.println(msg.getPassword());
        ctx.channel().write("Ok");
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
