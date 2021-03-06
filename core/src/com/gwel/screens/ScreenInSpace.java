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
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.gwel.ai.DroidPool;
import com.gwel.entities.*;
import com.gwel.spacegame.*;


public class ScreenInSpace implements Screen {
	final SpaceGame game;
	private final World world;
	private final MyCamera camera;
	private boolean mustDestroy;
	private final float game_speed;	// set to <1.0 for slow-mo

	static private final float GRAVITY_ACTIVE_RADIUS = 800.0f;
	static private final float LOCAL_RADIUS = GRAVITY_ACTIVE_RADIUS;

	private ArrayList<Planet> localPlanets = new ArrayList<>();
	private final LinkedList<Satellite> localSats = new LinkedList<>();
	private final LinkedList<MovingObject> freeBodies = new LinkedList<>();
	private final LinkedList<DroidShip> droids = new LinkedList<>();
	private final LinkedList<Projectile> projectiles = new LinkedList<>();
	private int numAsteroids;
	private final LinkedList<ShipTrail> trails = new LinkedList<>();
	private final DroidPool droidPool;
	private final AsteroidPool asteroidPool;
	
	private Starfield starfield;
	private Starfield deepfield;
	private boolean empty;
	private static final Color spaceColor = new Color();

	//Box2DDebugRenderer b2renderer;


	public ScreenInSpace(final SpaceGame game) {
		this.game = game;
		mustDestroy = false;
		game_speed = 1.0f;
		droidPool = importPool("ec8a21fd-4969-495e-8157-5f30e72a0715");
		asteroidPool = new AsteroidPool();
		numAsteroids = 0;

		world = new World(new Vector2(0.0f, 0.0f), true);
		world.setContactListener(new SpaceContactListener(game));
		camera = new MyCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.setZoomLimits(0.5f, 50f);
		camera.setCenter(game.ship.getPosition());
		
		game.ship.initBody(world);
		
		freeBodies.add(game.ship);

		//b2renderer = new Box2DDebugRenderer();
	}
	
	@Override
	public void dispose() {
		// This test case prevents the world from being destroyed during a step
		if (mustDestroy) {
			System.out.println("Destroying space");
			game.ship.dispose();	// Important, so the ship position and angle is saved
			world.dispose();
		} else {
			mustDestroy = true;
		}
	}

	@Override
	public void render(float delta_time) {
		if (mustDestroy)
			dispose(); // doesn't work
		
		handleInput();

		// UPDATING GAME STATE
		// Adding Droids
		if (droids.size() < Const.NUMBER_DROIDS/2) {
			spawnDroids(Const.NUMBER_DROIDS);
		}
		// Adding asteroids
		if (numAsteroids < Const.NUMBER_ASTEROIDS) {
			spawnAsteroid();
		}

		world.step(game_speed/60f, 8, 3);
		AABB localArea = new AABB(game.ship.getPosition().sub(LOCAL_RADIUS, LOCAL_RADIUS),
				game.ship.getPosition().add(LOCAL_RADIUS, LOCAL_RADIUS));
		ArrayList<Planet> localPlanetsPrev = localPlanets;
		localPlanets = game.quadTree.query(localArea);

		// Check for planets that newly entered the local zone
		for (Planet pl : localPlanets) {
			if (!localPlanetsPrev.contains(pl)) {
				pl.initBody(world);
				// Register its satellites
				localSats.addAll(pl.activateSatellites(world));
			}
		}
		// Check for planets that exited the local zone
		for (Planet pl : localPlanetsPrev) {
			if (!localArea.containsPoint(pl.getPosition())) {
				pl.dispose();
			}
		}

		ListIterator<Satellite> iterSat = localSats.listIterator();
		while (iterSat.hasNext()) {
			Satellite sat = iterSat.next(); // Can be optimized by declaring a tmp variable
			// Removing satellites belonging to planets outside of local zone
			if (sat.disposable || sat.freeFlying)
				iterSat.remove();
			// Register detached satellites as free bodies 
			if (sat.freeFlying)
				freeBodies.add(sat);
		}
		
		// Removing free bodies outside of local zone
		ListIterator<MovingObject> iterBodies = freeBodies.listIterator();
		while (iterBodies.hasNext()) {
			MovingObject bod = iterBodies.next();
			if (!localArea.containsPoint(bod.getPosition())) {
				if (bod.getClass() == Asteroid.class) {
					asteroidPool.free((Asteroid) bod);
					numAsteroids--;
				}
				bod.dispose();
				iterBodies.remove();
			}
		}
		
		// Removing dead Droids and droids outside of local zone
		ListIterator<DroidShip> iterDroids = droids.listIterator();
		while (iterDroids.hasNext()) {
			DroidShip droid = iterDroids.next();
			if (!localArea.containsPoint(droid.getPosition())) {
				droid.dispose();
			} else {
				droid.update();
			}
			if (droid.disposable) {
				System.out.println("removing droid");
				iterDroids.remove();
			}
		}
		
		// Applying gravity to the free bodies and droids
		for (Planet pl: localPlanets) {
			pl.update(); // Apply gravity force to attached satellites
			for (MovingObject bod: freeBodies) {
				bod.push(pl.getGravityAccel(bod.getPosition()).scl(bod.getMass()));
			}
			for (MovingObject bod: droids) {
				bod.push(pl.getGravityAccel(bod.getPosition()).scl(bod.getMass()));
			}
		}
		// Removing projectiles outside of local zone
		ListIterator<Projectile> iterProj = projectiles.listIterator();
		while (iterProj.hasNext()) {
			Projectile proj = iterProj.next();
			if (!localArea.containsPoint(proj.position) || proj.disposable)
				iterProj.remove();
			else
			    proj.update(world, game_speed);
		}
		// Updating and removing ship trails
		ListIterator<ShipTrail> iterTrails = trails.listIterator();
		while (iterTrails.hasNext()) {
			ShipTrail trail = iterTrails.next();
			if (trail.disposable)
				iterTrails.remove();
			else
			    trail.update();
		}
		
		for (Contact c: world.getContactList()) {
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
		camera.glideTo(game.ship.getPosition());
		if (camera.autozoom)
			camera.zoomTo(200.0f/game.ship.getVelocity().len());
		camera.update();
		
		starfield.update(camera.getTravelling(), camera.PPU);
		deepfield.update(camera.getTravelling(), camera.PPU);
		
		// North and East directions are POSITIVE !
		AABB cameraRange = new AABB(camera.sw.cpy().sub(Const.PLANET_MAX_RADIUS, Const.PLANET_MAX_RADIUS),
				camera.ne.cpy().add(Const.PLANET_MAX_RADIUS, Const.PLANET_MAX_RADIUS));
		
		float shipSpaceAngle = game.ship.getPosition().angle();
		spaceColor.fromHsv(shipSpaceAngle, 0.15f, 1.0f);
		
		Gdx.gl.glClearColor(spaceColor.r, spaceColor.g, spaceColor.b, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		shipSpaceAngle = (shipSpaceAngle+180f) % 360f;
		starfield.render(game.renderer, shipSpaceAngle);
		deepfield.render(game.renderer, shipSpaceAngle);
		game.renderer.setProjectionMatrix(new Matrix4().set(camera.affine));
		for (Planet p : game.quadTree.query(cameraRange)) {
			p.render(game.renderer);
		}
		for (ShipTrail trail: trails) {
			trail.render(game.renderer);
		}
		for (MovingObject b : freeBodies) {
			if (cameraRange.containsPoint(b.getPosition())) {
				b.render(game.renderer);
			}
		}
		for (DroidShip b : droids) {
			if (cameraRange.containsPoint(b.getPosition())) {
				b.render(game.renderer);
			}
		}
		for (Satellite sat: localSats) {
			if (cameraRange.containsPoint(sat.getPosition())) {
				sat.render(game.renderer);
			}
		}
		for (Projectile proj: projectiles) {
			if (cameraRange.containsPoint(proj.position))
				proj.render(game.renderer);
		}
		game.renderer.flush();

		//b2renderer.render(world, new Matrix4().set(camera.affine));

		/*
		// HUD
		Matrix4 normalProjection = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(),  Gdx.graphics.getHeight());
		game.batch.setProjectionMatrix(normalProjection);
		game.batch.begin();
		game.hud.fontHUD.draw(game.batch, String.valueOf(game.ship.hitpoints), 20, Gdx.graphics.getHeight()-game.font.getXHeight());
		game.fontHUD.draw(game.batch, String.valueOf(game.ship.ammunition), 20, 30);
		game.batch.end();
		 */
	}

	@Override
	public void resize(int width, int height) {
		starfield = new Starfield(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0.0001f, 0.9f, 1.5f);
		deepfield = new Starfield(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0.004f, 0.15f, 0.8f);
		game.renderer.setCamera(camera);
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
			float posX = MathUtils.random(game.ship.getPosition().x-LOCAL_RADIUS, game.ship.getPosition().x+LOCAL_RADIUS);
			float posY = MathUtils.random(game.ship.getPosition().y-LOCAL_RADIUS, game.ship.getPosition().y+LOCAL_RADIUS);
			world.QueryAABB(new QueryCallback() {
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
				droid.initBody(world);
				droids.add(droid);
				// Add ship trails
				trails.add(new ShipTrail(droid, new Vector2(0.7f, 0.08f), 0.2f, 256, new Color(0xFF0000FF), new Color(0xFFFF0000)));
				trails.add(new ShipTrail(droid, new Vector2(-0.7f, 0.08f), 0.2f, 256, new Color(0xFF0000FF), new Color(0xFFFF0000)));
				
				number--;
			}
		}
	}

	private void spawnAsteroid() {
		// Spawn a new Asteroid (from pool) in local area
		System.out.println("trying to spawn " + numAsteroids);
		Vector2 shipPosition = game.ship.getPosition();
		float asRad = Const.ASTEROID_MAX_RADIUS;
		int numTries = 4;
		while (numTries>0) {
			float angle = MathUtils.random(MathUtils.PI2);
			Vector2 queryPos = new Vector2(MathUtils.cos(angle), MathUtils.sin(angle));
			queryPos.scl(Const.ASTEROID_SPAWN_RADIUS).add(shipPosition);
			world.QueryAABB(new QueryCallback() {
				@Override
				public boolean reportFixture(Fixture fixture) {
					empty = false;
					return true;
				}}, queryPos.x-asRad, queryPos.y-asRad, queryPos.x+asRad, queryPos.y-asRad);

			if (empty) {
				Asteroid asteroid = asteroidPool.obtain();
				asteroid.setPosition(queryPos);
				Vector2 vel = game.ship.getPosition().sub(queryPos);
				vel.setLength2(MathUtils.random(Const.ASTEROID_MIN_VEL, Const.ASTEROID_MAX_VEL));
				vel.rotateRad(MathUtils.random(-0.1f, 0.1f));
				asteroid.setVelocity(vel);
				asteroid.setAngularVelocity(MathUtils.random(-0.6f, 0.6f));
				asteroid.initBody(world);
				freeBodies.add(asteroid);
				numAsteroids++;
				System.out.println("Asteroid spawned");
				break;
			}
			numTries--;
		}
	}
	
	private void handleInput() {
		float x_axis = 0.0f;
		float y_axis = 0.0f;
		float amp = 1.0f;
		
		if (game.hasController) {
			if(game.controller.getButton(game.PAD_BOOST)) {
				game.ship.accelerate(2.5f);
				camera.autozoom = true;
			}
			PovDirection pov = game.controller.getPov(0);
			if (pov == PovDirection.north) {
				camera.zoomIn();
				camera.autozoom = false;
			}
			if (pov == PovDirection.south) {
				camera.zoomOut();
				camera.autozoom = false;
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
			camera.zoomIn();
			camera.autozoom = false;
		}
		if (Gdx.input.isKeyPressed(Keys.Z)) {
			camera.zoomOut();
			camera.autozoom = false;
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
