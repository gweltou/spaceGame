package com.gwel.ai;

import java.util.ArrayList;
import java.util.UUID;

public class DroidPool {
	public String id;
	public ArrayList<Integer> scores;
	public int[] nnLayers;
	public String activationFunc;
	public ArrayList<NeuralNetwork> nn;
	
	public DroidPool() {
		id = UUID.randomUUID().toString();
		nn = new ArrayList<NeuralNetwork>();
	}
}
