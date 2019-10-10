/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hypothesis;

/**
 *
 * @author voice
 */
public class SigmoidActivator extends Activator{
    public double activate(double d)
    {
        return 1.0 / (1.0 + Math.pow(Math.E, -d));
    }
    public double deltaActivate(double d)
    {
        return activate(d) * (1.0 - activate(d));
    }
}
