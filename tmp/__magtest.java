// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import magarathea.*;

public class __magtest extends JITMemorySegment
{
    public __magtest()
    {
    }

    public void evaluate(int i)
    {
        ALU alu = new ALU();
        Sys sys = new Sys();
        Memory memory = new Memory(this);
        IO io = new IO(this);
        switch(i)
        {
			default:
			case 1:
				alu.operand = 4;
			case 2:
				alu.add(5);
			case 3:
				sys.shutdown(alu.result);
        }
    }
}
