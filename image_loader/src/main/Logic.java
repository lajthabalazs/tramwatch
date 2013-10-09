package main;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.imageio.ImageIO;

public class Logic {
	private static final int HISTORTY_LENGTH = 30;

	ArrayList<Trigger> triggers = new ArrayList<Trigger>();
	Trigger firstTramIn = new Trigger("First tram in");
	Trigger firstTramOut = new Trigger("First tram out");
	Trigger firstTramLeaving = new Trigger("First tram leaving");
	Trigger firstTramEntering = new Trigger("First tram entering");

	Trigger secondTramIn = new Trigger("Second tram in");
	Trigger secondTramOut = new Trigger("Second tram out");
	Trigger secondTramLeaving = new Trigger("Second tram leaving");
	Trigger secondTramEntering = new Trigger("Second tram entering");
	
	{
		triggers.add(firstTramLeaving);
		triggers.add(firstTramEntering);
		triggers.add(firstTramIn);
		triggers.add(firstTramOut);
		triggers.add(secondTramEntering);
		triggers.add(secondTramLeaving);
		triggers.add(secondTramIn);
		triggers.add(secondTramOut);
	}

	private BufferedImage img;

	private int imageIndex = 0;
	private BufferedImage[] imageRoundRobin = new BufferedImage[HISTORTY_LENGTH];
	int imageRoundRobinIndex = 0;
	private int[][][] aggregatedDifferences = new int[HISTORTY_LENGTH][][];

	int imageHeight;
	int imageWidth;
	int verticalTiles;
	int horizontalTiles;
	int gridSize = 12;
	
	private HashSet<ModelChangeListener> listeners = new HashSet<ModelChangeListener>();

	public void refreshTriggers() {
		for (Trigger trigger : triggers) {
			trigger.updateTrigger(aggregatedDifferences);
		}
	}

	public List<Trigger> getTriggers() {
		return new ArrayList<Trigger>(triggers); 
	}

	public int getTriggerCount() {
		return triggers.size();
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
			imageIndex++;
			imageRoundRobinIndex ++;
			if (imageRoundRobinIndex >= HISTORTY_LENGTH) {
				imageRoundRobinIndex = 0;
			}
			for (ModelChangeListener listener : listeners) {
				listener.modelChanged();
			}
		} catch (IOException e) {
			System.out.println("Error loading image ");
			e.printStackTrace();
		}
		verticalTiles = imageHeight / gridSize + 1;
		horizontalTiles = imageWidth / gridSize + 1;
	}
	
	public void calculateDifference() {
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

	public Image getImage() {
		return img;
	}

	public int getGridSize() {
		return gridSize;
	}

	public int getHorizontalTiles() {
		return horizontalTiles;
	}

	public int getVerticalTiles() {
		return verticalTiles;
	}

	public int getHistoryLength() {
		return HISTORTY_LENGTH;
	}

	public int getAggregatedDifferences(int index, int x, int y) {
		return aggregatedDifferences[index][x][y];
	}

	public boolean hasDifference(int index) {
		return aggregatedDifferences[index] != null;
	}
	
	public void registerListener(ModelChangeListener listener) {
		listeners.add(listener);
	}
	
	public void unregisterListener(ModelChangeListener listener) {
		listeners.remove(listener);
	}
}
