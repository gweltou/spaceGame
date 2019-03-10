package com.gwel.spacegame;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class MyCamera {
	Vector2 sw;
	Vector2 ne;
	Vector2 center;
	Vector2 pCenter;  // Previous center (in game unit coordinates)
	boolean autozoom;
	public int width;
	public int height;
	public final Matrix4 projection;
	public final Matrix4 view;
	public final Matrix4 combined;
	private final Vector3 tmp;
	private final Vector3 direction;
	private final Vector3 up;
	Affine2 affine;

	float PPU = 10.0f ; // Pixel per game unit

	public MyCamera(int width, int height) {
		this.center = new Vector2();
		this.pCenter = new Vector2();
		this.autozoom = true;
		this.width = width;
		this.height = height;
		
		projection = new Matrix4();
		view = new Matrix4();
		combined = new Matrix4();
		tmp = new Vector3();
		direction = new Vector3(0, 0, -1);
		up = new Vector3(0, 1, 0);
		affine = new Affine2();
	}

	public Vector2 world_to_camera(Vector2 position) {
		Vector2 cam_pos = position.sub(sw);
		cam_pos.scl(PPU);
		return cam_pos;
	}

	void setCenter(Vector2 pos) {
		this.pCenter.set(center);
		this.center.set(pos);
	}

	void translate(float dx, float dy) {
		center.add(dx, dy);
	}
	
	void glideTo(Vector2 pos) {
		setCenter(center.cpy().lerp(pos, 0.5f));
	}

	void zoom(float z) {
		this.PPU = MathUtils.clamp(this.PPU*z, 0.01f, 80.0f);
	}

	void setZoom(float ppu) {
		this.PPU = MathUtils.clamp(ppu, 0.01f, 80.0f);
	}

	void zoomTo(float ppu) {
		setZoom(MathUtils.lerp(PPU, ppu, 0.02f));
	}

	Vector2 getTravelling() {
		Vector2 travelling = center.cpy().sub(pCenter);
		return travelling.scl(PPU);
	}
	
	void update() {
		// North and East directions are POSITIVE !
		this.sw = new Vector2(center.x-width/(2.0f*PPU), center.y-height/(2.0f*PPU));
		this.ne = new Vector2(center.x+width/(2.0f*PPU), center.y+height/(2.0f*PPU));
		projection.setToOrtho(-PPU*width/2, PPU*width/2, PPU*height/2, -PPU*height/2, -1, 1);
		view.setToLookAt(new Vector3(center, 0.0f), tmp.set(center, 0.0f).add(direction), up);
		combined.set(projection);
		Matrix4.mul(combined.val, view.val);
		
		affine.idt();
		affine.scale(2.0f*PPU/width, 2.0f*PPU/height);
		affine.translate(-center.x, -center.y);
		
	}
}