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
package network.aika.neuron.phase.activation;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.phase.Ranked;
import network.aika.neuron.phase.RankedImpl;

import java.util.Comparator;

import static network.aika.neuron.phase.link.LinkPhase.SHADOW_FACTOR;

/**
 * Computes the gradient of the entropy function for this activation.
 *
 * @see <a href="https://aika.network/training.html">Aika Training</a>
 *
 * @author Lukas Molzberger
 */
public class EntropyGradient extends RankedImpl implements ActivationPhase {

    @Override
    public Ranked getPreviousRank() {
        return SHADOW_FACTOR;
    }

    @Override
    public void process(Activation act, int round) {
        Neuron n = act.getNeuron();

        if(n.isTemplate())
            return;

        act.initEntropyGradient();

        if(!act.gradientIsZero())
            QueueEntry.add(act, 0, PROPAGATE_GRADIENTS_SUM);
    }


    @Override
    public Comparator<Activation> getElementComparator() {
        return Comparator.naturalOrder();
    }

    public String toString() {
        return "Act-Phase: Entropy Gradient";
    }
}
