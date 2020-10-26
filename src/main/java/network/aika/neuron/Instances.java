package network.aika.neuron;

import network.aika.Model;
import network.aika.Writable;
import network.aika.neuron.activation.Reference;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Instances implements Writable {

    protected double N = 0;
    protected int lastPos;

    private Instances() {
    }

    public Instances(Model m, Reference ref) {
        this.lastPos = getAbsoluteBegin(m, ref);
    }

    public double getN() {
        return N;
    }

    public void setN(int N) {
        this.N = N;
    }

    public int getLastPos() {
        return lastPos;
    }

    public void update(Model m, Reference ref) {
        N += 1 + ((getAbsoluteBegin(m, ref) - lastPos) / ref.length());
        lastPos = getAbsoluteEnd(m, ref);
    }

    public int getAbsoluteBegin(Model m, Reference ref) {
        return m.getN() + (ref != null ? ref.getBegin() : 0);
    }

    public int getAbsoluteEnd(Model m, Reference ref) {
        return m.getN() + (ref != null ? ref.getEnd() : 0);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(N);
        out.writeInt(lastPos);
    }

    public static Instances read(DataInput in, Model m) throws IOException {
        Instances instances = new Instances();
        instances.readFields(in, m);
        return instances;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        N = in.readDouble();
        lastPos = in.readInt();
    }

    public String toString() {
        return "N:" + N + " lastPos:" + lastPos;
    }
}
