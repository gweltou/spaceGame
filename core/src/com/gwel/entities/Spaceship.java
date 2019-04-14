package com.gwel.entities;


import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.TimeUtils;
import com.gwel.spacegame.MyRenderer;
import com.gwel.spacegame.Enums;


public class Spaceship extends PhysicBody {
	public final float MAX_VEL = 20.0f;
	public final float MAX_ANG_VEL = 4.0f;
	private final float FIRE_COOLDOWN = 200.0f; // In milliseconds
	
	//public float speed_mag;
	private Vector2 size = new Vector2(1.7f, 1.8f);  // Size of spaceship in game units
	private float hitpoints;
	private long last_fire;
	private Affine2 transform;
	private float[][] triangles;
	private Vector2[] vertices;	// Used to set Box2D bounding shape
	private Vector2 p1_tmp = new Vector2();
	private Vector2 p2_tmp = new Vector2();
	private Vector2 p3_tmp = new Vector2();
	private ShipTail tail1, tail2;
	
	
	public Spaceship(Vector2 pos) {
		super(pos, (float) (Math.PI/2.0f)); // Initially pointing up
		
		transform = new Affine2();
		vertices = new Vector2[4];
		
		hitpoints = 200.0f;
		last_fire = TimeUtils.millis();
		tail1 = new ShipTail(this, new Vector2(0.7f, 0.08f), 0.2f, 512, new Color(0xBF3FBFFF), new Color(0x3FBF3FFF));
		tail2 = new ShipTail(this, new Vector2(-0.7f, 0.08f), 0.2f, 512, new Color(0xBF3FBFFF), new Color(0x3FBF3FFF));
		
		readShapeFromFile();
		disposable = false;
	}

	public void steer(float amount) {
		if (amount > 0 && body.getAngularVelocity() < MAX_ANG_VEL) {
			body.applyTorque(amount, true);
		} else if (amount < 0 && body.getAngularVelocity() > -MAX_ANG_VEL) {
			body.applyTorque(amount, true);
		}
	}

	public void accelerate(float amount) {
		Vector2 direction = new Vector2(1.0f, 1.0f);
		direction.setAngleRad(getAngle());
		push(direction.scl(amount*4.0f));
		float speed = getVelocity().len2();
		if (speed > MAX_VEL) {
			body.setLinearVelocity(getVelocity().limit(MAX_VEL));
		}
	}

	public void hit(float hp) {
		hitpoints -= hp;
		if (hitpoints <= 0.0)
			disposable = true;
	}
	
	public void fire(LinkedList<Projectile> projectiles) {
		long now = TimeUtils.millis();
		if (now-last_fire >= FIRE_COOLDOWN) {
			Vector2 dir = new Vector2(2.0f, 0.0f); // Here we set the bullet's velocity
			dir.setAngleRad(getAngle());
			Vector2 pos = this.getPosition();
			Projectile proj = new Projectile(this, pos, dir, 50.0f);
			projectiles.add(proj);
			last_fire = now;
		}
	}
	
	public void initBody(World world) {
		super.initBody(world);
		
		PolygonShape shape = new PolygonShape();
		shape.set(vertices);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.density = 0.1f; 
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.6f;
		fixtureDef.filter.categoryBits = 0x0002;
		Fixture fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.SHIP);
		shape.dispose();
	}

	private void readShapeFromFile() {
		// Extreme vertices used for B2D collision shape
		Vector2 highest = new Vector2(0.0f, -1.0f);
		Vector2 lowest = new Vector2(0.0f, 1.0f);
		Vector2 leftmost = new Vector2(1.0f, 0.0f);
		Vector2 rightmost = new Vector2(-1.0f, 0.0f);
		
		FileHandle file = Gdx.files.internal("svg/ship.tdat");
		String text = file.readString();

		String[] linesArray = text.split("\n");
		triangles = new float[linesArray.length][10];
		for (int j=0; j<linesArray.length; j++) {
			String[] values = linesArray[j].split("\t");
			float[] triangle = new float[10];
			int ii=0;
			for (int i=0; i<values.length;i++) {
				if (!values[i].isEmpty()) {
					triangle[ii++] = Float.parseFloat(values[i]);
				}
			}

			// Normalize color values (last 3 values)
			triangle[6] /= 255;
			triangle[7] /= 255;
			triangle[8] /= 255;
			triangle[9] = 1.0f; // Add alpha value
			
			// Look for extreme vertices values
			for (int i=0; i<6; i+=2) {
				if (triangle[i] < leftmost.x)
					leftmost.set(triangle[i], triangle[i+1]);
				if (triangle[i] > rightmost.x)
					rightmost.set(triangle[i], triangle[i+1]);
				if (triangle[i+1] < lowest.y)
					lowest.set(triangle[i], triangle[i+1]);
				if (triangle[i+1] > highest.y)
					highest.set(triangle[i], triangle[i+1]);
			}
			triangles[j] = triangle;
		}
		
		// Correct all coordinates so ship is of right size and centered at 0,0
		float width = rightmost.x - leftmost.x;
		float height = highest.y - lowest.y;
		// transformation are in inverse order
		Affine2 transform = new Affine2().idt();
		transform.translate(-size.x/2f, -size.y/2f);
		transform.scale(size.x/width, size.y/height);
		transform.translate(-leftmost.x, -lowest.y);
		
		for (float[] triangle: triangles) {
			for (int i=0; i<6; i+=2) {
				Vector2 point = new Vector2(triangle[i], triangle[i+1]);
				transform.applyTo(point);
				triangle[i] = point.x;
				triangle[i+1] = point.y;
			}
		}
		
		// Anti-clockwise
		vertices[0] = highest;
		vertices[1] = leftmost;
		vertices[2] = lowest;
		vertices[3] = rightmost;
		for (Vector2 vert: vertices) {
			transform.applyTo(vert);
		}
	}

	public void render(MyRenderer renderer) {
		tail1.update();
		tail2.update();
		tail1.render(renderer);
		tail2.render(renderer);
		
		transform.idt();
		transform.translate(getPosition());
		transform.rotateRad(getAngle() + MathUtils.PI/2);
		for (int i=0; i<triangles.length; i++) {
			p1_tmp.set(triangles[i][0], triangles[i][1]);
			transform.applyTo(p1_tmp);
			p2_tmp.set(triangles[i][2], triangles[i][3]);
			transform.applyTo(p2_tmp);
			p3_tmp.set(triangles[i][4], triangles[i][5]);
			transform.applyTo(p3_tmp);
			renderer.setColor(triangles[i][6], triangles[i][7], triangles[i][8], triangles[i][9]);
			renderer.triangle(p1_tmp, p2_tmp, p3_tmp);
		}
	}

	public float getBoundingRadius() {
		return size.y / 2f;
	}
}