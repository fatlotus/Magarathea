%	from,		to
message: "Welcome to the Lotus Interpreter\00"
...
	$message,	alu.op
!loop0
	#1,			alu.add
	% ALU delay
	alu.result,	ram.get
	% RAM delay
	ram.value,	jmp.zero
	!finish,	jmp.cond
	ram.value,	screen.write
	!loop0,		jmp.always
!finish
	!finish,	jmp.always