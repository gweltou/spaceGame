package com.gwel.surfaceEntities;

import java.util.Arrays;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.physics.box2d.World;

public class XenoTreeManager {
	private World world;
	public TreeParam tp;
	public TreeParamMod tpm;
	public FlowerParam fp;
	public LeafParam lp;
    private float surfaceLength;
    private float[] treeCoords;
    private RandomXS128 generator;
	
	public XenoTreeManager(RandomXS128 generator, World world, float surfaceLength) {
		this.generator = generator;
		this.world = world;
		this.surfaceLength = surfaceLength;
		float treeDensity = 0.02f;
		int numTrees = (int) (treeDensity * surfaceLength);
		
		tp = new TreeParam(generator);
		tpm = new TreeParamMod(generator);
		fp = new FlowerParam(generator);
		lp = new LeafParam(generator);
		
		treeCoords = new float[numTrees];
		for (int i=0; i<numTrees; i++) {
			treeCoords[i] = generator.nextFloat()*surfaceLength;
			
		}
		Arrays.sort(treeCoords);
		
		System.out.print("XenoTreeManager created with " + treeCoords.length + " tree coords");
		System.out.println(", surface length: " + surfaceLength);
	}
	
	XenoTree buildTree(float x, float y) {
		generator.setSeed((long) y);
		float width = generator.nextFloat()*5f + 1f;
		return new XenoTree(world, x, y, width, tp, tpm, fp, lp, null, generator);
	}

	public float[] getCoordsBetween(float leftCoord, float rightCoord) {
		System.out.println("From XenoTreeManager.getCoordsBetween: ");
		System.out.println("    leftCoord " + leftCoord + " rightCoord " + rightCoord);

		float offset = 0f;
		float[] coords;

		while (leftCoord < 0) {
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
			mid = (int) Math.floor((left + right) / 2);
			if (treeCoords[mid] < xCoord)
				left = mid + 1;
			else if (treeCoords[mid] > xCoord)
				right = mid - 1;
		}
		return left;
	}
}
