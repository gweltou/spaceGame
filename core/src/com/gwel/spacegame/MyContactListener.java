package com.gwel.spacegame;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.gwel.entities.DroidShip;
import com.gwel.entities.Planet;
import com.gwel.entities.Satellite;
import com.gwel.entities.Spaceship;

public class MyContactListener implements ContactListener {
	SpaceGame game;
	
	public MyContactListener(SpaceGame game) {
		this.game = game;
	}
	
	@Override
	public void beginContact(Contact contact) {
		Fixture f1 = contact.getFixtureA();
		Fixture f2 = contact.getFixtureB();
		
		
		//System.out.println(f1.getUserData() + " has hit "+ f2.getUserData());
		if (f1.getUserData() == "Satellite") {
			((Satellite) f1.getBody().getUserData()).detach();
		}
		if (f2.getUserData() == "Satellite") {
			((Satellite) f2.getBody().getUserData()).detach();
		}
		
		// Sensors for Neural Network
		if (f1.getUserData() == Enum.SENSOR_F || f2.getUserData() == Enum.SENSOR_F) {
			Fixture f = f1;
			if (f2.getUserData() == Enum.SENSOR_F) {
				f = f2;
			}
			float[] distances = contact.getWorldManifold().getSeparations();
			for (float distance: distances) {
				((DroidShip) f.getBody().getUserData()).setSensor(Enum.SENSOR_F, distance);
			}
		}
		if (f1.getUserData() == Enum.SENSOR_FR || f2.getUserData() == Enum.SENSOR_FR) {
			Fixture f = f1;
			if (f2.getUserData() == Enum.SENSOR_FR) {
				f = f2;
			}
			float[] distances = contact.getWorldManifold().getSeparations();
			for (float distance: distances) {
				((DroidShip) f.getBody().getUserData()).setSensor(Enum.SENSOR_FR, distance);
			}
		}
		if (f1.getUserData() == Enum.SENSOR_FL || f2.getUserData() == Enum.SENSOR_FL) {
			Fixture f = f1;
			if (f2.getUserData() == Enum.SENSOR_FL) {
				f = f2;
			}
			float[] distances = contact.getWorldManifold().getSeparations();
			for (float distance: distances) {
				((DroidShip) f.getBody().getUserData()).setSensor(Enum.SENSOR_FL, distance);
			}
		}
		if (f1.getUserData() == Enum.SENSOR_MR || f2.getUserData() == Enum.SENSOR_MR) {
			Fixture f = f1;
			if (f2.getUserData() == Enum.SENSOR_MR) {
				f = f2;
			}
			float[] distances = contact.getWorldManifold().getSeparations();
			for (float distance: distances) {
				((DroidShip) f.getBody().getUserData()).setSensor(Enum.SENSOR_MR, distance);
			}
		}
		if (f1.getUserData() == Enum.SENSOR_ML || f2.getUserData() == Enum.SENSOR_ML) {
			Fixture f = f1;
			if (f2.getUserData() == Enum.SENSOR_ML) {
				f = f2;
			}
			float[] distances = contact.getWorldManifold().getSeparations();
			for (float distance: distances) {
				((DroidShip) f.getBody().getUserData()).setSensor(Enum.SENSOR_ML, distance);
			}
		}
		if (f1.getUserData() == Enum.SENSOR_BR || f2.getUserData() == Enum.SENSOR_BR) {
			Fixture f = f1;
			if (f2.getUserData() == Enum.SENSOR_BR) {
				f = f2;
			}
			float[] distances = contact.getWorldManifold().getSeparations();
			for (float distance: distances) {
				((DroidShip) f.getBody().getUserData()).setSensor(Enum.SENSOR_BR, distance);
			}
		}
		if (f1.getUserData() == Enum.SENSOR_BL || f2.getUserData() == Enum.SENSOR_BL) {
			Fixture f = f1;
			if (f2.getUserData() == Enum.SENSOR_BL) {
				f = f2;
			}
			float[] distances = contact.getWorldManifold().getSeparations();
			for (float distance: distances) {
				((DroidShip) f.getBody().getUserData()).setSensor(Enum.SENSOR_BL, distance);
			}
		}
	}

	@Override
	public void endContact(Contact contact) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		Fixture f1 = contact.getFixtureA();
		Fixture f2 = contact.getFixtureB();
		
		float hitforce = 0.0f;
		//hitforce += impulse.getNormalImpulses()[0];
		//hitforce += impulse.getTangentImpulses()[0];
		
		for (float force: impulse.getNormalImpulses())
			hitforce += force;
		
		// Ship collision damage
		if (f1.getUserData() == Enum.SHIP && hitforce >= 1.0f) {
			((Spaceship) f1.getBody().getUserData()).hit(hitforce*hitforce);
		}
		if (f2.getUserData() == Enum.SHIP && hitforce >= 1.0f) {
			((Spaceship) f2.getBody().getUserData()).hit(hitforce*hitforce);
		}
		if (f1.getUserData() == Enum.DROID && hitforce >= 0f) {
			((DroidShip) f1.getBody().getUserData()).hit(hitforce*hitforce);
		}
		if (f2.getUserData() == Enum.DROID && hitforce >= 0f) {
			((DroidShip) f2.getBody().getUserData()).hit(hitforce*hitforce);
		}
		
		// Detect landing
		if (f1.getUserData() == "Planet" && f2.getUserData() == "Ship") {
			Vector2 normal = f1.getBody().getPosition().sub(f2.getBody().getPosition()).nor();
			Vector2 tangent = new Vector2(-normal.y, normal.x).nor();
			float normal_speed = normal.dot(f2.getBody().getLinearVelocity());
			float tangent_speed = Math.abs(tangent.dot(f2.getBody().getLinearVelocity()));
			if (normal_speed < 0.001f && tangent_speed < 0.001f && f2.getBody().getAngularVelocity() < 0.001f) {
				game.land((Planet) f1.getBody().getUserData());
			}
		} else if (f2.getUserData() == "Planet" && f1.getUserData() == "Ship") {
			Vector2 normal = f2.getBody().getPosition().sub(f1.getBody().getPosition()).nor();
			Vector2 tangent = new Vector2(-normal.y, normal.x).nor();
			float normal_speed = normal.dot(f1.getBody().getLinearVelocity());
			float tangent_speed = Math.abs(tangent.dot(f1.getBody().getLinearVelocity()));
			if (normal_speed < 0.001f && tangent_speed < 0.001f && f2.getBody().getAngularVelocity() < 0.001f) {
				game.land((Planet) f2.getBody().getUserData());
			}
		}
		
	}
}
