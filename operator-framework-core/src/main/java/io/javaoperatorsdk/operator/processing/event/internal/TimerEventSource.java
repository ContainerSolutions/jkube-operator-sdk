package io.javaoperatorsdk.operator.processing.event.internal;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.processing.KubernetesResourceUtils;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.javaoperatorsdk.operator.processing.event.Event;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerEventSource extends AbstractEventSource {

  private Logger log = LoggerFactory.getLogger(TimerEventSource.class);

  private final Timer timer = new Timer();

  private final Map<String, EventProducerTimeTask> onceTasks = new ConcurrentHashMap<>();
  private final Map<String, EventProducerTimeTask> timerTasks = new ConcurrentHashMap<>();

  public void schedule(CustomResource customResource, long delay, long period) {
    String resourceUid = KubernetesResourceUtils.getUID(customResource);
    if (timerTasks.containsKey(resourceUid)) {
      return;
    }
    EventProducerTimeTask task = new EventProducerTimeTask(resourceUid, true, null);
    timerTasks.put(resourceUid, task);
    timer.schedule(task, delay, period);
  }

  public void scheduleOnce(CustomResource customResource, long delay) {
    scheduleOnce(customResource, delay, null);
  }

  public void scheduleOnce(CustomResource customResource, long delay, List<Event> events) {
    String resourceUid = KubernetesResourceUtils.getUID(customResource);
    if (onceTasks.containsKey(resourceUid)) {
      cancelOnceSchedule(resourceUid);
    }
    EventProducerTimeTask task = new EventProducerTimeTask(resourceUid, false, events);
    onceTasks.put(resourceUid, task);
    timer.schedule(task, delay);
  }

  @Override
  public void eventSourceDeRegisteredForResource(String customResourceUid) {
    cancelSchedule(customResourceUid);
    cancelOnceSchedule(customResourceUid);
  }

  public void cancelSchedule(String customResourceUid) {
    TimerTask timerTask = timerTasks.remove(customResourceUid);
    if (timerTask != null) {
      timerTask.cancel();
    }
  }

  public void cancelOnceSchedule(String customResourceUid) {
    TimerTask timerTask = onceTasks.remove(customResourceUid);
    if (timerTask != null) {
      timerTask.cancel();
    }
  }

  public class EventProducerTimeTask extends TimerTask {

    protected final String customResourceUid;
    private final boolean repeated;
    private final List<Event> events;

    private EventProducerTimeTask(String customResourceUid, boolean repeated, List<Event> events) {
      this.customResourceUid = customResourceUid;
      this.repeated = repeated;
      this.events = events;
    }

    @Override
    public void run() {
      log.debug("Producing event for custom resource id: {}", customResourceUid);
      if (repeated) {
        eventHandler.handleEvent(new RepeatedTimerEvent(customResourceUid, TimerEventSource.this));
      } else {
        eventHandler.handleEvent(
            new OnceTimerEvent(customResourceUid, TimerEventSource.this, events));
      }
    }
  }
}
