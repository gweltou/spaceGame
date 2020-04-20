package com.gwel.surfaceEntities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.gwel.entities.Planet;
import com.gwel.spacegame.MyRenderer;

import java.util.Iterator;
import java.util.LinkedList;


public class InhabitantLayer implements Disposable {
    private final World world;
    private final float surfaceLength;
    private final LinkedList<Inhabitant> idle;
    private final LinkedList<Inhabitant>  alive;

    public InhabitantLayer(World world, Planet planet, Terrain terrain) {
        this.world = world;
        this.surfaceLength = planet.surfaceLength;
        this.idle = new LinkedList<>();
        this.alive = new LinkedList<>();

        //float inhabitantDensity = Math.max(0.0f, MathUtils.random(-0.1f, 0.2f));
        float inhabitantDensity = 0.1f;
        int numInhabitants = (int) (surfaceLength * inhabitantDensity);

        for (int i = 0; i < numInhabitants; i++) {
            float xPos = MathUtils.random(surfaceLength);
            Vector2 pos = new Vector2(xPos, terrain.getHeight(xPos));
            idle.add(new Inhabitant(pos));
        }

        System.out.print("InhabitantLayer created with " + idle.size() + " inhabitants");
        System.out.println(" (" + inhabitantDensity + " density)");
    }

    void addBetween(float leftCoord, float rightCoord) {
        // NPCs in idle list must have a position between 0 and surfaceLength
        //System.out.print("addBetween " + leftCoord + " and " + rightCoord);
        float offset = 0.0f;

        while (leftCoord < 0.0f) {
            leftCoord += surfaceLength;
            rightCoord += surfaceLength;
            offset -= surfaceLength;
        }
        while (leftCoord > surfaceLength) {
            leftCoord -= surfaceLength;
            rightCoord -= surfaceLength;
            offset += surfaceLength;
        }
        //System.out.println(" ("+leftCoord+" and "+rightCoord+", offset "+offset+")");

        Iterator it = idle.iterator();
        if (rightCoord < surfaceLength) {
            while (it.hasNext()) {
                Inhabitant npc = (Inhabitant) it.next();
                Vector2 pos = npc.getPosition();
                if (pos.x >= leftCoord && pos.x < rightCoord) {
                    it.remove();
                    pos.add(offset, 0.0f);
                    npc.setPosition(pos);
                    npc.initBody(world);
                    alive.add(npc);
                }
            }
        } else {
            while (it.hasNext()) {
                Inhabitant npc = (Inhabitant) it.next();
                Vector2 pos = npc.getPosition();
                if (pos.x >= leftCoord && pos.x < rightCoord) {
                    it.remove();
                    pos.add(offset, 0);
                    npc.setPosition(pos);
                    npc.initBody(world);
                    alive.add(npc);
                } else if (pos.x >= 0.0 && pos.x < rightCoord-surfaceLength) {
                    it.remove();
                    pos.add(offset+surfaceLength, 0);
                    npc.setPosition(pos);
                    npc.initBody(world);
                    alive.add(npc);
                }
            }
        }
    }

    void removeBetween(float leftCoord, float rightCoord) {
        //System.out.println("removeBetween " + leftCoord + " and " + rightCoord);

        Iterator it = alive.iterator();
        while (it.hasNext()) {
            Inhabitant npc = (Inhabitant) it.next();
            Vector2 pos = npc.getPosition();
            if (pos.x >= leftCoord && pos.x < rightCoord) {
                it.remove();
                float x = pos.x % surfaceLength;
                x += x < 0.0f ? surfaceLength : 0.0f;
                npc.dispose();
                npc.setPosition(new Vector2(x, pos.y));
                idle.add(npc);
            }
        }
    }

    LinkedList<Inhabitant> getInhabitants() {
        return alive;
    }

    public void update() {
        for (Inhabitant npc : alive) {
            npc.update();
        }
    }

    public void render(MyRenderer renderer) {
        for (Inhabitant npc : alive) {
            npc.render(renderer);
        }
    }

    @Override
    public void dispose() {
        for (Inhabitant npc : alive) {
            npc.dispose();
        }
    }
}
