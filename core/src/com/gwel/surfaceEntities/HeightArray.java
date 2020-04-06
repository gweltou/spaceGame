package com.gwel.surfaceEntities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;

public class HeightArray {
	public float span;
	public float[] values;
	public float vpu;
	private float amp;

	public HeightArray(float span, float vpu, float amp) {
		// span : length covered by the height array (in game units)
		// vpu	: values per game unit (value density)
		this.span = span;
		this.vpu = vpu;
		this.amp = amp;

		int numValues = MathUtils.ceil(span * vpu);
		values = new float[numValues];
		for (int i=0; i<numValues; i++) {
			values[i] = MathUtils.random.nextFloat() * amp;
		}
		System.out.print("span " + span);
		System.out.print("  num values " + numValues);
		System.out.println("  amp: " + amp);
	}
	
	public float getHeight(float x) {
		// x : a horizontal coordinate on planet surface (can be greater than planet length)
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
