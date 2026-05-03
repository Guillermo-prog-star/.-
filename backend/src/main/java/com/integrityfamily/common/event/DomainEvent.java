package com.integrityfamily.common.event;
import java.time.Instant; import java.util.Map;
public record DomainEvent(String eventType,String aggregateType,Long aggregateId,Instant occurredAt,Map<String,Object> payload) {
    public static DomainEvent of(String t,String at,Long id,Map<String,Object> p) {
        return new DomainEvent(t,at,id,Instant.now(),p);
    }
}


