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
import com.gwel.spacegame.SpaceGame;


public class ScreenInSpace implements Screen {
	final SpaceGame game;
	private World b2world;
	private boolean destroy;
	private float game_speed;	// set to <1.0 for slow-mo
	
	private ArrayList<Planet> local_planets = new ArrayList<Planet>();
	private ArrayList<Planet> local_planets_prev;
	private LinkedList<Satellite> local_sats = new LinkedList<Satellite>();
	private ListIterator<Satellite> sat_iter;
	private LinkedList<MovingObject> free_bodies = new LinkedList<MovingObject>();
	private ListIterator<MovingObject> bod_iter;
	private LinkedList<Projectile> projectiles = new LinkedList<Projectile>();
	private ListIterator<Projectile> proj_iter;
	
	private ShipTail tail1, tail2;
	private Starfield starfield;

	
	public ScreenInSpace(final SpaceGame game) {
		this.game = game;
		destroy = false;
		game_speed = 1.0f;
		
		b2world = new World(new Vector2(0.0f, 0.0f), true);
		b2world.setContactListener(new MyContactListener(game));
		
		starfield = new Starfield(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		game.ship.initBody(b2world);
		
		tail1 = new ShipTail(game.ship, new Vector2(0.7f, 0.08f), 0.2f);
		tail2 = new ShipTail(game.ship, new Vector2(-0.7f, 0.08f), 0.2f);
		free_bodies.add(game.ship);
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
	public void render(float delta_time) {
		if (destroy)
			dispose();
		
		handleInput();

		// UPDATING GAME STATE
		AABB local_range = new AABB(game.ship.getPosition().sub(SpaceGame.LOCAL_RADIUS, SpaceGame.LOCAL_RADIUS),
				game.ship.getPosition().add(SpaceGame.LOCAL_RADIUS, SpaceGame.LOCAL_RADIUS));
		//AABB exit_range = new AABB(ship.getPosition().sub(exit_radius, exit_radius),
		//		ship.getPosition().add(exit_radius, exit_radius));
		local_planets_prev = local_planets;
		local_planets = game.Qt.query(local_range);

		// Check for planets that newly entered the local zone
		for (Planet pl : local_planets) {
			if (!local_planets_prev.contains(pl)) {
				pl.activate(b2world);
				// Register its satellites
				local_sats.addAll(pl.activateSatellites(b2world));
			}
		}
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
			if (sat.disposable || sat.detachable)
				sat_iter.remove();
			// Register detached satellites as free bodies 
			if (sat.detachable)
				free_bodies.add(sat);
		}
		// Removing free bodies outside of local zone
		bod_iter = free_bodies.listIterator();
		while (bod_iter.hasNext()) {
			if (!local_range.containsPoint(bod_iter.next().getPosition()))
				bod_iter.remove();
		}
		// Applying gravity to the free bodies
		for (Planet pl: local_planets) {
			pl.update(); // Apply gravity force to attached satellites
			for (MovingObject bod: free_bodies) {
				bod.push(pl.getGravityAccel(bod.getPosition()).scl(bod.getMass()));
			}
		}
		// Removing projectiles outside of local zone
		proj_iter = projectiles.listIterator();
		while (proj_iter.hasNext()) {
			Projectile proj = proj_iter.next();
			if (!local_range.containsPoint(proj.position) || proj.disposable)
				proj_iter.remove();
			else
			    proj.update(b2world, game_speed);
		}
		b2world.step(game_speed/60f, 8, 3);

		tail1.update();
		tail2.update();
		starfield.update(game.camera.getTravelling());
		
		//  Camera update
		game.camera.glideTo(game.ship.getPosition());
		if (game.camera.autozoom)
			game.camera.zoomTo(200.0f/game.ship.getSpeed().len());
		game.camera.update();
		
		
		// North and East directions are POSITIVE !
		AABB camera_range = new AABB(game.camera.sw.cpy().sub(Planet.MAX_RADIUS, Planet.MAX_RADIUS), 
				game.camera.ne.cpy().add(Planet.MAX_RADIUS, Planet.MAX_RADIUS));
		
		Gdx.gl.glClearColor(0.96f, 0.96f, 0.96f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		starfield.render(game.renderer);
		
		game.renderer.setProjectionMatrix(new Matrix4().set(game.camera.affine));
		for (Planet p : game.Qt.query(camera_range)) {
			p.render(game.renderer);
		}
		tail1.render(game.renderer);
		tail2.render(game.renderer);
		for (MovingObject b : free_bodies) {
			if (camera_range.containsPoint(b.getPosition())) {
				b.render(game.renderer);
			}
		}
		for (Satellite sat: local_sats) {
			if (camera_range.containsPoint(sat.getPosition())) {
				sat.render(game.renderer);
			}
		}
		for (Projectile proj: projectiles) {
			if (camera_range.containsPoint(proj.position))
				proj.render(game.renderer);
		}
		game.renderer.flush();
	}

	@Override
	public void resize(int width, int height) {
		starfield = new Starfield(width, height);
	}

	@Override
	public void resume() {
	}

	@Override
	public void show() {
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
			y_axis = -game.controller.getAxis(game.PAD_YAXIS);
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
		
		if (Gdx.input.isKeyPressed(Keys.SPACE)) {
			game.ship.fire(projectiles);
		}
		
		amp = (float) Math.sqrt(x_axis*x_axis + y_axis*y_axis);
		if (amp > 1.0f)
			amp = 1.0f;
		// Calculate angle between ship angle and directional stick angle
		float dAngle = game.ship.getAngleDiff(MathUtils.atan2(y_axis, x_axis));
		float steering = 4.0f*dAngle-game.ship.getAngularSpeed();
		game.ship.steer(steering*amp*amp);
		game.ship.accelerate((1.0f-Math.abs(dAngle)/MathUtils.PI)*amp*amp);
		
		
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
	}
}
