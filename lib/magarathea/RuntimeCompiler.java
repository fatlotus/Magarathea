package magarathea;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RuntimeCompiler {
	private static class JITClassLoader extends ClassLoader {
		private ArrayList<byte[]> constantPool;
		private DataOutputStream bytecode;
		private ByteArrayOutputStream bytecodeBuffer;
		private HashMap<String,Integer> fields;
		private int fieldIndex = 2; // #1 is reserved for memory.
		
		public static String encodeObjectType(Class type) {
			return "L" + type.getName().replaceAll("\\.", "/") + ";";
		}
		
		public static String encodeIntType()  { return "I"; }
		public static String encodeLongType() { return "J"; }
		public static String encodeVoidType() { return "V"; }
		public static String encodeMethodType(String output, String args) { return "(" + args + ")" + output; }
		
		public String getClassName() { return "__magjit"; }
		public boolean isRunningJIT() { return true; }
		
		public int getSizeOfBytecode() { return bytecodeBuffer.size(); }
		
		public JITClassLoader() {
			constantPool = new ArrayList<byte[]>();
			bytecodeBuffer = new ByteArrayOutputStream();
			bytecode = new DataOutputStream(bytecodeBuffer);
			fields = new HashMap<String,Integer>();
		}
		
		public int addLocal(String name) {
			if (fields.containsKey(name)) {
				return fields.get(name);
			} else {
				int index = fieldIndex++;
				
				fields.put(name, index);
				
				return index;
			}
		}
		
		public void emitNull() {
			try {
				bytecode.writeByte(0x01);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitTableSwitch(int defaultAddr, ArrayList<Integer> values, int offsetOffset) {
			try {
				int startOffset = bytecodeBuffer.size();
				int nulls = 0;
				
				bytecode.writeByte(0xaa);
				while (bytecodeBuffer.size() % 4 != 0) { bytecode.writeByte(0); nulls++; }
				
				if (values.size() == 0) {
					bytecode.writeInt(0);
				} else {
					bytecode.writeInt(12 + 4 * values.size() + nulls + 1); // startOffset);
				}
				bytecode.writeInt(0);
				bytecode.writeInt(values.size() - 1);
				// bytecode.writeInt(42); // startOffset);
				
				
				for (int offset : values) {
					// System.err.println("TAB: " + (offset + offsetOffset));
					bytecode.writeInt(12 + 4 * values.size() + nulls + 1 + offset);
					// bytecode.writeInt(offset + offsetOffset);
				}
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitArrayAccess() {
			try {
				bytecode.writeByte(0x33);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitArrayPut() {
			try {
				bytecode.writeByte(0x54);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitNewArray(int componentType, int length) {
			try {
				emitConstantInt(length);
				
				bytecode.writeByte(0xbc);
				bytecode.writeByte((byte)componentType);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitGoto(int address) {
			try {
				bytecode.writeByte(0xc8);
				bytecode.writeInt(address);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitGotoIf(int address) {
			try {
				bytecode.writeByte(0x99);
				bytecode.writeShort((short)address);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitNewObject(String className) {
			try {
				short classIndex = constantizeClass(className);
				short constructorIndex = constantizeMethod(classIndex, "<init>", "()V");
				bytecode.writeByte(0xbb);
				bytecode.writeShort(classIndex);
				
				bytecode.writeByte(0x59);
				
				bytecode.writeByte(0xb7);
				bytecode.writeShort(constructorIndex);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitNewObjectSpec(String className, String argumentsSpecifier) {
			try {
				short classIndex = constantizeClass(className);
				short constructorIndex = constantizeMethod(classIndex, "<init>", argumentsSpecifier);
				bytecode.writeByte(0xbb);
				bytecode.writeShort(classIndex);
				
				bytecode.writeByte(0x59);
				
				emitLocal(0);
				
				bytecode.writeByte(0xb7);
				bytecode.writeShort(constructorIndex);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitConstantInt(int value) {
			try {
				short index = constantizeInt(value);
				bytecode.writeByte(0x13);
				bytecode.writeShort(index);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitConstant(String value) {
			try {
				short stringIndex = constantizeString(value);
				
				bytecode.writeByte(0x13);
				bytecode.writeShort(stringIndex);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitMethodCall(String className, String method, String declaration) {
			try {
				short classIndex = constantizeClass(className);
				short methodIndex = constantizeMethod(classIndex, method, declaration);
				
				bytecode.writeByte(0xb6);
				bytecode.writeShort(methodIndex);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitFieldAccess(boolean isstatic, String className, String field, String declaration) {
			try {
				short classIndex = constantizeClass(className);
				short methodIndex = constantizeField(classIndex, field, declaration);
				
				bytecode.writeByte(isstatic ? 0xb2 : 0xb4);
				bytecode.writeShort(methodIndex);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitFieldPut(boolean isstatic, String className, String field, String declaration) {
			try {
				short classIndex = constantizeClass(className);
				short methodIndex = constantizeField(classIndex, field, declaration);
				
				bytecode.writeByte(isstatic ? 0xb3 : 0xb5);
				bytecode.writeShort(methodIndex);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitStoreLocal(int number) {
			try {
				bytecode.writeByte(0x3a);
				bytecode.writeByte(number);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitLocal(int number) {
			try {
				bytecode.writeByte(0x19);
				bytecode.writeByte(number);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitStoreIntegerLocal(int number) {
			try {
				bytecode.writeByte(0x36);
				bytecode.writeByte(number);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitIntegerLocal(int number) {
			try {
				bytecode.writeByte(0x15);
				bytecode.writeByte(number);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitSwap() {
			try {
				bytecode.writeByte(0x5f);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public void emitBitwiseAnd() {
			try {
				bytecode.writeByte(0x7e);
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public short constantize(String value) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				
				dos.writeByte(0x01);
				dos.writeUTF(value);
				
				constantPool.add(baos.toByteArray());
				return (short)constantPool.size();
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public short constantizeInt(int value) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				
				dos.writeByte(0x03);
				dos.writeInt(value);
				
				constantPool.add(baos.toByteArray());
				return (short)constantPool.size();
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public short constantizeString(String value) {
			try {
				short stringIndex = constantize(value);
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				
				dos.writeByte(0x08);
				dos.writeShort(stringIndex);
				
				constantPool.add(baos.toByteArray());
				return (short)constantPool.size();
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public short constantizeClass(String className) {
			try {
				String binaryName = className.replaceAll("\\.", "/");
				
				short nameIndex = constantize(binaryName);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				
				dos.writeByte(0x07);
				dos.writeShort(nameIndex);
				
				constantPool.add(baos.toByteArray());
				return (short)constantPool.size();
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public short constantizeNameAndType(String name, String type) {
			try {
				short nameIndex = constantize(name);
				short typeIndex = constantize(type);
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
			
				dos.writeByte(12);
				dos.writeShort(nameIndex);
				dos.writeShort(typeIndex);
				
				constantPool.add(baos.toByteArray());
				return (short)constantPool.size();
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public short constantizeMethod(short classIndex, String methodName, String type) {
			try {
				short nameAndTypeIndex = constantizeNameAndType(methodName, type);
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
			
				dos.writeByte(10);
				dos.writeShort(classIndex);
				dos.writeShort(nameAndTypeIndex);
			
				constantPool.add(baos.toByteArray());
				return (short)constantPool.size();
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		public short constantizeField(short classIndex, String fieldName, String type) {
			try {
				short nameAndTypeIndex = constantizeNameAndType(fieldName, type);
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				
				dos.writeByte(9);
				dos.writeShort(classIndex);
				dos.writeShort(nameAndTypeIndex);
				
				constantPool.add(baos.toByteArray());
				return (short)constantPool.size();
			} catch (IOException e) { throw new RuntimeException(e); }
		}
		
		protected byte[] createBytecode() {
			try {
				short __magtest = constantizeClass(getClassName());
				short _magarathea_JITMemorySegment = constantizeClass("magarathea/JITMemorySegment");
				
				short _magarathea_JITMemorySegment__init_ = constantizeMethod(
					_magarathea_JITMemorySegment, "<init>", "()V");
				
				short _evaluate_ = constantize("evaluate");
				short _init_ = constantize("<init>");
				short _void_ = constantize("()V");
				short _int_void_ = constantize("(I)V");
				short _code_ = constantize("Code");
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				FileOutputStream fos = new FileOutputStream("tmp/__magjit.class");
				
				DataOutputStream dos;
				
				if (isRunningJIT()) { // if we're disassembling, output data to
				                      // file rather than load into JVM.
					dos = new DataOutputStream(baos);
				} else {
					dos = new DataOutputStream(fos);
				}
				
				dos.writeInt(0xCAFEBABE);

				dos.writeShort(0);
				dos.writeShort(0x32);
				dos.writeShort(1 + constantPool.size()); // constant pool size + 1
				
				for (int i = 0; i < constantPool.size(); i++) {
					dos.write(constantPool.get(i), 0, constantPool.get(i).length);
				}
				
				dos.writeShort(0x0001); // access flags
				dos.writeShort(__magtest); // this
				dos.writeShort(_magarathea_JITMemorySegment); // super
				dos.writeShort(0);
				// interfaces
				
				dos.writeShort(0); // fields.size());
				// fields
				
				dos.writeShort(2);
				// methods
				
				dos.writeShort(0x0001); // access flags
				dos.writeShort(_init_); // name 
				dos.writeShort(_void_); // descriptor
				dos.writeShort(1); // attributes
				// method attributes
				
				dos.writeShort(_code_);
				dos.writeInt(17);
				dos.writeShort(255);
				dos.writeShort(1);
				dos.writeInt(5);
				// method body
				dos.writeByte(0x2a); // aload_0
				dos.writeByte(0xb7); // invokespecial <init> super
				dos.writeShort(_magarathea_JITMemorySegment__init_);
				
				dos.writeByte(0xb1); // return;
				
				dos.writeShort(0); // exceptions
				dos.writeShort(0); // attributes
				
				// method - evaluate()V;
				dos.writeShort(0x0001);
				dos.writeShort(_evaluate_);
				dos.writeShort(_int_void_);
				dos.writeShort(1);
				
				dos.writeShort(_code_);
				dos.writeInt(13 + bytecodeBuffer.size());
				dos.writeShort(255);
				dos.writeShort(2 + fieldIndex);
				dos.writeInt(1 + bytecodeBuffer.size());
				
				dos.write(bytecodeBuffer.toByteArray());
				
				dos.writeByte(0xb1);
				
				dos.writeShort(0); // exceptions
				dos.writeShort(0); // attributes
				
				dos.writeShort(0); // attributes on overall class
				
				dos.close();
				
				return baos.toByteArray();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		public JITMemorySegment defineBytecode() {
			byte[] entireClassBytecode = createBytecode();
			
			if (isRunningJIT()) {
				Class c = defineClass("__magjit", entireClassBytecode, 0, entireClassBytecode.length);
				
				try {
					return (JITMemorySegment)c.newInstance();
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
			} else {
				return null;
			}
		}
	}
	
	private static final void writeLHS(JITClassLoader jit, OpcodeCollection collect, int even, int odd) {
		if ((even & 0xff000000) == 0x11000000) {
			if (odd != 0x90000000)
				jit.emitConstantInt(even & 0x00ffffff);
			
		} else {
			OpcodeCollection.Output lhs = collect.getLeftHandSide(even);
			
			if (lhs.isField()) {
				Field field = lhs.field;
				Class klass = field.getDeclaringClass();
				
				int localVariable = jit.addLocal(klass.getName());
				String fieldType = JITClassLoader.encodeIntType();
				
				jit.emitLocal(localVariable);
				jit.emitFieldAccess(false, klass.getName(), field.getName(), fieldType);
				
			} else if (lhs.isMethod()) {
				Method method = lhs.method;
				Class klass = method.getDeclaringClass();
				
				int localVariable = jit.addLocal(klass.getName());
				String fieldType = JITClassLoader.encodeObjectType(klass);
				String methodType = JITClassLoader.encodeMethodType(JITClassLoader.encodeIntType(), "");
				
				jit.emitLocal(localVariable);
				jit.emitMethodCall(klass.getName(), method.getName(), methodType);
			}
		}
	}
	
	private static final void writeRHS(JITClassLoader jit, OpcodeCollection collect,
	  int even, int odd, ArrayList<Integer> extents, int start, int end, int jumpOffset) {
		
		if (odd == 0x90000000) { // jmp.branch
			if ((even & 0xff000000) != 0x11000000 || (start % 8 != (even % 8))) {
				throw new RuntimeException("dynamic jumps not yet supported!");
			}
			
			jit.emitIntegerLocal(2);
			
			if (extents == null) {
				jit.emitGotoIf(0);
			} else {
				jit.emitGotoIf(extents.get((even & 0x00ffffff - start) / 8) + jumpOffset - jit.getSizeOfBytecode());
			}
		} else if (odd == 0x90000001) { // jmp.nonneg
			jit.emitConstantInt(1 << 31);
			jit.emitBitwiseAnd();
			
			jit.emitStoreIntegerLocal(2);
		} else if (odd == 0x90000002) { // jmp.zero
			jit.emitStoreIntegerLocal(2);
		} else {
			OpcodeCollection.Output rhs = collect.getRightHandSide(odd);
	
			if (rhs.isField()) {
				Field field = rhs.field;
				Class klass = field.getDeclaringClass();
				
				int localVariable = jit.addLocal(klass.getName());
				String fieldType = JITClassLoader.encodeIntType();
		
				jit.emitLocal(localVariable);
				jit.emitSwap();
				jit.emitFieldPut(false, klass.getName(), field.getName(), fieldType);
		
			} else if (rhs.isMethod()) {
				Method method = rhs.method;
				Class klass = method.getDeclaringClass();
		
				int localVariable = jit.addLocal(klass.getName());
				String fieldType = JITClassLoader.encodeObjectType(klass);
				String methodType = JITClassLoader.encodeMethodType(
					JITClassLoader.encodeVoidType(), JITClassLoader.encodeIntType());
		
				jit.emitLocal(localVariable);
				jit.emitSwap();
				jit.emitMethodCall(klass.getName(), method.getName(), methodType);
			}
		}
	}
	
	public static JITMemorySegment recompile(OpcodeCollection collect, byte[] code, int start, int end) {
		JITClassLoader jit = new JITClassLoader();
		JITClassLoader dryRun = new JITClassLoader(); // used only to generate extents.
		
		jit.addLocal("");
		jit.addLocal("__JMP");
		
		jit.emitConstantInt(0);
		jit.emitStoreIntegerLocal(2);
		
		for (Class klass : collect.getModules()) {
			if (klass != null) {
				int localVariable = jit.addLocal(klass.getName());
				
				boolean hasConstructorSpec = false;
				
				try {
					klass.getConstructor(new Class[] { JITMemorySegment.class });
					
					hasConstructorSpec = true;
				} catch (NoSuchMethodException e) { }
				
				if (hasConstructorSpec) {
					jit.emitNewObjectSpec(klass.getName(), "(Lmagarathea/JITMemorySegment;)V");
					dryRun.emitNewObjectSpec(klass.getName(), "(Lmagarathea/JITMemorySegment;)V");
				} else {
					jit.emitNewObject(klass.getName());
					dryRun.emitNewObject(klass.getName());
				}
				jit.emitStoreLocal(localVariable);
				dryRun.emitStoreLocal(localVariable);
			}
		}
		
		try {
			/* generate jump extents */
			ArrayList<Integer> extents = new ArrayList<Integer>();
			
			DataInput in = new DataInputStream (new ByteArrayInputStream (code, start, end));
			
			dryRun.emitIntegerLocal(1);
			dryRun.emitTableSwitch(0, extents, 0);
			
			int bytesAfterSwitch = dryRun.getSizeOfBytecode();
			
			for (int i = start; i < end; i += 8) {
				extents.add(dryRun.getSizeOfBytecode() - bytesAfterSwitch);
				
				int even = in.readInt();
				int odd = in.readInt();
				
				try {
					writeLHS(dryRun, collect, even, odd);
					writeRHS(dryRun, collect, even, odd, null, start, end, 0);
				} catch (NullPointerException e) { break; }
			}
			
			in = new DataInputStream (new ByteArrayInputStream (code, start, end));
			
			jit.emitIntegerLocal(1);
			jit.emitTableSwitch(0, extents, 0);
			
			int jumpOffsetOffset = jit.getSizeOfBytecode();
			
			for (int i = start; i < end; i += 8) {
				int even = in.readInt();
				int odd = in.readInt();
				
				try {
					writeLHS(jit, collect, even, odd);
					writeRHS(jit, collect, even, odd, extents, start, end, jumpOffsetOffset);
				} catch (NullPointerException e) { break; }
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return jit.defineBytecode();
	}
}