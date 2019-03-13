package com.gwel.spacegame;

import com.badlogic.gdx.physics.box2d.*;
import com.gwel.entities.Satellite;

public class MyContactListener implements ContactListener {

	@Override
	public void beginContact(Contact contact) {
		// TODO Auto-generated method stub
		System.out.println("Contact");
		Fixture f1 = contact.getFixtureA();
		Fixture f2 = contact.getFixtureB();
		
		System.out.println(f1.getUserData() + " has hit "+ f2.getUserData());
		if (f1.getUserData() == "Satellite") {
			System.out.println("hit a satellite");
			((Satellite) f1.getBody().getUserData()).detach();
		}
		if (f2.getUserData() == "Satellite") {
			System.out.println("hit a satellite");
			((Satellite) f2.getBody().getUserData()).detach();
		}
		
		if (f1.getUserData() == "Landing" && f2.getUserData() == "Ship") {
			System.out.println("In landing zone");
		}
		if (f1.getUserData() == "Ship" && f2.getUserData() == "Landing") {
			System.out.println("In landing zone");
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
