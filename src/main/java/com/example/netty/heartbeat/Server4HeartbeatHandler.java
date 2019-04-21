/**
 * @Sharable注解 - 
 *  代表当前Handler是一个可以分享的处理器。也就意味着，服务器注册此Handler后，可以分享给多个客户端同时使用。
 *  如果不使用注解描述类型，则每次客户端请求时，必须为客户端重新创建一个新的Handler对象。
 *  
 */
package com.example.netty.heartbeat;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
public class Server4HeartbeatHandler extends ChannelInboundHandlerAdapter {
	
	private static List<String> credentials = new ArrayList<>();
	private static final String HEATBEAT_SUCCESS = "SERVER_RETURN_HEARTBEAT_SUCCESS";
	public Server4HeartbeatHandler(){
		// 初始化客户端列表信息。一般通过配置文件读取或数据库读取。
		credentials.add("10.177.255.181_LENOVO-PC");
	}
	
	// 业务处理逻辑
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		//System.out.println("from client1:"+msg);
		// 获取读取的数据， 是一个缓冲。
				ByteBuf readBuffer = (ByteBuf) msg;
				// 创建一个字节数组，用于保存缓存中的数据。
				byte[] tempDatas = new byte[readBuffer.readableBytes()];
				// 将缓存中的数据读取到字节数组中。
				readBuffer.readBytes(tempDatas);
				String message = new String(tempDatas, "UTF-8");//数据前面已经读取到tempDatas中了
				//
				System.out.println("from client2 : " + message);
		if(message instanceof String){
			this.checkCredential(ctx, message);
		} else if (msg instanceof HeatbeatMessage){
			this.readHeatbeatMessage(ctx, msg);
		} else {
			ctx.writeAndFlush("wrong message").addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	private void readHeatbeatMessage(ChannelHandlerContext ctx, Object msg){
		HeatbeatMessage message = (HeatbeatMessage) msg;
		System.out.println(message);
		System.out.println("=======================================");
		ctx.writeAndFlush("receive heatbeat message");
	}

	/**
	 * 身份检查。检查客户端身份是否有效。
	 * 客户端身份信息应该是通过数据库或数据文件定制的。
	 * 身份通过 - 返回确认消息。
	 * 身份无效 - 断开连接
	 * @param ctx
	 * @param credential
	 */
	private void checkCredential(ChannelHandlerContext ctx, String credential){
		//System.out.println(credential);
		//System.out.println(credentials);
		if(credentials.contains(credential)){
			ByteBuf copiedBuffer = Unpooled.copiedBuffer(HEATBEAT_SUCCESS.getBytes());
			ctx.writeAndFlush(copiedBuffer);
		}else{
			//ctx.writeAndFlush("no credential contains").addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	// 异常处理逻辑
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("server exceptionCaught method run...");
		// cause.printStackTrace();
		ctx.close();
	}

}
