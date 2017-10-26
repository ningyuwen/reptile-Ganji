package impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 树
 * @param <T>
 */
public class MyTree<T> {
    private final int DEFAULT_SIZE = 2;
    private int size;
    private int count;
    private Object[] nodes;

    public MyTree() {
        this.size = this.DEFAULT_SIZE;
        this.nodes = new Object[this.size];
        this.count = 0;
    }

    public MyTree(MyNode<T> root) {
        this();
        this.count = 1;
        this.nodes[0] = root;
    }

    public MyTree(MyNode<T> root, int size) {
        this.size = size;
        this.nodes = new Object[this.size];
        this.count = 1;
        this.nodes[0] = root;
    }

    //添加一个节点
    public void add(MyNode<T> myNode) {
        for (int i = 0; i < this.size; i++) {
            if (this.nodes[i] == null) {
                nodes[i] = myNode;
                break;
            }
        }
        this.count++;
    }

    public void check(){
        if(this.count >= this.size){
            this.enlarge();
        }
    }

    //添加一个节点，并指明父节点
    public void add(MyNode<T> myNode, MyNode<T> parent) {
        this.check();
        myNode.setParent(this.position(parent));
        this.add(myNode);
    }

    //获取节点在数组的存储位置
    public int position(MyNode<T> myNode) {
        for (int i = 0; i < this.size; i++) {
            if (nodes[i] == myNode) {
                return i;
            }
        }
        return -1;
    }

    //获取整棵树有多少节点
    public int getSize(){
        return this.count;
    }

    //获取根节点
    @SuppressWarnings("unchecked")
    public MyNode<T> getRoot(){
        return (MyNode<T>) this.nodes[0];
    }

    //获取所以节点，以List形式返回
    @SuppressWarnings("unchecked")
    public List<MyNode<T>> getAllNodes(){
        List<MyNode<T>> list = new ArrayList<MyNode<T>>();
        for(int i=0;i<this.size;i++){
            if(this.nodes[i] != null){
                list.add((MyNode<T>)nodes[i]);
            }
        }
        return list;
    }

    //获取树的深度，只有根节点时为1
    @SuppressWarnings("unchecked")
    public int getDepth(){

        int max = 1;
        if(this.nodes[0] == null){
            return 0;
        }

        for(int i=0;i<this.count;i++){
            int deep = 1;
            int location = ((MyNode<T>)(this.nodes[i])).getParent();
            while(location != -1 && this.nodes[location] != null){
                location = ((MyNode<T>)(this.nodes[location])).getParent();
                deep++;
            }
            if(max < deep){
                max = deep;
            }
        }
        return max;
    }

    public void enlarge(){
        this.size = this.size + this.DEFAULT_SIZE;
        Object[] newNodes = new Object[this.size];
        newNodes = Arrays.copyOf(nodes, this.size);
        Arrays.fill(nodes, null);
        this.nodes = newNodes;
//        System.out.println("enlarge");
    }
}