package template;

import java.util.Comparator;

import logist.topology.Topology.City;

public class Edge {
    private int a, b;
    private double distance;

    public Edge(int a, int b, double distance){
        this.a = a;
        this.b = b;
        this.distance = distance;
    }

    public double getDistance(){
        return distance;
    }

    public int getA(){
        return a;
    }

    public int getB(){
        return b;
    }

    public static class SortDistance implements Comparator<Edge> {
		public int compare(Edge ex, Edge ey) {
			return Double.compare(ex.getDistance(), ey.getDistance());
		}
	}

}
