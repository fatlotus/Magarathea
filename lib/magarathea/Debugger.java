package magarathea;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import java.awt.Font;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Set;
import java.util.HashSet;
import java.util.Formatter;

public class Debugger {
	Computer computer;
	
	private class LazyListModel implements ListModel, MemoryListener {
		Set<ListDataListener> listeners;
		OpcodeCollection opcodes;
		
		public LazyListModel() {
			listeners = new HashSet<ListDataListener>();
			opcodes = OpcodeCollection.instance();
		}
		
		public Object getElementAt(int index) {
			int integerValue = computer.readFromRAM(4 * index);
			long longValue = (integerValue & 0x0fffffff) | ((long)((integerValue & 0xf0000000) >>> 24) << 24);
			String description = opcodes.explainOpcode(integerValue, (index % 2 == 1));
			
			if (index % 2 == 0 && description != null) {
				description += " =>";
			}
			
			if (description == null)
				description = "";
			
			
			Formatter f = new Formatter();
			f.format("%08x %s", longValue, description);
			
			return f.toString();
		}
		
		public int getSize() {
			return computer.getLengthOfRAM() / 4;
		}
		
		public void removeListDataListener(ListDataListener l) {
			listeners.remove(l);
		}
		
		public void addListDataListener(ListDataListener l) {
			listeners.add(l);
		}
		
		public void segmentWrittenTo(Computer c, int offset, int value) {
			ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, offset, offset);
			
			for (ListDataListener l : listeners) {
				l.contentsChanged(evt);
			}
		}
	}
	
	private class HexRenderer extends JPanel implements ListCellRenderer {
		JLabel column0, column1, column2, column3, column4;
		JLabel description;
		JPanel labelPanel;
		Color selectedColor;
		Color notSelectedColor;
		
		public HexRenderer() {
			JPanel leftSide = new JPanel();
			leftSide.setOpaque(false);
			
			labelPanel = new JPanel();
			labelPanel.setLayout(new GridLayout(1, 4, 10, 10));
			labelPanel.setOpaque(false);
			
			description = new JLabel("Hello, world!");
			description.setFont(new Font("Monaco", Font.PLAIN, 14));
			description.setForeground(new Color(100, 100, 100));
			
			column0 = new JLabel();
			column0.setForeground(new Color(150, 150, 150));
			column0.setFont(new Font("Monaco", Font.PLAIN, 14));
			
			column1 = makeAndAddLabel();
			column2 = makeAndAddLabel();
			column3 = makeAndAddLabel();
			column4 = makeAndAddLabel();
			
			selectedColor = new Color(50, 50, 50);
			notSelectedColor = new Color(20, 20, 20);
			
			setLayout(new BorderLayout());
			
			leftSide.add(column0);
			leftSide.add(labelPanel);
			
			add(leftSide, BorderLayout.WEST);
			add(description);
			
			setOpaque(true);
		}
		
		protected JLabel makeAndAddLabel() {
			JLabel label = new JLabel();
			label.setFont(new Font("Monaco", Font.PLAIN, 14));
			label.setForeground(new Color(230, 230, 230));
			
			labelPanel.add(label);
			
			return label;
		}
		
		public HexRenderer getListCellRendererComponent(
		  JList lst, Object value, int index, boolean isSelected, boolean isFocused) {
			column0.setText(Integer.toString(index * 4, 16) + "  ");
			column1.setText(value.toString().substring(0, 2));
			column2.setText(value.toString().substring(2, 4));
			column3.setText(value.toString().substring(4, 6));
			column4.setText(value.toString().substring(6, 8));
			
			description.setText(value.toString().substring(9));
			
			if (isSelected) {
				setBackground(selectedColor);
			} else {
				setBackground(notSelectedColor);
			}
			
			return this;
		}
	}
	
	private class DebuggerFrame extends JFrame {
		JList instructions;
		JButton stepButton;
		JButton continueButton;
		
		DebuggerFrame() {
			LazyListModel model = new LazyListModel();
			instructions = new JList(model);
			instructions.setPrototypeCellValue("ffffffff alu.operandXXXXXX");
			instructions.setCellRenderer(new HexRenderer());
			
			JScrollPane wrapper = new JScrollPane(instructions);
			wrapper.setBorder(null);
		
			add(wrapper);
			
			JPanel buttonsPanel = new JPanel();
			buttonsPanel.setLayout(new GridLayout(1, 3, 5, 5));
			
			stepButton = new JButton("Step");
			stepButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					computer.step();
					instructions.requestFocusInWindow();
				}
			});
			
			continueButton = new JButton("Continue");
			continueButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					computer.start();
					instructions.requestFocusInWindow();
				}
			});
			
			buttonsPanel.add(stepButton);
			buttonsPanel.add(continueButton);
			
			computer.addExecutionListener(new ExecutionListener() {
				public void programCounterChanged(Computer c) {
					if (c.getProgramCounter() == 0) {
						c.stop();
					}
					
					if (!c.isRunning()) {
						instructions.ensureIndexIsVisible(c.getProgramCounter() / 4);
						instructions.setSelectedIndex(c.getProgramCounter() / 4);
					}
				}
				
				public void executionStatusChanged(Computer c) {
					if (c.isRunning()) {
						stepButton.setEnabled(false);
						continueButton.setEnabled(false);
					} else {
						stepButton.setEnabled(true);
						continueButton.setEnabled(true);
					}
				}
			});
			
			computer.addMemoryListener(model);
			
			add(buttonsPanel, BorderLayout.NORTH);
			
			
			pack();
			
			setLocation(50, 200);
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