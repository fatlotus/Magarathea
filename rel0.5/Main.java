import java.io.*;
import java.util.*;

class DataBus {
	public DataBus() {
		reset();
	}
	
	public void reset() {
		longValue = 0l;
		intValue = 0;
		boolValue = false;

		longValueSpecified = intValueSpecified = boolValueSpecified = false;
	}
	
	long longValue;
	boolean longValueSpecified;
	
	int intValue;
	boolean intValueSpecified;
	
	boolean boolValue;
	boolean boolValueSpecified;
	
	public void put(int value) {
		intValue = value;
		longValue = (long)value;
		
		longValueSpecified = intValueSpecified = true;
	}
	
	public int asInt() {
		if (intValueSpecified) {
			return intValue;
		} else if (longValueSpecified) {
			return (int)longValue;
		} else if (boolValueSpecified) {
			return boolValue ? 1 : 0;
		}
		
		return 0;
	}
}

class Assembler {
	private HashMap<String, JumpDeclaration> jumps;
	private ArrayList<ASTElement> tree;
	private int lineNumber;
	private boolean nextLineIsVariable;
	private HashMap<String, Variable> variables;
	
	public Assembler() {
		tree = new ArrayList<ASTElement>();
		jumps = new HashMap<String, JumpDeclaration>();
		nextLineIsVariable = false;
		lineNumber = 0;
	}
	
	public abstract class ASTElement {
		private int offset = 0;
		private int lineno = 0;
		
		public void setOffset(int offst) {
			offset = offst;
		}
		
		public int getOffset() {
			return offset;
		}
		
		public void setLineNumber(int lineNumber) {
			lineno = lineNumber;
		}
		
		public int getLineNumber() {
			return lineno;
		}
	}
	public interface Side { }
	public interface Left extends Side {
		public int getLValue();
	}
	public interface Right extends Side {
		public int getRValue();
	}
	
	public class Custom implements Left, Right {
		private int value;
		
		public Custom(int val) {
			value = val;
		}
		
		public int getLValue() { return value; }
		public int getRValue() { return value; }
	}
	
	public class Constant implements Left {
		private int value;
		
		public Constant(int val) {
			value = val;
		}
		
		public int getLValue() {
			return value & 0x00ffffff;
		}
	}
	public class Register implements Left, Right {
		public int number;
		
		public Register(int num) {
			number = num;
		}
		
		public int getLValue() {
			return number & 0x007 | 0x020000A0;
		}
		
		public int getRValue() {
			return number & 0x007 | 0x020000A0;
		}
	}
	
	public class Memory implements Left, Right {
		public String variableName;
		public int value;
		
		public Memory(String variableName) {
			this.variableName = variableName;
			this.value = 0x00;
		}
		
		public void setValue(int value) {
			this.value = value;
		}
		
		public int getRValue() {
			return (value & 0x00ffffff) | 0x03000000;
		}
		
		public int getLValue() {
			return (value & 0x00ffffff) | 0x03000000;
		}
	}
	
	public class Jump implements Left {
		private String jumpName;
		private JumpDeclaration jmp;
		
		public Jump(String name) {
			jumpName = name;
			jmp = null;
		}
		
		public String getName() {
			return jumpName;
		}
		
		public JumpDeclaration getJumpDeclaration() {
			return jmp;
		}
		
		public void setJumpDeclaration(JumpDeclaration jump) {
			jmp = jump;
		}
		
		public int getLValue() {
			if (jmp != null) {
				return (jmp.getOffset() & 0x00ffffff);
			} else {
				return 0x00000000;
			}
		}
	}
	
	public class Variable {
		private String name;
		private byte[] value;
		
		public Variable(String name, String constantValue) {
			this(name, constantValue.getBytes());
		}
		
		public Variable(String name, byte[] value) {
			this.name = name;
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public byte[] getInitialValue() {
			return value;
		}
	}
	
	public class Line extends ASTElement {
		private Side left, right;
		
		public Line(Side lhs, Side rhs) {
			left = lhs;
			right = rhs;
		}
		
		public Side getLeft() {
			return left;
		}
		
		public Side getRight() {
			return right;
		}
	}
	
	public class JumpDeclaration extends ASTElement {
		private String jumpName;
		
		public JumpDeclaration(String name) {
			jumpName = name;
		}
		
		public String getName() {
			return jumpName;
		}
	}
	
	private void parse(BufferedReader r) throws IOException {
		r.readLine(); // skip first line
		
		while(r.ready()) {
			String line = r.readLine().trim();
			
			String[] fragments = line.split(",", 2);
			lineNumber++;
			
			parseLine(line);
		}
	}
	
	private void parseError(String message) {
		System.err.printf("line %5d: syntax error, %s\n", lineNumber, message);
	}
	
	private void linkerError(String message) {
		System.err.printf("line %5d: linker error, %s\n", lineNumber, message);
	}
	
	private byte[] parseConstant(String value) {
		if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
			byte[] buffer = new byte[value.length() - 1];
			int length = 0;
			int offset = 1;
			int maxlen = value.length() - 1;
			
			for (; offset < maxlen; offset++) {
				if (value.charAt(offset) == '\\') { // ESCAPE SEQUENCE. (Grant says "Run Away!")
					if (offset + 2 < maxlen) {
						return null;
					}
					
					buffer[length++] = (byte)Integer.parseInt (value.substring (offset, offset + 2));
					
					offset += 2;
				} else {
					buffer[length++] = (byte)value.charAt(offset); // UNSAFE.
				}
			}
			
			return buffer;
		}
		
		return null;
	}
	
	private void parseVariableLine(String line) {
		if (line.length() == 0) {
			/* do nothing on blank line */
		} else if (line.equals("...")) {
			nextLineIsVariable = false;
		} else {
			int offset = line.indexOf(":");
			
			if (offset != -1) {
				String variableName = line.substring(0, offset);
				String value = line.substring(offset + 1);
				byte[] binaryValue = parseConstant(value);
				
				if (binaryValue == null) {
					parseError("invalid constant");
				} else {
					if (variables.containsKey(variableName)) {
						parseError("duplicate variable " + variableName);
					} else {
						variables.put(variableName, new Variable(variableName, binaryValue));
					}
				}
			} else {
				parseError("line does not contain a colon");
			}
		}
	}
	
	private void parseLine(String line) {
		if (nextLineIsVariable) {
			parseVariableLine(line);
		} else {
			parseInstructionLine(line);
		}
	}
	
	private void parseInstructionLine(String line) {
		if (line.length() == 0) {
			/* do nothing on blank line*/
		} else if (line.charAt(0) == '!') { // JUMP
			String jumpName = line.substring(1);
			
			JumpDeclaration jmp = new JumpDeclaration(jumpName);
			
			if (jumps.containsKey(jumpName)) {
				parseError("duplicate jump \"" + jumpName + "\"");
			} else {
				jumps.put(jumpName, jmp);
				
				tree.add(jmp);
			}
			
		} else if (line.equals("...")) {
			nextLineIsVariable = true;
		} else {
			int offset = line.indexOf(",");
			
			if (offset != -1) {
				String fromVal = line.substring(0, offset).trim();
				String toVal = line.substring(offset + 1).trim();
				
				Left from;
				Right to;
				
				if (fromVal.charAt(0) == '#') { // HEX LITERAL
					
					try {
						from = new Constant (Integer.parseInt (fromVal.substring(1), 16));
					} catch (NumberFormatException e) {
						parseError("invalid hex constant " + fromVal.substring(1));
						
						from = null;
					}
					
				} else if (fromVal.charAt(0) == '!') { // JUMP LHS
					
					from = new Jump(fromVal.substring(1));
					
				} else if (fromVal.length() == 7 && fromVal.startsWith("alu.op")) { // OPERANDS
					
					from = new Register(fromVal.charAt(2) - '0');
					
				} else if (fromVal.equals("screen.out")) { // SCREEN OUTPUT
					
					from = new Custom(0x020000A0);
					
				} else if (fromVal.charAt(0) == '$') {
					 
					from = new Memory(fromVal.substring(1));
					
				} else {
					
					parseError("unknown  left hand side: \"" + fromVal + "\"");
					from = null;
					
				}
				
				if (toVal.equals("jmp.always")) {
					
					to = new Custom(0x01000001);
					
				} else if (toVal.equals("jmp.zero")) {
					
					to = new Custom(0x01000002);
					
				} else if (toVal.equals("screen.out")) {
					
					to = new Custom(0xA0000000);
					
				} else if (toVal.length() == 7 && toVal.startsWith("alu.op")) { // OPERANDS
					
					to = new Register(toVal.charAt(2) - '0');
					
				} else {
					
					parseError("unknown right hand side: \"" + toVal + "\"");
					to = null;
					
				}
				
				if (from != null && to != null) {
					Line lineElement = new Line(from, to);
					
					tree.add(lineElement);
				}
			} else {
				parseError("line does not contain a comma");
			}
		}
	}
	
	public void assemble(FileInputStream f, DataOutputStream out) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(f));
		int offset = 0;
		
		while (r.ready()) {
			lineNumber++;
			
			String line = r.readLine().trim();
			
			int commentIndex = line.indexOf("%");
			
			if (commentIndex != -1) {
				line = line.substring(0, commentIndex).trim();
			}
			
			parseLine(line);
		}
		
		
	}
}

class Simulator {
	private byte[] ram;
	private DataInputStream ramStream;
	private int[] registers;
	private int contition;
	
	public Simulator() {
		ram = new byte[40 * 1024];
		registers = new int[8];
		ramStream = new DataInputStream(new ByteArrayInputStream(ram));
	}
	
	public void runtime(InputStream f) throws IOException {
		int offset = 0;
		int read;
		
		while((read = f.read()) != -1) {
			ram[offset++] = (byte)read;
		}
		
		DataBus bus = new DataBus();
		
		while(ramStream.available() > 0) {
			int from = ramStream.readInt();
			int to = ramStream.readInt();
			
			get(from, bus);
			put(to, bus);
			
			bus.reset();
			
			try { Thread.sleep(100); } catch (InterruptedException e) { }
		}
	}
	
	private void get(int address, DataBus bus) {
		int mod = (address & 0xff000000) >>> 24;
		int sub = (address & 0x00ffffff);
		
		if(mod == 0x00) { // CONSTANT
			bus.put(sub);
		} else if (mod == 0x02) {
			if ((sub & ~0x7) == 0) { // REGISTER
				int register = sub & 0x07;
				bus.put(registers[register]);
			} else if (sub == 0xa0) { // SUM
				int opA = registers[0];
				int opB = registers[1];
				
				bus.put(opA + opB);
			} else if (sub == 0xa1) { // DIFF
				int opA = registers[0];
				int opB = registers[1];
				
				bus.put(opA - opB);
			} else {
				System.err.printf("GET: undef sys %#08x\n", sub);
				System.exit(1);
			}
		} else {
			System.err.printf("GET: undef %#08x (mod=%#02x)\n", address, mod);
			System.exit(1);
		}
	}
	
	private void put(int address, DataBus bus) {
		int mod = (address & 0xff000000) >>> 24;
		int sub = (address & 0x00ffffff);
		
		if (mod == 0xA0 && sub == 0) { // SCREEN.OUT
			if (bus.intValueSpecified) {
				System.out.println(bus.asInt());
			}
		} else if (mod == 0x01) {
			if (sub == 0) {
				System.exit(bus.intValue);
			} else if (sub == 0x01) {
				try {
					ramStream.reset();
					ramStream.skip(bus.asInt() % ram.length);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				System.err.printf("PUT: (sys) undef %#8x\n", address);
				System.exit(1);
			}
		} else if (mod == 0x02) {
			if ((sub & ~0x7) == 0) {
				registers[sub & 0x7] = bus.asInt();
			} else {
				System.err.printf("PUT: undef sys %#8x\n", sub);
				System.exit(1);
			}
		} else {
			System.err.printf("PUT: undef %#8x (%#x, %#x)\n", address, mod, sub);
			System.exit(1);
		}
	}
}

public class Main {
	private Main() { }
	
	private static void usage() {
		System.err.println("usage: ./oisc { --assemble | --simulate } path/to/file");
		System.exit(1);
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			usage();
			return;
		}
		
		boolean assembling = false;
		
		if (args[0].equals("--assemble")) {
			assembling = true;
		} else if (args[0].equals("--simulate")) {
			assembling = false;
		} else {
			usage();
			return;
		}
		
		FileInputStream fis;
		DataOutputStream out;
		
		if (!assembling) {
			args[1] += ".raw";
		}
		
		try {
			fis = new FileInputStream(args[1]);
		} catch(IOException e) {
			System.out.println("cannot open file: " + e.getMessage());
			return;
		}
		
		if (assembling) {
			try {
				out = new DataOutputStream(new FileOutputStream(args[1] + ".raw"));
			} catch(IOException e) {
				System.out.println("cannot open output file for writing: " + e.getMessage());
				return;
			}
			
			new Assembler().assemble(fis, out);
		} else {
			new Simulator().runtime(fis);
		}
	}
}