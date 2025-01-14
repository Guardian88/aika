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
package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.steps.activation.SumUpBias;
import network.aika.neuron.steps.link.SumUpLink;

import static network.aika.neuron.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ExcitatorySynapse<I extends Neuron<?>, O extends ExcitatoryNeuron<?>> extends Synapse<I, O> {

    public void updateSynapse(Link l, double delta) {
        if(l.getInput().isActive(true)) {
            addWeight(delta);

            QueueEntry.add(
                    l,
                    new SumUpLink(l.getInputValue(POS) * delta)
            );
        } else {
            addWeight(-delta);
            getOutput().addConjunctiveBias(delta, !l.isCausal());

            QueueEntry.add(
                    l.getOutput(),
                    new SumUpBias(delta)
            );
            QueueEntry.add(
                    l,
                    new SumUpLink((l.getInputValue(POS) * -delta) + delta)
            );
        }
    }
}
