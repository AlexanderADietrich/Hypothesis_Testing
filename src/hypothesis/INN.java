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
public class INN extends NN{
    public INN(double[][] inputs, double[][] outputs, int layers, int nodesPer, boolean sparse, int resolution)
    {
        this(inputs, outputs, layers, nodesPer, sparse, resolution, "ReLu");
    }
    public INN(double[][] inputs, double[][] outputs, int layers, int nodesPer, boolean sparse, int resolution, String activateCode)
    {
        this(inputs, outputs, layers, nodesPer, sparse, resolution, new String[]{activateCode});
    }
    public INN(double[][] inputs, double[][] outputs, int layers, int nodesPer, boolean sparse, int resolution, String[] activateCodes)
    {
        this.sparse = sparse;
        int n = 0;
        for (double[] list : outputs)
            for (double d : list)
                n++;
        //separated to avoid possible double max value
        double average = 0.0;
        for (double[] list : outputs)
            for (double d : list)
                average += d / (n*1.0);

        if (average < 1.0)
            average = 1.0;
        
        int connections = (inputs[0].length+outputs[0].length)*nodesPer + nodesPer*nodesPer*(layers-1);
        dw = 1.0/(average*Math.pow(connections, 2));


        this.inputs = inputs;
        this.outputs = outputs;
        this.layers = new INode[layers+2][];

        //Create inputs
        this.layers[0] = new INode[inputs[0].length];
        for (int i = 0; i < this.layers[0].length; i++)
        {
            this.layers[0][i] = new INode();
            this.layers[0][i].nonAct = true;
        }
        //Create hidden(s)
        for (int i = 1; i < this.layers.length-1; i++)
        {
            this.layers[i] = new INode[nodesPer];
            for (int w = 0; w < nodesPer; w++)
            {
                this.layers[i][w] = new INode(activateCodes[i % activateCodes.length]);
            }
            if (sparse) nodesPer /= 2;
        }
        //Create output
        this.layers[this.layers.length-1] = new INode[outputs[0].length];
        for (int i = 0; i < this.layers[this.layers.length-1].length; i++)
        {
            this.layers[this.layers.length-1][i] = new INode();
            this.layers[this.layers.length-1][i].nonAct = true;
        }
        //Link
        for (int i = 0; i < this.layers.length-1; i++)
        {
            for (int w = 0; w < this.layers[i].length; w++)
            {
                numWeights += this.layers[i+1].length;
                ((INode)(this.layers[i][w])).passLinks((INode[])this.layers[i+1], resolution);
            }
        }
        
        updaters = new Updater[this.layers.length-1];
        for (int i = 0; i < updaters.length; i++)
            updaters[i] = new Updater(this.layers[i]);
        dUpdaters = new DeltaUpdater[this.layers.length-1];
        for (int i = 0; i < dUpdaters.length; i++)
            dUpdaters[i] = new DeltaUpdater(this.layers[i]);
    }
}
