package org.aika.training;


import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.corpus.Conflicts;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.SearchNode;
import org.aika.lattice.NodeActivation;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import static org.aika.corpus.InterprNode.State.SELECTED;
import static org.aika.corpus.InterprNode.State.EXCLUDED;
import static org.aika.corpus.InterprNode.State.UNKNOWN;

public class MetaNetwork {


    public static void train(Document doc) {
        long v = doc.visitedCounter++;
        for(INeuron n: new ArrayList<>(doc.finallyActivatedNeurons)) {
            if(n.type == INeuron.Type.INHIBITORY) {
                for (Activation sAct : n.getFinalActivations(doc)) {
                    for(Activation.SynapseActivation sa: sAct.getFinalInputActivations()) {
                        Activation act = sa.input;
                        Neuron targetNeuron = act.key.node.neuron;
                        boolean newNeuron = false;
                        if(targetNeuron.get().type == INeuron.Type.META) {
                            newNeuron = true;
                            targetNeuron = doc.model.createNeuron(n.label.substring(2) + "-" + doc.getText(act.key.range));
                            INeuron.update(doc.model, doc.threadId, targetNeuron, n.bias, Collections.emptySet());
                        }

                        Activation metaNeuronAct = getMetaNeuronAct(sAct);
                        if(metaNeuronAct != null && metaNeuronAct.visited != v) {
                            metaNeuronAct.visited = v;
                            transferMetaSynapses(doc, metaNeuronAct, targetNeuron, newNeuron, n.provider, v);
                        }
                    }
                }
            }
        }
    }


    private static Activation getMetaNeuronAct(Activation sAct) {
        for(Activation.SynapseActivation sa: sAct.neuronInputs) {
            if(sa.input.key.node.neuron.get().label.startsWith("M-")) {
                return sa.input;
            }
        }
        return null;
    }


    private static void transferMetaSynapses(Document doc, Activation metaAct, Neuron targetNeuron, boolean newNeuron, Neuron supprN, long v) {
        TreeSet<Synapse> inputSynapses = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);

        Integer ridOffset = computeRidOffset(metaAct);
        for (Activation.SynapseActivation sa : metaAct.getFinalInputActivations()) {
            MetaSynapse ss = sa.synapse.meta;
            if (ss != null && (ss.metaWeight != 0.0 || ss.metaBias != 0.0)) {
                Neuron ina = sa.input.key.node.neuron;
                Neuron inb = null;
                Integer rid = null;

                if (ina.get().type == INeuron.Type.INHIBITORY && ss.metaWeight >= 0.0) {
                    List<Activation.SynapseActivation> inputs = sa.input.getFinalInputActivations();
                    for(Activation.SynapseActivation iSA: inputs) {
                        Activation iAct = iSA.input;
                        inb = iAct.key.node.neuron;
                        rid = iAct.key.rid;
                    }
                } else {
                    inb = ina;
                    rid = sa.input.key.rid;
                }

                if (inb != null) {
                    Synapse.Key osk = sa.synapse.key;
                    Synapse.Key nsk = new Synapse.Key(
                            osk.isRecurrent,
                            osk.relativeRid != null ?
                                    osk.relativeRid :
                                    (ss.metaRelativeRid && ridOffset != null && rid != null ? rid - ridOffset : null),
                            osk.absoluteRid,
                            osk.rangeMatch,
                            osk.rangeOutput
                    );

                    Synapse ns = new Synapse(inb, targetNeuron, nsk);
                    if (!ns.exists()) {
                        ns.weightDelta = ss.metaWeight;
                        ns.biasDelta = ss.metaBias;

                        inputSynapses.add(ns);
                    }
                }
            }
        }

        INeuron.update(doc.model, doc.threadId, targetNeuron, newNeuron ? metaAct.key.node.neuron.get().metaBias : 0.0, inputSynapses);

        if (newNeuron) {
            Activation.SynapseActivation inhibMetaLink = metaAct.getFinalOutputActivations().get(0);
            Synapse.Key inhibSynKey = inhibMetaLink.synapse.key;
            MetaSynapse inhibSS = inhibMetaLink.synapse.meta;
            doc.model.addSynapse(supprN,
                    new Input()
                            .setNeuron(targetNeuron)
                            .setWeight(inhibSS.metaWeight)
                            .setBias(inhibSS.metaBias)
                            .setRelativeRid(inhibSynKey.relativeRid)
                            .setAbsoluteRid(inhibSynKey.absoluteRid)
                            .setRangeMatch(inhibSynKey.rangeMatch)
                            .setRangeOutput(inhibSynKey.rangeOutput)
            );

            Activation.Key mak = metaAct.key;
            mak.interpretation.setState(EXCLUDED, v);
        }

        for(Synapse s: inputSynapses) {
            for (Activation iAct : s.input.get().getFinalActivations(doc)) {
                iAct.upperBound = 0.0;
                repropagate(doc, iAct);
            }
        }

        doc.propagate();

        for(Activation tAct: targetNeuron.get().getAllActivations(doc)) {
            if(!isConflicting(tAct)) {
                tAct.key.interpretation.setState(SELECTED, v);

                Activation sAct = getOutputAct(tAct.neuronOutputs, INeuron.Type.INHIBITORY);
                sAct.key.interpretation.setState(SELECTED, v);

                Activation mAct = getOutputAct(sAct.neuronOutputs, INeuron.Type.META);

                ArrayList<Activation> newActs = new ArrayList<>();
                if (mAct != null) {
                    mAct.visited = v;
                    newActs.add(mAct);
                }
                newActs.add(tAct);
                newActs.add(sAct);

                newActs.forEach(act -> doc.vQueue.add(0, act));
                doc.vQueue.processChanges(doc.selectedSearchNode, doc.visitedCounter++);

                if (tAct.getFinalState().value <= 0.0) {
                    tAct.key.interpretation.setState(EXCLUDED, v);
                    doc.selectedSearchNode.mark(Collections.singleton(mAct.key.interpretation), SELECTED);

                    newActs.forEach(act -> doc.vQueue.add(0, act));
                    doc.vQueue.processChanges(doc.selectedSearchNode, doc.visitedCounter++);
                }

                for (Activation act : newActs) {
                    if (act.isFinalActivation()) {
                        doc.finallyActivatedNeurons.add(act.key.node.neuron.get(doc));
                    }
                }
            }
        }
    }


    private static boolean isConflicting(Activation tAct) {
        ArrayList<InterprNode> tmp = new ArrayList<>();
        Conflicts.collectDirectConflicting(tmp, tAct.key.interpretation);
        for(InterprNode c: tmp) {
            if(c.state == SELECTED) return true;
        }
        return false;
    }


    private static Activation getOutputAct(TreeSet<Activation.SynapseActivation> outputActs, INeuron.Type type) {
        for(Activation.SynapseActivation sa: outputActs) {
            if(sa.output.key.node.neuron.get().type == type) {
                return sa.output;
            }
        }
        return null;
    }


    private static void repropagate(Document doc, NodeActivation<?> act) {
        act.key.node.propagateAddedActivation(doc, act);
        for(NodeActivation<?> oAct: act.outputs.values()) {
            if(!(oAct instanceof Activation)) {
                repropagate(doc, oAct);
            }
        }
    }


    private static Integer computeRidOffset(Activation mAct) {
        for (Activation.SynapseActivation sa : mAct.getFinalInputActivations()) {
            if(sa.synapse.key.relativeRid != null && sa.input.key.rid != null) {
                return sa.input.key.rid - sa.synapse.key.relativeRid;
            }
        }
        return null;
    }


    public static Neuron initMetaNeuron(Model m, Neuron n, double bias, double metaBias, Input... inputs) {
        n.get().metaBias = metaBias;
        return m.initNeuron(n, bias, INeuron.Type.META, inputs);
    }
}
