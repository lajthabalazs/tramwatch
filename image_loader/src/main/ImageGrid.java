package main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;

public class ImageGrid extends Component implements MouseListener {
	private static final long serialVersionUID = 4709581889925916389L;

	private static final HashMap<Integer, Color> colors = new HashMap<Integer, Color>();
	{
		for (int i = 0; i < 255; i++) {
			colors.put(i, new Color(i, 0, 0));
		}
	}

	private Logic logic;

	private String selectedTriggerName;

	public ImageGrid() {
		this.addMouseListener(this);
	}
	
	public void setLogic(Logic logic) {
		this.logic = logic;
	}

	public void paint(Graphics g) {
		if (logic == null) {
			return;
		}
		Image image = logic.getImage();
		g.drawImage(image, 0, 0, null);
		g.setColor(Color.BLACK);
		g.setColor(Color.red);
		int gridWidth = logic.getGridSize() * logic.getHorizontalTiles();
		int gridHeight = logic.getGridSize() * logic.getVerticalTiles();
		int index = 0;
		drawRectangles:
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 5; j++) {
				if (index == logic.getHistoryLength()) {
					break drawRectangles;
				}
				if (logic.hasDifference(index)) {
					for (int x = 0; x < logic.getHorizontalTiles(); x++) {
						for (int y = 0; y < logic.getVerticalTiles(); y++) {
							g.setColor(colors.get(logic.getAggregatedDifferences(index, x, y)));
							g.fillRect(i * gridWidth + x * logic.getGridSize(), j * gridHeight + y * logic.getGridSize(), logic.getGridSize(), logic.getGridSize());
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
		Trigger selectedTrigger = logic.getTrigger(selectedTriggerName);
		if (selectedTrigger != null) {
			for (int i = 0; i < logic.getHistoryLength(); i++) {
				if (!logic.hasDifference(i)) {
					continue;
				}
				for (int j = 0; j < logic.getHorizontalTiles(); j++) {
					for (int k = 0; k < logic.getVerticalTiles(); k++) {
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
							int historyX = i / logic.getVerticalTiles();
							int historyY = i - historyX * logic.getVerticalTiles();
							int xBase = historyX * gridWidth + j * logic.getGridSize();
							int yBase = historyY * gridHeight + k * logic.getGridSize();
							if (drawTop) {
								g.drawLine(xBase, yBase, xBase + logic.getGridSize() - 1, yBase);
							}
							if (drawBottom) {
								g.drawLine(xBase, yBase + logic.getGridSize() -1, xBase + logic.getGridSize() - 1, yBase + logic.getGridSize() -1);
							}
							if (drawLeft) {
								g.drawLine(xBase, yBase, xBase, yBase + logic.getGridSize() -1);
							}
							if (drawRight) {
								g.drawLine(xBase + logic.getGridSize() -1, yBase, xBase + logic.getGridSize() -1, yBase + logic.getGridSize() -1);
							}
						}
					}
				}
			}
		}
	}	

	public Dimension getPreferredSize() {
		if ((logic == null) || logic.getImage() == null) {
			return new Dimension(500, 600);
		} else {
			return new Dimension(Math.max(500, logic.getImage().getWidth(null) * 8), Math.max(
					400, logic.getImage().getHeight(null) * 6));
		}
	}


	@Override
	public void mousePressed(MouseEvent e) {
		if ((logic == null) || logic.getImage() == null) {
			return;
		}
		Point p = e.getPoint();
		
		int gridWidth = logic.getGridSize() * logic.getHorizontalTiles();
		int gridHeight = logic.getGridSize() * logic.getVerticalTiles();

		int historyX = p.x / gridWidth;
		int historyY = p.y / gridHeight;
		int historyIndex = historyX * logic.getVerticalTiles() + historyY;
		int x = (p.x - historyX * gridWidth) / logic.getGridSize();
		int y = (p.y - historyY * gridHeight)/ logic.getGridSize();
		Trigger selectedTrigger = logic.getTrigger(selectedTriggerName);		
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
	}

	@Override public void mouseClicked(MouseEvent e) {}
	@Override public void mouseReleased(MouseEvent e) {}
	@Override public void mouseEntered(MouseEvent e) {}
	@Override public void mouseExited(MouseEvent e) {}

	public void setSelectedTrigger(String selectedTriggerName) {
		this.selectedTriggerName = selectedTriggerName;
		
	}
}