package magarathea.peripherals;

import magarathea.IO;

public class Keyboard implements IO.Device {
	int state = 0;
	byte[] enteredQueue;
	byte[] typeQueue;
	int start, length;
	
	public Keyboard(int bufferSize) {
		state = 0;
		enteredQueue = new byte[bufferSize];
		typeQueue = new byte[bufferSize];
		start = 0;
		length = 0;
	}
	
	public void emitKeyEvent(byte type, byte character) {
		enteredQueue[(start + length) % enteredQueue.length]   = character;
		typeQueue   [(start + length) % enteredQueue.length] = character;
		
		start = (start % enteredQueue.length);
		length++;
	}
	
	public byte read() {
		switch(state) {
		case 0:
			if (length == 0) return 0;
			state = 1;
			return typeQueue[start % enteredQueue.length];
		case 1:
			state = 0;
			length++; start++;
			return enteredQueue[start % enteredQueue.length];
		default:
			throw new RuntimeException("Undefined behavior here.");
		}
	}
	
	public void write(byte x) { }
	
	public boolean poll() {
		return !(state == 0 && length == 0);
	}
}