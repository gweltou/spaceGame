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
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.TimeUtils;
import com.gwel.ai.DroidComparator;
import com.gwel.ai.NeuralNetwork;
import com.gwel.ai.DroidPool;
import com.gwel.entities.*;
import com.gwel.spacegame.Enums;
import com.gwel.spacegame.MyCamera;
import com.gwel.spacegame.SpaceContactListener;
import com.gwel.spacegame.SpaceGame;


public class ScreenTraining implements Screen {
	static private final int SIM_SPEED = 32;
	static private final float SPAWN_RADIUS = 30.0f;
	static private final float SMALL_PLANET_RADIUS = 15.0f;
	static private final float MEDIUM_PLANET_RADIUS = 22.0f;
	static private final float BIG_PLANET_RADIUS = 35.0f;
	static private final float WORLD_WIDTH =  2*(SPAWN_RADIUS + 5*SMALL_PLANET_RADIUS + 2*BIG_PLANET_RADIUS + DroidShip.SIGHT_DISTANCE);
	static private final float WORLD_HEIGHT = WORLD_WIDTH;
	static private final float BORDER_LEFT = -WORLD_WIDTH/2.0f;
	static private final float BORDER_RIGHT = WORLD_WIDTH/2.0f;
	static private final float BORDER_UP = WORLD_HEIGHT/2.0f;
	static private final float BORDER_DOWN = -WORLD_HEIGHT/2.0f;
	static private final boolean DEBUG_RENDERING = true;
	static private final int STARTING_POP = 32;
	static private final int WINNERS_PER_GENERATION = 6;
	static private final int N_OFFSPRINGS = 8;
	static private final int BRANCH_OUT_TRIES = 3;
	static private final int SIM_STEPS = 40000;
	
	final SpaceGame game;
	private final World world;
	private final MyCamera camera;
	private boolean destroy;
	
	private final ArrayList<Planet> localPlanets = new ArrayList<>();
	private final LinkedList<DroidShip> droids = new LinkedList<>();
	private final LinkedList<Projectile> projectiles = new LinkedList<>();
	private final PriorityQueue<DroidShip> podium = new PriorityQueue<>(5, new DroidComparator());
	private DroidPool currentPool;
	private int branchTries;
	
	Box2DDebugRenderer debugRenderer;
	ShapeRenderer renderer;
	
	private long lastFpsDisplay;
	private int steps;
	private boolean isEmpty;
	private float runMaxScore;
	
	
	public ScreenTraining(final SpaceGame game) {
		this.game = game;
		destroy = false;
		
		world = new World(new Vector2(0.0f, 0.0f), true);
		world.setContactListener(new SpaceContactListener(game));
		
		debugRenderer = new Box2DDebugRenderer();
		debugRenderer.setDrawAABBs(false);
		renderer = new ShapeRenderer();
		camera = new MyCamera(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		camera.setCenter(new Vector2(0.0f, 0.0f));
		camera.zoomTo(1.5f);
		camera.update();
		
		populatePlanets();
		lastFpsDisplay = TimeUtils.millis();
		steps = 0;
		branchTries = 0;
		runMaxScore = 0;
		
		//newPool();
		//populateShips(STARTING_POP);
		importPool("cb141565-a486-4173-8811-b08a1574aef9");
	}
	
	@Override
	public void render(float delta_time) {
		for (int i=0; i<SIM_SPEED; i++) {
			simStep();
		}
		
		handleInput();
		
		//  Camera update
//		if (!droids.isEmpty()) {
//			camera.glideTo(droids.getFirst().getPosition());
//		}

		Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		if (DEBUG_RENDERING) {
			// DEBUG RENDERING
			debugRenderer.render(world, new Matrix4().set(camera.affine));
			// Render projectiles
			renderer.setProjectionMatrix(new Matrix4().set(camera.affine));
			renderer.begin(ShapeType.Line);
			renderer.setColor(1, 0, 0, 1);
			for (Projectile proj: projectiles) {
				renderer.line(proj.position, proj.pPosition);
			}		
			renderer.end();
		} else {
			// CLASSIC RENDERING
			camera.update();
			game.renderer.setProjectionMatrix(new Matrix4().set(camera.affine));
			for (Planet p : localPlanets) {
				p.render(game.renderer);
			}
			for (DroidShip b : droids) {
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
			System.out.println("End of generation " + currentPool.generation);
			steps = 0;
			
			// Add remaining living droids to the podium
			for (DroidShip droid: droids) {
				droid.dispose();
				droid.setScore(steps);
				podium.add(droid);
			}
			droids.clear();
			
			// Calculate generation score
			System.out.print("Generations scores: ");
			float scoreGen = 0;
			for (DroidShip droid: podium) {
				scoreGen += droid.getScore();
			}
			scoreGen /= podium.size();
			currentPool.scores.add(Math.round(scoreGen));
			for (int score: currentPool.scores) {
				System.out.print("__");
				System.out.print(score);
			}
			System.out.print("\n");
			
			// Select the best droids of this generation
			System.out.print("Individual scores: ");
			ArrayList<DroidShip> winners = new ArrayList<>();
			for (int i = Math.min(currentPool.winnersPerGen, podium.size()); i >= 1; i--) {
				DroidShip droid = podium.remove();
				System.out.print("__" + droid.getScore());
				winners.add(droid);
			}
			System.out.print("\n");
			
			// Calculate winners mean score
			float scoreWin = 0;
			for (DroidShip droid: winners) {
				scoreWin += droid.getScore();
			}
			scoreWin /= winners.size();
			if (scoreWin > currentPool.bestGenScore) {
				currentPool.bestGenScore = (int) scoreWin;
				System.out.println("### New generation record ! : " + currentPool.bestGenScore);
				// Save best winners to pool (we will branch out from this generation if needed)
				ArrayList<float[][][]> nn = new ArrayList<>();
				for (DroidShip ship: winners) {
					nn.add(ship.nn.weights.clone());
				}
				currentPool.nnBest = nn;
				currentPool.bestGen = currentPool.generation;
				branchTries = 0;
				
				// Export only better species to hard-drive
				if (scoreWin > runMaxScore) {
					runMaxScore = scoreWin;
					exportPool(winners);
				}
			}
			
			if (currentPool.generation - currentPool.bestGen >= 8) {
				// Evolution stalled for too long, branch out from last best winners
				branchTries++;
				if (branchTries >= BRANCH_OUT_TRIES) {
					// Tried to branch out too many times, creating a new pool
					branchTries = 0;
					podium.clear();
					newPool();
					populateShips(STARTING_POP);
				} else {
					System.out.println("### New branch from generation " + currentPool.bestGen + " [" + branchTries + "]");
					podium.clear();
					currentPool.generation = currentPool.bestGen+1;
					// Set the winners with nn from the last best batch
					for (int i=0; i<winners.size(); i++) {
						winners.get(i).nn.weights = currentPool.nnBest.get(i);
					}
					populateShips(newGeneration(winners));
				}
			} else {
				// Respawn current pool
				podium.clear();
				droids.clear();
				populateShips(newGeneration(winners));
				currentPool.generation += 1;
			}
		}

		// UPDATING GAME STATE
		world.step(1.0f/60f, 8, 3);
		
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
		
		// Clean up dead Droids and wrap-around screen borders
		ListIterator<DroidShip> droidsIter = droids.listIterator();
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
				droid.initBody(world);
			} else if (droid.getPosition().x < BORDER_LEFT) {
				droid.dispose();
				droid.setPosition(droid.getPosition().add(WORLD_WIDTH, 0));
				droid.initBody(world);
			} else if (droid.getPosition().y > BORDER_UP) {
				droid.dispose();
				droid.setPosition(droid.getPosition().sub(0, WORLD_HEIGHT));
				droid.initBody(world);
			} else if (droid.getPosition().y < BORDER_DOWN) {
				droid.dispose();
				droid.setPosition(droid.getPosition().add(0, WORLD_HEIGHT));
				droid.initBody(world);
			}	
		}
		
		// Applying gravity to the Droids
		for (Planet pl: localPlanets) {
			for (DroidShip bod: droids) {
				bod.push(pl.getGravityAccel(bod.getPosition()).scl(bod.getMass()));
			}
		}
		// Removing projectiles outside of local zone
		ListIterator<Projectile> projIter = projectiles.listIterator();
		while (projIter.hasNext()) {
			Projectile proj = projIter.next();
			if (proj.disposable || proj.position.x < BORDER_LEFT || proj.position.x > BORDER_RIGHT ||
					proj.position.y < BORDER_DOWN || proj.position.y > BORDER_UP)
				projIter.remove();
			else
			    proj.update(world, 1.0f);
		}
		
		steps++;
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
			p.initBody(world);
			localPlanets.add(p);
			p = new Planet(new Vector2(	(float) (Math.cos(angle+Math.PI/6)*radius2),
					 						(float) (Math.sin(angle+Math.PI/6)*radius2)),
					 						BIG_PLANET_RADIUS, 0);
			p.initBody(world);
			localPlanets.add(p);
			angle += MathUtils.PI2/6;
		}
		float[] angles = {(float) (2*Math.PI/6), (float) (4*Math.PI/6), (float) (-2*Math.PI/6), (float) (-4*Math.PI/6)};
		for (float a: angles) {
			Planet p = new Planet(new Vector2(	(float) (Math.cos(a)*radius3),
					(float) (Math.sin(a)*radius3)),
					MEDIUM_PLANET_RADIUS, 0);
			p.initBody(world);
			localPlanets.add(p);
		}
	}
	
	private void populateShips(int number) {
		ArrayList<DroidShip> ships = new ArrayList<>();
		for (int i=0; i<number; i++) {
			DroidShip newDroid = new DroidShip(new Vector2(), 0, projectiles);
			newDroid.initNN(currentPool.activationFunc);
			ships.add(newDroid);
		}
		populateShips(ships);
	}
	private void populateShips(ArrayList<DroidShip> ships) {
		for (DroidShip droid: ships) {
			isEmpty = false;
			float posX = 0;
			float posY = 0;
			while (!isEmpty) {
				isEmpty = true;
				posX = MathUtils.random(BORDER_LEFT, BORDER_RIGHT);
				posY = MathUtils.random(BORDER_DOWN, BORDER_UP);
				world.QueryAABB(new QueryCallback() {
					@Override
					public boolean reportFixture(Fixture fixture) {
						isEmpty = false;
						return true;
					}}, posX-droid.getBoundingRadius(), posY-droid.getBoundingRadius(), posX+droid.getBoundingRadius(), posY+droid.getBoundingRadius());
			}

			Vector2 pos = new Vector2(posX, posY);
			float angle = (float) Math.random()*MathUtils.PI2;
			if (!droid.disposable)
				droid.dispose();
			droid.resetVars();
			droid.setPosition(pos);
			droid.setAngle(angle);
			droid.initBody(world);
			droids.add(droid);

			/*
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
		*/
		}
	}

	private ArrayList<DroidShip> newGeneration(ArrayList<DroidShip> winners) {
		ArrayList<DroidShip> newPool = new ArrayList<>();
		for (DroidShip droid: winners) {
			for (int i=0; i<currentPool.offsprings; i++) {
				DroidShip offspring = droid.copy();
				offspring.nn.mutate();
				newPool.add(offspring);
			}
		}
		newPool.addAll(winners);
		
		System.out.println(newPool.size() + " new Droids created from " + winners.size());
		return newPool;
	}
	
	private void newPool() {
		currentPool = new DroidPool();
		currentPool.nnLayers = DroidShip.nnLayers;
		currentPool.activationFunc = DroidShip.activation;
		currentPool.offsprings = N_OFFSPRINGS;
		currentPool.startPop = STARTING_POP;
		currentPool.winnersPerGen = WINNERS_PER_GENERATION;
		System.out.println("### New pool created: " + currentPool.id);
	}
	
	private void handleInput() {
		if (game.hasController) {
			PovDirection pov = game.controller.getPov(0);
			if (pov == PovDirection.north) {
				camera.zoomIn();
				camera.autozoom = false;
			}
			if (pov == PovDirection.south) {
				camera.zoomOut();
				camera.autozoom = false;
			}
		}
		if (Gdx.input.isKeyPressed(Keys.A)) {
			camera.zoomIn();
			camera.autozoom = false;
		}
		if (Gdx.input.isKeyPressed(Keys.Z)) {
			camera.zoomOut();
			camera.autozoom = false;
		}
	}
	
	void importPool(String filename) {
		System.out.println("### Importing pool '" + filename + "' from hard-drive");
		Json json = new Json();
		FileHandle entry = Gdx.files.internal("nn/" + filename + ".txt");
		DroidPool pool;
//		for (FileHandle entry: dirHandle.list()) {
			String text = entry.readString();
			pool = json.fromJson(DroidPool.class, text);
//		}
		currentPool = pool;
		droids.clear();
		podium.clear();
		runMaxScore = pool.bestGenScore;
		
		ArrayList<DroidShip> newDroids = new ArrayList<>();
		for (int i=0; i<pool.nn.size(); i++) {
			DroidShip droid = new DroidShip(new Vector2(), 0, projectiles);
			droid.initNN(pool.activationFunc);
			droid.nn.weights = pool.nn.get(i).clone();
			newDroids.add(droid);
		}
		populateShips(newGeneration(newDroids));	
	}
	
	void exportPool(ArrayList<DroidShip> ships) {
		System.out.println("### Saving current pool of droids to hard-drive");		
		
//		ArrayList<NeuralNetwork> nn = new ArrayList<NeuralNetwork>();
//		for (DroidShip ship: ships) {
//			nn.add(ship.nn);
//		}
		ArrayList<float[][][]> nn = new ArrayList<>();
		for (DroidShip ship: ships) {
			nn.add(ship.nn.weights);
		}
		currentPool.nn = nn;
		
		Json json = new Json();
		json.setElementType(DroidPool.class, "nn", NeuralNetwork.class);
		json.setElementType(DroidPool.class, "nnBest", NeuralNetwork.class);
		json.setElementType(DroidPool.class, "scores", Float.class);
		FileHandle file = Gdx.files.local("nn/" + currentPool.id + ".txt");
		file.writeString(json.prettyPrint(json.toJson(currentPool)), false);
	}
	
	@Override
	public void dispose() {
		// This test case prevents the world from being destroyed during a step
		if (destroy) {
			System.out.println("Destroying space");
			world.dispose();
		} else {
			destroy = true;
		}
	}
	
	@Override
	public void hide() {}

	@Override
	public void pause() {}
	
	@Override
	public void resize(int width, int height) {}

	@Override
	public void resume() {}

	@Override
	public void show() {}
}