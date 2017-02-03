/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objects;

import java.io.Serializable;

/**
 *
 * @author Aleks
 */
public abstract class GameObject implements Serializable {

    public GameObject(Point position) {
        this.position = new Point(position);
        synchronized (sync) {
            id = nextId++;
        }
    }

    private static final Object sync = new Object();
    private static Long nextId = 0L;
    private final long id;
    private final Point position;

    public Point getPosition() {
        return position;
    }

    public long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + (int) (this.id ^ (this.id >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GameObject other = (GameObject) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    public abstract void nextRound();

    public double distanceTo(GameObject other) {
        return position.distanceTo(other.position);
    }

    public double distanceTo(Point p) {
        return position.distanceTo(p);
    }

    public double directionTo(Point p) {
        return position.directionTo(p);
    }

    public double directionTo(GameObject other) {
        return position.directionTo(other.position);
    }

    @Override
    public String toString() {
        return "GameObject{" + "id=" + id + ", position=" + position + '}';
    }

}
