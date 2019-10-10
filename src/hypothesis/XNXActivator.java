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
public class XNXActivator extends Activator{
    public double activate(double d)
    {
        if (d > 0)
            return Math.pow(d, -d);
        return 0;
    }
    public double deltaActivate(double d)
    {
        if (d > 0)
            return -Math.pow(d, -d)*(Math.log(d)+1.0);
        return 0;
    }
}
