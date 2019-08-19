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
package network.aika.neuron.activation.search;

import network.aika.Document;
import network.aika.neuron.activation.Activation;


/**
 *
 * @author Lukas Molzberger
 */
public class Branch {
    boolean visited;
    boolean searched;
    double weight = 0.0;
    double weightSum = 0.0;

    SearchNode child = null;

    public boolean prepareStep(Document doc, SearchNode c) {
        child = c;

        child.updateActivations(doc);

        if (!child.followPath()) {
            return true;
        }

        searched = true;
        return false;
    }


    public void postStep(double returnWeight, double returnWeightSum) {
        weight = returnWeight;
        weightSum = returnWeightSum;

        child.setWeight(returnWeightSum);
        child.changeState(Activation.Mode.OLD);
    }


    public void repeat() {
        visited = false;
        searched = false;
    }


    public void cleanup() {
        if(child == null) {
            return;
        }
        child.getModifiedActivations()
                .values()
                .forEach(o -> o.cleanup());

        child = null;
    }
}