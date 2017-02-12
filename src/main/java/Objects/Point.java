/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objects;

import baseinvaders.Configurations;
import java.io.Serializable;

/**
 *
 * @author Aleks
 */
public class Point implements Serializable {

    private volatile double x, y;

    public Point(Point point) {
        this(point.getX(), point.getY());
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
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
        final Point other = (Point) obj;
        if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
            return false;
        }
        return true;
    }

    public double getX() {
        return x;
    }

    public Point setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public Point setY(double y) {
        this.y = y;
        return this;
    }

    public Point add(double x, double y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public static double distance(Point a, Point b) {
        double dist = Double.POSITIVE_INFINITY;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                dist = Math.min(Math.sqrt(Math.pow(a.x - (b.x + x * Configurations.getMapWidth()), 2) + Math.pow(a.y - (b.y + y * Configurations.getMapHeight()), 2)), dist);
            }
        }
        return dist;
    }

    public Point add(Point p) {
        this.x += p.x;
        this.y += p.y;
        return this;
    }

    public Point multiply(double m) {
        this.x *= m;
        this.y *= m;
        return this;
    }

    public double distanceTo(Point p) {
        return distance(this, p);
    }

    public static double directionTo(Point f, Point t) {
        double dist = Double.POSITIVE_INFINITY;
        double sx = t.x;
        double sy = t.y;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                double ld = Math.sqrt(Math.pow(f.x - (t.x + x * Configurations.getMapWidth()), 2) + Math.pow(f.y - (t.y + y * Configurations.getMapHeight()), 2));
                if (ld < dist) {
                    dist = ld;
                    sx = (t.x + x * Configurations.getMapWidth());
                    sy = (t.y + y * Configurations.getMapHeight());
                }
            }
        }
        return Math.atan2(sy - f.y, sx - f.x);
    }

    public double directionTo(Point p) {
        return directionTo(this, p);
    }

    @Override
    public String toString() {
        return "Point{" + "x=" + x + ", y=" + y + '}';
    }
}
