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

import network.aika.Config;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.phase.link.LinkPhase;

import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.phase.link.LinkPhase.*;

/**
 *
 * @author Lukas Molzberger
 */
public class SelfGradient implements ActivationPhase {

    @Override
    public ActivationPhase[] getNextActivationPhases(Config c) {
        return new ActivationPhase[0];
    }

    @Override
    public LinkPhase[] getNextLinkPhases(Config c) {
        return new LinkPhase[0];
    }

    @Override
    public void process(Activation act) {
        Neuron n = act.getNeuron();

        if(n.isTemplate())
            return;

        act.initSelfGradient();

        if(n.isInputNeuron())
            return;

        act.addLinksToQueue(
                INPUT,
                LinkPhase.SELF_GRADIENT,
                SHADOW_FACTOR,
//                PROPAGATE_OUTPUT_GRADIENT,
//                PROPAGATE_SELF_GRADIENT,
                UPDATE_WEIGHTS
        );
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    @Override
    public void tryToLink(Activation act, Visitor v) {
    }

    @Override
    public void propagate(Activation act, Visitor v) {
    }

    @Override
    public int getRank() {
        return 8;
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return 0;
    }
}
