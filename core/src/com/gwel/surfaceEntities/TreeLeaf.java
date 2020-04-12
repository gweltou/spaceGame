package com.gwel.surfaceEntities;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;

class TreeLeaf {
	private float x, y;
	private int angleGroup;
	private float size;
	private float briGlobal;
	private LeafParam lp;
	private TreeSegment seg;
	private WindManager wm;
	private final static Affine2 transform = new Affine2();

	TreeLeaf(TreeSegment seg, LeafParam lp, WindManager wm) {
		this.seg = seg;
		this.lp = lp;
		this.wm = wm;
		angleGroup = (int) Math.floor(MathUtils.random(WindManager.NUM_ANGLES));
		briGlobal = MathUtils.random(0.8f, 1.0f);
		size = 0.1f;
		x = MathUtils.random(-seg.r1-0.1f, seg.r1+0.1f);
		y = MathUtils.random(seg.h+0.1f);
	}

	void render(MyRenderer renderer) {
		transform.idt();
		transform.translate(x, -y);
		//transform.rotateRad(MathUtils.PI + seg.body.getAngle() + -wm.getAngle(angleGroup) + MathUtils.PI2);
		transform.scale(size, size);
		//fill(lp.hue, lp.sat, lp.bri1*briGlobal);
		renderer.pushMatrix(transform);
		System.out.println(lp.vertices);
		for (Vector2[] tri: lp.vertices) {
			
			renderer.triangle(tri[0], tri[1], tri[2]);
		}
		renderer.popMatrix();
	}
}


class LeafParam {
	private int type;
	private int n_triangles;
	public float hue;
	public float sat;
	public float bri1, bri2;
	public Vector2[][] vertices;

	LeafParam() {
		System.out.println("LeafParam created");
		hue = MathUtils.random.nextFloat()*0.4f;
		sat = MathUtils.random.nextFloat()*0.5f + 0.5f;
		bri1 = MathUtils.random.nextFloat()*0.5f + 0.5f;
		bri2 = MathUtils.random.nextFloat();
		type = (int) Math.floor(MathUtils.random.nextFloat()*32f);  // 5 bits random number
		Vector2[][] vertices_tmp = new Vector2[8][3];
		n_triangles = 0;

		float sq_width = MathUtils.random.nextFloat()*0.6f + 0.4f;
		float sq_height = MathUtils.random.nextFloat()*0.8f + 0.2f;
		float up_leaflet_len = MathUtils.random.nextFloat()*0.4f + 0.2f;
		float down_leaflet_len = up_leaflet_len + MathUtils.random.nextFloat()*0.4f + 0.2f;
		float side_leaflet_len = MathUtils.random.nextFloat()*0.9f + 0.1f;
		float updiag_leaflet_len = MathUtils.random.nextFloat()*0.9f + 0.1f;
		float downdiag_leaflet_len = MathUtils.random.nextFloat()*0.9f + 0.1f;

		if ((type & 1) == 0) {  // Type A
			// Center square
			vertices_tmp[n_triangles][0] = new Vector2(-sq_width/2f, sq_height);	// Bottom left
			vertices_tmp[n_triangles][1] = new Vector2(-sq_width/2f, 0.0f); 		// Top left
			vertices_tmp[n_triangles][2] = new Vector2(sq_width/2f, 0.0f); 			// Top right
			n_triangles++;
			vertices_tmp[n_triangles][0] = new Vector2(sq_width/2f, 0.0f); 			// Top right (second triangle)
			vertices_tmp[n_triangles][1] = new Vector2(sq_width/2f, -sq_height);	// Bottom right (second triangle)
			vertices_tmp[n_triangles][2] = new Vector2(-sq_width/2f, -sq_height);	// Bottom left (second triangle)
			n_triangles++;

			// Bottom leaflet
			vertices_tmp[n_triangles][0] = new Vector2(-sq_width/2f, -sq_height);
			vertices_tmp[n_triangles][1] = new Vector2(sq_width/2f, -sq_height);
			vertices_tmp[n_triangles][2] = new Vector2(0.0f, -sq_height - down_leaflet_len/2f);
			n_triangles++;

			if ((type & 2) == 2) {
				// Add top leaflet
				vertices_tmp[n_triangles][0] = new Vector2(-sq_width/2, 0.0f);
				vertices_tmp[n_triangles][1] = new Vector2(0.0f, up_leaflet_len);
				vertices_tmp[n_triangles][2] = new Vector2(sq_width/2, 0.0f);
				n_triangles++;
			}
			if ((type & 4) == 4) {
				// Add side leaflets
				vertices_tmp[n_triangles][0] = new Vector2(-sq_width/2f, -sq_height);
				vertices_tmp[n_triangles][1] = new Vector2(-(side_leaflet_len+sq_width)/2f, -sq_height/2f);
				vertices_tmp[n_triangles][2] = new Vector2(-sq_width/2f, 0.0f);
				n_triangles++;
				vertices_tmp[n_triangles][0] = new Vector2(sq_width/2f, -sq_height);
				vertices_tmp[n_triangles][1] = new Vector2((side_leaflet_len+sq_width)/2f, -sq_height/2);
				vertices_tmp[n_triangles][2] = new Vector2(sq_width/2, 0.0f);
				n_triangles++;
			}

		} else {  // Type B
			vertices_tmp[n_triangles][0] = new Vector2(-sq_width/2f, 0.0f);
			vertices_tmp[n_triangles][1] = new Vector2(sq_width/2f, 0.0f);
			vertices_tmp[n_triangles][2] = new Vector2(0.0f, -down_leaflet_len);
			n_triangles++;

			if ((type & 2) == 2) {
				// Add top leaflet
				vertices_tmp[n_triangles][0] = new Vector2(-sq_width/2f, 0.0f);
				vertices_tmp[n_triangles][1] = new Vector2(0.0f, up_leaflet_len);
				vertices_tmp[n_triangles][2] = new Vector2(sq_width/2f, 0.0f);
				n_triangles++;
			}
			if ((type & 4) == 4) {
				// Add side leaflets
				vertices_tmp[n_triangles][0] = new Vector2(0.0f, sq_height/2);
				vertices_tmp[n_triangles][1] = new Vector2(-side_leaflet_len, 0.0f);
				vertices_tmp[n_triangles][2] = new Vector2(0.0f, -sq_height/2);
				n_triangles++;
				vertices_tmp[n_triangles][0] = new Vector2(0.0f, sq_height/2);
				vertices_tmp[n_triangles][1] = new Vector2(side_leaflet_len, 0.0f);
				vertices_tmp[n_triangles][2] = new Vector2(0.0f, -sq_height/2);
				n_triangles++;
			}
		}

		vertices = new Vector2[n_triangles][3];
		System.arraycopy(vertices_tmp, 0, vertices, 0, n_triangles);
	}
}