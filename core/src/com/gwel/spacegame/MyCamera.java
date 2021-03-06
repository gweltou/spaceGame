package com.gwel.spacegame;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;

public class MyCamera {
	public Vector2 sw;
	public Vector2 ne;
	public final Vector2 center = new Vector2();
	private Vector2 pCenter;  // Previous center (in game unit coordinates)
	public boolean autozoom;
	private float minZoom, maxZoom;
	public int width;
	public int height;
	public Affine2 affine;
	public static Matrix4 normal;
	public float angle;
	private float finalPPU;	// Camera continuously interpolates toward this values
	private float finalAngle;
	public float PPU = 10.0f ; // Pixel per game unit

	public MyCamera(int width, int height) {
		this.width = width;
		this.height = height;
		affine = new Affine2();
		normal = new Matrix4().setToOrtho2D(0, 0, width, height);
		autozoom = true;
		minZoom = 0.02f;
		maxZoom = 100f;
		finalPPU = PPU;
		angle = 0.0f;
		finalAngle = 0.0f;
	}

	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		this.sw = new Vector2(center.x-width/(2.0f*PPU), center.y-height/(2.0f*PPU));
		this.ne = new Vector2(center.x+width/(2.0f*PPU), center.y+height/(2.0f*PPU));
		normal = new Matrix4().setToOrtho2D(0, 0, width, height);
		affine.idt();
		affine.scale(2.0f*PPU/width, 2.0f*PPU/height);
		affine.rotateRad(angle);
		affine.translate(-center.x, -center.y);
	}

	public Vector2 world_to_camera(Vector2 position) {
		Vector2 cam_pos = position.sub(sw);
		cam_pos.scl(PPU);
		return cam_pos;
	}

	public void setCenter(Vector2 pos) {
		if (pCenter == null)
			pCenter = pos.cpy();
		else
			pCenter.set(center);
		center.set(pos);
	}

	public void glideTo(Vector2 pos) {
		setCenter(center.cpy().lerp(pos, 0.5f));
	}

	public void setZoomLimits(float min, float max) {
		minZoom = min;
		maxZoom = max;
	}

	public void zoomTo(float ppu) {
		finalPPU = MathUtils.clamp(ppu, minZoom, maxZoom);
	}
	
	public void zoomIn() {
		zoomTo(finalPPU * 1.06f);
	}
	
	public void zoomOut() {
		zoomTo(finalPPU * 0.92f);
	}
	
	public Vector2 getTravelling() {
		return center.cpy().sub(pCenter);
	}
	
	public void update() {
		angle = utils.wrapAngleAroundZero(MathUtils.lerp(angle, finalAngle, 0.02f));
		PPU = MathUtils.lerp(PPU, finalPPU, 0.1f);
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