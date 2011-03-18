package magarathea;

public interface ExecutionListener {
	public void programCounterChanged(Computer c);
	public void executionStatusChanged(Computer c);
}