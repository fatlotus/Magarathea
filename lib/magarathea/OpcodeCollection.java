package magarathea;

import java.util.HashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.AccessibleObject;

import magarathea.ALU;
import magarathea.Sys;
import magarathea.anno.*;

class OpcodeCollection {
	private Class[] modules;
	private SegmentTable[] reads;
	private SegmentTable[] writes;
	private HashMap<String,Output> instructionsRead;
	private HashMap<String,Output> instructionsWrite;
	
	public class Output {
		public Output(AccessibleObject obj, String desc, int code) { // FIXME.
			this(obj instanceof Method ? (Method)obj : null,
			     obj instanceof Field  ? (Field)obj : null,
			     desc,
			     code);
		}
		
		private Output(Method met, Field f, String desc, int code) {
			method = met;
			descriptor = desc;
			field = f;
			opcode = code;
		}
		
		public Method method;
		public Field field;
		public String descriptor;
		public int opcode;
		
		public boolean isField() {
			return (field != null);
		}
		
		public boolean isMethod() {
			return (method != null);
		}
	}
	
	private static class SegmentTable {
		public Output fallback;
		public HashMap<Integer, Output> output;
		
		public SegmentTable() {
			output = new HashMap<Integer, Output>();
		}
		
		public void print() {
			System.err.println("---");
			for (int key : output.keySet()) {
				System.err.printf("%08x : %s\n", key, output.get(key));
			}
		}
		
		public void put(int index, Output value) {
			index = index & 0xffffff;
			
			if (output.containsKey(index)) {
				throw new OperationException("duplicate entry in segment table: " + index);
			} else {
				output.put(index, value);
			}
		}
		
		public boolean containsKey(int index) {
			return output.containsKey(index);
		}
		
		public Output get(int index) {
			if (output.containsKey(index)) {
				return output.get(index);
			} else if (fallback != null) {
				return fallback;
			} else {
				return null;
			}
		}
		
		public void putFallback(Output value) {
			if (fallback != null) {
				throw new OperationException("duplicate fallback entry");
			} else {
				fallback = value;
			}
		}
	}
	
	public static class OperationException extends RuntimeException {
		public OperationException(String message) {
			super(message);
		}
	}
	
	private OpcodeCollection() {
		modules = new Class[256];
		reads = new SegmentTable[256];
		writes = new SegmentTable[256];
		instructionsRead = new HashMap<String,Output>();
		instructionsWrite = new HashMap<String,Output>();
		
		registerBuiltins();
	}
	
	private static OpcodeCollection instance = new OpcodeCollection();
	public static OpcodeCollection instance() { return instance; }
	
	protected void registerBuiltins() {
		registerModule(ALU.class);
		registerModule(Sys.class);
		registerModule(Memory.class);
		registerModule(IO.class);
		registerModule(Jump.class);
	}
	
	public Class[] getModules() {
		return modules;
	}
	
	public String explainOpcode(int bytecode, boolean previousWasLHS) {
		if (!previousWasLHS && ((bytecode & 0xff000000) == 0x11000000)) {
			return "#" + (bytecode & 0x00ffffff);
		}
		
		Output output;
		
		if (previousWasLHS) {
			output = getRightHandSide(bytecode);
		} else {
			output = getLeftHandSide(bytecode);
		}
		
		if (output != null) {
			return output.descriptor;
		} else {
			return null;
		}
	}
	
	public Output getLeftHandSide(int operation) {
		SegmentTable segs = reads[(operation >>> 24) & 0xff];
		
		if (segs != null) {
			return segs.get(operation & 0xffffff);
		} else {
			return null;
		}
	}
	
	public Output getRightHandSide(int operation) {
		SegmentTable segs = writes[(operation >>> 24) & 0xff];
		
		if (segs != null) {
			return segs.get(operation & 0x00ffffff);
		} else {
			return null;
		}
	}
	
	public Output getLeftHandSide(String name) {
		return instructionsRead.get(name);
	}
	
	public Output getRightHandSide(String name) {
		return instructionsWrite.get(name);
	}
	
	/**
	 * Again, not idempotent, as below.
	 */
	private void registerOutput(AccessibleObject object,
	                            SegmentTable moduleReads,
	                            SegmentTable moduleWrites,
	                            String prefix,
								int id) {
		
		Write w = (Write)object.getAnnotation(Write.class);
		ReadWrite rw = (ReadWrite)object.getAnnotation(ReadWrite.class);
		Read r = (Read)object.getAnnotation(Read.class);
		
		boolean hasRead = (r != null);
		boolean hasWrite = (w != null);
		boolean hasReadWrite = (rw != null);
		
		int sum = (hasRead ? 1 : 0) + (hasWrite ? 1 : 0) + (hasReadWrite ? 1 : 0);
		
		if (sum == 0) {
			// ignore object
		} else if (sum > 1) {
			throw new OperationException("Described fields can have only one descriptor.");
		} else {
			boolean read = hasRead || hasReadWrite;
			boolean write = hasWrite || hasReadWrite;
			
			int position = -1;
			String label = prefix;
			
			if (hasRead)           { position = r.id() & 0xffffff | id;  label += r.name(); }
			else if (hasWrite)     { position = w.id() & 0xffffff | id;  label += w.name(); }
			else if (hasReadWrite) { position = rw.id() & 0xffffff | id; label += rw.name(); }
			else { throw new RuntimeException("assertion failed!"); }
			
			if (read) {
				Output out = new Output(object, label, position);
				
				if (moduleReads.containsKey(position)) {
					throw new OperationException("read slot already defined for 0x" +
					                             Integer.toString(position, 16) + " in " + label); // he he he
				} else {
					moduleReads.put(position, out);
				}
				
				if (instructionsRead.containsKey(label)) {
					throw new OperationException("read instruction \"" + label + "\" already defined.");
				} else {
					instructionsRead.put(label, out);
				}
			}
			
			if (write) {
				Output out = new Output(object, label, position);
				
				if (moduleWrites.containsKey(position)) {
					throw new OperationException("write slot already defined for 0x" +
					                             Integer.toString(position, 16) + " in " + label); // he he he
				} else {
					moduleWrites.put(position, out);
				}
				
				if (instructionsWrite.containsKey(label)) {
					throw new OperationException("write instruction \"" + label + "\" already defined.");
				} else {
					instructionsWrite.put(label, out);
				}
			}
		}
	}
	
	/**
	 * not idempotent in failure; will corrupt this instance.
	 */
	public void registerModule(Class<?> c) {
		Bus bus = (Bus)c.getAnnotation(Bus.class);
		int moduleID = bus.id();
		String prefix = (bus.prefix().length() == 0) ? "" : bus.prefix() + ".";
		
		if (moduleID > 0xff && moduleID < 0) {
			throw new OperationException("Invalid module id 0x" + Integer.toString(bus.id(), 16));
		} else if (modules[moduleID] != null) {
			throw new OperationException("Duplicate entry " + bus.id());
		}
		
		SegmentTable moduleReads = new SegmentTable();
		SegmentTable moduleWrites = new SegmentTable();
		
		for (Method method : c.getMethods()) {
			registerOutput(method, moduleReads, moduleWrites, prefix, moduleID << 24);
		}
		
		for (Field field : c.getDeclaredFields()) {
			registerOutput(field, moduleReads, moduleWrites, prefix, moduleID << 24);
		}
		
		modules[moduleID] = c;
		reads[moduleID] = moduleReads;
		writes[moduleID] = moduleWrites;
	}
}