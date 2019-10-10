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
public class DeltaUpdater extends Updater{
    public class DCalcThread extends Updater.CalcThread
    {
        public DCalcThread(Node[] nodes, int start, int stop) {
            super(nodes, start, stop);
        }
        public DCalcThread(CalcThread t)
        {
            super(t);
        }
        public void start()
        {
            if (t == null || !t.isAlive())
            {
                t = new Thread(this, Math.random()+"");
                t.start();
            }
        }
        public void run()
        {
            for (int i = 0; i < nodes.length; i++)
                nodes[i].dFeedForward();
        }
    }
    public DeltaUpdater(Node[] nodes) {
        super(nodes);
        //replace, make more efficent
        if (nodes.length < THREADS)
        {
            threads = new Thread[1];
            calcThreads = new DCalcThread[1];
            calcThreads[0] = new DCalcThread(nodes, 0, nodes.length);
        }
        else
        {
            for (int i = 0; i < calcThreads.length; i++)
            {
                calcThreads[i] = new DCalcThread(nodes, i*nodes.length/THREADS, (i+1)*nodes.length/THREADS);
            }
            calcThreads[calcThreads.length-1].stop = nodes.length-1;
        }
    }
    
}
