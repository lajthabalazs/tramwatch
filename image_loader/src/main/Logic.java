package main;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.imageio.ImageIO;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

public class Logic implements ModelChangeListener {
	private static final int HISTORTY_LENGTH = 30;

	HashMap<String, Trigger> triggers = new HashMap<String, Trigger>();
	ArrayList<String> triggerNames = new ArrayList<String>();
	
	private BufferedImage img;
	private File imageSourceDirectory = null;

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

	public Logic(){
		Trigger firstTramIn = new Trigger("First tram in");
		Trigger firstTramOut = new Trigger("First tram out");
		Trigger firstTramLeaving = new Trigger("First tram leaving");
		Trigger firstTramEntering = new Trigger("First tram entering");

		Trigger secondTramIn = new Trigger("Second tram in");
		Trigger secondTramOut = new Trigger("Second tram out");
		Trigger secondTramLeaving = new Trigger("Second tram leaving");
		Trigger secondTramEntering = new Trigger("Second tram entering");
		triggerNames.add(firstTramLeaving.getName());
		triggers.put(firstTramLeaving.getName(), firstTramLeaving);
		triggerNames.add(firstTramEntering.getName());
		triggers.put(firstTramEntering.getName(), firstTramEntering);
		triggerNames.add(firstTramIn.getName());
		triggers.put(firstTramIn.getName(), firstTramIn);
		triggerNames.add(firstTramOut.getName());
		triggers.put(firstTramOut.getName(), firstTramOut);

		triggerNames.add(secondTramLeaving.getName());
		triggers.put(secondTramLeaving.getName(), secondTramLeaving);
		triggerNames.add(secondTramEntering.getName());
		triggers.put(secondTramEntering.getName(), secondTramEntering);
		triggerNames.add(secondTramIn.getName());
		triggers.put(secondTramIn.getName(), secondTramIn);
		triggerNames.add(secondTramOut.getName());
		triggers.put(secondTramOut.getName(), secondTramOut);
		for (Trigger trigger : triggers.values()) {
			trigger.registerListener(this);
		}
	}
	
	public void refreshTriggers() {
		for (Trigger trigger : triggers.values()) {
			trigger.updateTrigger(aggregatedDifferences);
		}
	}

	public List<Trigger> getTriggers() {
		ArrayList<Trigger> triggers = new ArrayList<Trigger>();
		for (String triggerName : triggerNames) {
			triggers.add(this.triggers.get(triggerName));
		}
		return triggers;
	}

	public int getTriggerCount() {
		return triggers.size();
	}
	
	public void setImageSourceDirectory(File imageSourceDirectory) {
		this.imageSourceDirectory = imageSourceDirectory;
		imageIndex = 0;
		imageRoundRobinIndex = 0;
	}
	
	public void loadImage() {
		if (imageSourceDirectory == null) {
			return;
		}
		String formattedImageIndex = "" + imageIndex;
		while (formattedImageIndex.length() < 5) {
			formattedImageIndex = "0" + formattedImageIndex;
		}
		File f = new File(imageSourceDirectory, "frame" + formattedImageIndex + ".png");
		try {
			
			img = ImageIO.read(f);
		} catch (Exception e){
			System.out.println(f.toString() + " : " + e);
			return;
		}
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
		verticalTiles = imageHeight / gridSize + 1;
		horizontalTiles = imageWidth / gridSize + 1;
	}
	
	public void calculateDifference() {
		// Calculate and store differences between current image and last 29 images
		int[][][] currentPixelData = getPixelData(img, 0, 0, imageWidth, imageHeight);
		if (currentPixelData == null) {
			return;
		}
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
		if (image == null) {
			return null;
		}
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

	public void logTriggers() {
		for (String triggerName : triggerNames) {
			Trigger trigger = triggers.get(triggerName);
			if (trigger.isActive()) {
				System.out.println(imageIndex + " " + trigger.getName() + " ACTIVE " + trigger.getValue() + " (" + trigger.getRangeString() + ")");
			}

		}
	}

	public void exportToFile(File file) throws IOException {
		System.out.println("Save to file " + file);
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write("{");
		writer.write(String.format("%n"));
		writer.write("\"triggers\":");
		writer.write(String.format("%n"));
		writer.write("[");
		writer.write(String.format("%n"));
		boolean first = true;
		for (String triggerName : triggerNames) {
			if (first) {
				first = false;
			} else {
				writer.write(",");
				writer.write(String.format("%n"));
			}
			Trigger trigger = triggers.get(triggerName);
			writer.write(trigger.toJson());
		}
		writer.write(String.format("%n"));
		writer.write("]");
		writer.write(String.format("%n"));
		writer.write("}");
		writer.close();
	}

	public void loadFromFile(File file) throws JsonProcessingException, IOException {
		System.out.println("Load from file " + file);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(file);
		JsonNode triggers = root.get("triggers");
		for (JsonNode triggerNode : triggers) {
			Trigger trigger = Trigger.parseFromJson(triggerNode);
			if (this.triggers.containsKey(trigger.getName())) {
				this.triggers.get(trigger.getName()).unregisterListener(this);
				this.triggers.put(trigger.getName(), trigger);
			}
		}
		for (ModelChangeListener listener : listeners){
			listener.modelChanged();
		}
	}

	@Override
	public void modelChanged() {
		System.out.println("Model changed proxied.");
		for (ModelChangeListener listener : listeners) {
			listener.modelChanged();
		}
	}

	public Trigger getTrigger(String triggerName) {
		return triggers.get(triggerName);
	}
}
