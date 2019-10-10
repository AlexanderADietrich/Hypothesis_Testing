package hypothesis;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



/**
 * The purpose of this class is to test a classical implementation of an ANN
 * (this implementation being a activated multilayer perceptron with network-
 * wide bias) against various other hypotheses for speed and accuracy. 10/2/19
 * 
 * Observation 1: My multithreading code is in fact slower than simple iterative
 * code over many hyperparameters.
 * Observation 2: Grid performance is reliant on a high number of training iter
 * ations and the Grid's own random logic being beneficial.
 * Observation 3: Both network's predictive power at a single layer seems limit
 * ed, simply lagging behind the actual data at 1 previous day, 1 layer, 256 nod
 * es, 1000 iterations, 5 inn sections
 * Observation 4: Problem in observation 3 solved by switching to 1x16 network
 * and 16 days in advance of data as inputs.
 * 10/9/19 Sigmoid INN Created. It would be better at this point to make a NN
 * builder class, but the code overhead considering that INN and children are
 * simply modified constructors is very small.
 * Observation 5: Sigmoid INN's produce more interesting predictions, but take
 * approximately 2.2x the amount of time that NN's and INN's take.
 * Observation 6: Sigmoid INN's seem most promising for generalization as they
 * have the greatest ability to generate predictions rather than post-facto
 * trend following in my visual analysis of the tests recorded for parameters
 * REPEATS 10000 LAYERS 1 NODES 16 RESOLUTION 100 GRID_TEST = false
 * close, volume and 16.
 * Observation 7: SINN also shows best predictive power in grid tests.
 * Observation 8: SINN's problem is that INN has similar abilities without the
 * overhead. INN has the problem of infrequent predictive ability in comparison.
 * CREATING RSqINN, or Rectified Square Root INN
 * Observation 9: RSqINN, despite it's long name, is faster than SINN while re-
 * taining its increased nonlinearity when compared to ReLu based networks.
 * Observation 10: A NodeBuilder and NNBuilder class would make implementation
 * of new types of NN's much more efficient and increase my testing ability.
 * To describe my current process for this project it would be:
 * Plan->Code->Fix->Test->Repeat
 * This is by no means advanced, it is in the vaguest sense agile, yet it does
 * not include unrelated steps simply to follow one process or another.
 * Observation 11: NodeBuilder seems overkill when the main variable changed
 * in Node subclasses is activation function. Creating a separate Activator
 * class to make a middle-ground. This will allow me to remove unique subclasses
 * of INode and replace them with a new constructor. This also allowed removal
 * of INN subclasses in favor of a more detailed constructor.
 * Observation 12: SquareRoot INN's produce the most reliable predictive power
 * proportional to speed, but their predictions often spike which causes issues
 * when the stock is not spiking much (predictions oscillating between up and d-
 * own rapidly). This could be solved by a simple moving average applied post-
 * calculation, but this is difficult to implement in terms of the final predi-
 * ction and seems too much like a "bandaid-fix".
 * Observation 13: One function with novel properties that doesn't seem convolu-
 * ted is x^-x. This has some differentiation issues at 1, but other than that
 * has some useful properties: mimicking brain function in its diminishing
 * returns, dramatic nonlinearity near 0, among others. Seems worth trying.
 * Observation 14: If you thought square root was spiky, XNX is much worse.
 * This seems to be a problem related to its differentiation.
 * Observation 15: Part of the issue was implementation: (-x)^-x is much differ-
 * ent than -(x^-x). Less spiky after fix, still spiky.
 * Observation 16: Perhaps the spike problem could be solved by adding multiple
 * activations within layers. This has been implemented in a new constructor.
 * Observation 17: Chimaera network seems to inherit all the issues: lack of
 * prediction and spikes. Redoing implementation to have alternating layers
 * of activation rather than alternating nodes.
 * Observation 18: For obvious reasons, some activations are incompatible.
 * XNX that returns negative on derivative calls for positive numbers will
 * break a network that has a square root after
 * Observation 19: No combination network seems to perform much better than:
 * 1x16 RSQINN RES=100 {close volume open} 16 days.
 * Observation 20: Across multiple amounts of training (1000, 10000, and 20000
 * repeats) the spiking issue remains.
 * Observation 21: Rolling averages fix the issue of spiking, increasing predic-
 * tive power massively. It seems the RSQINN is ready for deployment in a 
 * predictive library, and this project is currently adjourned.
 * 
 * @author Alexander A Dietrich, beginnning Oct. 2019
 */
public class Driver {
    public static final int REPEATS = 10000;
    public static final int LAYERS = 1;
    public static final int NODES = 16;
    public static final int RESOLUTION = 100;
    public static final boolean GRID_TEST = false;
    public static void main(String[] args) {
        double[][][] split = Loader.loadData("RDFN", new String[]{"close", "volume", "open"}, 16, true);
        double[][] in = split[0];
        double[][] out = split[1];
        
        
        //INN chimnetwork = new INN(in, out, LAYERS, NODES, false, RESOLUTION, new String[]{"XNX", "Sigmoid"});
        INN rsqinetwork = new INN(in, out, LAYERS, NODES, false, RESOLUTION, "SquareRoot");
        //SINN sinetwork = new SINN(in, out, LAYERS, NODES, false, RESOLUTION);
        //INN inetwork = new INN(in, out, LAYERS, NODES, false, RESOLUTION);
        //NN network = new NN(in, out, LAYERS, NODES);
        
        testNN("RSqINN", rsqinetwork, in, out);
        testNNRolling("RSqINN", rsqinetwork, split[2], out, split[3][0], 5);
        //testNN("RSqINN", rsqinetwork, in, out);
        //testNN("SINN", sinetwork, in, out);
        //testNN("INN", inetwork, in, out);
        //testNN("NN", network, in, out);
        
        if (GRID_TEST)
        {
            Grid preProcessor = new Grid(8, 48);
            in = preProcessor.analyze(in);

            rsqinetwork = new INN(in, out, LAYERS, NODES, false, RESOLUTION, "SquareRoot");
            //sinetwork = new SINN(in, out, LAYERS, NODES, false, RESOLUTION);
            //inetwork = new INN(in, out, LAYERS, NODES, false, RESOLUTION);
            //network = new NN(in, out, LAYERS, NODES);
            
            testNN("RSqINNWG", rsqinetwork, in, out);
            //testNN("SINNWG", sinetwork, in, out);
            //testNN("INNWG", inetwork, in, out);
            //testNN("NNWG", network, in, out);
        }
    }
    
    public static void testNNRolling(String NNName, NN nn, double[][] in, double[][] out, double[] finalInput, int rollAmount)
    {
        double[] preds = new double[in.length];
        for (int i = 0; i < in.length; i++)
            preds[i] = nn.calc(in[i])[0];
        
        double[] rollingPreds = new double[in.length];
        for (int i = 0; i < in.length; i++)
        {
            double avg = 0.0;
            int sent = 0;
            for (int b = i; b >= 0 && b > i-rollAmount; b--)
            {
                avg += preds[b];
                sent++;
            }
                
            avg /= sent;
            rollingPreds[i] = avg;
        }
        
        String diagnostic = "";
        for (int i = 0; i < in.length-1; i++)
            diagnostic += rollingPreds[i] + "\t" + in[i+1][0] + "\n";
        diagnostic += rollingPreds[rollingPreds.length-1] + "\t" + finalInput[0] + "\n";
        
        Date date = new Date();
        String testTime = new SimpleDateFormat("HH_mm_ss", Locale.ENGLISH).format(date);
        Loader.saveString(diagnostic, "ROLL_"+NNName+"_"+testTime, true);
    }
    
    public static void testNN(String NNName, NN nn, double[][] in, double[][] out)
    {
        long t = System.currentTimeMillis();
        for (int c = 0; c < REPEATS; c++)
        {
            nn.updateWeightsNoThreading();
        }
        System.out.println(NNName + " Time: " + (System.currentTimeMillis()-t)/(1.0*REPEATS*in.length));
        //tn.dispWeights();
        double avgDist = 0.0;
        String diagnostic = "";
        for (int i = 0; i < in.length-1; i++)
        {
            double val = nn.calc(in[i])[0];
            double dist = out[i][0] - val;
            diagnostic += val + "\t" + out[i][0] + "\t" + dist + "\n";
            avgDist += Math.abs(dist);
        }
        System.out.println(NNName + " Average Distance: " + (avgDist/in.length));
        //modified from https://stackoverflow.com/questions/2654025/how-to-get-year-month-day-hours-minutes-seconds-and-milliseconds-of-the-cur from BalusC
        Date date = new Date();
        String testTime = new SimpleDateFormat("HH_mm_ss", Locale.ENGLISH).format(date);
        Loader.saveString(diagnostic, NNName+"_"+testTime, true);
    }
}
