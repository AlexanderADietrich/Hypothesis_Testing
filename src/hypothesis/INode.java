/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hypothesis;

import java.util.HashMap;

/**
 * Intelligent nodes have multiple weight sets.
 * @author voice
 */
public class INode extends Node{
    private class Storage
    {
        double[] list;
        public Storage(double[] list)
        {
            this.list=list;
        }
    }
    public Activator activate;
    public HashMap<Integer, Storage> storage = new HashMap<>();
    public int index = 0;
    public INode()
    {
        activate = new ReLuActivator();
    }
    public INode(String s)
    {
        if ("ReLu".equals(s))
            activate = new ReLuActivator();
        else if ("Sigmoid".equals(s))
            activate = new SigmoidActivator();
        else if ("SquareRoot".equals(s))
            activate = new SquareRootActivator();
        else if ("XNX".equals(s))
            activate = new XNXActivator();
        else
            activate = new Activator();
    }
    public void passLinks(INode[] links, int resolution)
    {
        this.links = new INode[links.length];
        weights = new double[links.length];
        int sent = 0;
        double[] temp = new double[links.length];
        for (INode n : links)
        {
            this.links[sent] = n;
            temp[sent++] = Math.random();
        }
        for (int i = 0; i < resolution; i++)
        {
            storage.put(i, new Storage(temp));
        }
        weights = storage.get(0).list;
    }
    public double sigmoid(double d)
    {
        return 1.0 / (1.0 + Math.pow(Math.E, -d));
    }
    public void determineWeights()
    {
        index = (int) (-0.00001 + sigmoid(currentInput)*storage.size());
        weights = storage.get(index).list;
    }
    public void input(double d)
    {
        if (nonAct) currentInput += d;
        else currentInput += activate.activate(d);
    }
    public void feedForward()
    {
        determineWeights();
        int sent = 0;
        for (Node n : links)
        {
            n.input(currentInput*weights[sent++]);
        }
    }
    public void dFeedForward()
    {
        int sent = 0;
        for (Node n : links)
        {
            if (n.nonAct) n.deltafeed += weights[sent++]*deltafeed;
            else n.deltafeed += activate.deltaActivate(currentInput)*weights[sent++]*deltafeed;
        }
    }
}
