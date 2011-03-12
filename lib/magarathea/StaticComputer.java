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
	
	public StaticComputer() {
		listeners = new HashSet<MemoryListener>();
		peripherals = new HashMap<Integer, IO.Device>();
		randomAccessMemory = new byte[1024 * 1024 * 8];
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		Assembler asm = new Assembler(OpcodeCollection.instance(), null, baos);
		asm.testWithIO();
		// asm.test("flynn> ");
		
		byte[] assembledCode = baos.toByteArray();
		
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
	
	public void execute() {
		JITMemorySegment seg = RuntimeCompiler.recompile(OpcodeCollection.instance(),
		  randomAccessMemory, 0, randomAccessMemory.length);
		
		seg.setComputer(this);
		seg.executeSafely(0);
	}
}