package magarathea;

public interface MemoryListener {
	public void segmentWrittenTo(Computer c, int offset, int value);
}