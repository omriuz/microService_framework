package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.Model;

public class TestModelEvent implements Event<Model.Results> {
    private final Model model;
    private Model.Results results;

    public TestModelEvent(Model model) {
        this.model = model;
        results = Model.Results.None;
    }

    public Model getModel() {
        return model;
    }

    public Model.Results getResults() {
        return results;
    }
    public void setResults(Model.Results newResult){
        this.results = newResult;
    }

}
