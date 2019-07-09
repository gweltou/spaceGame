package com.gwel.surfaceEntities;

import java.util.ArrayList;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.utils.Disposable;
import com.gwel.spacegame.MyRenderer;

class TreeSegment implements Disposable {
	public Body body;
	private PolygonShape ps;
	public ArrayList<RevoluteJoint> joints = new ArrayList<RevoluteJoint>();
	public float volume;  // virtual volume of polygon, used to calculate wind friction
	public float surface; // Used to calculate leaf and flower population size
	public int rank;
	public int level;
	public ArrayList<TreeFlower> flowers = new ArrayList<TreeFlower>();
	public ArrayList<TreeLeaf> leaves = new ArrayList<TreeLeaf>();
	public float r1, r2, h;
	private final static Affine2 transform = new Affine2();

	TreeSegment(World world, float x, float y, float angle, float wb, float wt, float h, boolean root, int rank, int level) {
		// x, y : position of bottom-center
		// angle : starting angle
		// wb : bottom width
		// wt : top width
		// h : height
		// root : static body if true
		// rank : number of segments from root to this segment
		// level : branching level of segment

		/*
	     THIS IS A 3D CONE
	     We calculate the volume of the lower truncated cone

	           |----------|
	          /|\         |
	       r2/_|_\ ___    H
	        /  |  \  |h   |
	       /___|___\_| ___|
	        r1
		 */

		this.rank = rank;
		this.level = level;
		this.h = h;  // height
		r1 = wb/2;  // bottom radius
		r2 = wt/2;  // top radius
		volume = h*MathUtils.PI / (3*(r1-r2));
		volume *= Math.pow(r1, 3) - Math.pow(r2, 3);
		surface = 0.5f * h * (r2 + r1);

		BodyDef bd = new BodyDef();
		if (root) {
			bd.type = BodyType.StaticBody;
		} else {
			bd.type = BodyType.DynamicBody;
		}
		bd.position.set(x, y);
		bd.angle = angle;
		body = world.createBody(bd);

		Vector2[] vertices = new Vector2[4];
		vertices[0] = new Vector2(-r1, 0);
		vertices[1] = new Vector2(r1, 0);
		vertices[2] = new Vector2(r2, h);
		vertices[3] = new Vector2(-r2, h);

		ps = new PolygonShape();
		ps.set(vertices);

		FixtureDef fd = new FixtureDef();
		fd.filter.groupIndex = -1;
		fd.shape = ps;
		fd.density = r1/10;
		body.createFixture(fd);
	}

	void render(MyRenderer renderer) {
		transform.idt();
		transform.translate(body.getPosition());
		transform.rotateRad(body.getAngle());
		
		//fill(col, 0.6, 1.0 - ((float) rank) / ExoTree.RANK_LIMIT);
		/*
	    for (int i = 0; i < ps.getVertexCount(); i++) {
	      Vec2 v = box2d.vectorWorldToPixels(ps.getVertex(i));
	      vertex(v.x, v.y);
	    }*/
		renderer.pushMatrix(transform);
		renderer.triangle(new Vector2(-r1, 0f), new Vector2(0f, -r1), new Vector2(r1, 0f));	// base triangle
		renderer.triangle(new Vector2(-r1, 0f), new Vector2(r1, 0f), new Vector2(r2, h));
		renderer.triangle(new Vector2(r2, h), new Vector2(-r2, h), new Vector2(-r1, 0f));
		renderer.popMatrix();
	}

	@Override
	public void dispose() {
		for (RevoluteJoint joint: joints) {
			body.getWorld().destroyJoint(joint);
		}
		body.getWorld().destroyBody(body);
	}
}