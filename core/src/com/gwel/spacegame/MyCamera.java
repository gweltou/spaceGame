package com.gwel.spacegame;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class MyCamera {
	public Vector2 sw;
	public Vector2 ne;
	public Vector2 center;
	private Vector2 pCenter;  // Previous center (in game unit coordinates)
	public boolean autozoom;
	public int width;
	public int height;
	public Affine2 affine;
	public float angle;
	private float finalZoom;	// Camera continuously interpolates toward this values
	private float finalAngle;

	public float PPU = 10.0f ; // Pixel per game unit

	public MyCamera(int width, int height) {
		center = new Vector2();
		autozoom = true;
		this.width = width;
		this.height = height;
		affine = new Affine2();
		finalZoom = PPU;
		angle = 0.0f;
		finalAngle = 0.0f;
	}

	public Vector2 world_to_camera(Vector2 position) {
		Vector2 cam_pos = position.sub(sw);
		cam_pos.scl(PPU);
		return cam_pos;
	}

	void setCenter(Vector2 pos) {
		if (pCenter == null)
			pCenter = pos.cpy();
		else
			pCenter.set(center);
		center.set(pos);
	}

	void translate(float dx, float dy) {
		center.add(dx, dy);
	}
	
	public void glideTo(Vector2 pos) {
		setCenter(center.cpy().lerp(pos, 0.5f));
	}

	public void zoom(float z) {
		this.PPU = MathUtils.clamp(this.PPU*z, 0.01f, 100.0f);
	}

	void setZoom(float ppu) {
		this.PPU = MathUtils.clamp(ppu, 0.01f, 100.0f);
	}

	public void zoomTo(float ppu) {
		setZoom(MathUtils.lerp(PPU, ppu, 0.02f));
	}

	public Vector2 getTravelling() {
		Vector2 travelling = center.cpy().sub(pCenter);
		return travelling.scl(PPU);
	}
	
	public void update() {
		angle = utils.wrapAngleAroundZero(MathUtils.lerp(angle, finalAngle, 0.02f));
		// North and East directions are POSITIVE !
		this.sw = new Vector2(center.x-width/(2.0f*PPU), center.y-height/(2.0f*PPU));
		this.ne = new Vector2(center.x+width/(2.0f*PPU), center.y+height/(2.0f*PPU));
		affine.idt();
		affine.scale(2.0f*PPU/width, 2.0f*PPU/height);
		affine.rotateRad(angle);
		affine.translate(-center.x, -center.y);
	}

	public void rotateTo(float angleRad) {
		finalAngle = angleRad;
	}
	
	public boolean containsPoint(double x, double y) {
		// North and East directions are POSITIVE !
		return (x >= sw.x && x <= ne.x && y <= ne.y && y >= sw.y);
	}
}