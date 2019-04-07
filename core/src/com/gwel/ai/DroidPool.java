package com.gwel.ai;

import java.util.ArrayList;
import java.util.UUID;

import com.gwel.spacegame.Enums;

public class DroidPool {
	public String id;
	public int generation;
	public ArrayList<Integer> scores;
	public int[] nnLayers;
	public Enums activationFunc;
	public int bestGen;
	public int startPop;
	public int offsprings;
	public int winnersPerGen;
	public ArrayList<float[][][]> nn;
	public ArrayList<float[][][]> nnBest;
	
	public DroidPool() {
		id = UUID.randomUUID().toString();
	}
}