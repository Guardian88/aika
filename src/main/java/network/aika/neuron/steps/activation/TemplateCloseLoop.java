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

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.visitor.TemplateTask;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 * Uses the Template Network defined in the {@link network.aika.neuron.Templates} to induce new template
 * activations and links.
 *
 * @author Lukas Molzberger
 */
public class TemplateCloseLoop extends TemplateTask implements ActivationStep {

    public static class TemplateCloseLoopInput extends TemplateCloseLoop {
        public TemplateCloseLoopInput() {
            super(INPUT);
        }
    }

    public static class TemplateCloseLoopOutput extends TemplateCloseLoop {
        public TemplateCloseLoopOutput() {
            super(OUTPUT);
        }
    }

    private TemplateCloseLoop(Direction dir) {
        super(dir);
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    @Override
    public void process(Activation act) {
        link(act);
    }

    public boolean checkIfQueued() {
        return true;
    }

    public String toString() {
        return "Act-Step: Template-CloseCycle (" + direction + ")";
    }
}
