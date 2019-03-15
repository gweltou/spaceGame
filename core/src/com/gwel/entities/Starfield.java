package com.gwel.entities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gwel.spacegame.MyRenderer;

public class Starfield {
	int nStars;
	int width, height;
	Vector3[] stars = new Vector3[nStars];

	Starfield(int width, int height) {
		this.width = width;
		this.height = height;
		nStars = width*height/300;
		for (int i=0; i<nStars; i++) {
			float x = MathUtils.random(width);
			float y = MathUtils.random(height);
			float z = (MathUtils.log(2, MathUtils.random(0.1f, 1))+3)*33; // logarithmic distribution on z axis
			stars[i] = new Vector3(x, y, z);
		}
	}

	void update(Vector2 travelling) {
		for (int i=0; i<nStars; i++) {
			stars[i].add(new Vector3(travelling.scl(2.0f/-stars[i].z), 0.0f));
			if (stars[i].x > width)
				stars[i].x -= width;
			if (stars[i].x < 0)
				stars[i].x += width;
			if (stars[i].y > height)
				stars[i].y -= height;
			if (stars[i].y < 0)
				stars[i].y += height;
		}
	}

	void render(MyRenderer renderer) {
		for (int i=0; i<nStars; i++) {
			/*
			if (stars[i].z > 65)
				stroke(int(MathUtils.random(255)));
			else
				stroke(255-stars[i].z);
			float size = constrain(2*cam.PPU/stars[i].z, 1, 5);
			strokeWeight(size);
			point(stars[i].x, stars[i].y);
			*/
		}
	}
}