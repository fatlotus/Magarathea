package magarathea;

public abstract class JITMemorySegment {
	private int startOffset, endOffset;
	private int jumpDestination;
	private Computer computer;
	
	public void setExtents(int start, int end) {
		startOffset = start;
		endOffset = end;
		
		jumpDestination = -1;
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
	
	protected void breakpoint(int offset) {
		getComputer().breakpoint(offset);
	}
	
	protected void jumpTo(int offset) {
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
				if (jumpDestination < startOffset || jumpDestination >= endOffset)
					throw exc;
				
				int internalOffset = (jumpDestination - startOffset) / 8;
				
				offset = internalOffset;
			}
		}
		
		throw new RuntimeException("uncompiled region encountered");
	}
	
	protected abstract void evaluate(int offset);
}