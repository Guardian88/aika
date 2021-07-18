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
package network.aika.neuron.steps.visitor;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.steps.VisitorStep;

import java.util.stream.Stream;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.steps.link.LinkStep.LINKING;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class LinkingVisitor extends VisitorStep {

    public LinkingVisitor(Direction dir) {
        super(dir);
    }

    @Override
    public Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir) {
         return dir.getSynapses(act.getNeuron())
                 .filter(s -> !exists(act, s, direction));
    }

    @Override
    public boolean exists(Activation act, Synapse s, Direction dir) {
        return dir.linkExists(act, s);
    }

    @Override
    public boolean checkPropagate(Activation act, Synapse targetSynapse) {
        return true;
    }

    @Override
    public void getNextSteps(Activation act) {
    }

    @Override
    public void getNextSteps(Link l) {
        QueueEntry.add(l, LINKING);
    }

    @Override
    public void closeLoopIntern(ActVisitor v, Activation iAct, Activation oAct) {
        if (!iAct.isActive(false))
            return;

        Synapse s = v.getTargetSynapse();

        if(!(v.getCurrentDir() == OUTPUT || s.isRecurrent()))
            return;

        oAct = s.branchIfNecessary(oAct, v);

        if(oAct != null)
            s.closeLoop(v, iAct, oAct);
    }
}
