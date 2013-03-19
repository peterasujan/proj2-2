/*
 *
 * CS61C Spring 2013 Project 2: Small World
 *
 * Partner 1 Name: Alec Guertin
 * Partner 1 Login: de
 *
 * Partner 2 Name: Peter Sujan
 * Partner 2 Login: cc
 *
 * REMINDERS: 
 *
 * 1) YOU MUST COMPLETE THIS PROJECT WITH A PARTNER.
 * 
 * 2) DO NOT SHARE CODE WITH ANYONE EXCEPT YOUR PARTNER.
 * EVEN FOR DEBUGGING. THIS MEANS YOU.
 *
 */

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.Math;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;


public class SmallWorld {
    // Maximum depth for any breadth-first search
    public static final int MAX_ITERATIONS = 20;

    // Example writable type
    public static class EValue implements Writable {

        public int exampleInt; //example integer field
        public long[] exampleLongArray; //example array of longs

        public EValue(int exampleInt, long[] exampleLongArray) {
            this.exampleInt = exampleInt;
            this.exampleLongArray = exampleLongArray;
        }

        public EValue() {
            // does nothing
        }

        // Serializes object - needed for Writable
        public void write(DataOutput out) throws IOException {
            out.writeInt(exampleInt);

            // Example of serializing an array:
            
            // It's a good idea to store the length explicitly
            int length = 0;

            if (exampleLongArray != null){
                length = exampleLongArray.length;
            }

            // always write the length, since we need to know
            // even when it's zero
            out.writeInt(length);

            // now write each long in the array
            for (int i = 0; i < length; i++){
                out.writeLong(exampleLongArray[i]);
            }
        }

        // Deserializes object - needed for Writable
        public void readFields(DataInput in) throws IOException {
            // example reading an int from the serialized object
            exampleInt = in.readInt();

            // example reading length from the serialized object
            int length = in.readInt();

            // Example of rebuilding the array from the serialized object
            exampleLongArray = new long[length];
            
            for(int i = 0; i < length; i++){
                exampleLongArray[i] = in.readLong();
            }

        }

        public String toString() {
            // We highly recommend implementing this for easy testing and
            // debugging. This version just returns an empty string.
            return new String();
        }

    }


    /* Represents a node, as well as its neighbors and whether it
     * has been visited.
     */
    public static class Vertex implements Writable {
	int dist;
        int visited;
	ArrayList<LongWritable> neighbors;
	LongWritable origin;

	public Vertex() {
	}

	public Vertex(int d, int v, ArrayList<LongWritable> n, LongWritable orig) {
	    dist = d;
	    visited = v;
	    neighbors = n;
	    origin = orig;
	}

	public void write(DataOutput out) throws IOException {
	    
	    out.writeInt(dist);
	    out.writeInt(visited);

	    int length = 0;
	    if (neighbors != null){
                length = neighbors.size();
            }

            out.writeInt(length);

            // now write each long in the array
            for (int i = 0; i < length; i += 1){
		out.writeLong(neighbors.get(i).get());
            }
	    out.writeLong(origin.get());
	}

	public void readFields(DataInput in) throws IOException {
	    
	    dist = in.readInt();
	    visited = in.readInt();
	    int length = in.readInt();
	    LongWritable x = new LongWritable();
	    neighbors = new ArrayList<LongWritable>();
	    for (int i = 0; i < length; i++) {
		x.readFields(in);
		neighbors.add(new LongWritable(x.get()));
	    }
	    origin = new LongWritable(in.readLong());
	}

	public String toString() {
	    String output = "";
	    for (int i = 0; i < neighbors.size(); i++) {
		output += " " + neighbors.get(i);
	    }
	    output += " ; " + dist + " ; " + visited + " ; " + origin;
	    return output;
	}
    }


    /* The first mapper. Part of the graph loading process, currently just an 
     * identity function. Modify as you wish. */
    public static class LoaderMap extends Mapper<LongWritable, LongWritable, 
        LongWritable, LongWritable> {

        @Override
        public void map(LongWritable key, LongWritable value, Context context)
                throws IOException, InterruptedException {
            int inputValue = Integer.parseInt(context.getConfiguration().get("inputValue"));
            context.write(key, value);
	    context.write(value, new LongWritable(-1));
        }
    }


    /* The first reducer. This is also currently an identity function (although it
     * does break the input Iterable back into individual values). Modify it
     * as you wish. In this reducer, you'll also find an example of loading
     * and using the denom field.  
     */
    public static class LoaderReduce extends Reducer<LongWritable, LongWritable, 
        LongWritable, Vertex> {

        public long denom;

        public void reduce(LongWritable key, Iterable<LongWritable> values, 
            Context context) throws IOException, InterruptedException {

	    int denom = Integer.parseInt(context.getConfiguration().get("denom"));

            // You can print it out by uncommenting the following line:
            //System.out.println(denom);
	    ArrayList<LongWritable> armenians = new ArrayList<LongWritable>();
            for (LongWritable value : values){
		if (value.get() != -1) {
		    armenians.add(new LongWritable(value.get()));
		}
            }
	    int distance = Integer.MAX_VALUE;
	    int visisted = -1;
	    double chance = (new Random()).nextDouble();
	    if (chance <= (1.0 / denom)) {
		distance = 0;
		visisted = 0;
	    }
	    if (armenians.size() == 0) {
		context.write(key, new Vertex(0, 1, armenians, key));
	    } else {
		context.write(key, new Vertex(distance, visisted, armenians, key));
	    }
	}
    }

    // ------- Add your additional Mappers and Reducers Here ------- //

    /* The BFS mapper.*/
    public static class BFSMap extends Mapper<LongWritable, Vertex, 
        LongWritable, Vertex> {

        @Override
        public void map(LongWritable key, Vertex value, Context context)
                throws IOException, InterruptedException {

            int inputValue = Integer.parseInt(context.getConfiguration().get("inputValue"));

	    if (value.visited == 0) {
		for (int i = 0; i < value.neighbors.size(); i += 1) {
		    Vertex distPlus = new Vertex(value.dist + 1, value.visited, new ArrayList<LongWritable>(), value.origin);
		    context.write(value.neighbors.get(i), distPlus);
		}
		Vertex visPlus = new Vertex(value.dist, value.visited + 1, value.neighbors, value.origin);
		context.write(key, new Vertex(value.dist, value.visited + 1, value.neighbors, value.origin));
	    } else {
		context.write(key, value);
	    }
	}
    }


    /* The BFS reducer. */
    public static class BFSReduce extends Reducer<LongWritable, Vertex, 
        LongWritable, Vertex> {

        public long denom;

        public void reduce(LongWritable key, Iterable<Vertex> values, 
            Context context) throws IOException, InterruptedException {
	    
	    int minDist = Integer.MAX_VALUE;
	    ArrayList<LongWritable> serbians = null;
	    HashSet<Long> blackSet = new HashSet<Long>();
	    HashMap<Long, Integer> minDistances = new HashMap<Long, Integer>();
            for (Vertex value : values){
		if (value.visited == 0 && key.get() == value.origin.get() && value.dist != 0) {
		    continue;
		}
		if (value.visited != 0) {;
		    blackSet.add(value.origin.get());
		    if (minDistances.containsKey(value.origin.get())) {
			minDistances.remove(value.origin.get());
		    }
		    context.write(key, value);
		}
		if (!blackSet.contains(value.origin.get()) && (!minDistances.containsKey(value.origin.get()) || (minDistances.get(value.origin.get()) > value.dist))) {
		    minDistances.put(value.origin.get(), value.dist);
		}	
		if (value.neighbors.size() > 0) {
		    serbians = value.neighbors;
		}
            }
	    Iterator<Long> keys = minDistances.keySet().iterator();
	    while (keys.hasNext()) { 
		Long org = keys.next();
		Vertex temp = new Vertex(minDistances.get(org), 0, serbians, new LongWritable(org));
		context.write(key, temp);
	    }
	}

    }

    /* The Histogram mapper. */
    public static class HistoMap extends Mapper<LongWritable, Vertex, LongWritable, LongWritable> {

	static LongWritable one = new LongWritable(1);

        @Override
        public void map(LongWritable key, Vertex value, Context context)
                throws IOException, InterruptedException {

	    if (value.visited == 1) {
		context.write(new LongWritable((long) value.dist), one);
	    }
	}
    }


    /* The Histogram reducer. */
    public static class HistoReduce extends Reducer<LongWritable, LongWritable, 
        LongWritable, LongWritable> {

        public long denom;

        public void reduce(LongWritable key, Iterable<LongWritable> values, 
            Context context) throws IOException, InterruptedException {
           
	    long total = 0;

	    for (LongWritable value : values){            
                total += value.get();
            }
	    context.write(key, new LongWritable(total));
        }

    }


    public static void main(String[] rawArgs) throws Exception {
        GenericOptionsParser parser = new GenericOptionsParser(rawArgs);
        Configuration conf = parser.getConfiguration();
        String[] args = parser.getRemainingArgs();

        // Pass in denom command line arg:
        conf.set("denom", args[2]);

        // Sample of passing value from main into Mappers/Reducers using
        // conf. You might want to use something like this in the BFS phase:
        // See LoaderMap for an example of how to access this value
        conf.set("inputValue", (new Integer(5)).toString());

        // Setting up mapreduce job to load in graph
        Job job = new Job(conf, "load graph");

        job.setJarByClass(SmallWorld.class);

        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(LongWritable.class);
        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(Vertex.class);

        job.setMapperClass(LoaderMap.class);
        job.setReducerClass(LoaderReduce.class);

        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        // Input from command-line argument, output to predictable place
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path("bfs-0-out"));

        // Actually starts job, and waits for it to finish
        job.waitForCompletion(true);

        // Repeats your BFS mapreduce
        int i = 0;
        while (i < MAX_ITERATIONS) {
            job = new Job(conf, "bfs" + i);
            job.setJarByClass(SmallWorld.class);

            // Feel free to modify these four lines as necessary:
            job.setMapOutputKeyClass(LongWritable.class);
            job.setMapOutputValueClass(Vertex.class);
            job.setOutputKeyClass(LongWritable.class);
            job.setOutputValueClass(Vertex.class);

            job.setMapperClass(BFSMap.class);
            job.setReducerClass(BFSReduce.class);

            job.setInputFormatClass(SequenceFileInputFormat.class);
            job.setOutputFormatClass(SequenceFileOutputFormat.class);

            // Notice how each mapreduce job gets gets its own output dir
            FileInputFormat.addInputPath(job, new Path("bfs-" + i + "-out"));
            FileOutputFormat.setOutputPath(job, new Path("bfs-"+ (i+1) +"-out"));

            job.waitForCompletion(true);
            i++;
        }

        // Mapreduce config for histogram computation
        job = new Job(conf, "hist");
        job.setJarByClass(SmallWorld.class);

        // Feel free to modify these two lines as necessary:
        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(LongWritable.class);

        // DO NOT MODIFY THE FOLLOWING TWO LINES OF CODE:
        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(LongWritable.class);

        // You'll want to modify the following based on what you call your
        // mapper and reducer classes for the Histogram Phase
        job.setMapperClass(HistoMap.class);
        job.setReducerClass(HistoReduce.class);

        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        // By declaring i above outside of loop conditions, can use it
        // here to get last bfs output to be input to histogram
        FileInputFormat.addInputPath(job, new Path("bfs-"+ i +"-out"));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.waitForCompletion(true);
    }
}
