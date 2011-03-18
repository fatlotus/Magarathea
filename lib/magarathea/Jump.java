package magarathea;

import magarathea.anno.*;

@Bus(id=0x90, prefix="jmp")
public class Jump {
	@Write(id=0x00, name="branch")
	public void branchTo(int op) {
		throw new RuntimeException("jumps are implemented in-line");
	}
	
	@Write(id=0x01, name="nonneg")
	public void notNegative(int op) {
		throw new RuntimeException("jumps are implemented in-line");
	}
	
	@Write(id=0x02, name="zero")
	public void isZero(int op) {
		throw new RuntimeException("jumps are implemented in-line");
	}
}