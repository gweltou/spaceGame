package com.gwel.ai;

import com.gwel.spacegame.Enums;

public class NeuralNetwork {
	public float[][][] weights;
	protected Enums activation;

	public NeuralNetwork(Enums activation) {
		this.activation = (Enums) activation;
	}
	public NeuralNetwork(NeuralNetwork other) {
		weights = other.weights.clone();
		activation = other.activation;
	}
	
	public void random(int[] layers) {
		weights = new float[layers.length-1][][];
		
		// Fill weights matrix with random floats between -1 and 1
		// Add 1 row (layers[i]+1) for the bias
		for (int i=0; i<weights.length; i++) {
			weights[i] = Np.sub(1.0f, Np.mul(2.0f, Np.random(layers[i+1], layers[i]+1)));
			//System.out.println(Arrays.deepToString(weights[i]));
		}
	}
	
	public float[] feedforward(float[] input) {
		float[] tmp;
		float[] output = new float[input.length+1];
		int i;
		for (i=0; i<input.length; i++)
			output[i] = input[i];
		output[i] = 1.0f;	// Add the bias
		int l;
		for (l=0; l<weights.length-1; l++) {
			tmp = Np.relu(Np.mul(weights[l], output));
			output = new float[tmp.length+1];
			for (i=0; i<tmp.length; i++)
				output[i] = tmp[i];
			output[i] = 1.0f;	// Add the bias
		}

		// Apply activation function
		switch (activation) {
		case ACTIVATION_SIGMOID:
			return Np.sigmoid(Np.mul(weights[l], output));
		case ACTIVATION_TANH:
			return Np.tanh(Np.mul(weights[l], output));
		case ACTIVATION_RELU:
			return Np.relu(Np.mul(weights[l], output));
		default:
			return Np.tanh(Np.mul(weights[l], output));
		}
	}
	
	public void mutate() {
		int totSynapses = weights.length * weights[0].length * weights[0][0].length;
		double mutateProb = 2/totSynapses;
		for (int i=0; i<weights.length; i++) {
			for (int j=0; j<weights[0].length; j++) {
				for (int k=0; k<weights[0][0].length; k++) {
					if (Math.random() < mutateProb)
						weights[i][j][k] = (float) (Math.random()*2 - 1);
				}
			}
		}
	}
}
