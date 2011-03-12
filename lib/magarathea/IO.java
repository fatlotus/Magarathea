package magarathea;

import java.util.HashMap;
import magarathea.anno.*;

@Bus(id=0x14, prefix="io")
public class IO {
	private HashMap<Integer, Device> devices;
	
	public static interface Device {
		public void write(byte x);
		public byte read();
		public boolean poll();
	}
	
	public IO(JITMemorySegment seg) {
		devices = new HashMap<Integer, Device>();
		seg.getComputer().prepareIOSubsystem(this);
	}
	
	@ReadWrite(id=0x000001, name="port") public int port;
	@ReadWrite(id=0x000002, name="value") public int value;
	
	@Write(id=0x000003, name="write")
	public void write(int value) {
		if (devices.containsKey(port)) {
			devices.get(port).write((byte)(value & 0xff));
		}
	}
	
	@Read(id=0x000004, name="poll")
	public int poll() {
		if (devices.containsKey(port)) {
			return devices.get(port).poll() ? 1 : 2;
		}
		return 0;
	}
	
	@Write(id=0x000005, name="read")
	public void read(int ignored) {
		if (devices.containsKey(port)) {
			value = devices.get(port).read();
		}
	}
	
	public void addDevice(int port, Device backing) {
		if (port > 65536) {
			throw new IllegalArgumentException("cannot assign port > 65,536");
		} else if (port < 0) {
			throw new IllegalArgumentException("cannot assign negative port");
		} else if (devices.containsKey(port)) {
			throw new IllegalStateException("already have a device with id " + port);
		}
		
		devices.put(port, backing);
	}
	
	public void removeDevice(int port, Device backing) {
		devices.remove(port);
	}
}