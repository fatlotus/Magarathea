package magarathea;

public abstract class JITMemorySegment {
	private int startOffset, endOffset;
	private int jumpDestination;
	private Computer computer;
	
	public void setExtents(int start, int end) {
		startOffset = start;
		endOffset = end;
	}
	
	public int getJumpDestination() {
		return jumpDestination;
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
	
	protected void breakpoint() {
		System.err.println("BREAKPOINTING!");
		getComputer().breakpoint();
	}
	
	protected void jumpTo(int offset) {
		System.err.println("JUMPING TO: " + offset);
		
		jumpDestination = offset;
		
		throw JumpException.instance;
	}
	
	public void executeSafely(int offset) {
		boolean done = false;
		
		while (!done) {
			try {
				evaluate(offset);
				done = true;
			} catch (JumpException exc) {
				offset = jumpDestination;
			}
		}
		
		throw new RuntimeException("uncompiled region encountered");
	}
	
	protected abstract void evaluate(int offset);
}