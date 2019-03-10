package Entities;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.gwel.spacegame.MyRenderer;


public class Spaceship extends PhysicBody {
	public float MAX_SPEED = 1.0f;
	//PShape sprite;
	float angle;
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

	public Spaceship(Vector2 startPos) {
		super();
		//this.sprite = loadShape("spaceship.svg");
		this.position.set(startPos);
		this.angle = MathUtils.PI/2;
		this.speed_mag = 0.0f;
		this.mass = 0.1f;
		p1 = new Vector2(-size.x/2.0f, -size.y/2.0f);
		p2 = new Vector2(0.0f, size.y/2.0f);
		p3 = new Vector2(size.x/2.0f, -size.y/2.0f);
		p1_tmp = new Vector2();
		p2_tmp = new Vector2();
		p3_tmp = new Vector2();
		transform = new Affine2();

		readShapeFromFile();
	}

	public void steer(float amount) {
		this.angle += amount;
		if (angle<0)
			this.angle += MathUtils.PI2;
		if (angle>MathUtils.PI2)
			this.angle -= MathUtils.PI2;
	}

	public void accelerate(float amount) {
		//System.out.println("ship accelerate");
		Vector2 direction = new Vector2(1.0f, 1.0f);
		direction.setAngleRad(angle);
		this.accel.add(direction.scl(amount));
	}

	public Projectile fire() {
		Vector2 force = new Vector2(1.0f, 1.0f);
		force.setAngleRad(angle);
		Vector2 pos = position.cpy();
		pos.add(force); // Translate the projectile starting position
		Projectile proj = new Projectile(pos);
		proj.push(force);
		return proj;
	}

	@Override
	public void update() {
		this.speed.add(accel);
		this.speed.limit(MAX_SPEED);
		this.speed_mag = speed.len2();
		this.accel.set(0.0f, 0.0f);
		this.position.add(speed);

		transform.idt();
		transform.translate(position.x, position.y);
		transform.rotateRad(angle + MathUtils.PI/2);
		transform.scale(0.01f, 0.01f);
	}

	private void readShapeFromFile() {
		FileHandle file = Gdx.files.internal("svg/ship.tdat");
		String text = file.readString();

		String[] linesArray = text.split("\n");
		triangles = new float[linesArray.length][9];
		for (int j=0; j<linesArray.length; j++) {
			String[] values = linesArray[j].split("\t");
			float[] triangle = new float[9];
			int ii=0;
			for (int i=0; i<values.length;i++) {
				if (!values[i].isEmpty()) {
					triangle[ii++] = Float.parseFloat(values[i]);
				}
			}
			triangle[6] /= 255;
			triangle[7] /= 255;
			triangle[8] /= 255;
			triangles[j] = triangle;
		}
	}

	public void render(MyRenderer renderer) {
		//Vector2 pos = cam.world_to_camera(position);
		//System.out.println("Rendering ship");

		for (int i=0; i<triangles.length; i++) {
			p1_tmp.set(triangles[i][0], triangles[i][1]);
			transform.applyTo(p1_tmp);
			p2_tmp.set(triangles[i][2], triangles[i][3]);
			transform.applyTo(p2_tmp);
			p3_tmp.set(triangles[i][4], triangles[i][5]);
			transform.applyTo(p3_tmp);
			renderer.setColor(triangles[i][6], triangles[i][7], triangles[i][8], 1);
			renderer.triangle(p1_tmp, p2_tmp, p3_tmp);
		}
	}
}