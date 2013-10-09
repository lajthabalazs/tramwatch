package main;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

public class MainFrame extends JFrame implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4102508633217679875L;

	private static final String SAVE_TRIGGERS = "Save triggers";
	private static final String LOAD_TRIGGERS = "Load triggers";
	private Logic logic;
	private ImageGrid imageGrid;
	private HashMap<Trigger, JLabel> actualValueLabels = new HashMap<Trigger, JLabel>();
	private HashMap<String, Trigger> triggers = new HashMap<String, Trigger>();
	private Trigger selectedTrigger;
	
	
	public MainFrame(Logic logic) {
		super("Tram watch v0.1");
		this.logic = logic;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		JPanel triggerPanel = createTriggerPanel();
		contentPane.add(triggerPanel,BorderLayout.SOUTH);
		
		imageGrid = new ImageGrid(logic);
		contentPane.add(imageGrid, BorderLayout.CENTER);
		
		// Menu bar
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem saveTriggers = new JMenuItem("Save triggers");
		saveTriggers.setActionCommand(SAVE_TRIGGERS);
		JMenuItem loadTriggers = new JMenuItem("Load triggers");
		loadTriggers.setActionCommand(LOAD_TRIGGERS);
		fileMenu.add(saveTriggers);
		fileMenu.add(loadTriggers);
		menuBar.add(fileMenu);
		setJMenuBar(menuBar);
		
		pack();
		setVisible(true);
	}
	
	private JPanel createTriggerPanel() {
		JPanel triggerPanel = new JPanel();
		List<Trigger> triggers = logic.getTriggers();
		for (Trigger trigger : triggers) {
			this.triggers.put(trigger.getName(), trigger);
		}
		triggerPanel.setLayout(new GridLayout(5, triggers.size()));
		ButtonGroup buttonGroup = new ButtonGroup();
		for (Trigger trigger : triggers) {
			JRadioButton radioButton = new JRadioButton(trigger.getName());
			buttonGroup.add(radioButton);
			radioButton.setActionCommand(trigger.getName());
			radioButton.addActionListener(this);
			triggerPanel.add(radioButton);
		}
		for (Trigger trigger : triggers) {
			final Trigger localTrigger = trigger;
			final JTextField minThresholdEdit = new JTextField(10);
			minThresholdEdit.addKeyListener(new KeyListener() {
				@Override public void keyTyped(KeyEvent e) {
					try {
						localTrigger.setMinThreshold(Integer.parseInt(minThresholdEdit.getText()));
					} catch (Exception ex) {
						System.out.println("" + ex);
					}
				}
				@Override public void keyReleased(KeyEvent e) {}
				@Override public void keyPressed(KeyEvent e) {}
			});
			triggerPanel.add(minThresholdEdit);
		}
		for (Trigger trigger : triggers) {
			final Trigger localTrigger = trigger;
			final JTextField maxThresholdEdit = new JTextField(10);
			maxThresholdEdit.addKeyListener(new KeyListener() {
				@Override public void keyTyped(KeyEvent e) {
					try {
						localTrigger.setMaxThreshold(Integer.parseInt(maxThresholdEdit.getText()));
					} catch (Exception ex) {
						System.out.println("" + ex);
					}
				}
				@Override public void keyReleased(KeyEvent e) {}
				@Override public void keyPressed(KeyEvent e) {}
			});
			triggerPanel.add(maxThresholdEdit);
		}
		for (Trigger trigger : triggers) {
			final Trigger localTrigger = trigger;
			final JToggleButton toggleButton = new JToggleButton(localTrigger.isExternal()?"External":"Internal");
			toggleButton.setSelected(localTrigger.isExternal());
			toggleButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					localTrigger.setExternal(toggleButton.isSelected());
					toggleButton.setText(toggleButton.isSelected()?"External":"Internal");
				}
			});
			triggerPanel.add(toggleButton);
		}
		for (Trigger trigger : triggers) {
			JLabel actualValueLabel = new JLabel("0");
			actualValueLabels .put(trigger, actualValueLabel);
			triggerPanel.add(actualValueLabel);
		}
		
		return triggerPanel;
	}
	
	public static void main(String[] args) {
		new MainFrame(new Logic());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		selectedTrigger = triggers.get(e.getActionCommand());
		imageGrid.setSelectedTrigger(selectedTrigger);
	}
}
