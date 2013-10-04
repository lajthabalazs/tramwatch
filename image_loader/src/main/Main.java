package main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class Main extends Component implements MouseListener, ActionListener {
	private static final long serialVersionUID = 4709581889925916389L;

	private static final HashMap<Integer, Color> colors = new HashMap<Integer, Color>();
	{
		for (int i = 0; i < 255; i++) {
			colors.put(i, new Color(i, 0, 0));
		}
	}
	private static final int HISTORTY_LENGTH = 30;

	private static final String SAVE_TRIGGERS = null;

	private static final String LOAD_TRIGGERS = null;
	private BufferedImage img;
	private int imageIndex = 0;
	private BufferedImage[] imageRoundRobin = new BufferedImage[HISTORTY_LENGTH];
	int imageRoundRobinIndex = 0;
	private int[][][] aggregatedDifferences = new int[HISTORTY_LENGTH][][];
	ArrayList<Trigger> triggers = new ArrayList<Trigger>();
	Trigger selectedTrigger;
	Trigger firstTramIn = new Trigger("First tram in");
	Trigger firstTramOut = new Trigger("First tram out");
	Trigger firstTramLeaving = new Trigger("First tram leaving");
	Trigger firstTramEntering = new Trigger("First tram entering");

	Trigger secondTramIn = new Trigger("Second tram in");
	Trigger secondTramOut = new Trigger("Second tram out");
	Trigger secondTramLeaving = new Trigger("Second tram leaving");
	Trigger secondTramEntering = new Trigger("Second tram entering");

	int imageHeight;
	int imageWidth;
	int verticalTiles;
	int horizontalTiles;
	int gridSize = 12;
	private boolean paused = false;

	public Main() {
		this.addMouseListener(this);
		triggers.add(firstTramLeaving);
		triggers.add(firstTramEntering);
		triggers.add(firstTramIn);
		triggers.add(firstTramOut);
		triggers.add(secondTramEntering);
		triggers.add(secondTramLeaving);
		triggers.add(secondTramIn);
		triggers.add(secondTramOut);
		loadImage();
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
						loadImage();
						calculateDifference();
						refreshTriggers();
					}
					invalidate();
					repaint();
					if (!paused) {
						imageIndex++;
						imageRoundRobinIndex ++;
						if (imageRoundRobinIndex >= HISTORTY_LENGTH) {
							imageRoundRobinIndex = 0;
						}
					}
				}
			}
		}).start();
	}

	public void paint(Graphics g) {
		g.drawImage(img, 0, 0, null);
		g.setColor(Color.BLACK);
		g.setColor(Color.red);
		int gridWidth = gridSize * horizontalTiles;
		int gridHeight = gridSize * verticalTiles;
		int index = 0;
		drawRectangles:
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 5; j++) {
				if (index == HISTORTY_LENGTH) {
					break drawRectangles;
				}
				if (aggregatedDifferences[index] != null) {
					for (int x = 0; x < horizontalTiles; x++) {
						for (int y = 0; y < verticalTiles; y++) {
							g.setColor(colors.get(aggregatedDifferences[index][x][y]));
							g.fillRect(i * gridWidth + x * gridSize, j * gridHeight + y * gridSize, gridSize, gridSize);
						}
					}
				} else {
				}
				index ++;
			}
		}
		g.setColor(Color.WHITE);
		for (int i = 0; i < 6; i++) {
			g.drawLine(i * gridWidth, 0, i*gridWidth, 5 * gridHeight);
		}
		for (int i = 0; i < 5; i++) {
			g.drawLine(0, i * gridHeight, 6 * gridWidth, i * gridHeight);
		}
		g.setColor(Color.black);
		// Draw borders
		Color color = null;
		if (selectedTrigger != null && aggregatedDifferences != null) {
			for (int i = 0; i < aggregatedDifferences.length; i++) {
				if (aggregatedDifferences[i] == null) {
					continue;
				}
				for (int j = 0; j < aggregatedDifferences[i].length; j++) {
					if (aggregatedDifferences[i][j] == null) {
						continue;
					}
					for (int k = 0; k < aggregatedDifferences[i][j].length; k++) {
						color = null;
						boolean drawTop = false;
						boolean drawRight = false;
						boolean drawLeft = false;
						boolean drawBottom = false;
						if (selectedTrigger.getCoefficient(i,j,k) == 1) {
							color = Color.GREEN;
						} else if (selectedTrigger.getCoefficient(i,j,k) == -1) {
							color = Color.RED;
						}
						// Check neighbors
						try {
							if (selectedTrigger.getCoefficient(i,j,k) != selectedTrigger.getCoefficient(i,j,k-1)) {
								drawTop = true;
							}
						} catch (Exception e) {
							drawTop = true;
						}
						try {
							if (selectedTrigger.getCoefficient(i,j,k) != selectedTrigger.getCoefficient(i,j,k+1)) {
								drawBottom = true;
							}
						} catch (Exception e) {
							drawBottom = true;
						}
						try {
							if (selectedTrigger.getCoefficient(i,j,k) != selectedTrigger.getCoefficient(i,j-1,k)) {
								drawLeft = true;
							}
						} catch (Exception e) {
							drawLeft = true;
						}
						try {
							if (selectedTrigger.getCoefficient(i,j,k) != selectedTrigger.getCoefficient(i,j + 1,k)) {
								drawRight = true;
							}
						} catch (Exception e) {
							drawRight = true;
						}
						if (color != null) {
							g.setColor(color);
							int historyX = i / verticalTiles;
							int historyY = i - historyX * verticalTiles;
							int xBase = historyX * gridWidth + j * gridSize;
							int yBase = historyY * gridHeight + k * gridSize;
							if (drawTop) {
								g.drawLine(xBase, yBase, xBase + gridSize - 1, yBase);
							}
							if (drawBottom) {
								g.drawLine(xBase, yBase + gridSize -1, xBase + gridSize - 1, yBase + gridSize -1);
							}
							if (drawLeft) {
								g.drawLine(xBase, yBase, xBase, yBase + gridSize -1);
							}
							if (drawRight) {
								g.drawLine(xBase + gridSize -1, yBase, xBase + gridSize -1, yBase + gridSize -1);
							}
						}
					}
				}
			}
		}
	}

	public void loadImage() {
		try {
			String formattedImageIndex = "" + imageIndex;
			while (formattedImageIndex.length() < 5) {
				formattedImageIndex = "0" + formattedImageIndex;
			}
			img = ImageIO.read(new File("f:\\crowd\\small\\frame"
					+ formattedImageIndex + ".png"));
			imageRoundRobin[imageRoundRobinIndex] = img;
			imageWidth = img.getWidth();
			imageHeight = img.getHeight();
		} catch (IOException e) {
			System.out.println("Error loading image ");
			e.printStackTrace();
		}
		verticalTiles = imageHeight / gridSize + 1;
		horizontalTiles = imageWidth / gridSize + 1;
	}
	
	private void calculateDifference() {
		// Calculate and store differences between current image and last 29 images
		int[][][] currentPixelData = getPixelData(img, 0, 0, imageWidth, imageHeight);
		for (int i = 1; i < HISTORTY_LENGTH; i++) {
			int roundRobinItemIndex = imageRoundRobinIndex - i;
			if (roundRobinItemIndex < 0) {
				roundRobinItemIndex += HISTORTY_LENGTH;
			}
			BufferedImage referenceImage = imageRoundRobin[roundRobinItemIndex];
			if (referenceImage != null) {
				int[][][] referencePixelData = getPixelData(referenceImage, 0, 0, imageWidth, imageHeight);
				aggregatedDifferences[i] = new int[horizontalTiles][verticalTiles];
				for (int xTileIndex = 0; xTileIndex < horizontalTiles; xTileIndex++) {
					for (int yTileIndex = 0; yTileIndex < verticalTiles; yTileIndex++) {
						long totalDifference = 0;
						int pixelCount = 0;
						nextX:
						for (int xPixel = xTileIndex * gridSize; xPixel < xTileIndex * gridSize + gridSize; xPixel++) {
							if (xPixel >= imageWidth){
								continue;
							}
							for (int yPixel = yTileIndex * gridSize; yPixel < yTileIndex * gridSize + gridSize; yPixel++) {
								if (yPixel >= imageHeight){
									continue nextX;
								}
								totalDifference += Math.abs(currentPixelData[xPixel][yPixel][0] - referencePixelData[xPixel][yPixel][0]); 
								totalDifference += Math.abs(currentPixelData[xPixel][yPixel][1] - referencePixelData[xPixel][yPixel][1]); 
								totalDifference += Math.abs(currentPixelData[xPixel][yPixel][2] - referencePixelData[xPixel][yPixel][2]);
								pixelCount += 3;
							}
						}
						aggregatedDifferences[i][xTileIndex][yTileIndex] = (int)(totalDifference / pixelCount);
					}
				}
			}
		}
	}

	private void refreshTriggers() {
		for (Trigger trigger : triggers) {
			trigger.updateTrigger(aggregatedDifferences);
		}
	}

	public Dimension getPreferredSize() {
		if (img == null) {
			return new Dimension(500, 800);
		} else {
			return new Dimension(Math.max(500, img.getWidth(null) * 8), Math.max(
					400, img.getHeight(null) * 6));
		}
	}

	public static void main(String[] args) {
		JFrame f = new JFrame("Load Image Sample");
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem saveTriggers = new JMenuItem("Save triggers");
		saveTriggers.setActionCommand(SAVE_TRIGGERS);
		JMenuItem loadTriggers = new JMenuItem("Load triggers");
		loadTriggers.setActionCommand(LOAD_TRIGGERS);
		fileMenu.add(saveTriggers);
		fileMenu.add(loadTriggers);
		menuBar.add(fileMenu);
		f.setJMenuBar(menuBar);

		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		JPanel panel  = new JPanel();
		f.setContentPane(panel);
		final Main main = new Main();
		JButton pp = new JButton("Play/Pause");
		pp.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				main.paused = !main.paused;
			}
		});
		JPanel triggerSelectorPanel = new JPanel();
		ButtonGroup buttonGroup = new ButtonGroup();
		for (int i = 0; i < 8; i++) {
			JRadioButton radioButton = new JRadioButton(main.triggers.get(i).getName());
			buttonGroup.add(radioButton);
			radioButton.setActionCommand("" + i);
			radioButton.addActionListener(main);
			triggerSelectorPanel.add(radioButton);
		}
		panel.add(triggerSelectorPanel);
		panel.add(pp);
		panel.add(main);
		f.pack();
		f.setVisible(true);
	}

	private static int[][][] getPixelData(BufferedImage image,
			int x, int y, int twidth, int theight) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();

		int[][][] result = new int[imageWidth][imageHeight][3];
		for (int i = x; i < twidth; i++) {
			for (int j = y; j < theight; j++) {
				result[i][j][2] = ((image.getRGB(i, j) & 0x0000ff));
				result[i][j][1] = ((image.getRGB(i, j) & 0x00ff00)>>8);
				result[i][j][0] = ((image.getRGB(i, j) & 0xff0000)>>16);
			}
		}
		return result;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		Point p = e.getPoint();
		
		int gridWidth = gridSize * horizontalTiles;
		int gridHeight = gridSize * verticalTiles;

		int historyX = p.x / gridWidth;
		int historyY = p.y / gridHeight;
		int historyIndex = historyX * verticalTiles + historyY;
		int x = (p.x - historyX * gridWidth) / gridSize;
		int y = (p.y - historyY * gridHeight)/ gridSize;
		if (selectedTrigger != null) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				if (selectedTrigger.getCoefficient(historyIndex, x, y) == 1) {
					selectedTrigger.setCoefficient(historyIndex, x, y, 0);
				} else {
					selectedTrigger.setCoefficient(historyIndex, x, y, 1);
				}
			}
			else if ((e.getButton() == MouseEvent.BUTTON2) || (e.getButton() == MouseEvent.BUTTON3)){
				if (selectedTrigger.getCoefficient(historyIndex, x, y) == -1) {
					selectedTrigger.setCoefficient(historyIndex, x, y, 0);
				} else {
					selectedTrigger.setCoefficient(historyIndex, x, y, -1);
				}
			}
		}
		System.out.println("Click " + x + " " + y);
	}

	@Override public void mouseClicked(MouseEvent e) {}
	@Override public void mouseReleased(MouseEvent e) {}
	@Override public void mouseEntered(MouseEvent e) {}
	@Override public void mouseExited(MouseEvent e) {}

	@Override
	public void actionPerformed(ActionEvent e) {
		int index = Integer.parseInt(e.getActionCommand());
		selectedTrigger = triggers.get(index);
	}
}