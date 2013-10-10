package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.codehaus.jackson.JsonProcessingException;

public class MainFrame extends JFrame implements ActionListener, ModelChangeListener {
	
	private static final long serialVersionUID = 4102508633217679875L;

	private static final String SAVE_TRIGGERS = "Save triggers";
	private static final String LOAD_TRIGGERS = "Load triggers";
	private Logic logic;
	private ImageGrid imageGrid;
	private HashMap<String, JLabel> actualValueLabels = new HashMap<String, JLabel>();
	private Trigger selectedTrigger;
	private boolean paused = true;	
	
	final JFileChooser fc = new JFileChooser();

	private HashMap<String, JTextField> minThresholdEdits = new HashMap<String, JTextField>();
	private HashMap<String, JTextField> maxThresholdEdits = new HashMap<String, JTextField>();
	
	public MainFrame() {
		super("Tram watch v0.1");
		this.logic = new Logic();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		JPanel triggerPanel = createTriggerPanel();
		contentPane.add(triggerPanel,BorderLayout.SOUTH);
		
		imageGrid = new ImageGrid();
		imageGrid.setLogic(logic);
		contentPane.add(imageGrid, BorderLayout.CENTER);
		
		final JButton button = new JButton("Resume");
		contentPane.add(button, BorderLayout.WEST);
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (paused) {
					button.setText("Pause");
					paused = false;
				} else {
					button.setText("Resume");
					paused = true;
				}
			}
		});
		// Menu bar
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem saveTriggers = new JMenuItem("Save triggers");
		saveTriggers.setActionCommand(SAVE_TRIGGERS);
		saveTriggers.addActionListener(this);
		JMenuItem loadTriggers = new JMenuItem("Load triggers");
		loadTriggers.setActionCommand(LOAD_TRIGGERS);
		loadTriggers.addActionListener(this);
		fileMenu.add(saveTriggers);
		fileMenu.add(loadTriggers);
		menuBar.add(fileMenu);
		setJMenuBar(menuBar);
		
		pack();
		setVisible(true);
		new Thread(new Runnable() {


			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (!paused) {
						MainFrame.this.logic.loadImage();
						MainFrame.this.logic.calculateDifference();
						MainFrame.this.logic.logTriggers();
					}
					MainFrame.this.logic.refreshTriggers();
					invalidate();
					repaint();
				}
			}
		}).start();
		
		selectedTrigger = logic.getTriggers().get(0);
		imageGrid.setSelectedTrigger(selectedTrigger);
		logic.registerListener(this);
	}

	private JPanel createTriggerPanel() {
		JPanel triggerPanel = new JPanel();
		List<Trigger> triggers = logic.getTriggers();
		triggerPanel.setLayout(new GridLayout(5, triggers.size() + 1));
		triggerPanel.add(new JLabel("Triggers"));
		ButtonGroup buttonGroup = new ButtonGroup();
		boolean first = true;
		for (Trigger trigger : triggers) {
			JRadioButton radioButton = new JRadioButton(trigger.getName());
			if (first) {
				first = false;
				radioButton.setSelected(true);
			}
			buttonGroup.add(radioButton);
			radioButton.setActionCommand(trigger.getName());
			radioButton.addActionListener(this);
			triggerPanel.add(radioButton);
		}
		triggerPanel.add(new JLabel("Max"));
		for (Trigger trigger : triggers) {
			final Trigger localTrigger = trigger;
			final JTextField maxThresholdEdit = new JTextField(10);
			maxThresholdEdits.put(localTrigger.getName(), maxThresholdEdit);
			maxThresholdEdit.setText("" + localTrigger.getMaxThreshold());
			maxThresholdEdit.getDocument().addDocumentListener(new DocumentListener() {
				@Override public void removeUpdate(DocumentEvent e) { update();}
				@Override public void insertUpdate(DocumentEvent e) {update();}
				@Override public void changedUpdate(DocumentEvent e) {update();}
				private void update() {
					try {
						int newMax = Integer.parseInt(maxThresholdEdit.getText());
						if (localTrigger.getMaxThreshold() != newMax) {
							localTrigger.setMaxThreshold(newMax);
						}
					} catch (Exception ex) {
					}
				}
			});
			triggerPanel.add(maxThresholdEdit);
		}
		triggerPanel.add(new JLabel("Min"));
		for (Trigger trigger : triggers) {
			final Trigger localTrigger = trigger;
			final JTextField minThresholdEdit = new JTextField(10);
			minThresholdEdits.put(localTrigger.getName(), minThresholdEdit);
			minThresholdEdit.setText("" + localTrigger.getMinThreshold());
			minThresholdEdit.getDocument().addDocumentListener(new DocumentListener() {
				@Override public void removeUpdate(DocumentEvent e) { update();}
				@Override public void insertUpdate(DocumentEvent e) {update();}
				@Override public void changedUpdate(DocumentEvent e) {update();}
				private void update() {
					try {
						int newMin = Integer.parseInt(minThresholdEdit.getText());
						if (localTrigger.getMinThreshold() != newMin) {
							localTrigger.setMinThreshold(newMin);
						}
					} catch (Exception ex) {
					}
				}
			});
			triggerPanel.add(minThresholdEdit);
		}
		triggerPanel.add(new JLabel("Range type"));
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
		triggerPanel.add(new JLabel("Value"));
		for (Trigger trigger : triggers) {
			JLabel actualValueLabel = new JLabel("0");
			actualValueLabel.setOpaque(true);
			actualValueLabels .put(trigger.getName(), actualValueLabel);
			triggerPanel.add(actualValueLabel);
		}
		
		return triggerPanel;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(LOAD_TRIGGERS)) {
			// Show load dialog
			int returnVal = fc.showOpenDialog(this);
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fc.getSelectedFile();
	            // Save file
	            try {
					logic.loadFromFile(file);
				} catch (JsonProcessingException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
	        }
		} else if (e.getActionCommand().equals(SAVE_TRIGGERS)) {
			// Show save dialog
			int returnVal = fc.showSaveDialog(this);
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fc.getSelectedFile();
	            // Save file
	            try {
					logic.exportToFile(file);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
	        }
		} else {
			selectedTrigger = logic.getTrigger(e.getActionCommand());
			imageGrid.setSelectedTrigger(selectedTrigger);
		}
	}

	@Override
	public void modelChanged() {
		System.out.println("Model changed");
		// Refresh triggers
		List<Trigger> triggers = logic.getTriggers();
		for (Trigger trigger : triggers) {
			actualValueLabels.get(trigger.getName()).setText("" + trigger.getValue());
			if (!maxThresholdEdits.get(trigger.getName()).getText().equals(trigger.getMaxThreshold())) {
				maxThresholdEdits.get(trigger.getName()).setText("" + trigger.getMaxThreshold());
			}
			if (!minThresholdEdits.get(trigger.getName()).getText().equals(trigger.getMinThreshold())) {
				minThresholdEdits.get(trigger.getName()).setText("" + trigger.getMinThreshold());
			}
			if (trigger.isActive()) {
				actualValueLabels.get(trigger.getName()).setBackground(Color.green);
			} else {
				actualValueLabels.get(trigger.getName()).setBackground(Color.yellow);
			}
		}
	}
	public static void main(String[] args) {
		new MainFrame();
	}
}
