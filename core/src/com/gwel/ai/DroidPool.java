package com.gwel.ai;

import java.util.ArrayList;
import java.util.UUID;

import com.gwel.spacegame.Enums;

public class DroidPool {
	public String id;
	public int[] nnLayers;
	public Enums activationFunc;
	public int generation;
	public int startPop;
	public int offsprings;
	public int winnersPerGen;
	public ArrayList<Integer> scores;
	public int bestGen;
	public int bestGenScore;
	
	public ArrayList<float[][][]> nn;
	public ArrayList<float[][][]> nnBest;
	
	public DroidPool() {
		id = UUID.randomUUID().toString();
		generation = 0;
		bestGenScore = 0;
		scores = new ArrayList<Integer>();
	}
}