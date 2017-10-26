package impl;

/**
 * 节点Node
 * @param <T>
 */
public class MyNode<T> {
    private T data;
    private int parent;

    public MyNode(){
    }

    public MyNode(T data){
        this.data = data;
    }

    public MyNode(T data, int parent){
        this.data = data;
        this.parent = parent;
    }

    public void setData(T data){
        this.data = data;
    }

    public T getData(){
        return this.data;
    }

    public void setParent(int parent){
        this.parent = parent;
    }

    public int getParent(){
        return this.parent;
    }
}