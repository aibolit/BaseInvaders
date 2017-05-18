/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objects;

import java.io.Serializable;

/**
 *
 * @author Brett
 */
public class Wall extends GameObject implements Serializable {

	private static final long serialVersionUID = 1L;
    private double length;
    private boolean vertical;
    private Line line;

    public Wall(Point center, double length, boolean vertical) {
        super(center);
        this.length = length;
        this.vertical = vertical;
        if (this.vertical) {
            line = new VerticalLine(center, length);
        } else {
            line = new HorizontalLine(center, length);
        }
    }

    public Wall(Wall wall) {
        super(new Point(wall.getPosition()));
        this.length = wall.length;
        this.vertical = wall.vertical;
        this.line = new Line(wall.getLine());
    }

    public double getLength() {
        return length;
    }

    public boolean getVertical() {
        return vertical;
    }

    public Line getLine() {
        return line;
    }

    @Override
    public void nextRound() {
    }
}
