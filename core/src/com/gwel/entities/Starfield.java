package com.gwel.entities;

import com.badlogic.gdx.graphics.Color;
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
	private float speedScale;
	private float sizeScale;
	private static final Color color = new Color();

	public Starfield(int width, int height, float density, float speedScale, float sizeScale) {
		this.width = width;
		this.height = height;
		this.speedScale = speedScale;
		this.sizeScale = sizeScale;
		nStars = (int) (width*height*density);
		stars = new Vector3[nStars];
		for (int i=0; i<nStars; i++) {
			float x = MathUtils.random(width);
			float y = MathUtils.random(height);
			float z = MathUtils.random();
			//float z = (MathUtils.log(10, MathUtils.random(0.1f, 1))+3)*33; // logarithmic distribution on z axis
			stars[i] = new Vector3(x, y, z);
		}
		transform = new Matrix4().idt();
		transform.translate(-1.0f, -1.0f, 0.0f);
		transform.scale(2.0f/width, 2.0f/height, 1.0f);
	}
	
	public void update(Vector2 travelling, float factor) {
		travelling.scl(factor);
		for (int i=0; i<nStars; i++) {
			stars[i].sub(stars[i].z*travelling.x*speedScale*speedScale, stars[i].z*travelling.y*speedScale*speedScale, 0.0f);
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

	public void render(MyRenderer renderer, float hue) {
		renderer.setProjectionMatrix(transform);
		float dh = (float) (1.0f*Math.sqrt(renderer.camera.PPU)*sizeScale*sizeScale);	// Width
		float dv = (float) (0.5f*Math.sqrt(renderer.camera.PPU)*sizeScale*sizeScale);	// Height
		color.fromHsv(hue, 0.5f, 0.8f);
		
		for (int i=0; i<nStars; i++) {
			color.a = 1f-stars[i].z;
			renderer.setColor(color);
			float dx = dh * (stars[i].z)/1;
			float dy = dv * (stars[i].z)/1;
			renderer.triangle(stars[i].x-dx, stars[i].y-dy, stars[i].x, stars[i].y+dx, stars[i].x+dx, stars[i].y-dy);
			renderer.triangle(stars[i].x-dx, stars[i].y+dy, stars[i].x, stars[i].y-dx, stars[i].x+dx, stars[i].y+dy);
		}
		renderer.flush();
	}
}