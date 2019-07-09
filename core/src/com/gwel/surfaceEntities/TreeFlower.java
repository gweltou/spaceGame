package com.gwel.surfaceEntities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;

class TreeFlower {
	float x, y;
	float angle;
	float size;
	private final Color color1 = new Color();
	private final Color color2 = new Color();
	FlowerParam fp;
	private WindManager wm;
	private int angleGroup;
	private static final Affine2 transform = new Affine2();

	TreeFlower(float w, float h, FlowerParam fp, WindManager wm) {
		// w : width radius of spawn
		// h : height radius of spawn
		x = MathUtils.random(-w-0.1f, w+0.1f);
		y = MathUtils.random(h);
		angle = MathUtils.random(MathUtils.PI2);
		size = 1 + MathUtils.random(-fp.sizeVar/2, fp.sizeVar/2);
		float globalBri = MathUtils.random(-0.18f, 0.18f);
		float bri1 = (float) MathUtils.clamp(fp.bri1+globalBri, 0.0f, 1.0f);
		float bri2 = (float) MathUtils.clamp(fp.bri2+globalBri, 0.0f, 1.0f);
		color1.fromHsv(fp.hue1, fp.sat1, bri1);
		color1.a = 1.0f;
		color2.fromHsv(fp.hue2, fp.sat2, bri2);
		color2.a = 1.0f;
		this.fp = fp;
		this.wm = wm;
		angleGroup = (int) Math.floor(MathUtils.random(WindManager.NUM_ANGLES));
	}

	void render(MyRenderer renderer) {
		transform.idt();
		transform.translate(x, -y);
		//transform.rotate(angle+wm.getAngle(angleGroup));
		transform.scale(size, size);
		renderer.setColor(color1);
		for (Vector2[] triangle: fp.vertices1)
			renderer.triangle(triangle[0], triangle[1], triangle[2]);
		renderer.setColor(color2);
		for (Vector2[] triangle: fp.vertices2)
			renderer.triangle(triangle[0], triangle[1], triangle[2]);
	}
}

class FlowerParam {
	public float hue1, hue2;
	public float sat1, sat2;
	public float bri1, bri2;
	public float size;
	public float coreSize;
	public float sizeVar;
	public int shape; // 0: 2 squares, 1: 2 triangles
	public Vector2[][] vertices1;
	public Vector2[][] vertices2;

	FlowerParam(RandomXS128 generator) {
		System.out.println("FlowerParam created");
		hue1 = generator.nextFloat()*360.0f;
		hue2 = generator.nextFloat()*360.0f;
		sat1 = generator.nextFloat();
		sat2 = generator.nextFloat();
		bri1 = generator.nextFloat();
		bri2 = generator.nextFloat();
		size = generator.nextFloat()*0.25f + 0.25f; // Radius of flower
		sizeVar = generator.nextFloat()*1.2f; //120% variation between smallest and largest
		coreSize = generator.nextFloat()*0.8f + 0.2f; // Proportional to size
		shape = (int) Math.floor(generator.nextFloat()*2f);

		switch (shape) {
		case 0:  // 2 squares
			vertices1 = new Vector2[2][3];
			float s = size;
			vertices1[0][0] = new Vector2(-s, -s);	// Bottom left
			vertices1[0][1] = new Vector2(-s, s);	// Top left
			vertices1[0][2] = new Vector2(s, s);	// Top right
			vertices1[1][0] = new Vector2(s, s);  	// Top right (second triangle)
			vertices1[1][1] = new Vector2(s, -s);	// Bottom right (second triangle)
			vertices1[1][2] = new Vector2(-s, -s); 	// Bottom left (second triangle)

			vertices2 = new Vector2[2][3];
			float ca = MathUtils.cos(MathUtils.PI/4);
			float sa = MathUtils.sin(MathUtils.PI/4);
			s = size*coreSize;
			vertices2[0][0] = new Vector2(-s*ca + s*sa, -s*sa - s*ca);	// Bottom left
			vertices2[0][1] = new Vector2(-s*ca - s*sa, -s*sa + s*ca);	// Top left
			vertices2[0][2] = new Vector2(s*ca - s*sa, s*sa + s*ca);	// Top right
			vertices2[1][0] = new Vector2(s*ca - s*sa, s*sa + s*ca);	// Top right (second triangle)
			vertices2[1][1] = new Vector2(s*ca + s*sa, s*sa - s*ca);	// Bottom right (second triangle)
			vertices2[1][2] = new Vector2(-s*ca + s*sa, -s*sa - s*ca);	// Bottom left (second triangle)
			break;

		case 1:  // 2 triangles
			float h = size * 1.4142f;  // We multiply by sq(2) so the size compares to the square flowers
			float hcos_pi6 = h * MathUtils.cos(MathUtils.PI/6);  // Rotation by PI/6
			float hsin_pi6 = h * MathUtils.sin(MathUtils.PI/6);
			vertices1 = new Vector2[1][3];
			vertices1[0][0] = new Vector2(0.0f, h);
			vertices1[0][1] = new Vector2(hcos_pi6, -hsin_pi6);
			vertices1[0][2] = new Vector2(-hcos_pi6, -hsin_pi6);

			vertices2 = new Vector2[1][3];
			vertices2[0][0] = new Vector2(0.0f, -h);
			vertices2[0][1] = new Vector2(-hcos_pi6, hsin_pi6);
			vertices2[0][2] = new Vector2(hcos_pi6, hsin_pi6);
			break;

		default:
			break;
		}
	}
}