package magarathea;

import magarathea.anno.*;

@Bus(prefix="mem", id=0x13)
public class Memory {
	JITMemorySegment segment;
	
	public Memory(JITMemorySegment seg) {
		segment = seg;
	}
	
	@ReadWrite(id=0x01, name="ptr")
	public int pointer;
	
	@ReadWrite(id=0x05, name="value")
	public int value;
	
	@Write(id=0x03, name="read")
	public void read(int ptr) {
		value = segment.getComputer().readFromRAM(ptr);
		
		System.err.println("GETTING @" + ptr + " = #" + value);
	}
	
	@Write(id=0x04, name="write")
	public void write(int value) {
		segment.getComputer().writeToRAM(pointer, value);
	}
}