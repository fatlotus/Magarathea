package magarathea;

public class JumpException extends RuntimeException {
	private JumpException() { }
	
	public static JumpException instance = new JumpException();
}