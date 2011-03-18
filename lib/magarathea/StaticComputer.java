package magarathea;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

public class StaticComputer implements Computer {
	byte[] randomAccessMemory;
	Set<MemoryListener> memoryListeners;
	Set<ExecutionListener> executionListeners;
	Map<Integer, IO.Device> peripherals;
	boolean isRunning;
	int currentOffset;
	
	public StaticComputer(byte[] assembledCode) {
		memoryListeners = new HashSet<MemoryListener>();
		executionListeners = new HashSet<ExecutionListener>();
		peripherals = new HashMap<Integer, IO.Device>();
		randomAccessMemory = new byte[1024 * 1024 * 8];
		isRunning = false;
		currentOffset = 0;
		
		copyIntoRAM(0, assembledCode, 0, assembledCode.length);
	}
	
	public int getLengthOfRAM() {
		return randomAccessMemory.length;
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
		for (MemoryListener listener : memoryListeners) {
			listener.segmentWrittenTo(this, offset, value);
		}
		
		randomAccessMemory[offset    ] = (byte)((value >>> 24) & 0xff);
		randomAccessMemory[offset + 1] = (byte)((value >>> 16) & 0xff);
		randomAccessMemory[offset + 2] = (byte)((value >>> 8) & 0xff);
		randomAccessMemory[offset + 3] = (byte)((value      ) & 0xff);
	}
	
	public int readFromRAM(int offset) {
		return ((int)(randomAccessMemory[offset    ] & 0xff) << 24) |
		       ((int)(randomAccessMemory[offset + 1] & 0xff) << 16) |
		       ((int)(randomAccessMemory[offset + 2] & 0xff) << 8)  |
		       ((int)(randomAccessMemory[offset + 3] & 0xff)     ) ;
	}
	
	public void addMemoryListener(MemoryListener l) {
		memoryListeners.add(l);
	}
	
	public void removeMemoryListener(MemoryListener l) {
		memoryListeners.remove(l);
	}
	
	public void addExecutionListener(ExecutionListener l) {
		executionListeners.add(l);
	}
	
	public void removeExecutionListener(ExecutionListener l) {
		executionListeners.remove(l);
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
		
		for (ExecutionListener l : executionListeners) {
			l.executionStatusChanged(this);
		}
		
		notifyAll();
	}
	
	public synchronized void stop() {
		isRunning = false;
		
		for (ExecutionListener l : executionListeners) {
			l.executionStatusChanged(this);
		}
	}
	
	public synchronized void breakpoint(int position) {
		try {
			currentOffset = position;
			
			for (ExecutionListener l : executionListeners) {
				l.programCounterChanged(this);
			}
			
			if (!isRunning()) {
				wait();
			}
		} catch (InterruptedException e) { throw new RuntimeException(e); }
	}
	
	public int getProgramCounter() {
		return currentOffset;
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