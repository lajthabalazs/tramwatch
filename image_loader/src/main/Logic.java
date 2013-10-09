package main;

import java.util.ArrayList;
import java.util.List;

public class Logic {
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

	public void refreshTriggers(int[][][] aggregatedDifferences) {
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
}
