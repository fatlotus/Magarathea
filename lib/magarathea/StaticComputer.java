package magarathea;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

public class StaticComputer implements Computer {
	byte[] randomAccessMemory;
	Set<MemoryListener> listeners;
	Map<Integer, IO.Device> peripherals;
	boolean isRunning;
	
	public StaticComputer(byte[] assembledCode) {
		listeners = new HashSet<MemoryListener>();
		peripherals = new HashMap<Integer, IO.Device>();
		randomAccessMemory = new byte[1024 * 1024 * 8];
		isRunning = false;
		
		copyIntoRAM(0, assembledCode, 0, assembledCode.length);
	}
	
	public void readSegmentOfRAM(byte[] buffer, int bufferOffset, int ramOffset, int length) {
		for (int i = ramOffset; i < ramOffset + length; i++) {
			buffer[i - ramOffset + bufferOffset] = randomAccessMemory[i];
		}
	}
	
	public void copyIntoRAM(int offset, byte[] buffer, int start, int end) {
		for (int i = start; i < end; i++) {
			randomAccessMemory[i - start + offset] = buffer[i];
		}
	}
	
	public void writeToRAM(int offset, int value) {
		for (MemoryListener listener : listeners) {
			listener.segmentWrittenTo(this, offset, value);
		}
		
		randomAccessMemory[offset    ] = (byte)((value >>> 24) & 0xff);
		randomAccessMemory[offset + 1] = (byte)((value >>> 16) & 0xff);
		randomAccessMemory[offset + 2] = (byte)((value >>> 8) & 0xff);
		randomAccessMemory[offset + 3] = (byte)((value      ) & 0xff);
	}
	
	public int readFromRAM(int offset) {
		return (randomAccessMemory[offset    ] << 24) |
		       (randomAccessMemory[offset + 1] << 16) |
		       (randomAccessMemory[offset + 2] << 8)  |
		       (randomAccessMemory[offset + 3]     ) ;
	}
	
	public void addMemoryListener(MemoryListener l) {
		listeners.add(l);
	}
	
	public void removeMemoryListener(MemoryListener l) {
		listeners.remove(l);
	}
	
	public void prepareIOSubsystem(IO io) {
		for (int port : peripherals.keySet()) {
			io.addDevice(port, peripherals.get(port));
		}
	}
	
	public void addPeripheral(int port, IO.Device device) {
		peripherals.put(port, device);
	}
	
	public synchronized boolean isRunning() {
		return isRunning;
	}
	
	public synchronized void start() {
		isRunning = true;
		notifyAll();
	}
	
	public synchronized void stop() {
		isRunning = false;
	}
	
	public synchronized void breakpoint() {
		if (!isRunning()) { // may be slightly unstable; things can be woken up for no reason.
			try {
				System.err.println("pausing...");
				wait();
				System.err.println("resuming!");
			} catch (InterruptedException e) { throw new RuntimeException(e); }
		}
	}
	
	public synchronized void step() {
		notifyAll();
	}
	
	public void execute() {
		long recompileStart = System.nanoTime();
		
		JITMemorySegment seg = RuntimeCompiler.recompile(OpcodeCollection.instance(),
		  randomAccessMemory, 0, randomAccessMemory.length);
		
		long recompileEnd = System.nanoTime();
		
		System.err.println("Recompilation took: " + (recompileEnd - recompileStart) + "ns");
		
		seg.setComputer(this);
		
		long start = System.currentTimeMillis();
		
		try {
			seg.executeSafely(1);
		} finally {
			long end = System.currentTimeMillis();
			
			System.err.println("Execution took: " + (end - start) + "ns");
		}
	}
}