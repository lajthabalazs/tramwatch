package main;

import java.util.HashMap;
import java.util.HashSet;

public class Trigger {
	HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>> coefficients = new HashMap<Integer, HashMap<Integer,HashMap<Integer,Integer>>>();
	private String name;
	private int minThreshold = 0;
	private int maxThreshold = 0;
	private boolean external = false;
	
	private HashSet<ModelChangeListener> listeners = new HashSet<ModelChangeListener>();
	
	private double value = 0;
	
	public Trigger(String name) {
		this.name = name;
	}
	
	synchronized public void setCoefficient(int image, int x, int y, int value) {
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
	
	synchronized public void updateTrigger(int[][][] aggregatedDifferences){
		value = 0.0;
		int total = 0;
		for (Integer imageKey : coefficients.keySet()) {
			for (Integer columnKey : coefficients.get(imageKey).keySet()) {
				for (Integer rowKey : coefficients.get(imageKey).get(columnKey).keySet()) {
					value += aggregatedDifferences[imageKey][columnKey][rowKey] * coefficients.get(imageKey).get(columnKey).get(rowKey);
					total += Math.max(0,coefficients.get(imageKey).get(columnKey).get(rowKey));
				}
			}
		}
		if (total == 0) {
			value = 0;
		} else {
			value = value / total;
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
	
	public double getValue() {
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

	public String getRangeString() {
		if (external) {
			return "x ] " + minThreshold + "," + maxThreshold + "[ x"; 
		} else {
			return minThreshold + "[ x ]" + maxThreshold;
		}
	}
}
