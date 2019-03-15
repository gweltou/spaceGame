package com.gwel.spacegame;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.gwel.entities.Satellite;

public class MyContactListener implements ContactListener {

	@Override
	public void beginContact(Contact contact) {
		// TODO Auto-generated method stub
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
			if (normal_speed < 2.0f && tangent_speed < 1.0f) {
				System.out.println("LANDED !");
			}
		} else if (f2.getUserData() == "Planet" && f1.getUserData() == "Ship") {
			Vector2 normal = f2.getBody().getPosition().sub(f1.getBody().getPosition()).nor();
			Vector2 tangent = new Vector2(-normal.y, normal.x).nor();
			float normal_speed = normal.dot(f1.getBody().getLinearVelocity());
			float tangent_speed = Math.abs(tangent.dot(f1.getBody().getLinearVelocity()));
			if (normal_speed < 2.0f && tangent_speed < 1.0f) {
				System.out.println("LANDED !");
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
		// TODO Auto-generated method stub
		
	}

}
