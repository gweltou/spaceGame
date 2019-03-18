package com.gwel.spacegame;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
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
		
		// Ship entered the planet's landing zone
		if (f1.getUserData() == "Planet" && f2.getUserData() == "Ship") {
			Vector2 normal = f1.getBody().getPosition().sub(f2.getBody().getPosition()).nor();
			Vector2 tangent = new Vector2(-normal.y, normal.x).nor();
			float normal_speed = normal.dot(f2.getBody().getLinearVelocity());
			float tangent_speed = Math.abs(tangent.dot(f2.getBody().getLinearVelocity()));
			if (normal_speed < 1.0f && tangent_speed < 0.5f) {
				game.land((Planet) f1.getBody().getUserData());
			}
		} else if (f2.getUserData() == "Planet" && f1.getUserData() == "Ship") {
			Vector2 normal = f2.getBody().getPosition().sub(f1.getBody().getPosition()).nor();
			Vector2 tangent = new Vector2(-normal.y, normal.x).nor();
			float normal_speed = normal.dot(f1.getBody().getLinearVelocity());
			float tangent_speed = Math.abs(tangent.dot(f1.getBody().getLinearVelocity()));
			if (normal_speed < 1.0f && tangent_speed < 0.5f) {
				game.land((Planet) f2.getBody().getUserData());
			}
		}
	}

	@Override
	public void endContact(Contact contact) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		Fixture f1 = contact.getFixtureA();
		Fixture f2 = contact.getFixtureB();
		
		float hitforce = 0.0f;
		hitforce += impulse.getNormalImpulses()[0];
		hitforce += impulse.getTangentImpulses()[0];
		/*
		for (float force: impulse.getNormalImpulses())
			hitforce += force;
		*/
		// Ship collision damage
		if (f1.getUserData() == "Ship" && hitforce >= 1.0f) {
			((Spaceship) f1.getBody().getUserData()).hit(hitforce*hitforce);
		}
		if (f2.getUserData() == "Ship" && hitforce >= 1.0f) {
			((Spaceship) f1.getBody().getUserData()).hit(hitforce*hitforce);
		}
	}
}
