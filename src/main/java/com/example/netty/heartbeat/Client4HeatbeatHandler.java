package com.example.netty.heartbeat;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

public class Client4HeatbeatHandler extends ChannelInboundHandlerAdapter{

	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private ScheduledFuture heatbeat;
	private InetAddress remoteAddr;
	private static final String HEARTBEAT_SUCCESS = "SERVER_RETURN_HEARTBEAT_SUCCESS";
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// 获取本地INET信息
		this.remoteAddr = InetAddress.getLocalHost();
		// 获取本地计算机名
		String computerName = System.getenv().get("COMPUTERNAME");
		String credentials = this.remoteAddr.getHostAddress() + "_" + computerName;
		System.out.println(credentials);
		// 发送到服务器，作为信息比对证书
//		InetSocketAddress address = new InetSocketAddress("localhost", 9999);
//		ctx.connect(address);
		ByteBuf byteBuf = Unpooled.copiedBuffer(credentials.getBytes());
		//ctx.writeAndFlush(credentials);
		ctx.writeAndFlush(byteBuf);
	}
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		/*// 获取本地INET信息
		this.remoteAddr = InetAddress.getLocalHost();
		// 获取本地计算机名
		String computerName = System.getenv().get("COMPUTERNAME");
		String credentials = this.remoteAddr.getHostAddress() + "_" + computerName;
		System.out.println(credentials);
		// 发送到服务器，作为信息比对证书
		ctx.writeAndFlush(credentials);*/
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try{
			//将msg转成String
			ByteBuf msg2 = (ByteBuf)msg;
			byte[] temp = new byte[msg2.readableBytes()];
			msg2.readBytes(temp);
			String str = new String(temp, "utf-8");
			System.out.println("客户端收到："+temp);
			if(str instanceof String){
				if(HEARTBEAT_SUCCESS.equals(str)){
					this.heatbeat = this.executorService.scheduleWithFixedDelay(new HeatbeatTask(ctx), 0L, 2L, TimeUnit.SECONDS);
					//System.out.println("client receive - " + str);
				}else{
					System.out.println("client receive - " + str);
				}
			}
		}finally{
			//ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("client exceptionCaught method run...");
		// cause.printStackTrace();
		// 回收资源
		if(this.heatbeat != null){
			this.heatbeat.cancel(true);
			this.heatbeat = null;
		}
		ctx.close();
	}
	
	class HeatbeatTask implements Runnable{
		private ChannelHandlerContext ctx;
		int a=0;
		public HeatbeatTask(){
			
		}
		public HeatbeatTask(ChannelHandlerContext ctx){
			this.ctx = ctx;
		}
		public void run(){
			try {
				System.out.println("客户端线程类执行次数："+a++);
				HeatbeatMessage msg = new HeatbeatMessage();
				msg.setIp(remoteAddr.getHostAddress());
				Sigar sigar = new Sigar();
				// CPU信息
				CpuPerc cpuPerc = sigar.getCpuPerc();
				Map<String, Object> cpuMsgMap = new HashMap<>();
				cpuMsgMap.put("Combined", cpuPerc.getCombined());
				cpuMsgMap.put("User", cpuPerc.getUser());
				cpuMsgMap.put("Sys", cpuPerc.getSys());
				cpuMsgMap.put("Wait", cpuPerc.getWait());
				cpuMsgMap.put("Idle", cpuPerc.getIdle());
				
				// 内存信息
				Map<String, Object> memMsgMap = new HashMap<>();
				Mem mem = sigar.getMem();
				memMsgMap.put("Total", mem.getTotal());
				memMsgMap.put("Used", mem.getUsed());
				memMsgMap.put("Free", mem.getFree());
				
				// 文件系统
				Map<String, Object> fileSysMsgMap = new HashMap<>();
				FileSystem[] list = sigar.getFileSystemList();
				fileSysMsgMap.put("FileSysCount", list.length);
				List<String> msgList = null;
				for(FileSystem fs : list){
					msgList = new ArrayList<>();
					msgList.add(fs.getDevName() + "总大小:    " + sigar.getFileSystemUsage(fs.getDirName()).getTotal() + "KB");
					msgList.add(fs.getDevName() + "剩余大小:    " + sigar.getFileSystemUsage(fs.getDirName()).getFree() + "KB");
					fileSysMsgMap.put(fs.getDevName(), msgList);
				}
				
				msg.setCpuMsgMap(cpuMsgMap);
				msg.setMemMsgMap(memMsgMap);
				msg.setFileSysMsgMap(fileSysMsgMap);
				
				String msgStr = msg.toString();
				ByteBuf copiedBuffer = Unpooled.copiedBuffer(msgStr.getBytes());
				
				ctx.writeAndFlush(copiedBuffer);
				System.out.println("客户端要发送："+msgStr);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
