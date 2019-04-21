package com.example.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

public class Client4HelloWorldHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try{
			ByteBuf readBuffer = (ByteBuf) msg;
			byte[] tempDatas = new byte[readBuffer.readableBytes()];
			readBuffer.readBytes(tempDatas);
			System.out.println("from server : " + new String(tempDatas, "UTF-8"));
			Thread.currentThread().sleep(1000);
			//ctx.channel().writeAndFlush(Unpooled.copiedBuffer("HEADcontent-length:1HEADBODYaBODY".getBytes()));
		}finally{
			// 用于释放缓存。避免内存溢出
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("client exceptionCaught method run...");
		cause.printStackTrace();
		//ctx.close(); //关闭通道？
	}

	@Override // 断开连接时执行
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelInactive method run...客户端通道断开");
	}

	@Override // 连接通道建立成功时执行
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelActive method run...客户端通道建立成功");
		ctx.writeAndFlush("HEADcontent-length:3HEADBODY客户端通道，刚刚建立成功BODY");
	}

	@Override // 每次读取完成时执行
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		//System.out.println("channelReadComplete method run...");
	}

}
