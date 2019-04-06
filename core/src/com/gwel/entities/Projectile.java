package com.gwel.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.gwel.spacegame.MyRenderer;
import com.gwel.spacegame.Enum;

public class Projectile {
	private MovingObject parent;
	public Vector2 position;
	public Vector2 pPosition;
	private Vector2 speed;
	private Vector2 speedX4;
	private Vector2 perpendicular;
	private float damage;
	public boolean disposable;
	
	public Projectile(MovingObject parent, Vector2 pos, Vector2 speed, float damage) {
		this.parent = parent;
		disposable = false;
		position = pos.cpy();
		this.speed = speed.cpy();
		this.speedX4 = speed.cpy().scl(4f);
		this.damage = damage;
		perpendicular = new Vector2(-speed.y, speed.x).nor().scl(0.08f);
		parent.push(speed.cpy().scl(-1)); // Push back
	}

	public void update(World world, float game_speed) {
		pPosition = position.cpy();
		position.add(speed.x*game_speed, speed.y*game_speed);
		
		world.rayCast(rcCallback, pPosition, position);
	}
	
	private RayCastCallback rcCallback = new RayCastCallback() {
		@Override
		public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
			// Filter out sensors
			if (fixture.getUserData() == Enum.SENSOR_F ||
					fixture.getUserData() == Enum.SENSOR_FL ||
					fixture.getUserData() == Enum.SENSOR_FR ||
					fixture.getUserData() == Enum.SENSOR_ML ||
					fixture.getUserData() == Enum.SENSOR_MR ||
					fixture.getUserData() == Enum.SENSOR_BL ||
					fixture.getUserData() == Enum.SENSOR_BR ||
					fixture.getUserData() == Enum.SENSOR_SFL ||
					fixture.getUserData() == Enum.SENSOR_SFR ||
					fixture.getUserData() == Enum.SENSOR_SML ||
					fixture.getUserData() == Enum.SENSOR_SMR ||
					fixture.getUserData() == Enum.SENSOR_SBL ||
					fixture.getUserData() == Enum.SENSOR_SBR) {
				return -1;
			}
			
			disposable = true;
			position = point.cpy();
			if (fixture.getUserData() == Enum.SATELLITE) {
				Satellite sat = (Satellite) fixture.getBody().getUserData();
				sat.detach();
				sat.push(speed.cpy().scl(damage*damage));
			} else if (fixture.getUserData() == Enum.DROID) {
				DroidShip droid = (DroidShip) fixture.getBody().getUserData();
				droid.hit(damage);
			}
			// DEBUG : projectile collide with sensors...
			return fraction;
		}
	};
	
	public void render(MyRenderer renderer) {
		pPosition.sub(this.speedX4);
		float scale = 1f + 2.0f/renderer.camera.PPU;
		renderer.setColor(0.0f, 1.0f, 0.5f, 0.9f);
		renderer.triangle(position.x+perpendicular.x*scale, position.y+perpendicular.y*scale,
						position.x-perpendicular.x*scale, position.y-perpendicular.y*scale,
						pPosition.x, pPosition.y);
	}
}