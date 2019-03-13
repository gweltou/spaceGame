package com.gwel.spacegame;

import com.badlogic.gdx.physics.box2d.*;
import com.gwel.entities.Satellite;

public class MyContactListener implements ContactListener {

	@Override
	public void beginContact(Contact contact) {
		// TODO Auto-generated method stub
		System.out.println("Contact");
		Body b1 = contact.getFixtureA().getBody();
		Body b2 = contact.getFixtureB().getBody();
		
		System.out.println(b1.getType()+" has hit "+ b2.getType());
		if (b1.getUserData().getClass() == Satellite.class) {
			System.out.println("hit a satellite");
			((Satellite) b1.getUserData()).detach();
		}
		if (b2.getUserData().getClass() == Satellite.class) {
			System.out.println("hit a satellite");
			((Satellite) b2.getUserData()).detach();
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
