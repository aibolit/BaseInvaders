/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objects;

import java.io.Serializable;

import baseinvaders.Configurations;

/**
 *
 * @author Brett
 */
public class Line implements Serializable {

    private static final long serialVersionUID = 1L;
    private volatile double x1, x2, y1, y2, length;


    public Line(Point center, double length, double rotation) {
        double distX = Math.cos(rotation) * length / 2;
        double distY = Math.sin(rotation) * length / 2;
        this.x1 = center.getX() - distX;
        this.x2 = center.getX() + distX;
        this.y1 = center.getY() - distY;
        this.y2 = center.getY() + distY;
        this.length = length;
    }

    public Line(Point p1, Point p2) {
        this(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public Line(Line line) {
        this(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    public Line(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.length = Math.sqrt(Math.pow((x2 - x1), 2)
                              + Math.pow((y2 - y1), 2));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.x1) ^ (Double.doubleToLongBits(this.x1) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.y1) ^ (Double.doubleToLongBits(this.y1) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.x2) ^ (Double.doubleToLongBits(this.x2) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.y2) ^ (Double.doubleToLongBits(this.y2) >>> 32));
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
        final Line other = (Line) obj;
        if (Double.doubleToLongBits(this.x1) != Double.doubleToLongBits(other.x1)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y1) != Double.doubleToLongBits(other.y1)) {
            return false;
        }
        if (Double.doubleToLongBits(this.x2) != Double.doubleToLongBits(other.x2)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y2) != Double.doubleToLongBits(other.y2)) {
            return false;
        }
        return true;
    }

    public double getX1() {
        return x1;
    }

    public Line setX1(double x1) {
        this.x1 = x1;
        return this;
    }

    public double getX2() {
        return x2;
    }

    public Line setX2(double x2) {
        this.x2 = x2;
        return this;
    }

    public double getY1() {
        return y1;
    }

    public Line setY1(double y1) {
        this.y1 = y1;
        return this;
    }

    public double getY2() {
        return y2;
    }

    public Line setY2(double y1) {
        this.y2 = y2;
        return this;
    }

    public double getLength() {
        return length;
    }

    public double getReflectedAngle(double angle) {

        // Angle of the wall
        double wallAngle = Math.atan2(y2 - y1, x2 - x1);

        // Angle perpendicular to the wall
        double wallAnglePerp = wallAngle + Math.PI / 2;

        // Angle between the given angle and the line perpendicular to the wall
        double diffAngle = wallAnglePerp - angle;

        // Calculate the reflected angle
        return (wallAnglePerp + diffAngle + Math.PI) % (Math.PI * 2);
    }

    // This technique is documentation further here:
    // http://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/
    public boolean intersects(Line line) {
        int o1 = orientation(this.x1, this.y1, this.x2, this.y2, line.x1, line.y1);
        int o2 = orientation(this.x1, this.y1, this.x2, this.y2, line.x2, line.y2);
        int o3 = orientation(line.x1, line.y1, line.x2, line.y2, this.x1, this.y1);
        int o4 = orientation(line.x1, line.y1, line.x2, line.y2, this.x2, this.y2);

        return o1 != o2 && o3 != o4;
    }

    // Finds the orientation of triplet ((x1, y1), (x2, y2), (x3, y3))
    // 0 --> p, q, and r are colinear
    // 1 --> Clockwise
    // 2 --> Counterclockwise
    private int orientation(
            double x1, double y1,
            double x2, double y2,
            double x3, double y3) {
        double val = (y2 - y1) * (x3 - x2) - (x2 - x1) * (y3 - y2);
        if (val == 0) return 0;
        return (val > 0) ? 1 : 2;
    }

    @Override
    public String toString() {
        return "Line{" + "x1=" + x1 + ", y1=" + y1
                     + ", x2=" + x2 + ", y2=" + y2 + "}";
    }
}
