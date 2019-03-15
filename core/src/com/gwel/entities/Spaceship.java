package com.gwel.entities;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.gwel.spacegame.MyRenderer;


public class Spaceship extends PhysicBody {
	public final float MAX_VEL = 500.0f;
	public final float MAX_ANG_VEL = 4.0f;
	private final float SCALE = 0.01f;
	
	public float speed_mag;
	Vector2 size = new Vector2(1.5f, 2.0f);  // Size of spaceship in game units
	Vector2 p1;
	Vector2 p2;
	Vector2 p3;
	Vector2 p1_tmp;
	Vector2 p2_tmp;
	Vector2 p3_tmp;
	Affine2 transform;
	float[][] triangles;
	
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

		p1 = new Vector2(-size.x/2.0f, -size.y/2.0f);
		p2 = new Vector2(0.0f, size.y/2.0f);
		p3 = new Vector2(size.x/2.0f, -size.y/2.0f);
		p1_tmp = new Vector2();
		p2_tmp = new Vector2();
		p3_tmp = new Vector2();
		transform = new Affine2();
	}

	public void steer(float amount) {
		if (amount > 0 && body.getAngularVelocity() < MAX_ANG_VEL) {
			body.applyTorque(amount, true);
		} else {
			if (amount < 0 && body.getAngularVelocity() > -MAX_ANG_VEL)
				body.applyTorque(amount, true);
		}
	}

	public void accelerate(float amount) {
		//System.out.println("ship accelerate");
		Vector2 direction = new Vector2(1.0f, 1.0f);
		direction.setAngleRad(getAngle());
		push(direction.scl(amount*1.5f));
		float speed = getSpeed().len2();
		if (speed > MAX_VEL) {
			body.setLinearVelocity(getSpeed().scl(MAX_VEL/speed));
		}
	}

	public Projectile fire() {
		Vector2 force = new Vector2(1.0f, 1.0f);
		force.setAngleRad(getAngle());
		Vector2 pos = this.getPosition();
		pos.add(force); // Translate the projectile starting position
		Projectile proj = new Projectile(pos);
		proj.push(force);
		return proj;
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
			// Scale vertices coordinates
			triangle[0] *= SCALE;
			triangle[1] *= SCALE;
			triangle[2] *= SCALE;
			triangle[3] *= SCALE;
			triangle[4] *= SCALE;
			triangle[5] *= SCALE;
			
			// Normalize color values (last 3 values)
			triangle[6] /= 255;
			triangle[7] /= 255;
			triangle[8] /= 255;
			triangle[9] = 1.0f;
			
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