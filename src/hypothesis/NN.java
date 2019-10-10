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
public class NN {
    double bias = 0.0;
    public static double reLu(double d)
    {
        return (d < 0) ? 0 : d;
    }
    public static double dReLu(double d)
    {
        return (d < 0) ? 0 : 1;
    }
    
    public static Node loadNodeFromString(String s, Node n)
    {
        n.nonAct = (s.charAt(0) == '1');
        s = s.substring(1);
        String[] split = s.split(Node.sep);
        n.weights = new double[split.length];
        for (int i = 0; i < split.length; i++)
        {
            try
            {
                double d = Double.parseDouble(split[i]);
                //reload death prevention
                if (Double.isNaN(d))
                    d = Math.random()-0.5;
                n.weights[i] = d;
            }
            //assumes is output node (ie no weights)
            catch (NumberFormatException nfe)
            {
                break;
            }
        }
        return n;
    }
    
    public NN getNN(double[] inputs, double[] outputs, int layers, int nodesPer, boolean sparse)
    {
        double[][] inp = new double[inputs.length][1];
        double[][] out = new double[inputs.length][1];
        for (int i = 0; i < inp.length; i++)
        {
            inp[i][0] = inputs[i];
            out[i][0] = outputs[i];
        }
        return new NN(inp, out, layers, nodesPer, sparse);
    }
    Updater[] updaters;
    DeltaUpdater[] dUpdaters;
    double[][]  inputs;
    double[][]  outputs;
    Node[][]    layers;
    double dw;
    boolean sparse;
    double numWeights;

    /**
     * Creates a completely uninitialized NN
     */
    public NN()
    {
        
    }
    
    public static NN getBestNN(double[][] inputs, double[][] outputs)
    {
        return getBestNN(inputs, outputs, 3, 5);
    }
    
    /**
     * genetically determines the best layer/node config
     * @param inputs
     * @param outputs 
     * @param startLayers 
     * @param startNodes 
     * @return  NN determined best by genetic algorithm
     */
    public static NN getBestNN(double[][] inputs, double[][] outputs, int startLayers, int startNodes)
    {
        NN[] pop = new NN[10];
        
        for (int i = 0; i < pop.length; i++)
        {
            pop[i] = new NN(inputs, outputs, (int)(Math.random()*startLayers)+1, (int)(Math.random()*startNodes)+2);
        }
        
        double least = Double.MAX_VALUE;
        int index = -1;
        for (int g = 0; g < 5; g++)
        {
            //optimize accuracy
            for (int i = 0; i < pop.length; i++)
            {
                //determine the best
                for (int b = 0; b < 10; b++)
                    pop[i].updateWeights();

                if (pop[i].getError() < (least))
                {
                    index = i;
                    least = pop[i].getError();
                }
            }
            for (int i = 0; i < pop.length; i++)
            {
                if (i != index)
                {
                    if (Math.random() < 0.5)
                        pop[i] = new NN(inputs, outputs, pop[index].layers.length-2, 
                                    pop[index].layers[1].length);
                    else
                    {
                        int layer = pop[i].layers.length-2 + (int)(Math.random()*3.0-1.0);
                        if (layer < 1)
                            layer = 1;

                        int nodes = pop[i].layers[1].length + (int)(Math.random()*3.0-1.0);
                        if (nodes < 2)
                            nodes = 2;

                        pop[i] = new NN(inputs, outputs, layer, 
                                    nodes);
                    }
                }
            }
        }
        System.out.println("Best Layers         : " + (pop[index].layers.length-2));
        System.out.println("Best Node Number    : " + pop[index].layers[1].length);
        return pop[index];
    }
    public NN(double[][] inputs, double[][] outputs, int layers, int nodesPer)
    {
        this(inputs, outputs, layers, nodesPer, false);
    }
    public NN(double[][] inputs, double[][] outputs, int layers, int nodesPer, boolean sparse)
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
        this.layers = new Node[layers+2][];

        //Create inputs
        this.layers[0] = new Node[inputs[0].length];
        for (int i = 0; i < this.layers[0].length; i++)
        {
            this.layers[0][i] = new Node();
            this.layers[0][i].nonAct = true;
        }
        //Create hidden(s)
        for (int i = 1; i < this.layers.length-1; i++)
        {
            this.layers[i] = new Node[nodesPer];
            for (int w = 0; w < nodesPer; w++)
            {
                this.layers[i][w] = new Node();
            }
            if (sparse) nodesPer /= 2;
        }
        //Create output
        this.layers[this.layers.length-1] = new Node[outputs[0].length];
        for (int i = 0; i < this.layers[this.layers.length-1].length; i++)
        {
            this.layers[this.layers.length-1][i] = new Node();
            this.layers[this.layers.length-1][i].nonAct = true;
        }
        //Link
        for (int i = 0; i < this.layers.length-1; i++)
        {
            for (int w = 0; w < this.layers[i].length; w++)
            {
                numWeights += this.layers[i+1].length;
                this.layers[i][w].passLinks(this.layers[i+1]);
            }
        }
        
        updaters = new Updater[this.layers.length-1];
        for (int i = 0; i < updaters.length; i++)
            updaters[i] = new Updater(this.layers[i]);
        dUpdaters = new DeltaUpdater[this.layers.length-1];
        for (int i = 0; i < dUpdaters.length; i++)
            dUpdaters[i] = new DeltaUpdater(this.layers[i]);
    }
    public void reset()
    {
        for (int i = 0; i < layers.length; i++)
            for (int w = 0; w < layers[i].length; w++)
                layers[i][w].reset();
    }
    public void dReset()
    {
        for (int i = 0; i < layers.length; i++)
            for (int w = 0; w < layers[i].length; w++)
                layers[i][w].dReset();
    }
    
    /**
     * Returns the error of the current NN in relation to its stored
     * inputs and outputs
     * @return double representing error
     */
    double getError()
    {
        return getError(1);
    }
    
    /**
     * Returns the error of the current NN in relation to its stored inputs 
     * and outputs, incrementing each loop by
     * @param skip
     * @return 
     */
    double getError(int skip)
    {
        return getError(inputs, outputs, skip);
    }
    
    /**
     * Returns the error of the current NN in relation to the given
     * @param inputs
     * @param outputs
     * @return 
     */
    double getError(double[][] inputs, double[][] outputs)
    {
        return getError(inputs, outputs, 1);
    }
    
    /**
     * Returns the error of the current NN in relation to the given
     * @param inputs 
     * @param outputs
     * @param skip increment to examine, useful for speed
     * @return double representing error
     */
    double getError(double[][] inputs, double[][] outputs, int skip)
    {
        double retVal = 0.0;
        for (int i = 0; i < inputs.length; i+= skip)
        {
            double[] vals = calc(inputs[i]);
            for (int b = 0; b < outputs[i].length; b++)
                retVal += Math.pow(vals[b]-outputs[i][b], 2);
        }
        return retVal;
    }
    
    /**
     * 
     * @param inputs assumed to be length of input nodes
     * @return outputs
     */
    public double[] calc(double[] inputs)
    {
        //reset
        reset();
        for (int i = 0; i < layers[0].length; i++)
            layers[0][i].input(inputs[i]);
        
        for (Updater e : updaters)
            e.run();
        /*//Input to inputs and feed forward
        for (int i = 0; i < layers[0].length; i++)
        {
            layers[0][i].input(inputs[i]);
            layers[0][i].feedForward();
        }
        for (int i = 1; i < layers.length-1; i++)
            for (int w = 0; w < layers[i].length; w++)
                layers[i][w].feedForward();*/
        double[] retVal = new double[layers[layers.length-1].length];
        for (int i = 0; i < layers[layers.length-1].length; i++)
            retVal[i] = layers[layers.length-1][i].currentInput+bias;
        return retVal;
    }
    
    /**
     * 
     * @param inputs assumed to be length of input nodes
     * @return outputs
     */
    public double[] calcNoThreading(double[] inputs)
    {
        //reset
        reset();
        for (int i = 0; i < layers[0].length; i++)
            layers[0][i].input(inputs[i]);
        
        for (int i = 0; i < layers.length-1; i++)
            for (Node n : layers[i])
                n.feedForward();
        
        double[] retVal = new double[layers[layers.length-1].length];
        for (int i = 0; i < layers[layers.length-1].length; i++)
            retVal[i] = layers[layers.length-1][i].currentInput+bias;
        return retVal;
    }
    
    
    
    //public double prevAvg = Double.MAX_VALUE;//Used to adapt dw, starts at max val
    /**
     * Assumes fully connected
     */
    public void updateWeights()
    {
        //for each input output pair
        for (int inp = 0; inp < inputs.length; inp++)
        {
            //populate with values
            double[] calcs = calc(inputs[inp]);
            //saves on time
            double[] dists = new double[calcs.length];
            for (int i = 0; i < dists.length; i++)
            {
                dists[i] = (calcs[i]-outputs[inp][i]);
                bias -= dists[i]*dw;
            }            
            
            //calc updates, update
            for (int i = 0; i < layers.length-1; i++)
            {
                for (int b = 0; b < layers[i].length; b++)
                {
                    for (int w = 0; w < layers[i][b].weights.length; w++)
                    {
                        //reset
                        dReset();
                        //feed forward
                        layers[i+1][w].deltafeed = layers[i][b].currentInput;
                        /**
                         * New feed forward takes more lines but pays off
                         * in that it doesn't move on to the next layer 
                         * before the previous is done
                         */

                        //Feed forward from next node
                        if (i+1 < layers.length-1) 
                            layers[i+1][w].dFeedForward();
                        //Feed forward from layers after next node
                        for (int r = i+2; r < layers.length-1; r++)
                            dUpdaters[r].run();
                        //Adjust weights for each output
                        for (int r = 0; r < layers[layers.length-1].length; r++)
                            layers[i][b].weights[w] -=
                                    dw
                                    *layers[layers.length-1][r].deltafeed
                                    *dists[r]; 
                    }
                }
            }
        }
    }
    
    public void updateWeightsNoThreading()
    {
        //for each input output pair
        for (int inp = 0; inp < inputs.length; inp++)
        {
            //populate with values
            double[] calcs = calcNoThreading(inputs[inp]);
            //saves on time
            double[] dists = new double[calcs.length];
            for (int i = 0; i < dists.length; i++)
            {
                dists[i] = (calcs[i]-outputs[inp][i]);
                bias -= dists[i]*dw;
            }            
            
            //calc updates, update
            for (int i = 0; i < layers.length-1; i++)
            {
                for (int b = 0; b < layers[i].length; b++)
                {
                    for (int w = 0; w < layers[i][b].weights.length; w++)
                    {
                        //reset
                        dReset();
                        //feed forward
                        layers[i+1][w].deltafeed = layers[i][b].currentInput;
                        /**
                         * New feed forward takes more lines but pays off
                         * in that it doesn't move on to the next layer 
                         * before the previous is done
                         */

                        //Feed forward from next node
                        if (i+1 < layers.length-1) 
                            layers[i+1][w].dFeedForward();
                        //Feed forward from layers after next node
                        for (int r = i+2; r < layers.length-1; r++)
                            for (Node n : layers[r])
                                n.dFeedForward();
                        //Adjust weights for each output
                        for (int r = 0; r < layers[layers.length-1].length; r++)
                            layers[i][b].weights[w] -=
                                    dw
                                    *layers[layers.length-1][r].deltafeed
                                    *dists[r]; 
                    }
                }
            }
        }
    }
    
    /**
     * Updates weights, assumes the values have already been populated.
     * @param outputs
     */
    public void updateWeightsCurrent(double[] outputs)
    {
            double[] calcs = new double[layers[layers.length-1].length];
            for (int i = 0; i < calcs.length; i++)
                calcs[i] = layers[layers.length-1][i].currentInput;
        
            //saves on time
            double[] dists = new double[calcs.length];
            for (int i = 0; i < dists.length; i++)
            {
                dists[i] = (calcs[i]-outputs[i]);
                bias -= dists[i]*dw;
            }            
            
            //calc updates, update
            for (int i = 0; i < layers.length-1; i++)
            {
                for (int b = 0; b < layers[i].length; b++)
                {
                    for (int w = 0; w < layers[i][b].weights.length; w++)
                    {
                        //reset
                        dReset();
                        //feed forward
                        layers[i+1][w].deltafeed = layers[i][b].currentInput;
                        /**
                         * New feed forward takes more lines but pays off
                         * in that it doesn't move on to the next layer 
                         * before the previous is done
                         */

                        //Feed forward from next node
                        if (i+1 < layers.length-1) 
                            layers[i+1][w].dFeedForward();
                        //Feed forward from layers after next node
                        for (int r = i+2; r < layers.length-1; r++)
                            dUpdaters[r].run();
                        //Adjust weights for each output
                        for (int r = 0; r < layers[layers.length-1].length; r++)
                            layers[i][b].weights[w] -=
                                    dw
                                    *layers[layers.length-1][r].deltafeed
                                    *dists[r]; 
                    }
                }
            }
    }
    
    
    public double prevAvg = Double.MAX_VALUE;//Used to adapt dw, starts at max val
    /**
     * "Adaptive Update Weights"
     */
    public void aUpdWeights()
    {
        System.out.println(dw);
        double currentAvg = 0.0;
        //for each input output pair
        for (int inp = 0; inp < inputs.length; inp++)
        {
            //populate with values
            double[] calcs = calc(inputs[inp]);
            //saves on time
            double[] dists = new double[calcs.length];
            for (int i = 0; i < dists.length; i++)
            {
                dists[i] = (calcs[i]-outputs[inp][i]);
                currentAvg += dists[i]/dists.length;
            }
            if (currentAvg-0.1 > prevAvg) dw = dw / 1.001;
            else dw = dw *1.001;
            prevAvg = currentAvg;
            //calc updates, update
            for (int i = 0; i < layers.length-1; i++)
            {
                for (int b = 0; b < layers[i].length; b++)
                {
                    for (int w = 0; w < layers[i][b].weights.length; w++)
                    {
                        //reset
                        dReset();
                        //feed forward
                        layers[i+1][w].deltafeed = layers[i][b].currentInput;
                        /**
                         * New feed forward takes more lines but pays off
                         * in that it doesn't move on to the next layer 
                         * before the previous is done
                         */

                        //Feed forward from next node
                        if (i+1 < layers.length-1) 
                            layers[i+1][w].dFeedForward();
                        //Feed forward from layers after next node
                        for (int r = i+2; r < layers.length-1; r++)
                            dUpdaters[r].run();
                        //Adjust weights for each output
                        for (int r = 0; r < layers[layers.length-1].length; r++)
                            layers[i][b].weights[w] -=
                                    0.000000001
                                    *(1.0/(i+1))
                                    *dw
                                    *layers[layers.length-1][r].deltafeed
                                    *dists[r]; 
                    }
                }
            }
        }
    }
    
    public void disp()
    {
        for (int i = 0; i < layers.length; i++)
        {
            for (int w = 0; w < layers[i].length; w++)
            {
                System.out.print(layers[i][w].currentInput + "\t");
            }
            System.out.println();
        }
    }
    
    @Override
    public String toString()
    {
        String retVal = "";
        retVal = retVal + "Bias" + bias + "\n";
        retVal = retVal + "DW" + dw + "\n";
        retVal = retVal + "Layers" + (layers.length-2) + "\n";
        retVal = retVal + "NodesPer" + layers[1].length + "\n";
        for (int i = 0; i < layers.length; i++)
        {
            retVal = retVal + "Layer" + i + " Length" + layers[i].length + "\n";
            for (int w = 0; w < layers[i].length; w++)
            {
                retVal = retVal + "\tNode" + w + "\n";
                if (layers[i][w].weights != null)
                    for (int c = 0; c < layers[i][w].weights.length; c++)
                        retVal = retVal + "\t\tWeight" + c + " Value" + layers[i][w].weights[c] + "\n";
            }
        }
        return retVal;
    }
    
    public void remake()
    {
        for (int b = 0; b < layers.length-1; b++)
        {
            for (int n = 0; n < layers[b].length; n++)
            {
                for (int w = 0; w < layers[b][n].weights.length; w++)
                {
                    layers[b][n].weights[w] = Math.random()*2.0 - 1.0;
                }
            }
        }
    }
    
    public static NN loadNN(String s, double[][] inputs, double[][] outputs)
    {
        String[] data = s.split("\n");
        double bias = 0;
        int index = 0;
        if (data[0].contains("Bias"))
            bias = Double.parseDouble(data[index++].substring(4));
        double dw = Double.parseDouble(data[index++].substring(2));
        int layers = Integer.parseInt(data[index++].substring(6));
        int nodesPer = Integer.parseInt(data[index++].substring(8));
        NN network = new NN(inputs, outputs, layers, nodesPer, false);
        network.dw = dw;
        network.bias = bias;
        //replace values in each layer
        for (int i = index; i < data.length; i++)
        {
            if (data[i].contains("Layer"))
            {
                String[] list = data[i].split(" ");
                int layer = Integer.parseInt(list[0].substring(5));
                int len = Integer.parseInt(list[1].substring(6));
                                
                int n = -1;
                int c = 0;
                while (i < data.length-1 && !data[++i].contains("Layer"))
                {
                    if (data[i].contains("Node"))
                    {
                        n = Integer.parseInt(data[i].substring(5));  
                        c = 0;
                        continue;
                    }
                    network.layers[layer][n].weights[c++] = Double.parseDouble(data[i].substring(5+data[i].indexOf("Value")));
                }
                i--;
            }
        }
        return network;
    }
    
    public String getComparison()
    {
        String retVal = "";
        
        for (int i = 0; i < inputs.length; i++)
        {
            for (int b = 0; b < inputs[i].length; b++)
                retVal = retVal + inputs[i][b] + "\t";
            retVal = retVal + outputs[i][0] + "\t" + calc(inputs[i])[0] + "\n";
        }
        
        return retVal;
    }
    
    public String getAllData()
    {
        String retVal = "";
        
        for (int i = 0; i < inputs.length; i++)
        {
            for (int b = 0; b < inputs[i].length; b++)
                retVal = retVal + inputs[i][b] + "\t";
            for (int b = 0; b < outputs[i].length; b++)
                retVal = retVal + outputs[i][b] + "\t";
            retVal = retVal + "\n";
        }
        
        return retVal;
    }
    
    public void dispWeights()
    {
        for (Node[] na : layers)
        {
            System.out.println("{");
            for (Node n : na)
            {
                System.out.print("\t{");
                if (n.weights != null)
                    for (double d : n.weights)
                        System.out.print(d + " ");
                System.out.println("}");
            }
            System.out.println("}");
        }
    }
}

