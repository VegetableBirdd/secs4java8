package com.shimizukenta.secs.secs1ontcpip;

import java.net.SocketAddress;
import java.util.Objects;

import com.shimizukenta.secs.secs1.Secs1CommunicatorConfig;

public class Secs1OnTcpIpCommunicatorConfig extends Secs1CommunicatorConfig {
	
	private static final long serialVersionUID = -7468433384957790240L;
	
	private SocketAddress socketAddr;
	private float reconnectSeconds;
	
	public Secs1OnTcpIpCommunicatorConfig() {
		super();
		
		socketAddr = null;
		reconnectSeconds = 5.0F;
	}
	
	public void socketAddress(SocketAddress socketAddr) {
		synchronized ( this ) {
			this.socketAddr = Objects.requireNonNull(socketAddr);
		}
	}
	
	public SocketAddress socketAddress() {
		synchronized ( this ) {
			
			if ( socketAddr == null ) {
				throw new IllegalStateException("SocketAddress not setted");
			}
			
			return socketAddr;
		}
	}
	
	public void reconnectSeconds(float seconds) {
		this.reconnectSeconds = seconds;
	}
	
	public float reconnectSeconds() {
		return this.reconnectSeconds;
	}
}
