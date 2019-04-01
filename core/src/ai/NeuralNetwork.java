package ai;

import java.util.Arrays;

public class NeuralNetwork {
	private float[][][] weights;

	public NeuralNetwork(int[] layers) {
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
			tmp = Np.tanh(Np.mul(weights[l], output));
			output = new float[tmp.length+1];
			for (i=0; i<tmp.length; i++)
				output[i] = tmp[i];
			output[i] = 1.0f;	// Add the bias
		}
		return Np.tanh(Np.mul(weights[l], output));
	}
}
