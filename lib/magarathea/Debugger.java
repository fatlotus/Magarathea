package magarathea;

import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Debugger {
	Computer computer;
	
	private class DebuggerFrame extends JFrame {
		DebuggerFrame() {
			setLayout(new GridLayout(3, 1, 5, 5));
			
			JButton stepButton = new JButton("Step");
			stepButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					computer.step();
				}
			});
			
			add(stepButton);
			
			pack();
		}
		
		/*
		public JComponent wrap(JComponent sub) {
			JPanel pan = new JPanel();
			pan.add(sub);
			return pan;
		}
		*/
	}
	
	public Debugger(Computer c) {
		computer = c;
	}
	
	public void runWithGUI() {
		DebuggerFrame frm = new DebuggerFrame();
		frm.setVisible(true);
		
		computer.execute();
	}
}