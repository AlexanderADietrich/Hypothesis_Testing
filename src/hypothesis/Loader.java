/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hypothesis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author voice
 */
public class Loader {
    public static double[] resize(double[] arr)
    {
        double[] retVal = new double[arr.length*2];
        int sent = 0;
        for (double d : arr)
            retVal[sent++] = d;
        return retVal;
    }
    /**
     * Assumes that trailing zeros are not desired
     * @param arr
     * @return arr without 0's
     */
    public static double[] desize(double[] arr)
    {
        int i = arr.length-1;
        for (; arr[i] == 0; i--)
            i = i;
        double[] retVal = new double[i];
        for (int b = 0; b < retVal.length; b++)
            retVal[b] = arr[b];
        return retVal;
    }
    public static String[] Keys = new String[]{
        "Open",
        "High",
        "Low",
        "Close",
        "Adj Close",
        "Volume"
    };
    public static final int RETRIES = 1;
    public static double[] loadDynamicStock(String symbol, String keyword, boolean retry)
    {
        int ind = -1;
        for (int b = 0; b < Keys.length; b++)
            if (Keys[b].toLowerCase().equals(keyword))
                ind = b;
        if (ind == -1)
        {
            System.out.println("Load failure, " + symbol + " contains invalid keyword");
            return null;
        }
        
        String cached = loadCache(symbol+keyword);
        if (!"".equals(cached))
        {
            String[] split = cached.split("\n");
            double[] retVal = new double[split.length];
            for (int i = 0; i < split.length; i++)
            {
                retVal[i] = Double.parseDouble(split[i]);
            }
            return retVal;
        }
        
        
        System.setProperty("http.agent", "Chrome");
        String address = "https://www.textise.net/showText.aspx?strURL=https%253A//finance.yahoo.com/quote/"
                + symbol
                + "/history%253Fp%253D"
                + symbol;
        ArrayList<Double> days = new ArrayList<>();
        ArrayList<Double> vals = new ArrayList<>();
        
        int sent = 0;
        int times = RETRIES;
        boolean found = false;
        while (vals.isEmpty() && (retry || times > 0) )
        {
            times--;
            try {
                URL u = new URL(address);
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
                    String input;
                    while ((input = in.readLine()) != null)
                    {
                        if (input.contains("similar to") || input.contains("Sorry"))
                        {
                            System.out.println("Load failure, input for " + symbol + " contains invalid text");
                            continue;
                        }
                        if (input.contains("Currency in USD"))
                        {
                            input = input.substring(input.indexOf("Currency in USD"));
                            input = input.substring(0, input.indexOf("price adjusted"));
                            input = input.replace("<span>", " ");
                            input = input.replace("</span>&nbsp;", " ");
                            input = input.substring(input.indexOf("Date  Open  High  Low  Close*  Adj Close**  Volume  ")+"Date  Open  High  Low  Close*  Adj Close**  Volume  ".length(), 
                                    input.indexOf("*Close "));
                            String[] list = input.split("  ");
                            int insent = 0;
                            int loc = -1;
                            double d;
                            while (++loc < list.length)
                            {
                                try
                                {
                                    d = Double.parseDouble(list[loc].replace(",", ""));
                                }
                                catch (Exception ex)
                                {
                                    continue;
                                }
                                if (insent % 6 == ind)
                                {
                                    days.add(1.0*sent++);
                                    vals.add(Double.parseDouble(list[loc].replace(",", "")));
                                }
                                insent++;
                            }
                        }
                    }
                } catch (IOException ex) {
                    System.out.println("FAILED TO OPEN BUFFERED READER");
                    ex.printStackTrace();
                    return null;
                }
            } catch (MalformedURLException ex) {
                System.out.println("FAILED TO OPEN URL");
                ex.printStackTrace();
                return null;
            }
        }
        
        if (vals.isEmpty())
        {
            System.out.println("Load failure, " + symbol + " failed to load data for " + Keys[ind]);
            return new double[0];
        }
        
        //keeping inp for use later
        double[] inp = new double[vals.size()];
        double[] out = new double[vals.size()];
        String toCache = "";
        boolean doCache = "".equals(cached);
        
        for (int i = 0; i < inp.length; i++)
        {
            
            inp[i] = days.get(i);
            out[i] = vals.get(vals.size()-i-1);
            if (doCache)
                toCache = toCache + out[i] + "\n";
        }
        if (doCache)
            cache(toCache, symbol+keyword);
        
        return out;
    }
    public static void saveString(String s, String address)
    {
        saveString(s, address, false);
    }
    public static void saveString(String s, String address, boolean cosmetic)
    {
        File f = new File(address+".txt");
        if (f.exists())
        {
            try 
            {
                f.delete();
                f.createNewFile();
            } 
            catch (IOException ex) 
            {
                System.out.println("FAILED RECREATING FILE");
                ex.printStackTrace();
            }
        }
        else
        {
            try {
                f.createNewFile();
            } 
            catch (IOException ex) 
            {
                System.out.println("FAILED CREATING FILE");
                ex.printStackTrace();
            }
        }
        
        try (PrintStream out = new PrintStream(new FileOutputStream(address + ".txt"))) 
        {
            if (cosmetic)
            {
                String[] split = s.split("\n");
                for (String st : split)
                    out.println(st);
            }
            else
            {
                out.print(s);
            }
        } 
        catch (FileNotFoundException ex) 
        {
            System.out.println("FAILED PRINTING TO FILE");
            ex.printStackTrace();
        }
    }
    public static String loadString(String address)
    {
        //System.out.println("C:\\Users\\voice\\Documents\\NetBeansProjects\\Hypothesis\\"+address);
        File f = new File("C:\\Users\\voice\\Documents\\NetBeansProjects\\Hypothesis_Testing\\"+address+".txt");
        boolean b = false;
        try {
            b = !f.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
        }
        String retVal = "";
        String temp;
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            try {
                while ((temp = br.readLine()) != null)
                {
                    retVal = retVal + temp + "\n";
                }
            } catch (IOException ex) {
                System.out.println("FAILED READING LINE");
                ex.printStackTrace();
            }
        } catch (FileNotFoundException ex) {
            System.out.println("FAILED OPENING READER");
            ex.printStackTrace();
        }
        if (retVal.length() > 0)
            retVal = retVal.substring(0, retVal.length()-1);
        return retVal;
    }
    
    public static double[][][] loadData(String stock, String[] keyTerms, int previous, boolean retry)
    {
        return loadData(stock, keyTerms, previous, retry, 0);
    }
    
    public static double[][][] loadData(String stock, String[] keyTerms, int previous, boolean retry, int offset)
    {
        return loadData(stock, keyTerms, previous, retry, offset, false);
    }
    /**
     * Returns four double[][]'s, inputs and outputs where inputs are stocks
     * loaded with keywords and outputs are the first keyword one day ahead, temp
     * a double[][] containing all inputs other than the final, and finalinput
     * of the inputs
     * @param stock
     * @param keyTerms
     * @param previous
     * @param retry
     * @param offset days forward to predict, default is 1 for (output == 0)
     * @param crunch
     * @return [0][0][0] on failure, data if not
     */
    public static double[][][] loadData(String stock, String[] keyTerms, int previous, boolean retry, int offset, boolean crunch)
    {
        double[][] outputs = new double[keyTerms.length][];
        
        for (int b = 0; b < outputs.length; b++)
        {
            outputs[b] = Loader.loadDynamicStock(stock, keyTerms[b], retry);
            //failed
            if (outputs[b] == null)
                return new double[0][0][0];
            if (outputs[b].length == 0)
                return new double[0][0][0];
        }
        
        //~~~~~~~~~~~~~~~~~normalize inputs~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        double[] averages = new double[outputs.length];
        
        for (int i = 0; i < outputs.length; i++)
        {
            //find input averages
            for (double d : outputs[i])
                averages[i] += d;
            averages[i] /= outputs[i].length;
            //normalize
            for (int b = 0; b < outputs[i].length; b++)
            {
                outputs[i][b] *= (1.0 / averages[i]);
            }
        }
        
        /*for (int i = 0; i < outputs.length; i++)
        {
            for (int b = 0; b < outputs[i].length; b++)
                System.out.print(outputs[i][b] + "\t");
            System.out.println();
        }*/
        
        //format to final
        double[][] inp = new double[outputs[0].length-previous][previous*outputs.length];
        double[][] out = new double[outputs[0].length-previous][1];
        
        for (int i = 0; i < inp.length; i++)
        {
            for (int b = 0; b < previous; b++)
            {
                for (int c = 0; c < outputs.length; c++)
                    // a b c a2 b2 c2
                    inp[i][b+previous*c] = outputs[c][i+b];
            }
            out[i][0] = outputs[0][i+previous];
        }
        
        double[] finalInput = new double[previous*keyTerms.length];
        for (int b = 0; b < previous; b++)
        {
            for (int c = 0; c < outputs.length; c++)
                    finalInput[b+c*previous] = outputs[c][outputs[c].length-previous+b];
        }
        
        double[][] unprocInp = inp;
        double[][] temp;
        //process offset, default offset is 1 for prediction purposes
        if (offset > 0)
        {
            //offset out by cutting beginning
            temp = out;
            out = new double[out.length - offset][1];
            System.arraycopy(temp, offset, out, 0, out.length);
            //offset inp by cutting end (doesn't affect pred as finalInput remains the same).
            temp = inp;
            inp = new double[inp.length - offset][temp[0].length];
            System.arraycopy(temp, 0, inp, 0, inp.length);
        }
        
        
        return new double[][][]{inp, out, unprocInp, new double[][]{finalInput}};
    }
    public static final String cacheSep = "CACHE";
    public static String cachedDate = "";
    public static void cache(String s, String address)
    {
        if (cachedDate.equals(""))
        {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            Date date = new Date();
            cachedDate = ""+dateFormat.format(date);
        }
        saveString(cachedDate + cacheSep + s, address+"Cache");
    }
    public static String loadCache(String address)
    {
        if (cachedDate.equals(""))
        {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            Date date = new Date();
            cachedDate = ""+dateFormat.format(date);
        }
        
        String s = loadString(address+"Cache");
        if (s.equals(""))
        {
            System.out.println("Load Failure");
            return "";
        }
        String date = s.substring(0, s.indexOf(cacheSep));
        if (!date.equals(cachedDate))
        {
            System.out.println("Old Cache or Load Failure");
            return "";
        }
        return s.substring(s.indexOf(cacheSep)+cacheSep.length());
    }
}
