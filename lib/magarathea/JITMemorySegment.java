package magarathea;

public abstract class JITMemorySegment {
	private int startOffset, endOffset;
	private Computer computer;
	
	public void setExtents(int start, int end) {
		startOffset = start;
		endOffset = end;
	}
	
	public void setComputer(Computer cmp) {
		computer = cmp;
	}
	
	public Computer getComputer() {
		return computer;
	}
	
	public boolean includesOffset(int offset) {
		return startOffset <= offset && endOffset > offset;
	}
	
	public void executeSafely(int offset) {
		evaluate(offset);
		
		throw new RuntimeException("uncompiled region encountered");
	}
	
	protected abstract void evaluate(int offset);
}