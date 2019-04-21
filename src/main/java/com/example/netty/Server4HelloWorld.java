/**
 * 1. 双线程组
 * 2. Bootstrap配置启动信息
 * 3. 注册业务处理Handler
 * 4. 绑定服务监听端口并启动服务
 */
package com.example.netty;

import java.util.ArrayList;
import java.util.List;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.timeout.IdleStateHandler;

public class Server4HelloWorld {
	// 监听线程组，监听客户端请求(轮询发现连接吗)
	private EventLoopGroup acceptorGroup = null;
	// 处理客户端相关操作线程组，负责处理与客户端的数据通讯（处理每一个连接的数据）
	private EventLoopGroup clientGroup = null;
	// 服务启动相关配置信息
	private ServerBootstrap bootstrap = null;
	public Server4HelloWorld(){
		init();
	}
	private void init(){
		// 初始化线程组,构建线程组的时候，如果不传递参数，则默认构建的线程组线程数是CPU核心数量。
		acceptorGroup = new NioEventLoopGroup();
		clientGroup = new NioEventLoopGroup();
		// 初始化服务的配置
		bootstrap = new ServerBootstrap();
		// 绑定线程组
		bootstrap.group(acceptorGroup, clientGroup);
		// 设定通讯模式为NIO， 同步？非阻塞
		bootstrap.channel(NioServerSocketChannel.class);
		// 设定缓冲区大小， 缓存区的单位是字节。
		bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
		// SO_SNDBUF发送缓冲区，SO_RCVBUF接收缓冲区，SO_KEEPALIVE开启心跳监测（保证连接有效）
		bootstrap.option(ChannelOption.SO_SNDBUF, 16*1024)
			.option(ChannelOption.SO_RCVBUF, 16*1024)
			/*
			 * 	该参数用于设置TCP连接，当设置该选项以后，连接会测试链接的状态，这个选项用于可能长时间没有数据交流的连接。
			 * 	当设置该选项以后，如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文。
			 */
			.option(ChannelOption.SO_KEEPALIVE, true);
	}
	/**
	 * 监听处理逻辑。
	 * @param port 监听端口。
	 * @param acceptorHandlers 处理器， 如何处理客户端请求。
	 * @return
	 * @throws InterruptedException
	 */
	public ChannelFuture doAccept(int port) throws InterruptedException{
		
		/*
		 * childHandler是服务的Bootstrap独有的方法。是用于提供处理对象的。
		 * 可以一次性增加若干个处理逻辑。是类似责任链模式的处理方式。
		 * 增加A，B两个处理逻辑，在处理客户端请求数据的时候，根据A-》B顺序依次处理。
		 * 
		 * ChannelInitializer - 用于提供处理器的一个模型对象。
		 *  其中定义了一个方法，initChannel方法。
		 *   方法是用于初始化处理逻辑责任链条的。
		 *   可以保证服务端的Bootstrap只初始化一次处理器，尽量提供处理逻辑的重用。
		 *   避免反复的创建处理器对象。节约资源开销。
		 */
		bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				/*
				 *  ch.pipeline().addLast(channelHandler);
				 * 	添加多个handler处理类时，无论是挨个添加，还是添加handler数组，必要时，注意添加顺序，一般读取数据的handler添加到最后。
				 */
				/*
				 * 1、以下是固定长度的解码器（服务端接收用的数据缓存区，字节数量到达了3个字节，才可以读取。读取到的数据也会从缓存区清空）
				 * （解码，为了解决粘包问题。所以设置了服务端、客户端，从各自缓存区读取数据的规则。）
				 */
				//ChannelHandler[] channelHandler = new ChannelHandler[2];
				// 信道处理，加一个固定长度的解码器，缓存中每三个字节读取一次，不足三个字节的，暂存在服务器端缓存中。
				// 既然是解码器，当然是用于接收数据。至于发送数据时的编码，不知道是自己组织，还是netty也有编码器类。
				//channelHandler[0] = new FixedLengthFrameDecoder(3);
				//信道处理，读取数据
				//channelHandler[1] = new Server4HelloWorldHandler();
				/*
				 * 2、结束标记(定界符解码器)
				 */
				//ChannelHandler[] channelHandler = new ChannelHandler[2];
				//ByteBuf delimiter = Unpooled.copiedBuffer("$E$".getBytes());
				//每个数据包的最大长度为1024个字节，netty建议数据有最大长度
				//channelHandler[0] = new DelimiterBasedFrameDecoder(1024, delimiter);
				
				//channelHandler[1] = new StringDecoder(Charset.forName("utf-8"));//解码为字符串？
				//信道处理，读取数据
				//channelHandler[1] = new Server4HelloWorldHandler();
				/*
				 * 3、自定义协议 （netty没有提供，需要自己在信道处理类中实现）
				 */
				//Server4HelloWorldHandler channelHandler = new Server4HelloWorldHandler();
				/* 4、上面是解决粘包的问题。  当前这个是读写超时的设置。
				 * 
				 * 1）、服务端一般查看读操作，没有收到客户端消息，则断开与其连接。 客户端可以查看写操作
				 * 2）、读取数据的handler需要写到最后，把读操作超时写在读取数据的handler前面，才起作用。
				 * 如果写在后面，则不管读有无超时，都会定时关闭连接。
				 */
				/*ChannelHandler channelHandler = new Server4HelloWorldHandler();
				//ch.pipeline().addLast(new ReadTimeoutHandler(30)); //读超时，如果30s内没有收到消息，就关闭该连接。
				//ch.pipeline().addLast(new WriteTimeoutHandler(30)); // ?当前服务发送消息时，如果30s内还未发送成功，就关闭该连接。
				ch.pipeline().addLast(channelHandler);*/
				/*
				 * 5、编解码
				 * 1）例如：服务端接收的数据是byte字节数组，那就需要先解码为字符，再由其他自定义handler接收字符。
				 */
				ChannelHandler channelHandler = new Server4HelloWorldHandler();
				/*ch.pipeline().addLast(new ByteToMessageDecoder() {
					
					 * Decode the from one ByteBuf to an other. 
						This method will be called till either the input ByteBuf has nothing to read when return from this method 
						or till nothing was read from the input ByteBuf.
					 
					@Override
					protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
						byte c;
						while(in.isReadable()) {
							c = in.readByte();
							out.add(c);//读完缓存区之后，大概数据已经放到out中了，然后自定义读取时，读到的就是out中的数据。
						}
					}
				});*/
				
				/*ch.pipeline().addLast(new MessageToByteEncoder<List<Object>>() {

					@Override
					protected void encode(ChannelHandlerContext ctx, List<Object> msg, ByteBuf out) throws Exception {
						
					}
				});*/
				/*
				 * 6、心跳机制
				 */
				ch.pipeline().addLast(new IdleStateHandler(30, 0, 0));//读写all，0表示禁用不触发空闲事件。
				ch.pipeline().addLast(channelHandler);
				
			}
		});
		// bind方法 - 绑定监听端口的。ServerBootstrap可以绑定多个监听端口。 多次调用bind方法即可
		// sync - 开始监听逻辑。 返回一个ChannelFuture。 返回结果代表的是监听成功后的一个对应的未来结果
		// 可以使用ChannelFuture实现后续的服务器和客户端的交互。
		ChannelFuture future = bootstrap.bind(port).sync();
		return future;
	}
	
	/**
	 * shutdownGracefully - 方法是一个安全关闭的方法。可以保证不放弃任何一个已接收的客户端请求。
	 */
	public void release(){
		this.acceptorGroup.shutdownGracefully();
		this.clientGroup.shutdownGracefully();
	}
	
	public static void main(String[] args){
		ChannelFuture future = null;
		Server4HelloWorld server = null;
		try{
			server = new Server4HelloWorld();
			future = server.doAccept(9999);
			System.out.println("server started.");
			
			// 关闭连接的。
			future.channel().closeFuture().sync();
		}catch(InterruptedException e){
			e.printStackTrace();
		}finally{
			if(null != future){
				try {
					future.channel().closeFuture().sync();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(null != server){
				server.release();
			}
		}
	}
	
}
