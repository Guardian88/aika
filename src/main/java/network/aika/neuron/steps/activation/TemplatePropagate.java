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
package network.aika.neuron.steps.activation;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.visitor.TemplateVisitor;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;


/**
 * Uses the Template Network defined in the {@link network.aika.neuron.Templates} to induce new template
 * activations and links.
 *
 * @author Lukas Molzberger
 */
public abstract class TemplatePropagate extends TemplateVisitor implements ActivationStep {

    private Direction direction;

    public TemplatePropagate(Direction dir) {
        direction = dir;
    }

    @Override
    public Phase getPhase() {
        return Phase.TEMPLATE;
    }

    public boolean checkIfQueued() {
        return true;
    }

    @Override
    public void process(Activation act) {
        Neuron<?> n = act.getNeuron();
        if (!n.allowTemplatePropagate(act))
            return;

        Map<Synapse, ActVisitor> templateSynapses = new TreeMap<>(
                Comparator.comparingInt(s -> s.getTemplateInfo().getTemplateSynapseId())
        );

        n.getTemplateGroup()
                .stream()
                .flatMap(tn ->
                        direction.getSynapses(tn)
                )
                .map(ts ->
                        new ActVisitor(this, act, ts, OUTPUT, direction)
                )
                .filter(v ->
                        v.getTargetSynapse().checkTemplatePropagate(v, act)
                )
                .forEach(v ->
                        templateSynapses.put(v.getTargetSynapse(), v)
                );

        direction.getLinks(act)
                .forEach(l ->
                        templateSynapses.remove(l.getSynapse().getTemplate())
                );

        templateSynapses.entrySet().forEach(e ->
                e.getKey().propagate(act, e.getValue())
        );
    }

    public String toString() {
        return "Act-Step: Template-Propagate-" + direction;
    }
}
