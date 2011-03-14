package magarathea;

import magarathea.anno.*;

@Bus(id=0x02, prefix="alu")
public class ALU {
	@ReadWrite(id=0xA00000, name="op") public int operand;
	@ReadWrite(id=0xA00001, name="result") public int result;
	
	@Write(id=0x000010, name="add")
	public void add(int value) {
		result = operand + value;
	}
	
	@Write(id=0x000012, name="sub")
	public void subtract(int value) {
		result = operand - value;
	}
	
	@Write(id=0x000011, name="print")
	public void print(int value) {
		System.out.println(": " + value);
	}
	
	/*
	@ReadRange(start=0x000000, end=0x9FFFFF);
	public long range(int value) {
		return value;
	}
	*/
}