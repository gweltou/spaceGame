package com.gwel.entities;

import com.badlogic.gdx.utils.Pool;

public class AsteroidPool extends Pool<Asteroid> {

    public AsteroidPool(int init, int max) {
        super(init, max);
    }

    public AsteroidPool() {
        // make pool with default 16 initial objects and no max
        super();
    }

    @Override
    protected Asteroid newObject() {
        return new Asteroid();
    }
}
