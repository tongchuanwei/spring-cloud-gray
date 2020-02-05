package cn.springlcoud.gray.event.server;

import cn.springcloud.gray.retriever.GenericRetriever;
import cn.springlcoud.gray.event.GrayEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * @author saleson
 * @date 2020-01-31 14:45
 */
@Slf4j
public abstract class AbstractGrayEventTrigger implements GrayEventTrigger {

    private GrayEventSender grayEventSender;
    private GenericRetriever<EventConverter> genericRetriever;


    public AbstractGrayEventTrigger(GrayEventSender grayEventSender, List<EventConverter> eventConverters) {
        this.grayEventSender = grayEventSender;
        this.genericRetriever = new GenericRetriever<>(eventConverters, EventConverter.class);
    }

    @Override
    public void triggering(Object eventSource, TriggerType triggerType) {
        GrayEvent grayEvent = convertGrayEvent(eventSource, triggerType);
        if (Objects.isNull(grayEvent)) {
            log.warn("转换失败, grayEvent is null, eventSource:{}, triggerType:{}", eventSource, triggerType);
            return;
        }
        if (Objects.isNull(grayEvent.getTriggerType())) {
            grayEvent.setTriggerType(triggerType);
        }
        grayEventSender.send(grayEvent);
    }

    protected abstract void logEventTrigger(Object eventSource, TriggerType triggerType, GrayEvent grayEvent);

    protected GrayEvent convertGrayEvent(Object eventSource, TriggerType triggerType) {
        EventConverter eventConverter = getEventConverter(eventSource.getClass());
        GrayEvent event = eventConverter.convert(eventSource, triggerType);
        if (Objects.isNull(event)) {
            return null;
        }
        logEventTrigger(eventSource, triggerType, event);
        return eventConverter.decorate(event);
    }


    protected EventConverter getEventConverter(Class<?> sourceCls) {
        EventConverter eventConverter = genericRetriever.retrieve(sourceCls);
        if (Objects.isNull(eventConverter)) {
            log.error("没有找到支持 '{}' 的EventConverter", sourceCls);
            throw new NullPointerException("has no EventConverter support");
        }
        return eventConverter;
    }


}