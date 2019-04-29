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
		
		//if (f1.getUserData() == Enums.SHIP || f2.getUserData() == Enums.SHIP)
		//	System.out.println(f1.getUserData() + " has hit "+ f2.getUserData());
		
		if (f1.getUserData() == Enums.SATELLITE) {
			((Satellite) f1.getBody().getUserData()).detach();
		}
		if (f2.getUserData() == Enums.SATELLITE) {
			((Satellite) f2.getBody().getUserData()).detach();
		}
	}

	@Override
	public void endContact(Contact contact) {}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		Fixture f1 = contact.getFixtureA();
		Fixture f2 = contact.getFixtureB();
		
		//float hitforce = 0.0f;
		float hitforce = impulse.getNormalImpulses()[0];
			
		
		// Ship collision damage
		if (hitforce >= 1.0) {
			if (f1.getUserData() == Enums.SHIP) {
				((Spaceship) f1.getBody().getUserData()).hit(hitforce*hitforce);
			}
			if (f2.getUserData() == Enums.SHIP) {
				((Spaceship) f2.getBody().getUserData()).hit(hitforce*hitforce);
			}
			if (f1.getUserData() == Enums.DROID) {
				((DroidShip) f1.getBody().getUserData()).hit(hitforce*hitforce);
			}
			if (f2.getUserData() == Enums.DROID) {
				((DroidShip) f2.getBody().getUserData()).hit(hitforce*hitforce);
			}
		}
		
		// Detect landing
		if (f1.getUserData() == Enums.PLANET && f2.getUserData() == Enums.SHIP) {
			Vector2 normal = f1.getBody().getPosition().sub(f2.getBody().getPosition()).nor();
			Vector2 tangent = new Vector2(-normal.y, normal.x).nor();
			float normal_speed = normal.dot(f2.getBody().getLinearVelocity());
			float tangent_speed = Math.abs(tangent.dot(f2.getBody().getLinearVelocity()));
			if (normal_speed < 0.001f && tangent_speed < 0.001f && f2.getBody().getAngularVelocity() < 0.001f) {
				game.land((Planet) f1.getBody().getUserData());
			}
		} else if (f2.getUserData() == Enums.PLANET && f1.getUserData() == Enums.SHIP) {
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
