package bgu.spl.mics.application.services;

import bgu.spl.mics.Event;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.Model;
import bgu.spl.mics.application.objects.Student;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Student is responsible for sending the {@link TrainModelEvent},
 * {@link TestModelEvent} and {@link PublishResultsEvent}.
 * In addition, it must sign up for the conference publication broadcasts.
 * This class may not hold references for objects which it is not responsible for.
 *
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class StudentService extends MicroService {
    Student student;
    Model currentModel;
    public StudentService(String name, Student student) {
        super(name);
        this.student = student;
        this.currentModel = student.getNextModel();
    }

    private Event<Boolean> buildTrain(){
        Event event = new TrainModelEvent(currentModel);
        return event;
    }
    private void sendTrain(){
        currentModel.setStatus(Model.Status.Training);
        student.setFuture(getBus().sendEvent(buildTrain()));
    }

    private Event<Model.Results> buildTest(){
        Event event = new TestModelEvent(currentModel);
        return event;
    }

    private void sendTest(){
        student.setFuture(getBus().sendEvent(buildTest()));
    }

    private void sendResult(){
        student.addPublishedModel(currentModel);
        getBus().sendEvent(buildResult());
    }
    private Event<Boolean> buildResult(){
        Event publishResult = new PublishResultsEvent(currentModel.getName());
        return publishResult;
    }
    private void subscribe(){
        subscribeBroadcast(PublishConferenceBroadcast.class, (message)->{
            ConcurrentLinkedQueue<String> publishedModels = message.getModelsToPublish();
            for(String modelName : publishedModels){
                if(student.getPublishedModelNames().contains(modelName)){
                    student.incPublications();
                }else{
                    student.incPapersRead();
                }
            }
        });

        subscribeBroadcast(TickBroadcast.class, t->{
//            if(student.isFinished()){;}
            if(currentModel.getStatus() == Model.Status.PreTrained){
                sendTrain();
            }
            else if(currentModel.getStatus() == Model.Status.Training){
                Boolean trainResult = (Boolean)student.getFuture().get(1, TimeUnit.MILLISECONDS); // need to be timed get()
                if(trainResult!=null){
                    sendTest();
                }
            }else if(currentModel.getStatus() == Model.Status.Trained){
                Model.Results testResult = (Model.Results)student.getFuture().get();
                if(testResult == Model.Results.Good){
                    currentModel = student.getNextModel();
                    sendResult();
                }else if(testResult == Model.Results.Bad){
                    sendTrain();
                }
            }
        });
    }

    @Override
    protected void initialize() {
        subscribe();
//        sendTrain();
    }
}
