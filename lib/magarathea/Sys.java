package magarathea;

import magarathea.anno.*;

@Bus(id=0x12)
public class Sys {
	@Write(id=0x000001, name="sys.shutdown")
	public void shutdown(int value) {
		System.exit(value);
	}
}