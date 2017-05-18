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
public class HorizontalLine extends Line implements Serializable {

    private static final long serialVersionUID = 1L;

    public HorizontalLine(Point center, double length) {
        super(center, length, 0);
    }

    @Override
    public double getReflectedAngle(double angle) {
        return Math.PI * 2 - angle;
    }
}
