/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.neuron.activation;

import network.aika.Document;
import network.aika.Utils;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.pattern.PatternNeuron;

import java.util.*;
import java.util.stream.Stream;

import static network.aika.neuron.Synapse.State.CURRENT;

/**
 *
 * @author Lukas Molzberger
 */
public class Activation implements Comparable<Activation> {

    public double value;
    public double net;
    public Fired fired;

    private int id;
    private INeuron<?> neuron;
    private Document doc;

    public double p;

    public TreeMap<Activation, Link> inputLinksFiredOrder = new TreeMap<>(FIRED_COMP);
    public Map<Neuron, Link> inputLinks = new TreeMap<>();
    public Map<Neuron, Link> outputLinks = new TreeMap<>();

    public boolean isFinal;


    public long visitedDown;
    public long visitedUp;

    public int round; // Nur als Abbruchbedingung
    public Activation nextRound;
    public Activation lastRound;

    public static Comparator<Activation> FIRED_COMP =
            Comparator
                    .<Activation, Fired>comparing(act -> act.getFired())
                    .thenComparing(act -> act);

    public static Comparator<Link> INPUT_COMP =
            Comparator.
                    <Link, Fired>comparing(l -> l.getInput().getFired())
                    .thenComparing(l -> l.getSynapse().getInput());


    public Activation(Document doc, INeuron<?> n, Activation lastRound, int round) {
        this.id = doc.getNewActivationId();
        this.doc = doc;
        this.neuron = n;
        this.round = round;

        this.net = n.getTotalBias(CURRENT);
        this.fired = null;

        this.lastRound = lastRound;
        lastRound.nextRound = this;

        doc.addActivation(this);
    }


    public int getId() {
        return id;
    }

    public Document getDocument() {
        return doc;
    }


    public String getLabel() {
        return getINeuron().getLabel();
    }


    public <N extends INeuron> N getINeuron() {
        return (N) neuron;
    }


    public Neuron getNeuron() {
        return neuron.getProvider();
    }


    public Fired getFired() {
        return fired;
    }


    public void followDown(long v, CollectResults c) {
        if(visitedDown == v) return;
        visitedDown = v;

        inputLinks
                .values()
                .stream()
                .forEach(l -> {
                    if(!(l.input.getINeuron() instanceof PatternNeuron)) {
                        l.input.followDown(v, c);
                    }
                    l.input.followUp(v, c);
                });
    }


    public void followUp(long v, CollectResults c) {
        if(visitedDown == v || visitedUp == v) return;
        visitedUp = v;

        if(isConflicting(v, this) || c.collect(this)) {
            return;
        }

        outputLinks
                .values()
                .stream()
                .forEach(l -> l.output.followUp(v, c));
    }


    public Activation cloneAct() {
        Activation clonedAct = new Activation(doc, neuron, this, round + 1);

        inputLinks
                .values()
                .forEach(l -> {
                    new Link(l.synapse, l.input, clonedAct).link();
                });

        return clonedAct;
    }


    public void setValue(double v) {
        this.value = v;
    }


    public void setFired(Fired fired) {
        this.fired = fired;
    }


    @Override
    public int compareTo(Activation act) {
        return Integer.compare(id, act.id);
    }


    public interface CollectResults {
        boolean collect(Activation act);
    }


    public boolean isConflicting(long v, Activation act) {
        return act.inputLinks.values().stream()
                .filter(l -> l.isRecurrent() && l.isNegative(CURRENT))
                .flatMap(l -> l.input.inputLinks.values().stream())  // Hangle dich durch die inhib. Activation.
                .anyMatch(l -> l.input.visitedDown != v);
    }


    public Stream<Link> getOutputLinks(Synapse s) {
        return outputLinks.values().stream()
                .filter(l -> l.synapse == s);
    }


    public void addLink(Link l) {
        l.link();

        assert !isFinal;

        if(inputLinks.isEmpty() || l.input.fired.compareTo(inputLinksFiredOrder.lastKey().fired) > 0) {
            sumUpLink(l);
        } else {
            compute();
        }
    }


    public void sumUpLink(Link l) {
        if(l.synapse == null) return;

        double w = l.synapse.getWeight();

        net += l.input.value * w;

        checkIfFired(l);
    }


    private void compute() {
        fired = null;
        net = 0.0;
        for (Link l: inputLinksFiredOrder.values()) {
            sumUpLink(l);
        }
    }


    public void checkIfFired(Link l) {
        if(fired == null && net > 0.0) {
            fired = neuron.incrementFired(l.input.fired);
            doc.getQueue().add(this);
        }
    }


    public void process() {
        value = neuron.getActivationFunction().f(net);

        isFinal = true;

        doc.getLinker().linkForward(this);

        lastRound = null;
    }


    public boolean isActive() {
        return value > 0.0;
    }

    public boolean equals(Activation s) {
        return Math.abs(value - s.value) <= INeuron.WEIGHT_TOLERANCE;
    }


    public String toString() {
        return getId() + " " +
                getINeuron().getClass().getSimpleName() + ":" + getLabel() +
                " value:" + Utils.round(value) +
                " net:" + Utils.round(net) +
                " p:" + Utils.round(p);
    }

    public double getP() {
        return 0.0;
    }


    public static class Builder {
        public double value = 1.0;
        public int inputTimestamp;
        public int fired;
        public List<Activation> inputLinks = new ArrayList<>();


        public Builder setValue(double value) {
            this.value = value;
            return this;
        }


        public Builder setInputTimestamp(int inputTimestamp) {
            this.inputTimestamp = inputTimestamp;
            return this;
        }

        public Builder setFired(int fired) {
            this.fired = fired;
            return this;
        }


        public List<Activation> getInputLinks() {
            return this.inputLinks;
        }


        public Builder addInputLink(Activation iAct) {
            inputLinks.add(iAct);
            return this;
        }
    }


    public static class OscillatingActivationsException extends RuntimeException {

        private String activationsDump;

        public OscillatingActivationsException(String activationsDump) {
            super("Maximum number of rounds reached. The network might be oscillating.");

            this.activationsDump = activationsDump;
        }


        public String getActivationsDump() {
            return activationsDump;
        }
    }

}
