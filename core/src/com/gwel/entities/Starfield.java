package com.gwel.entities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gwel.spacegame.MyRenderer;

public class Starfield {
	int nStars;
	int width, height;
	Vector3[] stars;
	Matrix4 transform;

	public Starfield(int width, int height) {
		this.width = width;
		this.height = height;
		nStars = width*height/400;
		stars = new Vector3[nStars];
		for (int i=0; i<nStars; i++) {
			float x = MathUtils.random(width);
			float y = MathUtils.random(height);
			float z = (MathUtils.log(10, MathUtils.random(0.1f, 1))+3)*33; // logarithmic distribution on z axis
			stars[i] = new Vector3(x, y, z);
		}
		transform = new Matrix4().idt();
		transform.translate(-1.0f, -1.0f, 0.0f);
		transform.scale(2.0f/width, 2.0f/height, 1.0f);
	}
	
	public void update(Vector2 travelling) {
		for (int i=0; i<nStars; i++) {
			stars[i].sub(32*travelling.x/stars[i].z, 32*travelling.y/stars[i].z, 0.0f);
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

	public void render(MyRenderer renderer) {
		renderer.setColor(0.4f, 0.5f, 0.0f, 0.6f);
		renderer.setProjectionMatrix(transform);
		float dx = (float) (1.0f*Math.sqrt(renderer.camera.PPU));	// Width
		float dy = (float) (0.5f*Math.sqrt(renderer.camera.PPU));	// Height
		
		for (int i=0; i<nStars; i++) {
			renderer.triangle(stars[i].x-dx, stars[i].y-dy, stars[i].x, stars[i].y+dx, stars[i].x+dx, stars[i].y-dy);
			renderer.triangle(stars[i].x-dx, stars[i].y+dy, stars[i].x, stars[i].y-dx, stars[i].x+dx, stars[i].y+dy);
		}
		renderer.flush();
	}
}