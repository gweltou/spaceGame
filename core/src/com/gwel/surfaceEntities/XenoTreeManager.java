package com.gwel.surfaceEntities;

import java.util.Arrays;

import com.badlogic.gdx.math.RandomXS128;

public class XenoTreeManager {
	private static TreeParam tp;
	private static TreeParamMod tpm;
	private static FlowerParam fp;
	private static LeafParam lp;
    private float surfaceLength;
    private float[] treeCoords;
	
	public XenoTreeManager(RandomXS128 generator, float surfaceLength) {
		this.surfaceLength = surfaceLength;
		float treeDensity = 0.001f;
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
	}
	
	public float[] getCoordsBetween(float leftCoord, float rightCoord) {
		leftCoord %= surfaceLength;
		if (leftCoord < 0)	leftCoord += surfaceLength;
		rightCoord %= surfaceLength;
		if (rightCoord < 0)	rightCoord += surfaceLength;
		
		return treeCoords;
	}
	
	

}
