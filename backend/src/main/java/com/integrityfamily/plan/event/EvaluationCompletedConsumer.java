package com.integrityfamily.plan.event;


/**
 * @deprecated OBSOLETE - This class has been replaced by the native asynchronous 
 * RabbitMQ message-driven consumers:
 * 1. PlanConsumer (for action plans)
 * 2. AiInsightConsumer (for AI clinical insights)
 * 3. AnalyticsEventConsumer (for dashboard read-model sync)
 */
@Deprecated
// @Component
public class EvaluationCompletedConsumer {
    // This consumer has been deprecated and its component annotation removed 
    // to prevent Spring bean registration and save memory resources.
}


