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
	boolean draw_up;
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
		draw_up = true;
		transform = new Matrix4().idt();
		transform.translate(-1.0f, -1.0f, 0.0f);
		transform.scale(2.0f/width, 2.0f/height, 1.0f);
	}
	
	public void update(Vector2 travelling) {
		for (int i=0; i<nStars; i++) {
			stars[i].add(-8*travelling.x/stars[i].z, -8*travelling.y/stars[i].z, 0.0f);
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
		renderer.setColor(1.0f, 1.0f, 1.0f, 0.5f);
		renderer.setProjectionMatrix(transform);
		float dx = 0.04f*renderer.camera.PPU;
		float dy = 0.02f*renderer.camera.PPU;
		
		draw_up = !draw_up;
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
			if (draw_up)
				renderer.triangle(stars[i].x-dx, stars[i].y-dy, stars[i].x, stars[i].y+dx, stars[i].x+dx, stars[i].y-dy);
			else
				renderer.triangle(stars[i].x-dx, stars[i].y+dy, stars[i].x, stars[i].y-dx, stars[i].x+dx, stars[i].y+dy);
		}
		renderer.flush();
	}
}