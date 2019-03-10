package com.gwel.spacegame;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
//import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
//import com.badlogic.gdx.math.Vector3;

import Entities.PhysicBody;
import Entities.Planet;
import Entities.Satellite;
import Entities.Spaceship;
import Entities.ShipTail;


public class SpaceGame extends Game {
	MyCamera camera;
	
	MyRenderer renderer;
	Controller controller;
	boolean hasController;
	
	// GAME UNIVERSE VARIABLES
	final static float UNIVERSE_SIZE = 100000.0f;
	final static float GRAVITY_ACTIVE_RADIUS = 800.0f;
	final static int NUMBER_PLANETS = 10000;
	float local_radius = GRAVITY_ACTIVE_RADIUS;
	float exit_radius = GRAVITY_ACTIVE_RADIUS+200.0f;
	QuadTree Qt;
	Spaceship ship;
	ShipTail tail1, tail2;
	
	// For Physics
	ArrayList<Planet> local_planets = new ArrayList<Planet>();
	ArrayList<Planet> local_planets_prev;
	LinkedList<Satellite> local_sats = new LinkedList<Satellite>();
	LinkedList<PhysicBody> free_bodies = new LinkedList<PhysicBody>();
	LinkedList<PhysicBody> to_free_bodies = new LinkedList<PhysicBody>();
	ListIterator<PhysicBody> bod_iter;
	ListIterator<Satellite> sat_iter;
		
	@Override
	public void create () {
		if(Controllers.getControllers().size == 0)
            hasController = false;
        else {
            controller = Controllers.getControllers().first();
            hasController = true;
        }
				
		AABB universe_boundary = new AABB(new Vector2(0, 0), new Vector2(UNIVERSE_SIZE, UNIVERSE_SIZE));
		Qt = new QuadTree(universe_boundary);
		populateUniverse(Qt);
		ship = new Spaceship(new Vector2(UNIVERSE_SIZE/2, UNIVERSE_SIZE/2));
		tail1 = new ShipTail(ship, new Vector2(0.8f, 0.2f), 0.2f);
		tail2 = new ShipTail(ship, new Vector2(-0.70f, 0.2f), 0.2f);
		camera = new MyCamera(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		camera.setCenter(ship.position);
		camera.update();
		free_bodies.add(ship);
		
		
		renderer = new MyRenderer(camera);
	}

	@Override
	public void render () {
		handleInput();

		// UPDATING GAME STATE
		AABB local_range = new AABB(ship.position.cpy().sub(local_radius, local_radius),
				ship.position.cpy().add(local_radius, local_radius));
		AABB exit_range = new AABB(ship.position.cpy().sub(exit_radius, exit_radius),
				ship.position.cpy().add(exit_radius, exit_radius));
		local_planets_prev = local_planets;
		local_planets = Qt.query(local_range);

		// Check for planets that exited the local zone
		ArrayList<Planet> exited = new ArrayList<Planet>();
		for (Planet pl : local_planets_prev) {
			if (!local_range.containsPoint(pl.position)) {
				exited.add(pl);
			}
		}
		for (Planet pl: local_planets) {
			pl.update(); // Updates children satellites as well
		}
		for (PhysicBody bod: free_bodies) {
			bod.update();
		}
		
		// PHYSICS
		bod_iter = free_bodies.listIterator();
		while (bod_iter.hasNext()) {
			PhysicBody bod = bod_iter.next();

			// Free_body - Planets collisions
			for (Planet pl : local_planets) {
				if (!local_planets_prev.contains(pl)) {
					// Planetary System has newly entered the local range
					local_sats.addAll(pl.activateSatellites());
				}
				Vector2 dPos = pl.position.cpy().sub(bod.position); // Vector from body to planet
				float dist2 = dPos.len2();
				// Maybe it's useless to limit the planet that actually attracts to a circular zone...
				if (dist2 < GRAVITY_ACTIVE_RADIUS*GRAVITY_ACTIVE_RADIUS) {
					// Planet is in the active zone
					// Physic body is affected by the planet's gravity field
					bod.push(pl.getGravityForce(dPos).scl(bod.mass));
				}
				// Check if body collides with planet
				if (dist2 < pl.radius * pl.radius) {
					System.out.println("colliding with planet");
					//float a = dPos.heading() - ship.speed.heading();
					float a = bod.speed.angleRad(dPos);
					// Set the body position outside the planet surface and reflect its speed
					//bod.position.set(dPos.setLength(pl.radius+0.1f).add(pl.position));
					bod.position.set(pl.position.cpy().sub(dPos.setLength(pl.radius+0.1f)));
					bod.speed.rotateRad(2*a);
					bod.speed.scl(-0.8f); // A Bump will slow the ship down
				}
			}

			// Free_body - Satellites collision
			sat_iter = local_sats.listIterator();
			while (sat_iter.hasNext()) {
				Satellite sat = sat_iter.next();

				// Remove satellite if it belongs to an exited planet (outside of local zone)
				if (exited.contains(sat.parent)) {
					//System.out.println("Satellite removed from local_sats");
					sat_iter.remove();
					continue;
				}

				Vector2 dPos = sat.position.cpy().sub(bod.position); // Vector from body to satellite
				
				float dist2 = dPos.len2();
				// Check if body collides with satellite
				if (dist2 < sat.radius*sat.radius) {
					// Elastic collision between body and satellite
					Vector2 tangent = new Vector2(-dPos.y, dPos.x).nor(); // Perpendicular to dPos
					Vector2 norm = dPos.cpy().nor();
					float vn_b = norm.dot(bod.speed);
					float vn_s = norm.dot(sat.speed);
					float vt_b = tangent.dot(bod.speed);
					float vt_s = tangent.dot(sat.speed);
					float nvn_b = (vn_b * (bod.mass-sat.mass) + 2*sat.mass*vn_s) / (bod.mass+sat.mass);
					float nvn_s = (vn_s * (sat.mass-bod.mass) + 2*bod.mass*vn_b) / (bod.mass+sat.mass);
					Vector2 v_b = norm.cpy().scl(nvn_b).add(tangent.cpy().scl(vt_b));
					Vector2 v_s = norm.cpy().scl(nvn_s).add(tangent.cpy().scl(vt_s));
					
					bod.push(v_b.sub(bod.speed).scl(bod.mass));
					sat.push(v_s.sub(sat.speed).scl(sat.mass));
					sat.detach();
					sat_iter.remove();
					to_free_bodies.add(sat);
				}
			}
			// remove body from list if it's outside local range
			if (!exit_range.containsPoint(bod.position)) {
				System.out.println("free body removed");
				bod_iter.remove();
			}
		}
		for (PhysicBody bod: to_free_bodies) {
			free_bodies.add(bod);
		}
		to_free_bodies.clear();
		
		tail1.update();
		tail2.update();

		
		//  Camera update
		camera.glideTo(ship.position);
		if (camera.autozoom)
			camera.zoomTo(4.0f/ship.speed_mag);
		//stars.update(camera.getTravelling());
		//stars.render(camera);
		camera.update();
		
		// North and East directions are POSITIVE !
		AABB camera_range = new AABB(camera.sw.cpy().sub(Planet.MAX_RADIUS, Planet.MAX_RADIUS), 
				camera.ne.cpy().add(Planet.MAX_RADIUS, Planet.MAX_RADIUS));
		ArrayList<Planet> planets_draw = Qt.query(camera_range);
		//tail.render(camera);


		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		//shapeRenderer.begin(ShapeType.Filled);
		
		for (Planet p : planets_draw) {
			p.render(renderer);
		}
		tail1.render(renderer);
		tail2.render(renderer);
		for (PhysicBody b : free_bodies) {
			if (camera_range.containsPoint(b.position)) {
				b.render(renderer);
			}
		}
		renderer.flush();
		//System.out.println(free_bodies.size());
	}
	
	
	private void handleInput() {
		if (hasController) {
			if(controller.getButton(Ps4Controller.CROSS)) {
				ship.accelerate(0.005f);
				camera.autozoom = true;
			}
			PovDirection pov = controller.getPov(0);
			if (pov == PovDirection.north) {
				camera.zoom(1.04f);
				camera.autozoom = false;
			}
			if (pov == PovDirection.south) {
				camera.zoom(0.95f);
				camera.autozoom = false;
			}
			float x_axis = controller.getAxis(Ps4Controller.LSTICK_X);
			float y_axis = controller.getAxis(Ps4Controller.LSTICK_Y);
			if (Math.abs(x_axis) > 0.25)
				ship.steer(-x_axis / 10.0f);
		}
	}

	@Override
	public void resize (int width, int height) {
		System.out.println("Resizing camera viewport");
		camera.width = width;
		camera.height = height;
		camera.update();
	}
	
	@Override
	public void dispose () {
		 renderer.dispose();
	}

	
	void populateUniverse(QuadTree qt) {
		long start_time = TimeUtils.millis();
		int i=0;
		while (i<NUMBER_PLANETS) {
			Vector2 position = new Vector2(MathUtils.random(UNIVERSE_SIZE), MathUtils.random(UNIVERSE_SIZE));
			float radius = MathUtils.random(Planet.MIN_RADIUS, Planet.MAX_RADIUS);

			// Check if other planets are near this position
			float min_dist = 3*Planet.MAX_RADIUS;
			// North and East directions are POSITIVE !
			AABB range = new AABB(position.cpy().sub(min_dist, min_dist),
								  position.cpy().add(min_dist, min_dist));
			ArrayList<Planet> neighbours = qt.query(range);
			boolean empty = true;
			for (Planet p : neighbours) {
				if (position.dst2(p.position) < min_dist*min_dist) {
					empty = false;
					break;
				}
			}
			if (empty) {
				Planet new_planet = new Planet(position, radius);
				qt.insert(new_planet);
				i += 1;
			}
		}
		System.out.print("time spent populating (ms): ");
		System.out.println(TimeUtils.millis()-start_time);
	}
}