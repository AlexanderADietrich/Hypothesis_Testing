/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hypothesis;

import java.util.HashMap;
import java.util.Iterator;



/**
 *
 * @author voice
 */
public class Grid {
    public double reLu(double d)
    {
        return (d < 0) ? 0 : d;
    }
    double currentAverage = 0.0;
    public class GridNode
    {
        double currentInput = 0.0;
        HashMap<GridNode, Double> links;
        
        public GridNode()
        {
            links = new HashMap<>();
        }
        public void passLinks(GridNode[] links)
        {
            for (GridNode g : links)
                this.links.put(g, Math.random()-0.5);
        }
        public void pullVal()
        {
            currentInput *= 0.3;
            for (GridNode g : links.keySet())
            {
                currentInput += g.links.get(this)*reLu(g.currentInput);
            }
        }
    }
    
    public Grid(int size, int inputSize)
    {
        this(size);
        inputNodes = new GridNode[inputSize];
        for (int r = 0; r < inputSize; r++)
        {
            int y = (int)(Math.random()*size);
            int x = (int)(Math.random()*size);
            
            boolean breaker = false;
            for (int i = 0; i < inputNodes.length; i++)
            {
                if (inputNodes[i] != null)
                {
                    if (inputNodes[i] == grid[y][x])
                    {
                        r--;
                        breaker = true;
                        break;
                    }
                }
            }
            if (breaker)
                continue;
            
            this.inputNodes[r] = grid[y][x];
        }
        
    }
    
    
    public GridNode[][] grid;
    public GridUpdater[] updaters;
    public GridNode[] inputNodes;
    public GridNode[] outputNodes;
    public Grid(int size)
    {
        //initialize
        grid = new GridNode[size][size];
        for (int y = 0; y < grid.length; y++)
            for (int x = 0; x < grid[y].length; x++)
                grid[y][x] = new GridNode();
        //pass links
        int sent = 0;
        GridNode[] temp;
        GridNode[] copy;
        for (int y = 0; y < grid.length; y++)
        {
            for (int x = 0; x < grid[y].length; x++)
            {
                sent = 0;
                temp = new GridNode[4];
                if (!(x+1 < 0 || x+1 >= size || y < 0 || y >= size))//right
                    temp[sent++] = grid[x+1][y];
                if (!(x < 0 || x >= size || y+1 < 0 || y+1 >= size))//down
                    temp[sent++] = grid[x][y+1];
                if (!(x-1 < 0 || x-1 >= size || y < 0 || y >= size))//left
                    temp[sent++] = grid[x-1][y];
                if (!(x < 0 || x >= size || y-1 < 0 || y-1 >= size))//up
                    temp[sent++] = grid[x][y-1];
                copy = new GridNode[sent];
                for(int i = 0; i < sent; i++)
                    copy[i] = temp[i];
                grid[x][y].passLinks(copy);
            }
        }
        sent = 0;
        updaters = new GridUpdater[size];
        for (GridNode[] list : grid)
        {
            updaters[sent++] = new GridUpdater(list);
        }
        this.inputNodes = new GridNode[size];
        this.outputNodes = new GridNode[size*size];
        for (int r = 0; r < size; r++)
        {
            int y = (int)(Math.random()*size);
            int x = (int)(Math.random()*size);
            
            boolean breaker = false;
            for (int i = 0; i < inputNodes.length; i++)
            {
                if (inputNodes[i] != null)
                {
                    if (inputNodes[i] == grid[y][x])
                    {
                        r--;
                        breaker = true;
                        break;
                    }
                }
            }
            if (breaker)
                continue;
            
            this.inputNodes[r] = grid[y][x];
        }
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                outputNodes[y*size + x] = grid[y][x];
    }
    
    public void tick()
    {
        for (GridUpdater g : updaters)
            g.run();
    }
    public void disp()
    {
        for (int y = 0; y < grid.length; y++)
        {
            for (int x = 0; x < grid[y].length; x++)
            {
                System.out.print(grid[x][y].currentInput + "\t");
            }
            System.out.println();
        }
    }
    public class GridUpdater implements Runnable
    {
        public Thread t;
        public GridNode[] nodes;
        public String ID;
        @Override
        public void run() {
            if (t == null)
            {
                t = new Thread(this, ID);
                t.start();
            }
            for (GridNode g : nodes)
                g.pullVal();
        }
        public GridUpdater(GridNode[] nodes)
        {
            this.nodes = nodes;
            this.ID = Math.random()+"";
        }
    }
    public String toString()
    {
        String retVal = "";
        for (int y = 0; y < grid.length; y++)
        {
            
            for (int x = 0; x < grid[y].length; x++)
            {
                boolean b = false;
                for (int i = 0; i < inputNodes.length; i++)
                    if (grid[y][x] == inputNodes[i])
                        b = true;
                
                retVal = retVal + "/";
                retVal = retVal + ((b) ? "TRUE" : "FALSE") + "/";
                
                Iterator it = grid[y][x].links.keySet().iterator();
                String[] list = new String[4];
                while (it.hasNext())
                {
                    GridNode node = (GridNode) it.next();
                    for (int ly = 0; ly < grid.length; ly++)
                    {
                        for (int lx = 0; lx < grid.length; lx++)
                        {
                            if (grid[ly][lx] == node)
                            {
                                if (ly > y)
                                    list[0] = "UP" + grid[y][x].links.get(node) + "/";
                                else if (ly < y)
                                    list[1] = "DOWN" + grid[y][x].links.get(node) + "/";
                                else if (lx > x)
                                    list[2] = "RIGHT" + grid[y][x].links.get(node) + "/";
                                else
                                    list[3] = "LEFT" + grid[y][x].links.get(node) + "/";
                            }
                        }
                    }
                }
                
                for (int i = 0; i < list.length; i++)
                {
                    if (list[i] != null)
                    {
                        retVal = retVal + list[i];
                    }
                }
                
                retVal = retVal + " ";
            }
            retVal = retVal + "\n";
        }
        return retVal;
    }
    public static Grid loadFromString(String s, boolean allInp)
    {
        String[] levels = s.split("\n");
        String[][] nodes = new String[levels.length][levels.length];
        for (int i = 0; i < levels.length; i++)
            nodes[i] = levels[i].split(" ");
        Grid g = new Grid(levels.length);
        if (allInp)
            g.inputNodes = new GridNode[g.grid.length*g.grid.length];
        else
            g.inputNodes = new GridNode[g.inputNodes.length];
        int insent = 0;
        
        for (int y = 0; y < nodes.length; y++)
            for (int x = 0; x < nodes.length; x++)
            {
                String[] process = nodes[y][x].split("/");
                
                if (nodes[y][x].contains("TRUE"))
                    g.inputNodes[insent++] = g.grid[y][x];
                
                for (int i = 1; i < process.length; i++)
                {
                    if (process[i].contains("UP"))
                        g.grid[y][x].links.put(g.grid[y+1][x], Double.parseDouble(process[i].substring(2)));
                    if (process[i].contains("DOWN"))
                        g.grid[y][x].links.put(g.grid[y-1][x], Double.parseDouble(process[i].substring(4)));
                    if (process[i].contains("RIGHT"))
                        g.grid[y][x].links.put(g.grid[y][x+1], Double.parseDouble(process[i].substring(5)));
                    if (process[i].contains("LEFT"))
                        g.grid[y][x].links.put(g.grid[y][x-1], Double.parseDouble(process[i].substring(4)));
                }
            }
        return g;
    }
    
    /**
     * Analyze inputs and generate gridstates, equivalent of generateHiInputs
     * of gridbrain yet placed within grid class, capable of handling multiple
     * different input output pairs, and using the memory the grid contains.
     * @param inputs
     * @return inputs as analyzed by grid
     */
    public double[][] analyze(double[][] inputs)
    {
        //INITIALIZE
        double[][] higherInputs = new double[10][this.outputNodes.length];
        double[][] hitemp;
        int sent = 0;
        for (int i = 0; i < inputs.length; i++)
        {
            double[] temp = new double[this.outputNodes.length];
            
            //INPUT
            for (int b = 0; b < inputs[i].length; b++)
            {
                this.inputNodes[b].currentInput = inputs[i][b];
            }
            
            //CALCULATE
            this.tick();
            
            
            //DISPLAY
            //this.disp();
            //System.out.println();
            
            //STORE
            for (int b = 0; b < temp.length; b++)
                temp[b] = this.outputNodes[b].currentInput;
            higherInputs[sent++] = temp;
            
            //RESIZE
            if (sent > higherInputs.length/2)
            {
                hitemp = new double[higherInputs.length*2][this.outputNodes.length];
                for (int b = 0; b < sent; b++)
                    hitemp[b] = higherInputs[b];
                higherInputs = hitemp;
            }
        }
        //DESIZE
        hitemp = new double[sent][this.outputNodes.length];
        for (int b = 0; b < sent; b++)
            hitemp[b] = higherInputs[b];
        higherInputs = hitemp;
        
        //RETURN
        return higherInputs;
    }
    
    public void setAllInputs()
    {
        inputNodes = new GridNode[grid.length*grid.length];
        for (int y = 0; y < grid.length; y++)
            for (int x = 0; x < grid.length; x++)
                inputNodes[y*grid.length + x] = grid[y][x];
    }
    
    public double[] getOutput()
    {
        double[] out = new double[inputNodes.length];
        for (int i = 0; i < out.length; i++)
            out[i] = inputNodes[i].currentInput;
        return out;
    }
    public void clear()
    {
        for (int y = 0; y < grid.length; y++)
            for (int x = 0; x < grid.length; x++)
                grid[y][x].currentInput = 0.0;
    }
}
