package com.gwel.entities;


import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.TimeUtils;
import com.gwel.spacegame.MyRenderer;


public class Spaceship extends PhysicBody {
	public final float MAX_VEL = 50.0f;
	public final float MAX_ANG_VEL = 6.0f;
	private final float FIRE_COOLDOWN = 200.0f; // In milliseconds
	
	public float speed_mag;
	private Vector2 size = new Vector2(1.7f, 1.8f);  // Size of spaceship in game units
	private Vector2 p1_tmp;
	private Vector2 p2_tmp;
	private Vector2 p3_tmp;
	private Affine2 transform;
	float[][] triangles;
	
	private float hitpoints;
	private long last_fire;
	
	public Spaceship(World world, Vector2 pos) {
		super(pos);
		bodyDef.angle = (float) (Math.PI/2.0f); // Initially pointing up
		body = world.createBody(bodyDef);
		body.setUserData(this);
		PolygonShape shape = readShapeFromFile();
		// Create a fixture definition to apply our shape to
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.density = 0.1f; 
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.6f;
		fixtureDef.filter.categoryBits = 0x0002;
		Fixture fixture = body.createFixture(fixtureDef);
		fixture.setUserData("Ship");
		shape.dispose();

		p1_tmp = new Vector2();
		p2_tmp = new Vector2();
		p3_tmp = new Vector2();
		transform = new Affine2();
		
		hitpoints = 200.0f;
		last_fire = TimeUtils.millis();
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
		push(direction.scl(amount*8.0f));
		float speed = getSpeed().len2();
		if (speed > MAX_VEL) {
			body.setLinearVelocity(getSpeed().limit(MAX_VEL));
		}
	}

	public void hit(float hp) {
		hitpoints -= hp;
		System.out.println(hitpoints);
	}
	
	public void fire(LinkedList<Projectile> projectiles) {
		long now = TimeUtils.millis();
		if (now-last_fire >= FIRE_COOLDOWN) {
			Vector2 dir = new Vector2(2.0f, 0.0f); // Here we set the bullet's velocity
			dir.setAngleRad(getAngle());
			Vector2 pos = this.getPosition();
			Projectile proj = new Projectile(this, pos, dir, 10.0f);
			projectiles.add(proj);
			last_fire = now;
		}
	}

	private PolygonShape readShapeFromFile() {
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
		transform.applyTo(highest);
		transform.applyTo(lowest);
		transform.applyTo(leftmost);
		transform.applyTo(rightmost);
		
		// Polygon used for box2d
		Vector2[] vertices = {highest, leftmost, lowest, rightmost};
		PolygonShape shape = new PolygonShape();
		shape.set(vertices);
		return shape;
	}

	public void render(MyRenderer renderer) {
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
}