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
package network.aika.neuron.inhibitory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import network.aika.neuron.activation.visitor.Visitor;
import network.aika.neuron.steps.link.SumUpLink;

import static network.aika.neuron.sign.Sign.POS;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class InhibitorySynapse extends Synapse<Neuron<?>, InhibitoryNeuron> {


    @Override
    public boolean checkTemplatePropagate(Direction dir, Activation act) {
        return false;
    }

    public void transition(ActVisitor v, Synapse s, Link l) {
        s.inhibitoryTransitionLoop(v, l);
    }

    @Override
    public void samePatternTransitionLoop(ActVisitor v, Link l) {
        l.follow(v);
    }

    @Override
    public void inputPatternTransitionLoop(ActVisitor v, Link l) {
        l.follow(v);
    }

    @Override
    public void patternTransitionLoop(ActVisitor v, Link l) {
    }

    public void updateSynapse(Link l, double delta) {
        if(l.getInput().isActive(true)) {
            addWeight(delta);

            QueueEntry.add(
                    l,
                    new SumUpLink(l.getInputValue(POS) * delta)
            );
        }
    }

    @Override
    public void updateReference(Link l) {
        l.getOutput().propagateReference(
                l.getInput().getReference()
        );
    }

    @Override
    public Activation branchIfNecessary(Activation oAct, Visitor v) {
        return oAct;
    }

    public void setWeight(double weight) {
        super.setWeight(weight);
        input.getNeuron().setModified(true);
    }

    public void addWeight(double weightDelta) {
        super.addWeight(weightDelta);
        input.getNeuron().setModified(true);
    }

    @Override
    protected boolean checkCausality(Activation fromAct, Activation toAct, Visitor v) {
        return true;
    }
}
