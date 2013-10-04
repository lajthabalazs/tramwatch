package main;

import java.util.HashMap;

public class Trigger {
	HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>> coefficients = new HashMap<Integer, HashMap<Integer,HashMap<Integer,Integer>>>();
	private String name;
	private int minThreshold;
	private int maxThreshold;
	private boolean external = false;
	
	private int value;
	
	public Trigger(String name) {
		this.name = name;
	}
	
	public void setCoefficient(int image, int x, int y, int value) {
		if (value == 0) {
			HashMap<Integer, HashMap<Integer, Integer>> imageCoefficients = coefficients.get(image);
			if (imageCoefficients == null) {
				return;
			}
			HashMap<Integer, Integer> columnCoefficients = imageCoefficients.get(x);
			if (columnCoefficients == null) {
				return;
			}
			columnCoefficients.remove(y);
			if (columnCoefficients.size() == 0) {
				imageCoefficients.remove(columnCoefficients);
				if (imageCoefficients.size() == 0) {
					coefficients.remove(imageCoefficients);
				}
			}
		} else {
			HashMap<Integer, HashMap<Integer, Integer>> imageCoefficients = coefficients.get(image);
			if (imageCoefficients == null) {
				imageCoefficients = new HashMap<Integer, HashMap<Integer,Integer>>();
				coefficients.put(image, imageCoefficients);
			}
			HashMap<Integer, Integer> columnCoefficients = imageCoefficients.get(x);
			if (columnCoefficients == null) {
				columnCoefficients = new HashMap<Integer, Integer>();
				imageCoefficients.put(x, columnCoefficients);
			}
			columnCoefficients.put(y, value);
		}
	}
	
	public int getCoefficient(int image, int x, int y) {
		try {
			return coefficients.get(image).get(x).get(y);
		} catch(Exception e) {
			return 0;
		}
	}
	
	public void updateTrigger(int[][][] aggregatedDifferences){
		value = 0;
		for (int i = 0; i < aggregatedDifferences.length; i++) {
			if (aggregatedDifferences[i] != null) {
				for (int j = 0; j < aggregatedDifferences[i].length; j++) {
					for (int k = 0; k < aggregatedDifferences[i][j].length; k++) {
						try {			
							value += aggregatedDifferences[i][j][k] * aggregatedDifferences[i][j][k];
						}catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	public boolean isExternal() {
		return external;
	}

	public void setExternal(boolean external) {
		this.external = external;
	}

	public boolean isActive(){
		if (external) {
			return value < minThreshold || value > maxThreshold;
		} else {
			return value > minThreshold && value < maxThreshold;
		}
	}
	
	public int getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	public int getMinThreshold() {
		return minThreshold;
	}

	public void setMinThreshold(int minThreshold) {
		this.minThreshold = minThreshold;
	}

	public int getMaxThreshold() {
		return maxThreshold;
	}

	public void setMaxThreshold(int maxThreshold) {
		this.maxThreshold = maxThreshold;
	}
}
