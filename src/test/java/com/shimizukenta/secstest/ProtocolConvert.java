package com.shimizukenta.secstest;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.shimizukenta.secs.SecsCommunicator;
import com.shimizukenta.secs.SecsException;
import com.shimizukenta.secs.gem.COMMACK;
import com.shimizukenta.secs.gem.ONLACK;
import com.shimizukenta.secs.hsmsss.HsmsSsCommunicator;
import com.shimizukenta.secs.hsmsss.HsmsSsCommunicatorConfig;
import com.shimizukenta.secs.hsmsss.HsmsSsProtocol;
import com.shimizukenta.secs.secs1ontcpip.Secs1OnTcpIpCommunicator;
import com.shimizukenta.secs.secs1ontcpip.Secs1OnTcpIpCommunicatorConfig;
import com.shimizukenta.secs.secs2.Secs2;
import com.shimizukenta.secs.secs2.Secs2Exception;
import com.shimizukenta.secstestutil.Secs1OnTcpIpHsmsSsConverter;
import com.shimizukenta.secstestutil.TcpIpAdapter;

public class ProtocolConvert {
	
	private static final int testCycle = 100;
	
	private boolean equipComm;
	private boolean hostComm;
	public int equipCounter;
	public int hostCounter;
	
	public ProtocolConvert() {
		this.equipComm = false;
		this.hostComm = false;
		this.equipCounter = 0;
		this.hostCounter = 0;
	}
	
	public void equipCommunicateState(boolean f) {
		synchronized ( this ) {
			equipComm = f;
		}
	}
	
	public void hostCommunicateState(boolean f) {
		synchronized ( this ) {
			hostComm = f;
		}
	}
	
	public boolean bothCommunicatable() {
		synchronized ( this ) {
			return equipComm && hostComm;
		}
	}
	
	public static void main(String[] args) {
		
		int deviceId = 10;
		SocketAddress innerAddr  = new InetSocketAddress("127.0.0.1", 0);
		SocketAddress secs1Addr  = new InetSocketAddress("127.0.0.1", 23000);
		SocketAddress hsmsSsAddr = new InetSocketAddress("127.0.0.1", 5000);
		
		final ProtocolConvert inst = new ProtocolConvert();
		
		/*
		 * connection
		 * host(hsmsActive) <-> converter <-> adapter <-> equip(secs1)
		 * 
		 */
		
		try (
				TcpIpAdapter adapter = TcpIpAdapter.open(secs1Addr, innerAddr);
				) {
			
			Secs1OnTcpIpCommunicatorConfig secs1ConverterConfig = new Secs1OnTcpIpCommunicatorConfig();
			secs1ConverterConfig.socketAddress(adapter.socketAddressB());
			secs1ConverterConfig.deviceId(deviceId);
			secs1ConverterConfig.isEquip(false);
			secs1ConverterConfig.isMaster(false);
			secs1ConverterConfig.retry(0);
			
			HsmsSsCommunicatorConfig hsmsSsConverterConfig = new HsmsSsCommunicatorConfig();
			hsmsSsConverterConfig.socketAddress(hsmsSsAddr);
			hsmsSsConverterConfig.protocol(HsmsSsProtocol.PASSIVE);
			hsmsSsConverterConfig.sessionId(deviceId);
			hsmsSsConverterConfig.isEquip(true);
			hsmsSsConverterConfig.rebindIfPassive(5.0F);
			
			try (
					Secs1OnTcpIpHsmsSsConverter converter = Secs1OnTcpIpHsmsSsConverter.open(secs1ConverterConfig, hsmsSsConverterConfig);
					) {
				
				Secs1OnTcpIpCommunicatorConfig equipConfig = new Secs1OnTcpIpCommunicatorConfig();
				equipConfig.communicatorName("Equip");
				equipConfig.socketAddress(secs1Addr);
				equipConfig.deviceId(deviceId);
				equipConfig.isEquip(true);
				equipConfig.isMaster(true);
				equipConfig.retry(0);
				
				HsmsSsCommunicatorConfig hostConfig = new HsmsSsCommunicatorConfig();
				hostConfig.communicatorName("Host");
				hostConfig.socketAddress(hsmsSsAddr);
				hostConfig.protocol(HsmsSsProtocol.ACTIVE);
				hostConfig.sessionId(deviceId);
				hostConfig.isEquip(false);
				
				try (
						SecsCommunicator equip = Secs1OnTcpIpCommunicator.newInstance(equipConfig);
						SecsCommunicator host  = HsmsSsCommunicator.newInstance(hostConfig);
						) {
					
					equip.addSecsLogListener(ProtocolConvert::echo);
					
					equip.addSecsCommunicatableStateChangeListener(state -> {
						synchronized ( inst ) {
							inst.equipCommunicateState(state);
							inst.notifyAll();
						}
					});
					
					equip.addTrySendMessagePassThroughListener(msg -> {
						echo("equip-pt-trysnd: strm: " + msg.getStream() + ", func: " + msg.getFunction());
					});
					equip.addSendedMessagePassThroughListener(msg -> {
						echo("equip-pt-sended: strm: " + msg.getStream() + ", func: " + msg.getFunction());
					});
					equip.addReceiveMessagePassThroughListener(msg -> {
						echo("equip-pt-recved: strm: " + msg.getStream() + ", func: " + msg.getFunction());
					});
					
					equip.addSecsMessageReceiveListener(msg -> {
						
						try {
							
							int strm = msg.getStream();
							int func = msg.getFunction();
							boolean wbit = msg.wbit();
							
							if ( wbit ) {
								
								switch ( strm ) {
								case 1: {
									
									switch ( func ) {
									case 1: {
										
										equip.gem().s1f2(msg);
										break;
									}
									case 13: {
										
										equip.gem().s1f14(msg, COMMACK.OK);
										break;
									}
									case 15: {
										
										equip.gem().s1f16(msg);
										break;
									}
									case 17: {
										
										equip.gem().s1f18(msg, ONLACK.OK);
										break;
									}
									}
									break;
								}
								}
							}
						}
						catch ( SecsException e ) {
							echo(e);
						}
						catch ( InterruptedException ignore ) {
						}
					});
					
					
					host.addSecsLogListener(ProtocolConvert::echo);
					
					host.addSecsCommunicatableStateChangeListener(state -> {
						synchronized ( inst ) {
							inst.hostCommunicateState(state);
							inst.notifyAll();
						}
					});
					
					host.addTrySendMessagePassThroughListener(msg -> {
						echo("host-pt-trysnd: strm: " + msg.getStream() + ", func: " + msg.getFunction());
					});
					host.addSendedMessagePassThroughListener(msg -> {
						echo("host-pt-sended: strm: " + msg.getStream() + ", func: " + msg.getFunction());
					});
					host.addReceiveMessagePassThroughListener(msg -> {
						echo("host-pt-recved: strm: " + msg.getStream() + ", func: " + msg.getFunction());
					});
					
					host.addSecsMessageReceiveListener(msg -> {
						
						int strm = msg.getStream();
						int func = msg.getFunction();
						boolean wbit = msg.wbit();
						
						if ( wbit ) {
							
							try {
								switch ( strm ) {
								case 1: {
									
									switch ( func ) {
									case 1: {
										
										host.gem().s1f2(msg);
										break;
									}
									case 13: {
										
										host.gem().s1f14(msg, COMMACK.OK);
										break;
									}
									}
									break;
								}
								case 6: {
									
									switch ( func ) {
									case 3: {
										
										host.gem().s6f4(msg);
										break;
									}
									}
									break;
								}
								}
							}
							catch ( SecsException e ) {
								echo(e);
							}
							catch ( InterruptedException ignore ) {
							}
						}
					});
					
					
					equip.open();
					host.open();
					
					synchronized ( inst ) {
						
						echo("wait-until-both-communicatable");
						
						for ( ;; ) {
							if ( inst.bothCommunicatable() ) {
								break;
							}
							inst.wait();
						}
						
						echo("both-communicated");
					}
					
					
					final int m = testCycle;
					
					final Collection<Thread> threads = new ArrayList<>();
					
					threads.addAll(Arrays.asList(
							new Thread(() -> {
								
								try {
									
									for ( ; inst.equipCounter < m; ) {
										
										equip.gem().s1f13();
										Thread.sleep(1L);
										
										equip.gem().s1f1();
										Thread.sleep(1L);
										
										++ inst.equipCounter;
										echo("equip-count: " + inst.equipCounter);
									}
								}
								catch ( SecsException e ) {
									
									threads.forEach(Thread::interrupt);
									echo(e);
									
									throw new RuntimeException(e);
								}
								catch ( InterruptedException e ) {
								}
							}),
							new Thread(() -> {
								
								try {
									for (; inst.hostCounter < m; ) {
										
										host.gem().s1f13();
										Thread.sleep(1L);
										
										try {
											host.gem().s1f17();
										}
										catch ( Secs2Exception e ) {
											echo(e);
										}
										Thread.sleep(1L);
										
										try {
											host.gem().s1f15();
										}
										catch ( Secs2Exception e ) {
											echo(e);
										}
										Thread.sleep(1L);
										
										++ inst.hostCounter;
										echo("host-count: " + inst.hostCounter);
									}
								}
								catch ( SecsException e ) {
									
									threads.forEach(Thread::interrupt);
									echo(e);
									
									throw new RuntimeException(e);
								}
								catch ( InterruptedException e ) {
								}
							})
							));
					
					
					threads.forEach(Thread::start);
					
					for ( Thread th : threads ) {
						th.join();
					}
				}
			}
		}
		catch ( InterruptedException ignore ) {
		}
		catch ( Throwable t ) {
			echo(t);
		}
		
		echo("reach end");
		echo("equip-count: " + inst.equipCounter);
		echo("host-count: " + inst.hostCounter);
	}
	
	private static synchronized void echo(Object o) {
		if ( o instanceof Throwable) {
			((Throwable) o).printStackTrace();
		} else {
			System.out.println(o);
		}
		System.out.println();
	}
	
	protected static Secs2 createS6F3Secs2() {
		
		Secs2 v = Secs2.list(
				Secs2.int4(-100, -200, -300, -400, -500),
				Secs2.uint4(100, 200, 300, 400, 500),
				Secs2.int8(-1000, -2000, -3000, -4000, -5000),
				Secs2.uint8(1000, 2000, 3000, 4000, 5000),
				Secs2.float4(-0.10F, -0.20F, -0.30F, -0.40F, -0.50F),
				Secs2.float8(-0.010D, -0.020D, -0.030D, -0.040D, -0.050D)
				);
		
		Secs2 vv = Secs2.list(
				Secs2.ascii("values"),
				v, v, v, v, v,
				v, v, v, v, v
				);

		Secs2 ss = Secs2.list(
				Secs2.ascii("ASCII"),
				Secs2.binary((byte)0x1),
				Secs2.list(
						Secs2.binary((byte)0xFF, (byte)0x0, (byte)0x1, (byte)0x2, (byte)0x3),
						Secs2.int1(-1, -2, -3, -4, -5),
						Secs2.uint1(1, 2, 3, 4, 5),
						Secs2.int2(-10, -20, -30, -40, -50),
						Secs2.uint2(10, 20, 30, 40, 50)
						),
				Secs2.bool(true),
				Secs2.int4(100, 200, 300, 400, 500),
				Secs2.float4(0.10F, 0.20F, 0.30F, 0.40F, 0.50F),
				vv
				);
		
		return ss;
	}
	
}
