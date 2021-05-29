package network.aika.neuron.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.link.LinkStep;

import static network.aika.neuron.activation.direction.Direction.INPUT;

public class CheckIfFired implements ActivationStep {

    @Override
    public void process(Activation act) {
        act.updateValue();

        if(!act.checkIfFired())
            return;

        act.propagate();
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }

    public boolean checkIfQueued() {
        return true;
    }

    public String toString() {
        return "Act-Step: CheckIfFired";
    }
}