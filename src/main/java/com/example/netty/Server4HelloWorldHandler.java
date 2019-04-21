/**
 * @Sharable注解 - 
 *  代表当前Handler是一个可以分享的处理器。也就意味着，服务器注册此Handler后，可以分享给多个客户端同时使用。
 *  如果不使用注解描述类型，则每次客户端请求时，必须为客户端重新创建一个新的Handler对象。
 *  如果handler是一个Sharable的，一定避免定义可写的实例变量。
 *  bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new XxxHandler());
			}
		});
 */
package com.example.netty;

import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;

@Sharable
public class Server4HelloWorldHandler extends ChannelInboundHandlerAdapter {
	//SimpleChannelInboundHandler
	/**
	 * 业务处理逻辑
	 * 用于处理读取数据请求的逻辑。
	 * ctx - 上下文对象。其中包含于客户端建立连接的所有资源。 如： 对应的Channel
	 * msg - 读取到的数据。 默认类型是ByteBuf，是Netty自定义的。是对ByteBuffer的封装。 不需要考虑复位问题(读取数据之后，netty自己清空数据缓存区？？)。
	 * 
	 * netty4.x中，继承该类ChannelInboundHandlerAdapter的handler，缓存区中的数据msg不会自动释放。
	 * 	每一个信道中的处理，一般都要经过一个或多个handler，这就是handler链。 
	 * 	缓冲区中的数据会在handler链中传递，注意中途不能释放缓存区，而最后一个handler，需要释放，否则可能引发缓存溢出。
	 */
	List<Object> list = new ArrayList<>();/*当前类的每一个实例，大概就是一个channel通道。所以字段属于channel私有。 */
	int reader_idle_count = 0;
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		reader_idle_count = 0; /* 收到数据后，重置读取空闲次数 */
		// 获取读取的数据， 是一个缓冲。
		ByteBuf readBuffer = (ByteBuf) msg;
		// 创建一个字节数组，用于保存缓存中的数据。
		byte[] tempDatas = new byte[readBuffer.readableBytes()];
		// 将缓存中的数据读取到字节数组中。
		readBuffer.readBytes(tempDatas);
		String message = new String(tempDatas, "UTF-8");//数据前面已经读取到tempDatas中了
		//
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		System.out.println("from client : " + message+"###"+df.format(new Date()));
		
		/*释放缓存区，netty4.x中，继承该类ChannelInboundHandlerAdapter的handler，缓存区中的数据不会自动释放。*/
		ReferenceCountUtil.release(msg);// ByteBuf实现了ReferenceCounted
		
		//按照协议的规定，读取。
		message = ProtocolParser.parse(message);
		if(null == message){
			System.out.println("error request from client");
			return ;//不符合协议格式的数据，丢弃，不使用。
		}
		
		if("exit".equals(message)){
			ctx.close();//关闭，大概就是短连接，关闭的应该就是tcp连接。关闭之后，客户端需要重新连接，才能发送消息。
			return;
		}
		String line = "server message to client!";
		// 写操作自动释放缓存，避免内存溢出问题。(这是把上面的字符串，全部发送给客户端，然后清空缓存？测试过，多个客户端共用一个服务端信道时，数据不会出错。)
		// 大概，服务端为每个客户端，都分别建立一个channel通道，并使用不同的线程，来处理。所以，多个客户端，不会串信息。
		ctx.writeAndFlush(Unpooled.copiedBuffer("abc".getBytes("UTF-8")));
		ByteBuf buf = Unpooled.copiedBuffer("a".getBytes());
		// 注意，如果调用的是write方法。不会刷新缓存，缓存中的数据不会发送到客户端，必须再次调用flush方法才行。
		// ctx.write(Unpooled.copiedBuffer(line.getBytes("UTF-8")));
		 //ctx.flush();
		//尝试向指定的客户端发送消息
		/*for(Object o:list) {
			ChannelHandlerContext o2 = (ChannelHandlerContext)o;
			o2.writeAndFlush(Unpooled.copiedBuffer("东方大厦的".getBytes("UTF-8")));
		}*/
	}
	
	/**
	 * 异常处理逻辑， 当客户端异常退出的时候，也会运行。
	 * ChannelHandlerContext关闭，也代表当前与客户端连接的资源关闭。
	 */
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		
			System.out.println("server exceptionCaught method run...");
			cause.printStackTrace();
			//ctx.close();
		
		super.exceptionCaught(ctx, cause);
	}
	/**
	 * 有新连接时，会生产新的ctx
	 */
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		list.add(ctx); // 在这儿把通道处理上下文收集起来，写出消息时，就可以使用他向指定客户端发送消息。
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println("通道注册之后，数量："+list.size()+"##"+df.format(new Date()));
		super.channelRegistered(ctx);
	}
	/**
	 * 断开连接时，netty会调用该方法
	 */
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		list.remove(ctx);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println("通道删除之后，数量："+list.size()+"##"+df.format(new Date()));
		// TODO Auto-generated method stub
		super.channelUnregistered(ctx);
	}
	static class ProtocolParser{
		//按协议发送的数据示例：HEADcontent-length:3HEADBODYabcBODY
		public static String parse(String message){ 
			String[] temp = message.split("HEADBODY");
			temp[0] = temp[0].substring(4);//content-length:3
			temp[1] = temp[1].substring(0, (temp[1].length()-4));//body中的数据
			int length = Integer.parseInt(temp[0].substring(temp[0].indexOf(":")+1));//取出body长度值
			if(length != temp[1].length()){
				return null;
			}
			return temp[1];
		}
		public static String transferTo(String message){
			message = "HEADcontent-length:" + message.length() + "HEADBODY" + message + "BODY";
			return message;
		}
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
	    if (evt instanceof IdleStateEvent) {
	        IdleStateEvent event = (IdleStateEvent) evt;
	        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        switch (event.state()) {
	            case READER_IDLE:
	                //handleReaderIdle(ctx);
	            	System.out.println("读空闲"+df.format(new Date()));
	            	System.out.println(++reader_idle_count);
	            	ctx.writeAndFlush(Unpooled.copiedBuffer("心跳包".getBytes()));/*发生一次空闲事件，就下发一次心跳包，查看客户端是否有回应*/
	            	if(reader_idle_count >= 4) {
	            		ctx.close();/* 如果连续累计了4此空闲事件，就关闭连接 */
	            	}
	                break;
	            case WRITER_IDLE:
	               // handleWriterIdle(ctx);
	            	System.out.println("写空闲"+df.format(new Date()));
	                break;
	            case ALL_IDLE:
	               // handleAllIdle(ctx);
	            	System.out.println("all空闲"+df.format(new Date()));
	                break;
	            default:
	                break;
	        }
	    }
	}
	
}
