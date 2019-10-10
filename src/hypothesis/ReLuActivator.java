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
public class ReLuActivator extends Activator{
    public double activate(double d)
    {
        if (d > 0)
            return 0;
        return d;
    }
    public double deltaActivate(double d)
    {
        if (d > 0)
            return 0;
        return 1;
    }
}
