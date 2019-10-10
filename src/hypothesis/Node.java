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
public class Node {
    //Used for calculations
        public double currentInput = 0.0;
        //Used for derivative
        public double deltafeed = 0.0;
        /**
         * Links are nodes this is linked to.
         *      EXAMPLE: links[w].input(currentInput(weights[w]));
         */
        public Node[]       links;
        public double[]     weights;
        boolean nonAct = false;
        public Node()
        {

        }
        public Node(Node[] links)
        {
            this.links = links;
            weights = new double[links.length];
            int sent = 0;
            for (Node n : links)
                weights[sent++] = Math.random();
        }
        public void passLinks(Node[] links)
        {
            this.links = new Node[links.length];
            weights = new double[links.length];
            int sent = 0;
            for (Node n : links)
            {
                this.links[sent] = n;
                weights[sent++] = Math.random();
            }
        }
        public void input(double d)
        {
            if (nonAct) currentInput += d;
            else currentInput += NN.reLu(d);
        }
        public void feedForward()
        {
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
                else n.deltafeed += NN.dReLu(currentInput)*weights[sent++]*deltafeed;
            }
        }
        public void reset()
        {
            currentInput = 0;
        }
        public void dReset()
        {
            deltafeed = 0;
        }
        public static final String sep = "WS";
        public String toString()
        {
            String retVal = (nonAct) ? "1" : "0";
            if (weights != null)
                for (double d : weights)
                    retVal = retVal + d + sep;
            return retVal;
        }
}
