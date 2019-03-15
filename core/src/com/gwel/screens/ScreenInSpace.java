package com.gwel.screens;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.gwel.entities.*;
import com.gwel.spacegame.AABB;
import com.gwel.spacegame.MyContactListener;
import com.gwel.spacegame.Ps4Controller;
import com.gwel.spacegame.SpaceGame;


public class ScreenInSpace implements Screen {
	final SpaceGame game;
	private World b2world;
	private boolean destroy;
	
	private ArrayList<Planet> local_planets = new ArrayList<Planet>();
	private ArrayList<Planet> local_planets_prev;
	private LinkedList<Satellite> local_sats = new LinkedList<Satellite>();
	private LinkedList<PhysicBody> free_bodies = new LinkedList<PhysicBody>();
	//private ListIterator<PhysicBody> bod_iter;
	private ListIterator<Satellite> sat_iter;
	
	private Spaceship ship;
	private ShipTail tail1, tail2;

	
	public ScreenInSpace(final SpaceGame game) {
		this.game = game;
		destroy = false;
		
		b2world = new World(new Vector2(0.0f, 0.0f), true);
		b2world.setContactListener(new MyContactListener(game));
		
		ship = new Spaceship(b2world, game.camera.center);
		tail1 = new ShipTail(ship, new Vector2(0.8f, 0.2f), 0.2f);
		tail2 = new ShipTail(ship, new Vector2(-0.70f, 0.2f), 0.2f);
		free_bodies.add(ship);
	}
	
	@Override
	public void dispose() {
		// This test case prevents the world from being destroyed during a step
		if (destroy) {
			b2world.dispose();
		} else {
			destroy = true;
		}
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void render(float arg0) {
		if (destroy)
			dispose();
		
		handleInput();

		// UPDATING GAME STATE
		AABB local_range = new AABB(ship.getPosition().sub(SpaceGame.LOCAL_RADIUS, SpaceGame.LOCAL_RADIUS),
				ship.getPosition().add(SpaceGame.LOCAL_RADIUS, SpaceGame.LOCAL_RADIUS));
		//AABB exit_range = new AABB(ship.getPosition().sub(exit_radius, exit_radius),
		//		ship.getPosition().add(exit_radius, exit_radius));
		local_planets_prev = local_planets;
		local_planets = game.Qt.query(local_range);

		// Check for planets that newly entered the local zone
		for (Planet pl : local_planets) {
			if (!local_planets_prev.contains(pl)) {
				System.out.println("New planet in local zone");
				pl.activate(b2world);
				// Register its satellites
				local_sats.addAll(pl.activateSatellites(b2world));
			}
		}
		//System.out.println(local_sats.size());
		// Check for planets that exited the local zone
		for (Planet pl : local_planets_prev) {
			if (!local_range.containsPoint(pl.getPosition())) {
				pl.dispose();
			}
		}
		
		sat_iter = local_sats.listIterator();
		while (sat_iter.hasNext()) {
			Satellite sat = sat_iter.next(); // Can be optimized by declaring a tmp variable
			// Removing satellites belonging to planets outside of local zone
			if (sat.disposable || sat.detached)
				sat_iter.remove();
			// Register detached satellites as free bodies 
			if (sat.detached)
				free_bodies.add(sat);
		}
		// Applying gravity to free bodies
		for (Planet pl: local_planets) {
			pl.update(); // Apply gravity force to attached satellites
			for (PhysicBody bod: free_bodies) {
				bod.push(pl.getGravityAccel(bod.getPosition()).scl(bod.getMass()));
			}
		}
		b2world.step(1/60f, 8, 3);

		tail1.update();
		tail2.update();

		//  Camera update
		game.camera.glideTo(ship.getPosition());
		if (game.camera.autozoom)
			game.camera.zoomTo(100.0f/ship.getSpeed().len());
		game.camera.update();
		game.renderer.setProjectionMatrix(new Matrix4().set(game.camera.affine));
		
		// North and East directions are POSITIVE !
		AABB camera_range = new AABB(game.camera.sw.cpy().sub(Planet.MAX_RADIUS, Planet.MAX_RADIUS), 
				game.camera.ne.cpy().add(Planet.MAX_RADIUS, Planet.MAX_RADIUS));
		
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		//stars.update(camera.getTravelling());
		//stars.render(camera);
		
		for (Planet p : game.Qt.query(camera_range)) {
			p.render(game.renderer);
		}
		tail1.render(game.renderer);
		tail2.render(game.renderer);
		for (PhysicBody b : free_bodies) {
			if (camera_range.containsPoint(b.getPosition())) {
				b.render(game.renderer);
			}
		}
		for (Satellite sat: local_sats) {
			if (camera_range.containsPoint(sat.getPosition())) {
				sat.render(game.renderer);
			}
		}
		
		game.renderer.flush();
	}

	@Override
	public void resize(int arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void show() {
		// TODO Auto-generated method stub

	}

	private void handleInput() {
		if (game.hasController) {
			if(game.controller.getButton(Ps4Controller.CROSS)) {
				ship.accelerate(1.0f);
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
			float x_axis = game.controller.getAxis(Ps4Controller.LSTICK_X);
			float y_axis = game.controller.getAxis(Ps4Controller.LSTICK_Y);
			if (Math.abs(x_axis) > 0.25)
				ship.steer(-x_axis);
		}

		if (Gdx.input.isKeyPressed(Keys.P)) {
			System.out.println("Screenshot");
			byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

			// this loop makes sure the whole screenshot is opaque and looks exactly like what the user is seeing
			for(int i = 4; i < pixels.length; i += 4) {
				pixels[i - 1] = (byte) 255;
			}

			Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
			BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
			PixmapIO.writePNG(Gdx.files.external("screenshot.png"), pixmap);
			pixmap.dispose();
		}
		
		if (Gdx.input.isKeyPressed(Keys.UP)) {
			float angle = ship.getAngleDiff(MathUtils.PI/2.0f);
			float force = MathUtils.clamp(angle-ship.getAngularSpeed()*0.2f, -1.0f, 1.0f);
			ship.steer(force);
			ship.accelerate(1.0f-Math.abs(angle)/MathUtils.PI);
		}
		if (Gdx.input.isKeyPressed(Keys.LEFT)) {
			//ship.steer(1.0f);
			float angle = ship.getAngleDiff(MathUtils.PI);
			float force = MathUtils.clamp(angle-ship.getAngularSpeed()*0.2f, -1.0f, 1.0f);
			ship.steer(force);
			ship.accelerate(1.0f-Math.abs(angle)/MathUtils.PI);
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
			//ship.steer(-1.0f);
			float angle = ship.getAngleDiff(0.0f);
			float force = MathUtils.clamp(angle-ship.getAngularSpeed()*0.2f, -1.0f, 1.0f);
			ship.steer(force);
			ship.accelerate(1.0f-Math.abs(angle)/MathUtils.PI);
		}
		if (Gdx.input.isKeyPressed(Keys.DOWN)) {
			float angle = ship.getAngleDiff(-MathUtils.PI/2.0f);
			float force = MathUtils.clamp(angle-ship.getAngularSpeed()*0.2f, -1.0f, 1.0f);
			ship.steer(force);
			ship.accelerate(1.0f-Math.abs(angle)/MathUtils.PI);
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
