package com.gwel.entities;

import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.TimeUtils;
import com.gwel.ai.NeuralNetwork;
import com.gwel.spacegame.Enums;
import com.gwel.spacegame.MyRenderer;


public class DroidShip extends PhysicBody {
	public final float MAX_VEL = 20.0f;
	public final float MAX_ANG_VEL = 4.0f;
	private final float FIRE_COOLDOWN = 200.0f; // In milliseconds
	public final static float SIGHT_DISTANCE = 50.0f;
	private final static int NN_INPUTS = 17;
	public static int[] nnLayers = {14, 28, 28, 28, 4};
	public static Enums activation = Enums.ACTIVATION_TANH;
	
	//public float speed_mag;
	private final Vector2 size = new Vector2(1.7f, 1.8f);  // Size of spaceship in game units
	private Vector2 pPosition;
	
	public float hitpoints;
	private long lastFire;
	private int amunition;
	private float dstCounter = 0; // For NN training
	private int[] scores = new int[5];
	private int iScore = 0;
	
	private Affine2 transform;
	private float[][] triangles;
	private Vector2[] vertices;	// Used to set Box2D bounding shape
	private Vector2 p1_tmp = new Vector2();
	private Vector2 p2_tmp = new Vector2();
	private Vector2 p3_tmp = new Vector2();
	
	public NeuralNetwork nn = null;
	private float[] nnInput;
	private LinkedList<Projectile> projectiles;
	
	
	public DroidShip(Vector2 position, float angle, LinkedList<Projectile> projectiles) {
		super(position, angle);
		this.projectiles = projectiles;
		
		transform = new Affine2();
		vertices = new Vector2[4];
		
		nnInput = new float[NN_INPUTS];
		resetVars();
		
		readShapeFromFile();
	}

	public void update() {
		pPosition = position;
		position = body.getPosition().cpy();
		dstCounter += position.dst(pPosition);		
		
		float[] output;
		output = nn.feedforward(nnInput, activation);
		// reset nnInput to 0
		for (int i=0; i<nnInput.length; i++)
			nnInput[i] = 0.0f;
		
		float max = -1.0f;
		int maxIdx = 0;
		for (int i=0; i<output.length; i++) {
			if (output[i] > max) {
				max = output[i];
				maxIdx = i;
			}
		}
		
		switch (maxIdx) {
			case 0: steer(1.0f);
					break;
			case 1:	steer(-1.0f);
					break;
			case 2: accelerate(1.0f);
					break;
			case 3:	fire(projectiles);
					break;
		}
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
		if (hitpoints <= 0.0) {
			//System.out.println("Droid died");
			disposable = true;
		}
	}
	
	public void fire(LinkedList<Projectile> projectiles) {
		if (amunition == 0) 
			return;
		
		long now = TimeUtils.millis();
		if (now-lastFire >= FIRE_COOLDOWN) {
			Vector2 dir = new Vector2(2.0f, 0.0f); // Here we set the bullet's velocity
			dir.setAngleRad(getAngle());
			Vector2 pos = this.getPosition();
			Projectile proj = new Projectile(this, pos, dir, 10.0f);
			projectiles.add(proj);
			lastFire = now;
			amunition--;
		}
	}
	
	public void initBody(World world) {
		super.initBody(world);
		
		// Main colliding shape
		PolygonShape shape = new PolygonShape();
		shape.set(vertices);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.density = 0.1f; 
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.6f;
		fixtureDef.filter.categoryBits = 0x0002;
		Fixture fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.DROID);
		shape.dispose();
		
		// Create sensors
		Vector2 sight = new Vector2(SIGHT_DISTANCE, 0f); //.rotateRad(MathUtils.PI/2f);
		PolygonShape coneSensor = new PolygonShape();
		Vector2[] verts = new Vector2[3];
		verts[0] = new Vector2(0f ,0f);
		
		// Front sensor
		verts[1] = sight.cpy().rotateRad(-MathUtils.PI/16f);
		verts[2] = sight.cpy().rotateRad(MathUtils.PI/16f);
		coneSensor.set(verts);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = coneSensor;
		fixtureDef.isSensor = true;
		fixtureDef.filter.groupIndex = -1;
		fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.SENSOR_F);
		
		// Front Symmetrical Sensors
		sight.scl(0.8f);
		verts[1] = sight.cpy().rotateRad(MathUtils.PI/16f);
		verts[2] = sight.cpy().rotateRad(4*MathUtils.PI/16f);
		coneSensor.set(verts);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = coneSensor;
		fixtureDef.isSensor = true;
		fixtureDef.filter.groupIndex = -1;
		fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.SENSOR_FL);
		
		verts[1] = sight.cpy().rotateRad(-MathUtils.PI/16f);
		verts[2] = sight.cpy().rotateRad(-4*MathUtils.PI/16f);
		coneSensor.set(verts);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = coneSensor;
		fixtureDef.isSensor = true;
		fixtureDef.filter.groupIndex = -1;
		fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.SENSOR_FR);
		
		// Middle Symmetrical Sensors
		sight.scl(0.8f);
		verts[1] = sight.cpy().rotateRad(4*MathUtils.PI/16f);
		verts[2] = sight.cpy().rotateRad(8*MathUtils.PI/16f);
		coneSensor.set(verts);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = coneSensor;
		fixtureDef.isSensor = true;
		fixtureDef.filter.groupIndex = -1;
		fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.SENSOR_ML);

		verts[1] = sight.cpy().rotateRad(-4*MathUtils.PI/16f);
		verts[2] = sight.cpy().rotateRad(-8*MathUtils.PI/16f);
		coneSensor.set(verts);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = coneSensor;
		fixtureDef.isSensor = true;
		fixtureDef.filter.groupIndex = -1;
		fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.SENSOR_MR);
		
		// Rear Symmetrical Sensors
		sight.scl(0.8f);
		verts[1] = sight.cpy().rotateRad(8*MathUtils.PI/16f);
		verts[2] = sight.cpy().rotateRad(14*MathUtils.PI/16f);
		coneSensor.set(verts);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = coneSensor;
		fixtureDef.isSensor = true;
		fixtureDef.filter.groupIndex = -1;
		fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.SENSOR_BL);

		verts[1] = sight.cpy().rotateRad(-8*MathUtils.PI/16f);
		verts[2] = sight.cpy().rotateRad(-14*MathUtils.PI/16f);
		coneSensor.set(verts);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = coneSensor;
		fixtureDef.isSensor = true;
		fixtureDef.filter.groupIndex = -1;
		fixture = body.createFixture(fixtureDef);
		fixture.setUserData(Enums.SENSOR_BR);
		
		coneSensor.dispose();
	}
	
	public void initNN() {
		nn = new NeuralNetwork();
		nn.random(nnLayers);
	}
	public void initNN(NeuralNetwork n) {
		// Copy another neural network into this
		nn = new NeuralNetwork(n);
	}
	
	public void setSensor(Enums sensor, float distance) {
		distance = Math.min(distance / SIGHT_DISTANCE, 1.0f); // Normalize distance
		switch(sensor) {
		// OBSTACLE SENSORS
		case SENSOR_BR:
			nnInput[0] = Math.max(1-distance, nnInput[0]);
			break;
		case SENSOR_MR:
			nnInput[1] = Math.max(1-distance, nnInput[1]);
			break;
		case SENSOR_FR:
			nnInput[2] = Math.max(1-distance, nnInput[2]);
			break;
		case SENSOR_F:
			nnInput[3] = Math.max(1-distance, nnInput[3]);
			break;
		case SENSOR_FL:
			nnInput[4] = Math.max(1-distance, nnInput[4]);
			break;
		case SENSOR_ML:
			nnInput[5] = Math.max(1-distance, nnInput[5]);
			break;
		case SENSOR_BL:
			nnInput[6] = Math.max(1-distance, nnInput[6]);
			break;
		// SHIP SENSORS
		case SENSOR_SBR:
			nnInput[7] = Math.max(1-distance, nnInput[7]);
			break;
		case SENSOR_SMR:
			nnInput[8] = Math.max(1-distance, nnInput[8]);
			break;
		case SENSOR_SFR:
			nnInput[9] = Math.max(1-distance, nnInput[9]);
			break;
		case SENSOR_SF:
			nnInput[10] = Math.max(1-distance, nnInput[10]);
			break;
		case SENSOR_SFL:
			nnInput[11] = Math.max(1-distance, nnInput[11]);
			break;
		case SENSOR_SML:
			nnInput[12] = Math.max(1-distance, nnInput[12]);
			break;
		case SENSOR_SBL:
			nnInput[13] = Math.max(1-distance, nnInput[13]);
			break;
		default:
			break;
		}
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

	@Override
	public float getBoundingRadius() {
		return size.y / 2.0f;
	}
	
	public void setScore(int steps) {
		if (iScore == scores.length) {
			// Scores table is full
			for (int i=0; i<scores.length-1; i++) {
				// Shift scores to the left
				scores[i] = scores[i+1];
			}
			scores[iScore-1] = (int) (steps + amunition + hitpoints + dstCounter);
		} else {
			scores[iScore++] = (int) (steps + amunition + hitpoints + dstCounter);
		}
	}
	
	public int getScore() {
		int totScore = 0;
		for (int i=0; i<iScore; i++)
			totScore += scores[i];
		return totScore/iScore;
	}
	
	public void resetVars() {
		hitpoints = 200;
		amunition = 100;
		dstCounter = 0.0f;
		lastFire = 0;
	}

	public DroidShip copy() {
		DroidShip newDroid = new DroidShip(getPosition(), getAngle(), projectiles);
		newDroid.initNN(this.nn);
		return newDroid;
	}
}