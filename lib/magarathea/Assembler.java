package magarathea;

import java.io.*;

public class Assembler {
	private BufferedReader input;
	private DataOutput output;
	private OpcodeCollection collect;
	
	public Assembler(OpcodeCollection coll, BufferedReader in, OutputStream out) {
		input = in;
		output = new DataOutputStream(out);
		collect = coll;
	}
	
	public void testConditionals() {
		processInstruction("#10", "alu.op");
		processInstruction("#1",  "alu.sub");
		processInstruction("alu.result", "alu.op");
		processInstruction("alu.result", "jmp.nonneg");
		processInstruction("alu.result", "alu.print");
		processInstruction("#8", "jmp.branch");
		processInstruction("alu.result", "sys.shutdown");
	}
	
	public void testWithIO() {
		/*  0 */ processInstruction("#0", "io.port");
		/*  8 */ processInstruction("#32768", "mem.ptr");
		/* 16 */ processInstruction("#1", "io.read");
		/* 24 */ processInstruction("#48", "alu.op");
		/* 32 */ processInstruction("io.value", "alu.add");
		/* 40 */ processInstruction("alu.result", "mem.write");
		/* 48 */ processInstruction("#8", "jmp.branch");
	}
	
	public void test(String message) {
		int jumpOffset = 8;
		int offset = 0x8000;
		message += "    ";
		
		for (int i = 0; i < message.length() - 4; i += 3) {
			int v = (
				(message.charAt(i) << 16) |
				(message.charAt(i + 1) << 8) |
				(message.charAt(i + 2))
			);
			
			processInstruction("#" + offset, "mem.ptr");
			processInstruction("#" + (int)v, "mem.write");
			
			jumpOffset += 16;
			offset += 3;
		}
		
		processInstruction("#" + 0x8781, "mem.ptr");
		processInstruction("#" + (message.length() - 4), "mem.write");
		
		jumpOffset += 16;
		
		processInstruction("#" + jumpOffset, "jmp.branch");
	}
	
	public void test3() {
		/*   8 */ processInstruction("#42", "alu.print");
		/*  16 */ processInstruction("#46", "alu.print");
	}
	
	public void test2() {
		/*   0 */ processInstruction("#0", "mem.ptr");
		/*   8 */ processInstruction("mem.ptr", "mem.read");
		/*  16 */ processInstruction("mem.value", "alu.print");
		/*  24 */ processInstruction("mem.ptr", "alu.op");
		/*  32 */ processInstruction("#1", "alu.add");
		/*  48 */ processInstruction("alu.result", "mem.ptr");
		/*  56 */ processInstruction("#8", "jmp.branch");
	}
	
	public void assembleFile(File filename) throws IOException {
		BufferedReader lineReader = new BufferedReader(new FileReader(filename));
		
		while (lineReader.ready()) {
			String line = lineReader.readLine();
		}
	}
	
	private void op(int value) {
		try {
			output.writeInt(value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void processLeftHandSide(String left) {
		if (left.charAt(0) == '#') {
			op(0x11000000 | (Integer.parseInt(left.substring(1)) & 0x00ffffff));
		} else {
			OpcodeCollection.Output out = collect.getLeftHandSide(left);
			
			if (out == null) throw new RuntimeException("unknown LHS: " + left);
			op(out.opcode);
		}
	}
	
	private void processRightHandSide(String right) {
		if (right.equals("jmp.branch")) {
			op(0x90000000);
		} else if (right.equals("jmp.nonneg")) {
			op(0x90000001);
		} else if (right.equals("jmp.zero")) {
			op(0x90000002);
		} else {
			OpcodeCollection.Output out = collect.getRightHandSide(right);
			
			if (out == null) throw new RuntimeException("unknown RHS: " + right);
			op(out.opcode);
		}
	}
	
	private void processInstruction(String left, String right) {
		processLeftHandSide(left);
		processRightHandSide(right);
	}
}