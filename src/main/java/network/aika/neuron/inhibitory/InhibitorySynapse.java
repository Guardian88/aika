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

import network.aika.neuron.*;
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.visitor.Visitor;


/**
 *
 * @author Lukas Molzberger
 */
public class InhibitorySynapse extends Synapse<Neuron<?>, InhibitoryNeuron> {

    public InhibitorySynapse() {
        super();
    }

    public InhibitorySynapse(Neuron<?> input, InhibitoryNeuron output) {
        super(input, output);
    }

    @Override
    public void updateReference(Link l) {
        l.getOutput().propagateReference(
                l.getInput().getReference()
        );
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        return false;
    }

    @Override
    public InhibitorySynapse instantiateTemplate(Neuron<?> input, InhibitoryNeuron output) {
        assert input.getTemplateGroup().contains(getInput());

        InhibitorySynapse s = new InhibitorySynapse(input, output);
        initFromTemplate(s);
        return s;
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