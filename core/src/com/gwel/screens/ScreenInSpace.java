package com.gwel.screens;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.gwel.ai.DroidPool;
import com.gwel.entities.*;
import com.gwel.spacegame.AABB;
import com.gwel.spacegame.Const;
import com.gwel.spacegame.Enums;
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
	private ListIterator<MovingObject> iterBodies;
	private LinkedList<DroidShip> droids = new LinkedList<DroidShip>();
	private ListIterator<DroidShip> iterDroids;
	private LinkedList<Projectile> projectiles = new LinkedList<Projectile>();
	private ListIterator<Projectile> iterProj;
	private LinkedList<ShipTrail> trails = new LinkedList<ShipTrail>();
	private ListIterator<ShipTrail> iterTrails;
	private DroidPool droidPool;
	
	private Starfield starfield;
	private Starfield deepfield;
	private boolean empty;

	
	public ScreenInSpace(final SpaceGame game) {
		this.game = game;
		destroy = false;
		game_speed = 1.0f;
		droidPool = importPool("ec8a21fd-4969-495e-8157-5f30e72a0715");
		
		b2world = new World(new Vector2(0.0f, 0.0f), true);
		b2world.setContactListener(new MyContactListener(game));
		
		game.ship.initBody(b2world);
		
		free_bodies.add(game.ship);
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
	public void render(float delta_time) {
		if (destroy)
			dispose(); // doesn't work
		
		handleInput();

		// UPDATING GAME STATE
		// Adding Droids
		if (droids.size() < 5) {
			spawnDroids(32);
		}
		b2world.step(game_speed/60f, 8, 3);
		AABB local_range = new AABB(game.ship.getPosition().sub(SpaceGame.LOCAL_RADIUS, SpaceGame.LOCAL_RADIUS),
				game.ship.getPosition().add(SpaceGame.LOCAL_RADIUS, SpaceGame.LOCAL_RADIUS));
		local_planets_prev = local_planets;
		local_planets = game.Qt.query(local_range);

		// Check for planets that newly entered the local zone
		for (Planet pl : local_planets) {
			if (!local_planets_prev.contains(pl)) {
				pl.initBody(b2world);
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
		iterBodies = free_bodies.listIterator();
		while (iterBodies.hasNext()) {
			MovingObject bod = iterBodies.next();
			if (!local_range.containsPoint(bod.getPosition())) {
				bod.dispose();
				iterBodies.remove();
			}
		}
		
		// Removing dead Droids and droids outside of local zone
		iterDroids = droids.listIterator();
		while (iterDroids.hasNext()) {
			DroidShip bod = iterDroids.next();
			if (!local_range.containsPoint(bod.getPosition())) {
				bod.dispose();
			} else {
				bod.update();
			}
			if (bod.disposable) {
				System.out.println("removing droid");
				iterDroids.remove();
			}
		}
		
		// Applying gravity to the free bodies and droids
		for (Planet pl: local_planets) {
			pl.update(); // Apply gravity force to attached satellites
			for (MovingObject bod: free_bodies) {
				bod.push(pl.getGravityAccel(bod.getPosition()).scl(bod.getMass()));
			}
			for (MovingObject bod: droids) {
				bod.push(pl.getGravityAccel(bod.getPosition()).scl(bod.getMass()));
			}
		}
		// Removing projectiles outside of local zone
		iterProj = projectiles.listIterator();
		while (iterProj.hasNext()) {
			Projectile proj = iterProj.next();
			if (!local_range.containsPoint(proj.position) || proj.disposable)
				iterProj.remove();
			else
			    proj.update(b2world, game_speed);
		}
		// Updating and removing ship trails
		iterTrails = trails.listIterator();
		while (iterTrails.hasNext()) {
			ShipTrail trail = iterTrails.next();
			if (trail.disposable)
				iterTrails.remove();
			else
			    trail.update();
		}
		
		for (Contact c: b2world.getContactList()) {
			Fixture f1 = c.getFixtureA();
			Fixture f2 = c.getFixtureB();
			Fixture sensor, object;
			
			if (!c.isTouching())
				continue;
			
			// Sensors for Neural Network
			if (f1.getUserData() == Enums.SENSOR_F || f2.getUserData() == Enums.SENSOR_F) {
				if (f1.getUserData() == Enums.SENSOR_F) {
					sensor = f1;
					object = f2;
				} else {
					sensor = f2;
					object = f1;
				}
				// Distance between the 2 bodies
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				Vector2 normal = dPos.cpy().nor(); 
				float vns = normal.dot(sensor.getBody().getLinearVelocity());  
				float vno = normal.dot(object.getBody().getLinearVelocity());  
				// Check if object is another ship or droid
				if (object.getUserData() == Enums.DROID || object.getUserData() == Enums.SHIP) {
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_SF, dPos.len()-thickness, vns+vno);
				} else {
					// Another type of obstacle
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_F, dPos.len()-thickness, vns+vno);
				}
			}
			if (f1.getUserData() == Enums.SENSOR_FR || f2.getUserData() == Enums.SENSOR_FR) {
				if (f1.getUserData() == Enums.SENSOR_FR) {
					sensor = f1;
					object = f2;
				} else {
					sensor = f2;
					object = f1;
				}
				// Distance between the 2 bodies
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				Vector2 normal = dPos.cpy().nor(); 
				float vns = normal.dot(sensor.getBody().getLinearVelocity());  
				float vno = normal.dot(object.getBody().getLinearVelocity());  
				// Check if object is another ship or droid
				if (object.getUserData() == Enums.DROID || object.getUserData() == Enums.SHIP) {
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_SFR, dPos.len()-thickness, vns+vno);
				} else {
					// Another type of obstacle
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_FR, dPos.len()-thickness, vns+vno);
				}
			}
			if (f1.getUserData() == Enums.SENSOR_FL || f2.getUserData() == Enums.SENSOR_FL) {
				if (f1.getUserData() == Enums.SENSOR_FL) {
					sensor = f1;
					object = f2;
				} else {
					sensor = f2;
					object = f1;
				}
				// Distance between the 2 bodies
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				Vector2 normal = dPos.cpy().nor(); 
				float vns = normal.dot(sensor.getBody().getLinearVelocity());  
				float vno = normal.dot(object.getBody().getLinearVelocity());  
				// Check if object is another ship or droid
				if (object.getUserData() == Enums.DROID || object.getUserData() == Enums.SHIP) {
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_SFL, dPos.len()-thickness, vns+vno);
				} else {
					// Another type of obstacle
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_FL, dPos.len()-thickness, vns+vno);
				}
			}
			if (f1.getUserData() == Enums.SENSOR_MR || f2.getUserData() == Enums.SENSOR_MR) {
				if (f1.getUserData() == Enums.SENSOR_MR) {
					sensor = f1;
					object = f2;
				} else {
					sensor = f2;
					object = f1;
				}
				// Distance between the 2 bodies
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				Vector2 normal = dPos.cpy().nor(); 
				float vns = normal.dot(sensor.getBody().getLinearVelocity());  
				float vno = normal.dot(object.getBody().getLinearVelocity());  
				// Check if object is another ship or droid
				if (object.getUserData() == Enums.DROID || object.getUserData() == Enums.SHIP) {
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_SMR, dPos.len()-thickness, vns+vno);
				} else {
					// Another type of obstacle
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_MR, dPos.len()-thickness, vns+vno);
				}
			}
			if (f1.getUserData() == Enums.SENSOR_ML || f2.getUserData() == Enums.SENSOR_ML) {
				if (f1.getUserData() == Enums.SENSOR_ML) {
					sensor = f1;
					object = f2;
				} else {
					sensor = f2;
					object = f1;
				}
				// Distance between the 2 bodies
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				Vector2 normal = dPos.cpy().nor(); 
				float vns = normal.dot(sensor.getBody().getLinearVelocity());  
				float vno = normal.dot(object.getBody().getLinearVelocity());  
				// Check if object is another ship or droid
				if (object.getUserData() == Enums.DROID || object.getUserData() == Enums.SHIP) {
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_SML, dPos.len()-thickness, vns+vno);
				} else {
					// Another type of obstacle
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_ML, dPos.len()-thickness, vns+vno);
				}
			}
			if (f1.getUserData() == Enums.SENSOR_BR || f2.getUserData() == Enums.SENSOR_BR) {
				if (f1.getUserData() == Enums.SENSOR_BR) {
					sensor = f1;
					object = f2;
				} else {
					sensor = f2;
					object = f1;
				}
				// Distance between the 2 bodies
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				Vector2 normal = dPos.cpy().nor(); 
				float vns = normal.dot(sensor.getBody().getLinearVelocity());  
				float vno = normal.dot(object.getBody().getLinearVelocity());  
				// Check if object is another ship or droid
				if (object.getUserData() == Enums.DROID || object.getUserData() == Enums.SHIP) {
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_SBR, dPos.len()-thickness, vns+vno);
				} else {
					// Another type of obstacle
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_BR, dPos.len()-thickness, vns+vno);
				}
			}
			if (f1.getUserData() == Enums.SENSOR_BL || f2.getUserData() == Enums.SENSOR_BL) {
				if (f1.getUserData() == Enums.SENSOR_BL) {
					sensor = f1;
					object = f2;
				} else {
					sensor = f2;
					object = f1;
				}
				// Distance between the 2 bodies
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				Vector2 normal = dPos.cpy().nor(); 
				float vns = normal.dot(sensor.getBody().getLinearVelocity());  
				float vno = normal.dot(object.getBody().getLinearVelocity());  
				// Check if object is another ship or droid
				if (object.getUserData() == Enums.DROID || object.getUserData() == Enums.SHIP) {
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_SBL, dPos.len()-thickness, vns+vno);
				} else {
					// Another type of obstacle
					((DroidShip) sensor.getBody().getUserData()).setSensor(Enums.SENSOR_BL, dPos.len()-thickness, vns+vno);
				}
			}
		}
		
		//  Camera update
		game.camera.glideTo(game.ship.getPosition());
		if (game.camera.autozoom)
			game.camera.zoomTo(200.0f/game.ship.getVelocity().len());
		game.camera.update();
		
		starfield.update(game.camera.getTravelling());
		deepfield.update(game.camera.getTravelling());
		
		// North and East directions are POSITIVE !
		AABB camera_range = new AABB(game.camera.sw.cpy().sub(Const.PLANET_MAX_RADIUS, Const.PLANET_MAX_RADIUS), 
				game.camera.ne.cpy().add(Const.PLANET_MAX_RADIUS, Const.PLANET_MAX_RADIUS));
		
		Gdx.gl.glClearColor(0.96f, 0.96f, 0.96f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		starfield.render(game.renderer);
		deepfield.render(game.renderer);
		
		game.renderer.setProjectionMatrix(new Matrix4().set(game.camera.affine));
		for (Planet p : game.Qt.query(camera_range)) {
			p.render(game.renderer);
		}
		for (ShipTrail trail: trails) {
			trail.render(game.renderer);
		}
		for (MovingObject b : free_bodies) {
			if (camera_range.containsPoint(b.getPosition())) {
				b.render(game.renderer);
			}
		}
		for (MovingObject b : droids) {
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
		
		// HUD
		Matrix4 normalProjection = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(),  Gdx.graphics.getHeight());
		game.batch.setProjectionMatrix(normalProjection);
		game.batch.begin();
		game.fontHUD.draw(game.batch, String.valueOf(game.ship.hitpoints), 20, Gdx.graphics.getHeight()-game.font.getXHeight());
		game.fontHUD.draw(game.batch, String.valueOf(game.ship.ammunition), 20, 30);
		game.batch.end();
	}

	@Override
	public void resize(int width, int height) {
		starfield = new Starfield(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0.0001f, 0.9f, 1.5f);
		deepfield = new Starfield(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0.004f, 0.15f, 0.8f);
	}
	
	DroidPool importPool(String filename) {
		System.out.println("### Importing pool '" + filename + "' from hard-drive");
		Json json = new Json();
		FileHandle entry = Gdx.files.internal("nn/" + filename + ".txt");
		DroidPool pool;
		String text = entry.readString();
		pool = json.fromJson(DroidPool.class, text);
		
		return pool;
		/*
		ArrayList<DroidShip> newDroids = new ArrayList<DroidShip>();
		for (int i=0; i<pool.nn.size(); i++) {
			DroidShip droid = new DroidShip(new Vector2(), 0, projectiles);
			droid.initNN(pool.activationFunc);
			droid.nn.weights = pool.nn.get(i).clone();
			newDroids.add(droid);
		}
		*/
	}
	
	private void spawnDroids(int number) {
		System.out.println("Spawning droids");
		while (number>0) {
			empty = true;
			float posX = MathUtils.random(game.ship.getPosition().x-SpaceGame.LOCAL_RADIUS, game.ship.getPosition().x+SpaceGame.LOCAL_RADIUS);
			float posY = MathUtils.random(game.ship.getPosition().y-SpaceGame.LOCAL_RADIUS, game.ship.getPosition().y+SpaceGame.LOCAL_RADIUS);
			b2world.QueryAABB(new QueryCallback() {
			@Override
			public boolean reportFixture(Fixture fixture) {
				empty = false;
				return true;
			}}, posX-4, posY-4, posX+4, posY+4);
			
			if (empty) {
				Vector2 pos = new Vector2(posX, posY);
				float angle = (game.ship.getPosition().sub(pos)).angleRad();
				int i = MathUtils.random(droidPool.nnBest.size()-1);
				DroidShip droid = new DroidShip(pos, angle, projectiles);
				droid.initNN(droidPool.activationFunc);
				droid.nn.weights = droidPool.nnBest.get(i).clone();
				droid.initBody(b2world);
				droids.add(droid);
				// Add ship trails
				trails.add(new ShipTrail(droid, new Vector2(0.7f, 0.08f), 0.2f, 256, new Color(0xFF0000FF), new Color(0xFFFF0000)));
				trails.add(new ShipTrail(droid, new Vector2(-0.7f, 0.08f), 0.2f, 256, new Color(0xFF0000FF), new Color(0xFFFF0000)));
				
				number--;
			}
		}
	}
	
	private void handleInput() {
		float x_axis = 0.0f;
		float y_axis = 0.0f;
		float amp = 1.0f;
		
		if (game.hasController) {
			if(game.controller.getButton(game.PAD_BOOST)) {
				game.ship.accelerate(2.5f);
				game.camera.autozoom = true;
			}
			PovDirection pov = game.controller.getPov(0);
			if (pov == PovDirection.north) {
				game.camera.zoomIn();
				game.camera.autozoom = false;
			}
			if (pov == PovDirection.south) {
				game.camera.zoomOut();
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
			game.camera.zoomIn();
			game.camera.autozoom = false;
		}
		if (Gdx.input.isKeyPressed(Keys.Z)) {
			game.camera.zoomOut();
			game.camera.autozoom = false;
		}
		if (Gdx.input.isKeyPressed(Keys.SPACE)) {
			game.ship.fire(projectiles);
		}
		
		amp = (float) Math.sqrt(x_axis*x_axis + y_axis*y_axis);
		if (amp > 1.0f)
			amp = 1.0f;
		if (amp < 0.15)
			amp = 0.0f;
		// Calculate angle between ship angle and directional stick angle
		float dAngle = game.ship.getAngleDiff(MathUtils.atan2(y_axis, x_axis));
		float steering = 4.0f*dAngle-game.ship.getAngularVelocity();
		//game.ship.steer(steering*amp*amp);
		game.ship.steer(steering*amp);
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
	
	@Override
	public void hide() {}

	@Override
	public void pause() {}
	
	@Override
	public void resume() {}

	@Override
	public void show() {}
}
