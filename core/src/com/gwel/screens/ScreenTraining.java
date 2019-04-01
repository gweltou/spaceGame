package com.gwel.screens;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.gwel.entities.*;
import com.gwel.spacegame.MyContactListener;
import com.gwel.spacegame.SpaceGame;


public class ScreenTraining implements Screen {
	private final float SPAWN_RADIUS = 30.0f;
	private final float WORLD_WIDTH = 200.0f;
	private final float WORLD_HEIGHT = 150.0f;
	private final float BORDER_LEFT = -WORLD_WIDTH/2.0f;
	private final float BORDER_RIGHT = WORLD_WIDTH/2.0f;
	private final float BORDER_UP = WORLD_HEIGHT/2.0f;
	private final float BORDER_DOWN = -WORLD_HEIGHT/2.0f;
	
	final SpaceGame game;
	private World b2world;
	private boolean destroy;
	private float game_speed;	// set to <1.0 for slow-mo
	
	private ArrayList<Planet> local_planets = new ArrayList<Planet>();
	private LinkedList<DroidShip> droids = new LinkedList<DroidShip>();
	private ListIterator<DroidShip> droidsIter;
	private LinkedList<Projectile> projectiles = new LinkedList<Projectile>();
	private ListIterator<Projectile> proj_iter;
	
	Box2DDebugRenderer debugRenderer;
	
	
	public ScreenTraining(final SpaceGame game) {
		this.game = game;
		destroy = false;
		game_speed = 1.0f;
		
		b2world = new World(new Vector2(0.0f, 0.0f), true);
		b2world.setContactListener(new MyContactListener(game));
		
		debugRenderer=new Box2DDebugRenderer();
		
		populatePlanets();
		populateShips(64);
		
		//game.ship.initBody(b2world);

		//free_bodies.add(game.ship);
	}
	
	@Override
	public void dispose() {
		// This test case prevents the world from being destroyed during a step
		if (destroy) {
			System.out.println("Destroying space");
			game.ship.dispose();	// Important, so the ship position and angle is saved
			b2world.dispose();
		} else {
			destroy = true;
		}
	}

	@Override
	public void hide() {		
	}

	@Override
	public void pause() {		
	}

	@Override
	public void render(float delta_time) {
		if (destroy)
			dispose(); // doesn't work
		
		handleInput();

		// UPDATING GAME STATE
		//AABB local_range = new AABB(
		//local_planets = game.Qt.query(local_range);
		droidsIter = droids.listIterator();
		while (droidsIter.hasNext()) {
			DroidShip droid = droidsIter.next();
			droid.update();
			if (droid.disposable) {
				droid.dispose();
				droidsIter.remove();
			}
		}
		
		// Applying gravity to the free bodies
		for (Planet pl: local_planets) {
			pl.update(); // Apply gravity force to attached satellites
			for (DroidShip bod: droids) {
				bod.push(pl.getGravityAccel(bod.getPosition()).scl(bod.getMass()));
			}
		}
		// Removing projectiles outside of local zone
		proj_iter = projectiles.listIterator();
		while (proj_iter.hasNext()) {
			Projectile proj = proj_iter.next();
			if (proj.disposable || proj.position.x < BORDER_LEFT || proj.position.x > BORDER_RIGHT ||
					proj.position.y < BORDER_DOWN || proj.position.y > BORDER_UP)
				proj_iter.remove();
			else
			    proj.update(b2world, game_speed);
		}
		b2world.step(game_speed/60f, 8, 3);

		//  Camera update
		game.camera.update();
		
		
		// North and East directions are POSITIVE !
		//AABB camera_range = new AABB(game.camera.sw.cpy().sub(Planet.MAX_RADIUS, Planet.MAX_RADIUS), 
		//		game.camera.ne.cpy().add(Planet.MAX_RADIUS, Planet.MAX_RADIUS));
		
		Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		/*
		game.renderer.setProjectionMatrix(new Matrix4().set(game.camera.affine));
		for (Planet p : b2World) {
			p.render(game.renderer);
		}
		for (MovingObject b : droids) {
			if (camera_range.containsPoint(b.getPosition())) {
				b.render(game.renderer);
			}
		}
		for (Projectile proj: projectiles) {
			if (camera_range.containsPoint(proj.position))
				proj.render(game.renderer);
		}
		game.renderer.flush();
		*/
		debugRenderer.render(b2world, new Matrix4().set(game.camera.affine));
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void resume() {
	}

	@Override
	public void show() {
	}
	
	private void populatePlanets() {
		float SMALL_RADIUS = 15.0f;
		float BIG_RADIUS = 35.0f;
		float angle = 0f;
		float radius1 = SPAWN_RADIUS+4*SMALL_RADIUS;
		float radius2 = radius1+SMALL_RADIUS+BIG_RADIUS;
		for (int i=0; i<6; i++) {
			Planet p = new Planet(new Vector2(	(float) (Math.cos(angle)*radius1),
												(float) (Math.sin(angle)*radius1)),
									SMALL_RADIUS, 0);
			p.initBody(b2world);
			p = new Planet(new Vector2(	(float) (Math.cos(angle+Math.PI/6)*radius2),
					 						(float) (Math.sin(angle+Math.PI/6)*radius2)),
					 						BIG_RADIUS, 0);
			p.initBody(b2world);
			angle += MathUtils.PI2/6;
		}
	}
	
	private void populateShips(int number) {
		boolean isEmpty;
		for (int i=0; i<number;) {
			float radius = (float) (Math.random()*SPAWN_RADIUS);
			Vector2 pos = new Vector2().setToRandomDirection().scl(radius);
			isEmpty = true;
			for (DroidShip otherShip: droids) {
				if (pos.dst(otherShip.getPosition()) < 3.0f*otherShip.BOUNDING_SPHERE_RADIUS)
					isEmpty = false;
			}
			if (isEmpty) {
				float angle = (float) Math.random()*MathUtils.PI2;
				DroidShip droid = new DroidShip(pos, angle, projectiles);
				droid.initBody(b2world);
				droid.initNN();
				droids.add(droid);
				i++;
			}
		}
	}

	private void handleInput() {
		float x_axis = 0.0f;
		float y_axis = 0.0f;
		float amp = 1.0f;
		
		if (game.hasController) {
			if(game.controller.getButton(game.PAD_BOOST)) {
				game.ship.accelerate(1.0f);
				game.camera.autozoom = true;
			}
			PovDirection pov = game.controller.getPov(0);
			if (pov == PovDirection.north) {
				game.camera.zoom(1.04f);
				game.camera.autozoom = false;
			}
			if (pov == PovDirection.south) {
				game.camera.zoom(0.95f);
				game.camera.autozoom = false;
			}
			
			// Fire
			if (game.controller.getButton(game.PAD_FIRE)) {
				game.ship.fire(projectiles);
			}
			x_axis = game.controller.getAxis(game.PAD_XAXIS);
			y_axis = game.PAD_YDIR * game.controller.getAxis(game.PAD_YAXIS);
		}
		
		if (Gdx.input.isKeyPressed(Keys.UP)) {
			y_axis += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Keys.DOWN)) {
			y_axis -= 1.0f;
		}
		if (Gdx.input.isKeyPressed(Keys.LEFT)) {
			x_axis -= 1.0f;
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
			x_axis += 1.0f;
		}
		
		if (Gdx.input.isKeyPressed(Keys.A)) {
			game.camera.zoom(1.04f);
			game.camera.autozoom = false;
		}
		if (Gdx.input.isKeyPressed(Keys.Z)) {
			game.camera.zoom(0.95f);
			game.camera.autozoom = false;
		}
	}
}