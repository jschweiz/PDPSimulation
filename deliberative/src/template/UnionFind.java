package template;

public class UnionFind {
    int[] parent;
    int[] rank;

    public UnionFind(int size){
        parent = new int[size];
        rank = new int[size];

        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
    }   

    public int find(int i){
        int p = parent[i];
        if (p == i){
            return i;
        }
        else {
            int r = find(p);
            parent[i] = r;
            return r;
        }
    }

    public void union(int i, int j) {
        int root1 = find(i);
        int root2 = find(j);

        if (rank[root1] < rank[root2]){
            parent[root1] = root2;
        }
        else if (rank[root1] > rank[root2]){
            parent[root2] = root1;
        }
        else {
            parent[root2] = root1;
            rank[root1]++;
        }
    }
}