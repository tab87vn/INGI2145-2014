 import org.apache.log4j.Logger;
 import org.apache.log4j.Level;
 import java.io.*;
 import java.util.*;
 import com.google.common.collect.Lists;
 import scala.Tuple2;
 import org.apache.spark.api.java.*;
 import org.apache.spark.api.java.function.*;
 import org.apache.spark.util.Vector;

 public class WikipediaKMeans {

   //Finds the closest centroid to a point
   static int closestPoint(Vector p, List<Vector> centers) {
     // TODO 1-Implement this function
     // Refer to the http://spark.apache.org/docs/1.1.0/api/java/ API
     // for more info on Vector Data structure
   }

   //Computes the centroid from a list of points
   static Vector average(List<Vector> ps) {
     int numVectors = ps.size();
     Vector out = new Vector(ps.get(0).elements());
     for (int i = 0; i < numVectors; i++) {
       out.addInPlace(ps.get(i));
     }
     return out.divide(numVectors);
   }

   public static void main(String[] args) throws Exception {
     //Logger spark
     Logger.getLogger("spark").setLevel(Level.WARN);
     //Setting our spark home directory
     String sparkHome = "/usr/local/spark";
     System.out.println("Home directory: " + sparkHome);
     //Setting path to bundled jar file 
     String jarFile = "target/kmeans-1.0.jar";
     //Setting master to local (to use a remote cluster, change this with the appropriate link)
     String master = "local";
     //Generate a spark context
     JavaSparkContext sc = new JavaSparkContext(master, "WikipediaKMeans",
       sparkHome, jarFile);

     //K-Means clustering params
     int K = 10; //num. of clusters
     double convergeDist = .000001; //convergence threshold

     //Reading wikipeda 24-dim dataset from external file
     JavaPairRDD<String, Vector> data = sc.textFile(
       "src/main/resources/wiki-stats").mapToPair(
       new PairFunction<String, String, Vector>() {
         public Tuple2<String, Vector> call(String in) throws Exception {
           String[] parts = in.split("#");
           return new Tuple2<String, Vector>(
            parts[0], JavaHelpers.parseVector(parts[1]));
         }
       }).cache();
     long count = data.count();
     System.out.println("Number of records: " + count);

     //Generate initial K centroids by taking a random sample from our list of points
     List<Tuple2<String, Vector>> centroidTuples = data.takeSample(false, K, 42);
     final List<Vector> centroids = Lists.newArrayList();
     for (Tuple2<String, Vector> t: centroidTuples) {
       centroids.add(t._2());
     }
     System.out.println("Done selecting initial centroids");


     double tempDist;
     do {
       //Find the closest point to each centroid
       JavaPairRDD<Integer, List<Vector>> closest = data.mapToPair(
         new PairFunction<Tuple2<String, Vector>, Integer, List<Vector>>() {
           public Tuple2<Integer, List<Vector>> call(Tuple2<String, Vector> in) throws Exception {
             return new Tuple2<Integer, List<Vector>>(closestPoint(in._2(), centroids), 
             Arrays.asList(in._2()));
           }
         }
       );

      //Group the points in a list for each centroid
       JavaPairRDD<Integer, List<Vector>> pointsGroup = closest.reduceByKey(
         new Function2<List<Vector>, List<Vector>, List<Vector>>() {
            public List<Vector> call(List<Vector> l1, List<Vector> l2) throws Exception {
                return Lists.newArrayList(Iterables.concat(l1, l2));
          }
        });

       //Calculate the new centroids by averaging points
       Map<Integer, Vector> newCentroids = pointsGroup.mapValues(
         new Function<List<Vector>, Vector>() {
          public Vector call(List<Vector> ps) throws Exception {
            // TODO 2-Call the averaging function
         }
       }).collectAsMap();

       // TODO 3-Calculate tempDist - the squared distance between old and new centroids
       tempDist = 0.0;
       
       for (Map.Entry<Integer, Vector> t: newCentroids.entrySet()) {
         centroids.set(t.getKey(), t.getValue());
       }
       System.out.println(">>Finished iteration (delta = " + tempDist + ")");
     } while (tempDist > convergeDist); //Stop clustering if we go below threshold

     //Print out each cluster along with contained articles
     System.out.println("Cluster with some articles:");
     int numArticles = 10;
     for (int i = 0; i < centroids.size(); i++) {
       final int index = i;
       List<Tuple2<String, Vector>> samples =
       data.filter(new Function<Tuple2<String, Vector>, Boolean>() {
         public Boolean call(Tuple2<String, Vector> in) throws Exception {
         return closestPoint(in._2(), centroids) == index;
       }}).take(numArticles);
       for(Tuple2<String, Vector> sample: samples) {
        System.out.println(sample._1());
       }
       System.out.println();
     }
     sc.stop();
     System.exit(0);
   }
 }
