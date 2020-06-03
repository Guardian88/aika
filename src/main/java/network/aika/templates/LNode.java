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
package network.aika.templates;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class LNode<N extends Neuron> {

    protected String label;

    protected List<LLink> links = new ArrayList<>();

    protected Class<N> neuronClass;

    protected abstract Activation follow(Neuron n, Activation act, LLink from, Activation startAct);

    public LNode() {
    }

    public LNode setLabel(String label) {
        this.label = label;
        return this;
    }

    public LNode setNeuronClass(Class<N> neuronClass) {
        this.neuronClass = neuronClass;
        return this;
    }

    public boolean isOpenEnd() {
        return links.size() <= 1;
    }

    public boolean checkNeuron(Neuron n) {
        if(neuronClass != null && !n.getClass().equals(neuronClass)) {
            return false;
        }

        return true;
    }

    public void addLink(LLink l) {
        links.add(l);
    }

    public String toString() {
        return label + " " + (neuronClass != null ? neuronClass.getSimpleName() : "X");
    }
}
