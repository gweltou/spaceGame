package Entities;

import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;

public abstract class PhysicBody {
	public Vector2 position;
	Vector2 pposition;  // Previous position
	public Vector2 speed;
	public Vector2 accel;
	public float mass;

	PhysicBody() {
		position = new Vector2();
		pposition = new Vector2();
		speed = new Vector2(0.0f, 0.0f);
		accel = new Vector2(0.0f, 0.0f);
	}

	public void push(Vector2 force) {
		accel.add(force.cpy().scl(1.0f/mass));
	}

	public void update() {
		speed.add(accel);
		accel.set(0.0f, 0.0f);
		pposition.set(position);
		position.add(speed);
	}

	public abstract void render(MyRenderer renderer);
}
