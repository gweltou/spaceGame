package com.gwel.surfaceEntities;


import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;


public class HeightArray {
	public float span;
	public float[] values;
	public float vpu;
	private float wrap;
	
	public HeightArray(RandomXS128 generator, float span, float vpu, float wrap) {
		// span : length covered by the height array (in game units)
		// vpu	: values per game unit (value density)
		this.span = span;
		this.vpu = vpu;
		this.wrap = wrap;
		int numValues = MathUtils.ceil(span * vpu);
		values = new float[numValues];
		for (int i=0; i<numValues; i++) {
			values[i] = generator.nextFloat();
		}
		System.out.println("span " + span);
		System.out.println("num values " + numValues);
	}
	
	public float getHeight(float x) {
		x %= wrap;
		if (x < 0)	x += wrap;
		// Calculate a decimal index (between 0 and values.length) from x
		float floatIdx = values.length * (x%span) / span;
		// Wrap around if index is negative
		if (floatIdx < 0.0f)	floatIdx += values.length;
	    int il = MathUtils.floor(floatIdx) % values.length;
	    float lVal = values[il];
	    float rVal = values[(il + 1) % values.length];
	    float wr = floatIdx - il;
	    float wl = 1.0f - wr;
	    // interpolate between the 2 indexes
	    return lVal * wl + rVal * wr;
	}
}
