package magarathea;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.io.UnsupportedEncodingException;

import magarathea.peripherals.Keyboard;

public class Runner {
	private static class MainWindow extends JFrame {
		byte[] value;
		JTextArea output;
		int caretPosition;
		
		class MemoryDocument extends PlainDocument implements MemoryListener {
			private Computer computer;
			
			public MemoryDocument(Computer c) {
				computer = c;
				
				for (int i = 0; i < getLength(); i++) {
					try {
						insertString(i, " ", new SimpleAttributeSet());
					} catch (BadLocationException e) {
						throw new RuntimeException(e);
					}
				}
				
				c.addMemoryListener(this);
			}
			
			public void segmentWrittenTo(Computer c, int offset, int value) {
				int length = offset - 0x8000;
				
				if (length >= 0 && length < getLength()) {
					String stringValue = new String(new char[] {
						(char)((value >>> 16) & 0xff),
						(char)((value >>> 8)  & 0xff),
						(char)((value) & 0xff)
					});
					
					try {
						replace(length, 3, stringValue, new SimpleAttributeSet());
					} catch (BadLocationException e) {
						throw new RuntimeException(e);
					}
				} else if (offset == 0x8781) {
					caretPosition = value;
					
					output.setCaretPosition(caretPosition);
				}
			}
			
			public int getLength() { return 80*24; }
		}
		
		public MainWindow(StaticComputer c) {
			caretPosition = 0;
			
			setResizable(false);
			
			output = new JTextArea(new MemoryDocument(c));
			output.setFont(new Font("monospaced", Font.PLAIN, 13));
			output.setMargin(new Insets(5, 5, 5, 5));
			/* output.setEditable(false); */
			output.setLineWrap(true);
			output.setRows(24);
			output.setColumns(80);
			output.setNavigationFilter(new NavigationFilter() {
				public void moveDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
					fb.setDot(caretPosition, bias);
				}
				
				public void setDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
					fb.setDot(caretPosition, bias);
				}
			});
			
			output.getCaret().setBlinkRate(0);
			
			final Keyboard kbd = new Keyboard(256);
			output.addKeyListener(new KeyListener() {
				public void keyPressed(KeyEvent e) {
					e.consume();
					kbd.emitKeyEvent((byte)1, (byte)e.getKeyCode());
				}
				
				public void keyReleased(KeyEvent e) {
					e.consume();
					kbd.emitKeyEvent((byte)1, (byte)e.getKeyCode());
				}
				
				public void keyTyped(KeyEvent e) { e.consume(); }
			});
			c.addPeripheral(0, kbd);
			
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			add(output);
			
			pack();
		}
		
		public void write(String message) {
			output.append(message);
		}
	}
	
	private static void testBinary(int val) {
		System.err.println("V: " + val + " is negative: " + (val & (1 << 31)));
	}
	
	public static void main(String[] args) {
		testBinary(2);
		testBinary(1);
		testBinary(0);
		testBinary(-1);
		testBinary(-2);
		testBinary(1 << 31);
		
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Magarathea");
	
		StaticComputer c = new StaticComputer();
		
		MainWindow win = new MainWindow(c);
		win.setLocationRelativeTo(null);
		win.setVisible(true);
		
		c.execute();
	}
}