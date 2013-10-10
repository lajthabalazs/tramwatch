package main;

import java.util.HashMap;
import java.util.HashSet;

import org.codehaus.jackson.JsonNode;

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
				System.out.println("Coefficient removed");
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
			}
			HashMap<Integer, Integer> columnCoefficients = imageCoefficients.get(x);
			if (columnCoefficients == null) {
				columnCoefficients = new HashMap<Integer, Integer>();
				imageCoefficients.put(x, columnCoefficients);
			}
			columnCoefficients.put(y, value);
			for (ModelChangeListener listener : listeners) {
				System.out.println("Coefficient added");
				listener.modelChanged();
			}
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
					if (aggregatedDifferences != null &&
							aggregatedDifferences[imageKey] != null &&
							aggregatedDifferences[imageKey][columnKey] != null) {
						value += aggregatedDifferences[imageKey][columnKey][rowKey] * coefficients.get(imageKey).get(columnKey).get(rowKey);
						total += Math.max(0,coefficients.get(imageKey).get(columnKey).get(rowKey));
					}
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
		System.out.println("Min changed from " + this.minThreshold + " to " + minThreshold);
		this.minThreshold = minThreshold;
		for (ModelChangeListener listener : listeners) {
			System.out.println("Min set ");
			listener.modelChanged();
		}

	}

	public int getMaxThreshold() {
		return maxThreshold;
	}

	public void setMaxThreshold(int maxThreshold) {
		System.out.println("Max changed from " + this.maxThreshold + " to " + maxThreshold);
		this.maxThreshold = maxThreshold;
		for (ModelChangeListener listener : listeners) {
			System.out.println("Max set ");
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
		builder.append("\"minThreshold\":");
		builder.append(minThreshold);
		builder.append(",\n");
		builder.append("\"maxThreshold\":");
		builder.append(maxThreshold);
		builder.append(",\n");
		builder.append("\"coefficients\":\n");
		builder.append("[\n");
		boolean first = true;
		for (Integer imageKey : coefficients.keySet()) {
			for (Integer columnKey : coefficients.get(imageKey).keySet()) {
				for (Integer rowKey : coefficients.get(imageKey).get(columnKey).keySet()) {
					if (first) {
						first = false;
					} else {
						builder.append(",\n");
					}
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

	public static Trigger parseFromJson(JsonNode triggerNode) {
		System.out.println("Parsing json trigger");
		Trigger trigger = new Trigger(triggerNode.get("name").getTextValue());
		trigger.setMinThreshold(triggerNode.get("minThreshold").getIntValue());
		trigger.setMaxThreshold(triggerNode.get("maxThreshold").getIntValue());
		JsonNode coefficients = triggerNode.get("coefficients");
		for (JsonNode coeffNode : coefficients) {
			System.out.println("Parsing json coefficient");
			trigger.setCoefficient(coeffNode.get("image").getIntValue(),
					coeffNode.get("x").getIntValue(),
					coeffNode.get("y").getIntValue(),
					coeffNode.get("c").getIntValue());
		}
		
		return trigger;
	}
}
