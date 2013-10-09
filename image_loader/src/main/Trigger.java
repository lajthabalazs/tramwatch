package main;

import java.util.HashMap;
import java.util.HashSet;

public class Trigger {
	HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>> coefficients = new HashMap<Integer, HashMap<Integer,HashMap<Integer,Integer>>>();
	private String name;
	private int minThreshold;
	private int maxThreshold;
	private boolean external = false;
	
	private HashSet<ModelChangeListener> listeners = new HashSet<ModelChangeListener>();
	
	private int value = 0;
	
	public Trigger(String name) {
		this.name = name;
	}
	
	public void setCoefficient(int image, int x, int y, int value) {
		System.out.println("Set coeff " + image + " " + x + " " + y + " " + value );
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
			for (ModelChangeListener listener : listeners) {
				listener.modelChanged();
			}
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
				for (ModelChangeListener listener : listeners) {
					listener.modelChanged();
				}
			}
			HashMap<Integer, Integer> columnCoefficients = imageCoefficients.get(x);
			if (columnCoefficients == null) {
				columnCoefficients = new HashMap<Integer, Integer>();
				imageCoefficients.put(x, columnCoefficients);
				for (ModelChangeListener listener : listeners) {
					listener.modelChanged();
				}
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
		for (Integer imageKey : coefficients.keySet()) {
			for (Integer columnKey : coefficients.get(imageKey).keySet()) {
				for (Integer rowKey : coefficients.get(imageKey).get(columnKey).keySet()) {
					value += aggregatedDifferences[imageKey][columnKey][rowKey] * coefficients.get(imageKey).get(columnKey).get(rowKey);
				}
			}
		}
	}
	
	public boolean isExternal() {
		return external;
	}

	public void setExternal(boolean external) {
		this.external = external;
		for (ModelChangeListener listener : listeners) {
			listener.modelChanged();
		}
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
		for (ModelChangeListener listener : listeners) {
			listener.modelChanged();
		}

	}

	public int getMaxThreshold() {
		return maxThreshold;
	}

	public void setMaxThreshold(int maxThreshold) {
		this.maxThreshold = maxThreshold;
		for (ModelChangeListener listener : listeners) {
			listener.modelChanged();
		}
	}
	
	public String toJson(){
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		builder.append("\"name\":");
		builder.append("\"");
		builder.append(name);
		builder.append("\"");
		builder.append(",\n");
		builder.append("\"coefficients\":\n");
		builder.append("[\n");
		for (Integer imageKey : coefficients.keySet()) {
			for (Integer columnKey : coefficients.get(imageKey).keySet()) {
				for (Integer rowKey : coefficients.get(imageKey).get(columnKey).keySet()) {
					builder.append("{");
					builder.append("\"image\":");
					builder.append(imageKey);
					builder.append(",");
					builder.append("\"x\":");
					builder.append(columnKey);
					builder.append(",");
					builder.append("\"y\":");
					builder.append(rowKey);
					builder.append(",");
					builder.append("\"c\":");
					builder.append(coefficients.get(imageKey).get(columnKey).get(rowKey));
					builder.append("}");
				}
			}
		}
		builder.append("]\n");
		builder.append("}");
		return builder.toString();
	}

	public void registerListener(ModelChangeListener listener) {
		listeners.add(listener);
	}
	
	public void unregisterListener(ModelChangeListener listener) {
		listeners.remove(listener);
	}
}
