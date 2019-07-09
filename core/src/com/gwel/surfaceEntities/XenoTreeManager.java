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
		
		System.out.println("XenoTreeManager created with " + treeCoords.length + " coords");
	}
	
	XenoTree buildTree(float x, float y) {
		generator.setSeed((long) x);
		float width = generator.nextFloat()*10f + 2f;
		return new XenoTree(world, x, y, width, generator);
	}
	
	public float[] getCoordsBetween(float leftCoord, float rightCoord) {
		float[] coords;
		leftCoord %= surfaceLength;
		if (leftCoord < 0)	leftCoord += surfaceLength;
		rightCoord %= surfaceLength;
		if (rightCoord < 0)	rightCoord += surfaceLength;
		int leftIndex = findLeftIndex(leftCoord);
		int rightIndex = findLeftIndex(rightCoord) ;//% treeCoords.length;
		System.out.println("l " + leftCoord + " r " + rightCoord);
		System.out.println("li " + leftIndex + " ri " + rightIndex);
		if (leftIndex > rightIndex) {
			coords = new float[treeCoords.length - leftIndex + rightIndex];
			int i=0;
			while (i+leftIndex < treeCoords.length)
				coords[i++] = treeCoords[leftIndex + i];
			for (int j=0; j<rightIndex; j++)
				coords[i++] = treeCoords[j];
		} else {
			int n = rightIndex - leftIndex;
			coords = new float[n];
			for (int i=0; i<n; i++)
				coords[i] = treeCoords[i+leftIndex];
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
