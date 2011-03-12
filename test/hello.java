/*
%	from,		to
...
message: "Welcome to the Lotus Interpreter\00"
...
	$message,	alu.op
!loop0
	#1,			alu.add
	% ALU delay
	alu.result,	ram.get
	% RAM delay
	ram.value,	jmp.zero
	!finish,	jmp.goreset
	ram.value,	screen.write
	!loop0,		jmp.always
!finish
	#0,			sys.shutdown
*/
class __magtest extends RuntimeSystem {
	private ALU alu;
	private Sys sys;
	
	public __magtest() {
		alu.op = 0x1000;
		
		loop0:
		alu.add(1);
		loadRAM(alu.result);
		sys.jmpzero(ramValue);
		if(jmpCondition()) goto loop1;
		
		sys.write(ramValue);
		goto loop0;
		loop1:
		sys.shutdown(0);
	}
}