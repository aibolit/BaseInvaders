package Objects;

import java.io.Serializable;

/**
 *
 * @author Brett
 */
public class WormHole extends GameObject implements Serializable {

    private static final long serialVersionUID = 1L;
    private double radius;
    private Point destination;

    public WormHole(Point position, double radius, Point destination) {
        super(position);
        this.radius = radius;
        this.destination = destination;
    }

    public WormHole(WormHole wormHole) {
        super(wormHole.getPosition());
        this.radius = wormHole.getRadius();
    }

    public double getRadius() {
        return radius;
    }

    public Point getDestination() {
        return destination;
    }

    @Override
    public void nextRound() {
    }

    @Override
    public String toString() {
        return "WormHole{" + "radius=" + radius + ", position=" + getPosition() + ", destination=" + destination + '}';
    }


}