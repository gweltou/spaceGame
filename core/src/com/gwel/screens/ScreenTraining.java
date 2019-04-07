package com.gwel.screens;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.PriorityQueue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.TimeUtils;
import com.gwel.ai.DroidComparator;
import com.gwel.ai.NeuralNetwork;
import com.gwel.ai.DroidPool;
import com.gwel.entities.*;
import com.gwel.spacegame.Enums;
import com.gwel.spacegame.MyContactListener;
import com.gwel.spacegame.SpaceGame;


public class ScreenTraining implements Screen {
	private final int SIM_SPEED = 8;
	private final float SPAWN_RADIUS = 30.0f;
	private final float SMALL_PLANET_RADIUS = 15.0f;
	private final float BIG_PLANET_RADIUS = 35.0f;
	private final float WORLD_WIDTH =  2*(SPAWN_RADIUS + 5*SMALL_PLANET_RADIUS + 2*BIG_PLANET_RADIUS + DroidShip.SIGHT_DISTANCE);
	private final float WORLD_HEIGHT = WORLD_WIDTH;
	private final float BORDER_LEFT = -WORLD_WIDTH/2.0f;
	private final float BORDER_RIGHT = WORLD_WIDTH/2.0f;
	private final float BORDER_UP = WORLD_HEIGHT/2.0f;
	private final float BORDER_DOWN = -WORLD_HEIGHT/2.0f;
	private final boolean DEBUG_RENDERING = true;
	private final int STARTING_POP = 32;
	private final int WINNERS_PER_GENERATION = 6;
	private final int N_OFFSPRINGS = 8;
	private final int SIM_STEPS = 20000;
	
	final SpaceGame game;
	private World b2world;
	private boolean destroy;
	
	private ArrayList<Planet> localPlanets = new ArrayList<Planet>();
	private LinkedList<DroidShip> droids = new LinkedList<DroidShip>();
	private ListIterator<DroidShip> droidsIter;
	private LinkedList<Projectile> projectiles = new LinkedList<Projectile>();
	private ListIterator<Projectile> proj_iter;
	private PriorityQueue<DroidShip> podium = new PriorityQueue<DroidShip>(5, new DroidComparator());
	private int currentGen;
	private DroidPool currentPool;
	private ArrayList<Integer> scores = new ArrayList<Integer>();
	private int scoreWinBest;	// Mean score of best winners so far
	
	Box2DDebugRenderer debugRenderer;
	ShapeRenderer renderer;
	
	long lastFpsDisplay;
	int steps;
	
	
	public ScreenTraining(final SpaceGame game) {
		this.game = game;
		destroy = false;
		
		b2world = new World(new Vector2(0.0f, 0.0f), true);
		b2world.setContactListener(new MyContactListener(game));
		
		debugRenderer=new Box2DDebugRenderer();
		debugRenderer.setDrawAABBs(false);
		renderer = new ShapeRenderer();
		
		newPool();
		populatePlanets();
		populateShips(STARTING_POP);
		lastFpsDisplay = TimeUtils.millis();
		steps = 0;
	}
	
	@Override
	public void dispose() {
		// This test case prevents the world from being destroyed during a step
		if (destroy) {
			System.out.println("Destroying space");
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
		for (int i=0; i<SIM_SPEED; i++) {
			simStep();
		}
		
		handleInput();
		
		//  Camera update
//		if (!droids.isEmpty()) {
//			game.camera.glideTo(droids.getFirst().getPosition());
//		}
		game.camera.update();

		Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		if (DEBUG_RENDERING) {
			// DEBUG RENDERING
			debugRenderer.render(b2world, new Matrix4().set(game.camera.affine));
			// Render projectiles
			renderer.setProjectionMatrix(new Matrix4().set(game.camera.affine));		
			renderer.begin(ShapeType.Line);
			renderer.setColor(1, 0, 0, 1);
			for (Projectile proj: projectiles) {
				renderer.line(proj.position, proj.pPosition);
			}		
			renderer.end();
		} else {
			// CLASSIC RENDERING
			game.renderer.setProjectionMatrix(new Matrix4().set(game.camera.affine));
			for (Planet p : localPlanets) {
				p.render(game.renderer);
			}
			for (MovingObject b : droids) {
				b.render(game.renderer);
			}
			for (Projectile proj: projectiles) {
				proj.render(game.renderer);
			}
			game.renderer.flush();
		}

		// Display Framerate
		/*
		if (TimeUtils.millis() - lastFpsDisplay > 4000) {
			System.out.println("Fps: " + String.valueOf(Gdx.graphics.getFramesPerSecond()));
			lastFpsDisplay = TimeUtils.millis();
		}
		*/
	}
	

	public void simStep() {		
		// END OF SIMULATION FOR CURRENT GENERATION
		if (steps > SIM_STEPS || droids.isEmpty()) {
			System.out.println("End of generation " + String.valueOf(currentGen));
			steps = 0;
			
			// Add remaining living droids to the podium
			for (DroidShip droid: droids) {
				droid.dispose();
				droid.setScore(steps);
				podium.add(droid);
			}
			
			// Calculate generation score
			System.out.print("Generations scores: ");
			float scoreGen = 0;
			for (DroidShip droid: podium) {
				scoreGen += droid.getScore();
			}
			scoreGen /= podium.size();
			scores.add((int) Math.round(scoreGen));
			for (int score: scores) {
				System.out.print("__");
				System.out.print(score);
			}
			System.out.print("\n");
			
			// Select the best droids of this generation
			System.out.print("Individual scores: ");
			ArrayList<DroidShip> winners = new ArrayList<DroidShip>();
			for (int i = Math.min(WINNERS_PER_GENERATION, podium.size()); i >= 1; i--) {
				DroidShip droid = podium.remove();
				System.out.print("__" + String.valueOf(droid.getScore()));
				winners.add(droid);
			}
			System.out.print("\n");
			
			// Calculate winners mean score
			float scoreWin = 0;
			for (DroidShip droid: winners) {
				scoreWin += droid.getScore();
			}
			scoreWin /= winners.size();
			if (scoreWin > scoreWinBest) {
				scoreWinBest = (int) scoreWin;
				System.out.println("### New generation record ! : " + scoreWinBest);
				// Save best winners to pool
				ArrayList<float[][][]> nn = new ArrayList<float[][][]>();
				for (DroidShip ship: winners) {
					nn.add(ship.nn.weights.clone());
				}
				currentPool.nnBest = nn;
				currentPool.bestGen = currentGen;
			}
						
			// Save every 10 generations to hard-drive
			if (currentGen>0 && scores.size()%10 == 0) {
				exportPool(winners);
			}
			
			if (currentGen - currentPool.bestGen >= 10) {
				// Evolution stalled for too long, branch out from last best winners
				System.out.println("### New branch from generation " + currentPool.bestGen);
				droids.clear();
				podium.clear();
				currentGen = currentPool.bestGen+1;
				//newPool();
				// Set the winners with nn from the last best batch
				for (int i=0; i<winners.size(); i++) {
					winners.get(i).nn.weights = currentPool.nnBest.get(i);
				}
				populateShips(newGeneration(winners));	
			} else {
				// Respawn current pool
				podium.clear();
				droids.clear();
				populateShips(newGeneration(winners));
				currentGen += 1;
			}
		}

		// UPDATING GAME STATE
		b2world.step(1.0f/60f, 8, 3);
		
		for (Contact c: b2world.getContactList()) {
			Fixture f1 = c.getFixtureA();
			Fixture f2 = c.getFixtureB();
			Fixture f;
			
			// Sensors for Neural Network
			// OBSTACLE SENSORS
			if (f1.getUserData() == Enums.SENSOR_F || f2.getUserData() == Enums.SENSOR_F) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_F) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				//float alpha = MathUtils.atan2(dPos.y, dPos.x);	// MathUtils.atan2 is faster than Math.atan2
				//float cosa = MathUtils.cos(alpha);
				//float sina = MathUtils.sin(alpha);
				//cosa *= ((Collidable) f1.getBody().getUserData()).getBoundingRadius() + ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				//sina *= ((Collidable) f1.getBody().getUserData()).getBoundingRadius() + ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_F, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_FR || f2.getUserData() == Enums.SENSOR_FR) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_FR) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_FR, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_FL || f2.getUserData() == Enums.SENSOR_FL) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_FL) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_FL, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_MR || f2.getUserData() == Enums.SENSOR_MR) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_MR) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_MR, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_ML || f2.getUserData() == Enums.SENSOR_ML) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_ML) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_ML, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_BR || f2.getUserData() == Enums.SENSOR_BR) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_BR) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_BR, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_BL || f2.getUserData() == Enums.SENSOR_BL) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_BL) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_BL, dPos.len()-thickness);
			}
			
			// SHIP SENSORS
			if (f1.getUserData() == Enums.SENSOR_SF || f2.getUserData() == Enums.SENSOR_SF) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_SF) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_SF, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_SFR || f2.getUserData() == Enums.SENSOR_SFR) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_SFR) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_SFR, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_SFL || f2.getUserData() == Enums.SENSOR_SFL) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_SFL) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_SFL, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_SMR || f2.getUserData() == Enums.SENSOR_SMR) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_SMR) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_SMR, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_SML || f2.getUserData() == Enums.SENSOR_SML) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_SML) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_SML, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_SBR || f2.getUserData() == Enums.SENSOR_SBR) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_SBR) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_BR, dPos.len()-thickness);
			}
			if (f1.getUserData() == Enums.SENSOR_SBL || f2.getUserData() == Enums.SENSOR_SBL) {
				f = f1;
				if (f2.getUserData() == Enums.SENSOR_SBL) {
					f = f2;
				}
				Vector2 dPos = f1.getBody().getPosition().sub(f2.getBody().getPosition());
				float thickness = ((Collidable) f1.getBody().getUserData()).getBoundingRadius()
						+ ((Collidable) (f2.getBody().getUserData())).getBoundingRadius();
				((DroidShip) f.getBody().getUserData()).setSensor(Enums.SENSOR_SBL, dPos.len()-thickness);
			}
		}		
		
		// Clean up dead Droids and wrap-around screen borders
		droidsIter = droids.listIterator();
		while (droidsIter.hasNext()) {
			DroidShip droid = droidsIter.next();
			droid.update();
			if (droid.disposable) {
				droid.dispose();
				droid.setScore(steps);
				podium.add(droid);
				droidsIter.remove();
			} else if (droid.getPosition().x > BORDER_RIGHT) {
				droid.dispose();
				droid.setPosition(droid.getPosition().sub(WORLD_WIDTH, 0));
				droid.initBody(b2world);
			} else if (droid.getPosition().x < BORDER_LEFT) {
				droid.dispose();
				droid.setPosition(droid.getPosition().add(WORLD_WIDTH, 0));
				droid.initBody(b2world);
			} else if (droid.getPosition().y > BORDER_UP) {
				droid.dispose();
				droid.setPosition(droid.getPosition().sub(0, WORLD_HEIGHT));
				droid.initBody(b2world);
			} else if (droid.getPosition().y < BORDER_DOWN) {
				droid.dispose();
				droid.setPosition(droid.getPosition().add(0, WORLD_HEIGHT));
				droid.initBody(b2world);
			}	
		}
		
		// Applying gravity to the Droids
		for (Planet pl: localPlanets) {
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
			    proj.update(b2world, 1.0f);
		}
		
		steps++;
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
		float angle = 0f;
		float radius1 = SPAWN_RADIUS+4*SMALL_PLANET_RADIUS;
		float radius2 = radius1+SMALL_PLANET_RADIUS+BIG_PLANET_RADIUS;
		float radius3 = radius2+2*SMALL_PLANET_RADIUS;
		for (int i=0; i<6; i++) {
			Planet p = new Planet(new Vector2(	(float) (Math.cos(angle)*radius1),
												(float) (Math.sin(angle)*radius1)),
									SMALL_PLANET_RADIUS, 0);
			p.initBody(b2world);
			localPlanets.add(p);
			p = new Planet(new Vector2(	(float) (Math.cos(angle+Math.PI/6)*radius2),
					 						(float) (Math.sin(angle+Math.PI/6)*radius2)),
					 						BIG_PLANET_RADIUS, 0);
			p.initBody(b2world);
			localPlanets.add(p);
			angle += MathUtils.PI2/6;
		}
		float[] angles = {(float) (2*Math.PI/6), (float) (4*Math.PI/6), (float) (-2*Math.PI/6), (float) (-4*Math.PI/6)};
		for (float a: angles) {
			Planet p = new Planet(new Vector2(	(float) (Math.cos(a)*radius3),
					(float) (Math.sin(a)*radius3)),
					SMALL_PLANET_RADIUS, 0);
			p.initBody(b2world);
			localPlanets.add(p);
		}
	}
	
	private void populateShips(int number) {
		ArrayList<DroidShip> ships = new ArrayList<DroidShip>();
		for (int i=0; i<number; i++) {
			DroidShip newDroid = new DroidShip(new Vector2(), 0, projectiles);
			newDroid.initNN();
			ships.add(newDroid);
		}
		populateShips(ships);
	}
	private void populateShips(ArrayList<DroidShip> ships) {
		boolean isEmpty, spawned;
		for (DroidShip droid: ships) {
			spawned = false;
			while (!spawned) {
				float radius = (float) (Math.random()*SPAWN_RADIUS);
				Vector2 pos = new Vector2().setToRandomDirection().scl(radius);
				isEmpty = true;
				for (DroidShip otherShip: droids) {
					if (pos.dst(otherShip.getPosition()) < 4.0f*otherShip.getBoundingRadius())
						isEmpty = false;
				}
				if (isEmpty) {
					float angle = (float) Math.random()*MathUtils.PI2;
					if (!droid.disposable)
						droid.dispose();
					droid.resetVars();
					droid.setPosition(pos);
					droid.setAngle(angle);
					droid.initBody(b2world);
					droids.add(droid);
					spawned = true;
				}
			}
		}
	}

	private ArrayList<DroidShip> newGeneration(ArrayList<DroidShip> winners) {
		ArrayList<DroidShip> newPool = new ArrayList<DroidShip>();
		for (DroidShip droid: winners) {
			for (int i=0; i<N_OFFSPRINGS; i++) {
				DroidShip offspring = droid.copy();
				offspring.nn.mutate();
				newPool.add(offspring);
			}
		}
		for (DroidShip droid: winners) {
			newPool.add(droid);
		}
		
		System.out.println(String.valueOf(newPool.size() + " new Droids created from " + String.valueOf(winners.size())));
		return newPool;
	}
	
	private void newPool() {
		currentPool = new DroidPool();
		currentPool.nnLayers = DroidShip.nnLayers;
		currentPool.activationFunc = DroidShip.activation;
		currentPool.offsprings = N_OFFSPRINGS;
		currentPool.startPop = STARTING_POP;
		currentPool.winnersPerGen = WINNERS_PER_GENERATION;
		currentGen = 0;
		scores.clear();
		scoreWinBest = 0;
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
	
	void importPool(String f) {
		Json json = new Json();
		FileHandle dirHandle = Gdx.files.internal("nn");
		for (FileHandle entry: dirHandle.list()) {
			String text = entry.readString();
			DroidPool pool = json.fromJson(DroidPool.class, text);
		}
	}
	
	void exportPool(ArrayList<DroidShip> ships) {
		System.out.println("### Saving current pool of droids to hard-drive");		
		
//		ArrayList<NeuralNetwork> nn = new ArrayList<NeuralNetwork>();
//		for (DroidShip ship: ships) {
//			nn.add(ship.nn);
//		}
		ArrayList<float[][][]> nn = new ArrayList<float[][][]>();
		for (DroidShip ship: ships) {
			nn.add(ship.nn.weights);
		}
		
		currentPool.nn = nn;
		currentPool.scores = scores;
		currentPool.generation = currentGen;
		
		Json json = new Json();
		json.setElementType(DroidPool.class, "nn", NeuralNetwork.class);
		json.setElementType(DroidPool.class, "nnBest", NeuralNetwork.class);
		json.setElementType(DroidPool.class, "scores", Float.class);
		FileHandle file = Gdx.files.local("nn/" + currentPool.id + ".txt");
		file.writeString(json.prettyPrint(json.toJson(currentPool)), false);
	}
}