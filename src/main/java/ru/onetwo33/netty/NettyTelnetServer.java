package ru.onetwo33.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import ru.onetwo33.netty.handlers.ByteBufInputHandler;
import ru.onetwo33.netty.handlers.TelnetMessageHandler;
import ru.onetwo33.netty.handlers.OutputHandler;

public class NettyTelnetServer {
    public NettyTelnetServer() {
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(
									new ByteBufInputHandler(),
									new OutputHandler(),
                                    new TelnetMessageHandler()
                            );
                        }
                    });
            ChannelFuture future = bootstrap.bind(4000).sync();
            System.out.println("Server started");
            future.channel().closeFuture().sync();
            System.out.println("Server closed");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            auth.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new NettyTelnetServer();
    }
}