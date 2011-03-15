package magarathea;

import magarathea.anno.*;

@Bus(prefix="mem", id=0x13)
public class Memory {
	JITMemorySegment segment;
	
	public Memory(JITMemorySegment seg) {
		segment = seg;
	}
	
	@ReadWrite(id=0x01, name="addr")
	public int pointer;
	
	@ReadWrite(id=0x05, name="result")
	public int value;
	
	@Write(id=0x03, name="read")
	public void read(int ptr) {
		value = segment.getComputer().readFromRAM(ptr);
	}
	
	@Write(id=0x04, name="write")
	public void write(int value) {
		System.err.printf("putting #%d into %x\n", value, pointer);
		segment.getComputer().writeToRAM(pointer, value);
	}
}