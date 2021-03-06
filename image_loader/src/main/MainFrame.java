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
import javax.swing.filechooser.FileFilter;

import org.codehaus.jackson.JsonProcessingException;

public class MainFrame extends JFrame implements ActionListener, ModelChangeListener {
	
	private static final long serialVersionUID = 4102508633217679875L;

	private static final String SAVE_TRIGGERS = "Save triggers";
	private static final String LOAD_TRIGGERS = "Load triggers";

	private static final String LOAD_IMAGES = "Load images";
	private Logic logic;
	private ImageGrid imageGrid;
	private HashMap<String, JLabel> actualValueLabels = new HashMap<String, JLabel>();
	private boolean paused = true;	
	
	final JFileChooser triggerFileChooser;
	final JFileChooser imageFolderChooser;

	private HashMap<String, JTextField> minThresholdEdits = new HashMap<String, JTextField>();
	private HashMap<String, JTextField> maxThresholdEdits = new HashMap<String, JTextField>();
	
	public MainFrame() {
		super("Tram watch v0.1");
		triggerFileChooser = new JFileChooser(new File("triggers"));
		triggerFileChooser.addChoosableFileFilter(new FileFilter() {
			
			@Override public String getDescription() { return "JSON based description files."; }
			
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}
				String[] parts = f.getName().split("\\.");
				return parts[parts.length - 1].toLowerCase().equals("json");
			}
		});
		imageFolderChooser = new JFileChooser();
		imageFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		imageFolderChooser.addChoosableFileFilter(new FileFilter() {
			@Override public String getDescription() { return "Image directories."; }
			
			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}
		});
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
		JMenuItem loadImages = new JMenuItem("Load images");
		loadImages.setActionCommand(LOAD_IMAGES);
		loadImages.addActionListener(this);
		JMenuItem saveTriggers = new JMenuItem("Save triggers");
		saveTriggers.setActionCommand(SAVE_TRIGGERS);
		saveTriggers.addActionListener(this);
		JMenuItem loadTriggers = new JMenuItem("Load triggers");
		loadTriggers.setActionCommand(LOAD_TRIGGERS);
		loadTriggers.addActionListener(this);
		fileMenu.add(loadImages);
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
		
		imageGrid.setSelectedTrigger(logic.getTriggers().get(0).getName());
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
			JTextField maxThresholdEdit = new JTextField(10);
			maxThresholdEdits.put(trigger.getName(), maxThresholdEdit);
			maxThresholdEdit.setText("" + trigger.getMaxThreshold());
			maxThresholdEdit.setActionCommand("MAX" + trigger.getName());
			maxThresholdEdit.addActionListener(this);
			triggerPanel.add(maxThresholdEdit);
		}
		triggerPanel.add(new JLabel("Min"));
		for (Trigger trigger : triggers) {
			JTextField minThresholdEdit = new JTextField(10);
			minThresholdEdits.put(trigger.getName(), minThresholdEdit);
			minThresholdEdit.setText("" + trigger.getMinThreshold());
			minThresholdEdit.addActionListener(this);
			minThresholdEdit.setActionCommand("MIN" + trigger.getName());
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
		if (e.getActionCommand().equals(LOAD_IMAGES)) {
			// Show load dialog
			int returnVal = imageFolderChooser.showOpenDialog(this);
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = imageFolderChooser.getSelectedFile();
	            // Save file
				logic.setImageSourceDirectory(file);
	        }
		} else if (e.getActionCommand().equals(LOAD_TRIGGERS)) {
			// Show load dialog
			int returnVal = triggerFileChooser.showOpenDialog(this);
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = triggerFileChooser.getSelectedFile();
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
			int returnVal = triggerFileChooser.showSaveDialog(this);
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = triggerFileChooser.getSelectedFile();
	            if (!file.getName().toLowerCase().endsWith(".json")) {
	            	file = new File(file.getAbsolutePath() + ".json");
	            }
	            // Save file
	            try {
					logic.exportToFile(file);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
	        }
		} else if (e.getActionCommand().startsWith("MAX")){
			String triggerName = e.getActionCommand().substring(3);
			Trigger trigger = logic.getTrigger(triggerName);
			JTextField maxThresholdEdit = maxThresholdEdits.get(triggerName);
			try {
				int newMax = Integer.parseInt(maxThresholdEdit.getText());
				if (trigger.getMaxThreshold() != newMax) {
					trigger.setMaxThreshold(newMax);
				}
			} catch (Exception ex) {
				maxThresholdEdit.setText("" + trigger.getMaxThreshold());
			}
		} else if (e.getActionCommand().startsWith("MIN")){
			String triggerName = e.getActionCommand().substring(3);
			Trigger trigger = logic.getTrigger(triggerName);
			JTextField minThresholdEdit = minThresholdEdits.get(triggerName);
			try {
				int newMin = Integer.parseInt(minThresholdEdit.getText());
				if (trigger.getMinThreshold() != newMin) {
					trigger.setMinThreshold(newMin);
				}
			} catch (Exception ex) {
				minThresholdEdit.setText("" + trigger.getMinThreshold());
			}
		} else {
			imageGrid.setSelectedTrigger(logic.getTrigger(e.getActionCommand()).getName());
		}
	}

	@Override
	public void modelChanged() {
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
