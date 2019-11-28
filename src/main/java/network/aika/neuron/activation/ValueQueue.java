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


import java.util.ArrayDeque;

import network.aika.neuron.activation.Activation.OscillatingActivationsException;
import network.aika.neuron.activation.search.SearchNode;


/**
 *
 * @author Lukas Molzberger
 */
public class ValueQueue {
    private final ArrayDeque<Activation> queue = new ArrayDeque<>();


    public void propagateActivationValue(Activation act, SearchNode sn, boolean lowerBoundChange, boolean upperBoundChange)  {
        if(!lowerBoundChange && !upperBoundChange)
            return;

        act.getOutputLinks()
                .filter(l -> l.getOutput().needsPropagation(sn, lowerBoundChange, upperBoundChange))
                .forEach(l -> add(l.getOutput(), sn));
    }


    public void add(Activation act, SearchNode sn) {
        if(act == null || act.getCurrentOption().isQueued()) return;

        if(!act.addToValueQueue(queue, sn)) return;

        act.getCurrentOption().setQueued(true);
    }


    public double process(SearchNode sn) throws OscillatingActivationsException {
        add(sn.getActivation(), sn);

        double delta = 0.0;
        while (!queue.isEmpty()) {
            Activation act = queue.pollFirst();
            act.getCurrentOption().setQueued(false);

            delta += act.process(sn);
        }
        return delta;
    }
}

