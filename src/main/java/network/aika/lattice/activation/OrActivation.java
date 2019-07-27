package network.aika.lattice.activation;

import network.aika.Document;
import network.aika.lattice.NodeActivation;
import network.aika.lattice.OrNode;
import network.aika.lattice.refinement.OrEntry;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.link.Linker;

import java.util.*;


public class OrActivation extends NodeActivation<OrNode> {
    private Map<Integer, Link> orInputs = new TreeMap<>();
    private Activation outputAct;

    public OrActivation(Document doc, OrNode node) {
        super(doc, node);
    }

    public Activation getOutputAct() {
        return outputAct;
    }

    public void setOutputAct(Activation outputAct) {
        this.outputAct = outputAct;
    }

    @Override
    public Activation getInputActivation(int i) {
        throw new UnsupportedOperationException();
    }

    public void link(Link l) {
        l.setOutput(this);
        orInputs.put(l.input.id, l);
        l.input.outputsToOrNode.put(id, l);
    }


    public static class Link {
        public OrEntry oe;

        private NodeActivation<?> input;
        private OrActivation output;

        public Link(OrEntry oe, NodeActivation<?> input) {
            this.oe = oe;
            this.input = input;
        }


        public int size() {
            return oe.synapseIds.length;
        }

        public int get(int i) {
            return oe.synapseIds[i];
        }


        public void linkOutputActivation(Activation act) {
            Linker l = act.getDocument().getLinker();
            for (int i = 0; i < size(); i++) {
                int synId = get(i);
                Synapse s = act.getSynapseById(synId);
                if(s != null) {
                    Activation iAct = input.getInputActivation(i);
                    l.link(s, iAct, act);
                }
            }
            l.process();
        }


        public Collection<network.aika.neuron.activation.link.Link> getInputLinks(Neuron n) {
            List<network.aika.neuron.activation.link.Link> inputActs = new ArrayList<>();
            for (int i = 0; i < size(); i++) {
                int synId = get(i);
                Synapse s = n.getSynapseById(synId);
                Activation iAct = input.getInputActivation(i);
                inputActs.add(new network.aika.neuron.activation.link.Link(s, iAct, null));
            }
            return inputActs;
        }

        public void setOutput(OrActivation output) {
            this.output = output;
        }
    }

}


