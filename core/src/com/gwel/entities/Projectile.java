package com.gwel.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.gwel.spacegame.MyRenderer;

public class Projectile {
	private Spaceship parent;
	public Vector2 position;
	private Vector2 pPosition;
	private Vector2 speed;
	private Vector2 speedX4;
	private Vector2 perpendicular;
	private float damage;
	public boolean disposable;
	
	public Projectile(Spaceship parent, Vector2 pos, Vector2 speed, float damage) {
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
			disposable = true;
			position = point.cpy();
			if (fixture.getUserData() == "Satellite") {
				Satellite sat = (Satellite) fixture.getBody().getUserData();
				sat.detach();
				sat.push(speed.cpy().scl(damage*damage));
			}
			return 1;
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