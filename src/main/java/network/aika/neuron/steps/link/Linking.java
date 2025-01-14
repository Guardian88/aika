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
package network.aika.neuron.steps.link;


import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.visitor.LinkingTask;

import static network.aika.neuron.activation.direction.Direction.INPUT;


/**
 * Uses the visitor to link neighbouring links to the same output activation.
 *
 * @author Lukas Molzberger
 */
public class Linking extends LinkingTask implements LinkStep {

    public Linking() {
        super(INPUT);
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    public boolean checkIfQueued() {
        return true;
    }

    @Override
    public void process(Link l) {
        link(l);

        QueueEntry.add(l, COUNTING);
    }

    public String toString() {
        return "Link-Step: Linking";
    }
}
