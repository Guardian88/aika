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
package org.aika;


import org.aika.corpus.Document;
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.lattice.AndNode;
import org.aika.lattice.Node;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The model consists of two layers. The first layer is the actual neural network consisting of neurons and synapses.
 * The second layer is a pattern lattice containing a boolean logic representation of all the neurons. Whenever the
 * synapse weights of a neuron are adjusted, then the underlying boolean logic representation of this neuron will be
 * updated too.
 * <p>
 * <p>The model supports parallel processing using a fixed number of threads.
 *
 * @author Lukas Molzberger
 */
public class Model {

    public int numberOfThreads = 1;

    public int[] lastCleanup;

    public Document[] docs;

    public SuspensionHook suspensionHook;


    public AtomicInteger currentId = new AtomicInteger(0);

    // Important: the id field needs to be referenced by the provider!
    public WeakHashMap<Integer, WeakReference<Provider<? extends AbstractNode>>> providers = new WeakHashMap<>();
    public Map<Integer, Provider<? extends AbstractNode>> activeProviders = new TreeMap<>();

    public Statistic stat = new Statistic();

    public int defaultThreadId = 0;

    public Set<Provider<AndNode>> numberOfPositionsQueue = Collections.synchronizedSet(new TreeSet<>(new Comparator<Provider<AndNode>>() {
        @Override
        public int compare(Provider<AndNode> n1, Provider<AndNode> n2) {
            int r = Integer.compare(n1.get().numberOfPositionsNotify, n2.get().numberOfPositionsNotify);
            if (r != 0) return r;
            return n1.compareTo(n2);
        }
    }));

    public volatile int numberOfPositions;


    /**
     * Creates a model with a single thread.
     */
    public Model() {
        this(null, 1);
    }


    public Model(SuspensionHook sh, int numberOfThreads) {
        assert numberOfThreads >= 1;
        this.numberOfThreads = numberOfThreads;

        lastCleanup = new int[numberOfThreads];
        docs = new Document[numberOfThreads];
        suspensionHook = sh;
    }


    public Neuron createNeuron() {
        return new INeuron(this).provider;
    }


    public Neuron createNeuron(String label) {
        return new INeuron(this, label).provider;
    }


    public Neuron createNeuron(String label, String outputText) {
        return new INeuron(this, label, outputText).provider;
    }


    public Document createDocument(String txt) {
        return createDocument(txt, 0);
    }


    public Document createDocument(String txt, int threadId) {
        Document doc = new Document(txt, this, threadId);

        if (txt != null) {
            doc.changeNumberOfPositions(doc.length());

            if (docs[threadId] != null) {
                throw new RuntimeException("Two documents are using the same thread. Call clearActivations() first, before processing the next document.");
            }
            docs[threadId] = doc;
        }

        return doc;
    }


    public <P extends Provider<? extends Node>> P lookupNodeProvider(int id) {
        synchronized (providers) {
            WeakReference<Provider<? extends AbstractNode>> wr = providers.get(id);
            if(wr != null) {
                P p = (P) wr.get();
                if (p != null) {
                    return p;
                }
            }

            return (P) new Provider(this, id);
        }
    }



    public Neuron lookupNeuron(int id) {
        synchronized (providers) {
            WeakReference<Provider<? extends AbstractNode>> wr = providers.get(id);
            if(wr != null) {
                Neuron n = (Neuron) wr.get();
                if (n != null) {
                    return n;
                }
            }

            return new Neuron(this, id);
        }
    }


    public void resetFrequency() {
        for (int t = 0; t < numberOfThreads; t++) {
            synchronized (activeProviders) {
                for (Provider<? extends AbstractNode> p : activeProviders.values()) {
                    if (p != null && p.get() instanceof Node) {
                        ((Node) p.get()).frequency = 0;
                    }
                }
            }
        }
    }


    public void register(Provider p) {
        synchronized (activeProviders) {
            activeProviders.put(p.id, p);
        }
    }


    public void unregister(Provider p) {
        synchronized (activeProviders) {
            activeProviders.remove(p.id);
        }
    }


    /**
     * Suspend all neurons and logic nodes whose last used document id is lower/older than {@param docId}.
     *
     * @param docId
     */
    public void suspendUnusedNodes(int docId) {
        List<Provider> tmp;
        synchronized (activeProviders) {
            tmp = new ArrayList<>(activeProviders.values());
        }
        for (Provider p: tmp) {
            suspend(docId, p);
        }
    }


    /**
     * Suspend all neurons and logic nodes in memory.
     *
     */
    public void suspendAll() {
        suspendUnusedNodes(Integer.MAX_VALUE);
    }


    private boolean suspend(int docId, Provider<? extends AbstractNode> p) {
        AbstractNode an = p.getIfNotSuspended();
        if (an != null && an.lastUsedDocumentId <= docId) {
            p.suspend();
            return true;
        }
        return false;
    }


    /**
     * Discards all unsuspended neurons and logic nodes.
     */
    public void discardAll() {
        List<Provider> tmp;
        synchronized (activeProviders) {
            tmp = new ArrayList<>(activeProviders.values());
        }
        for (Provider p: tmp) {
            p.discard();
        }
    }



    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public Neuron initNeuron(Neuron n, double bias, Collection<Input> inputs) {
        Set<Synapse> is = new TreeSet<>(Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP);

        for (Input input : inputs) {
            Synapse s = input.getSynapse(n);
            s.nw = input.weight;
            bias -= Math.abs(input.weight) * input.biasDelta;
            is.add(s);
        }

        return INeuron.init(this, defaultThreadId, n, bias, is);
    }


    public void addSynapse(Neuron n, Input input) {
        double biasDelta = 0.0;

        Synapse s = input.getSynapse(n);

        s.nw = input.weight;

        biasDelta -= Math.abs(input.weight) * input.biasDelta;

        INeuron.addSynapse(this, defaultThreadId, n, biasDelta, s);
    }

    /**
     * Creates a neuron with the given bias.
     *
     * @param n
     * @param bias
     * @param inputs
     * @return
     */
    public Neuron initNeuron(Neuron n, double bias, Input... inputs) {
        return initNeuron(n, bias, new TreeSet<>(Arrays.asList(inputs)));
    }


    public void removeProvider(Provider p) {
        activeProviders.remove(p.id);
        providers.remove(p.id);
    }


    public static class Statistic {
        public volatile int synapses;
        public volatile int neurons;
        public volatile int nodes;
        public volatile int orNodes;
    }
}
