package com.gwel.ai;

import java.util.Comparator;
import com.gwel.entities.DroidShip;

public class DroidComparator implements Comparator<DroidShip>{
	@Override
	public int compare(DroidShip o1, DroidShip o2) {
		// Higher score is sorted before lower score
		if (o1.getScore() < o2.getScore())
			return 1;
		if (o1.getScore() > o2.getScore())
			return -1;
		return 0;
	}
}
