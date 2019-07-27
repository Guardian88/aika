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
package network.aika.lattice.refinement;

import network.aika.Model;
import network.aika.Provider;
import network.aika.Writable;
import network.aika.lattice.InputNode;
import network.aika.neuron.relation.Relation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * @author Lukas Molzberger
 */
public class Refinement implements Comparable<Refinement>, Writable {

    public RelationsMap relations;
    public Provider<InputNode> input;

    private Refinement() {}


    public Refinement(RelationsMap relations, Provider<InputNode> input) {
        this.relations = relations;
        this.input = input;
    }

    public boolean isConvertible() {
        for(Relation rel: relations.relations) {
            if(rel != null) return true;
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(relations);
        sb.append(input.get().logicToString());
        sb.append(")");
        return sb.toString();
    }


    public void write(DataOutput out) throws IOException {
        relations.write(out);
        out.writeInt(input.getId());
    }


    public void readFields(DataInput in, Model m) throws IOException {
        relations = RelationsMap.read(in, m);
        input = m.lookupNodeProvider(in.readInt());
    }


    public static Refinement read(DataInput in, Model m) throws IOException {
        Refinement k = new Refinement();
        k.readFields(in, m);
        return k;
    }


    @Override
    public int compareTo(Refinement ref) {
        int r = input.compareTo(ref.input);
        if(r != 0) return r;

        return relations.compareTo(ref.relations);
    }

    public boolean contains(Refinement ref, RefValue rv) {
        for(int i = 0; i < ref.relations.length(); i++) {
            Relation ra = ref.relations.get(i);
            Relation rb = relations.get(rv.offsets[i]);

            if((ra == null && rb != null) || (ra != null && rb == null)) return false;

            if(ra != null && rb != null && ra.compareTo(rb) != 0) {
                return false;
            }
        }

        return true;
    }
}
