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
	public Body body = null;
	private PolygonShape ps;
	public ArrayList<RevoluteJoint> joints = new ArrayList<RevoluteJoint>();
	public float volume;  // virtual volume of polygon, used to calculate wind friction
	public float surface; // Used to calculate leaf and flower population size
	public int rank;
	public int level;
	private Vector2 position;
	private float angle;
	private Vector2[] vertices = new Vector2[4];
	public ArrayList<TreeFlower> flowers = new ArrayList<TreeFlower>();
	public ArrayList<TreeLeaf> leaves = new ArrayList<TreeLeaf>();
	public float r1, r2, h;
	private final static Affine2 transform = new Affine2();
	private boolean isRoot;

	TreeSegment(float x, float y, float angle, float wb, float wt, float h, boolean root, int rank, int level) {
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

	           p5 |------------|
	             /|\           |
	    r2----p4/_|_\p3____    H
	           /  |  \    |h   |
	    r1--p0/___o___\p1_| ___|

		 */

		this.rank = rank;
		this.level = level;
		this.h = h;  // height
		this.position = new Vector2(x, y);
		this.angle = angle;
		isRoot = root;
		r1 = wb/2;  // bottom radius
		r2 = wt/2;  // top radius
		volume = h*MathUtils.PI / (3*(r1-r2));
		volume *= Math.pow(r1, 3) - Math.pow(r2, 3);
		surface = 0.5f * h * (r2 + r1);

		vertices[0] = new Vector2(-r1, 0f);	// p0
		vertices[1] = new Vector2(r1, 0f);	// p1
		vertices[2] = new Vector2(r2, h);		// p3
		vertices[3] = new Vector2(-r2, h);		// p4
	}

	public void initBody(World world) {
		/* Initialize physics body
		 */
		BodyDef bd = new BodyDef();
		if (isRoot) {
			bd.type = BodyType.StaticBody;
		} else {
			bd.type = BodyType.DynamicBody;
		}
		bd.position.set(position);
		bd.angle = angle;
		body = world.createBody(bd);

		ps = new PolygonShape();
		ps.set(vertices);

		FixtureDef fd = new FixtureDef();
		fd.filter.groupIndex = -1;
		fd.filter.maskBits = 0; // Collides with nothing
		fd.shape = ps;
		fd.density = r1/10;
		body.createFixture(fd);
	}

	public Vector2 getPosition() {
		if (body == null) {
			return position.cpy();
		}
		return body.getPosition();
	}

	public float getAngle() {
		if (body == null) {
			return angle;
		}
		return body.getAngle();
	}

	void render(MyRenderer renderer) {
		transform.idt();
		if (body == null) {
			transform.translate(position);
			transform.rotateRad(angle);
		} else {
			transform.translate(body.getPosition());
			transform.rotateRad(body.getAngle());
		}
		
		//fill(col, 0.6, 1.0 - ((float) rank) / ExoTree.RANK_LIMIT);
		renderer.pushMatrix(transform);
		renderer.triangle(vertices[0], new Vector2(0f, -r1), vertices[1]);	// base triangle
		renderer.triangle(vertices[0], vertices[1], vertices[2]);
		renderer.triangle(vertices[2], vertices[3], vertices[0]);
		renderer.triangle(vertices[2], new Vector2(0f, h+r2), vertices[3]);
		renderer.popMatrix();
	}

	@Override
	public void dispose() {
		if (body != null) {
			for (RevoluteJoint joint : joints) {
				body.getWorld().destroyJoint(joint);
			}
			body.getWorld().destroyBody(body);
		}
	}
}