package com.gwel.surfaceEntities;

import java.util.Arrays;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.World;
import com.gwel.entities.Planet;


public class XenoTreeManager {
	private World world;
	public TreeParam tp;
	public TreeParamMod tpm;
	public FlowerParam fp;
	public LeafParam lp;
    private float surfaceLength;
    private float[] treeCoords;

	public XenoTreeManager(World world, Planet planet) {
		this.world = world;
		this.surfaceLength = planet.surfaceLength;
		float treeDensity = Math.max(0.0f, MathUtils.random(-0.1f, 0.2f));
		int numTrees = (int) (treeDensity * surfaceLength);
		
		tp = new TreeParam();
		tpm = new TreeParamMod();
		fp = new FlowerParam();
		lp = new LeafParam();
		
		treeCoords = new float[numTrees];
		for (int i=0; i<numTrees; i++) {
			treeCoords[i] = MathUtils.random.nextFloat()*surfaceLength;
			
		}
		Arrays.sort(treeCoords);
		
		System.out.print("XenoTreeManager created with " + treeCoords.length + " trees ");
		System.out.println("(" + treeDensity + " density)");
	}
	
	XenoTree buildTree(float x, float y) {
		MathUtils.random.setSeed((long) (y*100));
		float width = MathUtils.random.nextFloat()*4f + 1f;
		return new XenoTree(world, x, y, width, tp, tpm, fp, lp, null);
	}

	public float[] treesBetween(float leftCoord, float rightCoord) {
		float offset = 0.0f;
		float[] coords;

		// Add padding around the queried field so trees appear a bit sooner
		leftCoord -= 20;
		rightCoord += 20;

		while (leftCoord < 0.0f) {
			leftCoord += surfaceLength;
			rightCoord += surfaceLength;
			offset -= surfaceLength;
		}
		while (leftCoord > surfaceLength) {
			leftCoord -= surfaceLength;
			rightCoord -= surfaceLength;
			offset += surfaceLength;
		}

		if (rightCoord > surfaceLength) {
			// Add coordinates from leftCoord to surfaceLength an from 0 to rightCoord-surfaceLength
			int leftIndex = findLeftIndex(leftCoord);
			int rightIndex = findLeftIndex(rightCoord-surfaceLength);
			coords = new float[treeCoords.length - leftIndex + rightIndex];
			int i = 0;
			for (; i<treeCoords.length-leftIndex; i++) {
				coords[i] = treeCoords[leftIndex + i] + offset;
			}
			for (int j=0; j<rightIndex; j++) {
				coords[i+j] = treeCoords[j] + offset + surfaceLength;
			}
		} else {
			int leftIndex = findLeftIndex(leftCoord);
			int rightIndex = findLeftIndex(rightCoord);
			coords = new float[rightIndex - leftIndex];
			for (int i=0; i<coords.length; i++) {
				coords[i] = treeCoords[leftIndex+i] + offset;
			}
		}
		return coords;
	}
	
	int findLeftIndex(float xCoord) {
		int left = 0;
		int right = treeCoords.length - 1;
		int mid;
		while (left <= right) {
			mid = (int) Math.floor((left + right) / 2f);
			if (treeCoords[mid] < xCoord)
				left = mid + 1;
			else if (treeCoords[mid] > xCoord)
				right = mid - 1;
		}
		return left;
	}
}
