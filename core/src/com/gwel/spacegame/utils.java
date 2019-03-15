package com.gwel.spacegame;

import com.badlogic.gdx.math.MathUtils;

public final class utils {
	public static float wrapAngleAroundZero (float a) {
		if (a >= 0) {
			float rotation = a % MathUtils.PI2;
			if (rotation > MathUtils.PI) rotation -= MathUtils.PI2;
			return rotation;
		} else {
			float rotation = -a % MathUtils.PI2;
			if (rotation > MathUtils.PI) rotation -= MathUtils.PI2;
			return -rotation;
		}
}
}
