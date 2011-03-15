package magarathea;

public interface Computer {
	public void writeToRAM(int offset, int value);
	public int readFromRAM(int offset);
	public void readSegmentOfRAM(byte[] buffer, int bufferOffset, int ramOffset, int length);
	public void addMemoryListener(MemoryListener l);
	public void removeMemoryListener(MemoryListener l);
	public void execute();
	public void prepareIOSubsystem(IO io);
	public boolean isRunning();
	public void start();
	public void stop();
	public void step();
	void breakpoint();
}