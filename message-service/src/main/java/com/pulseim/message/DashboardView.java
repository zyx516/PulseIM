package com.pulseim.message;
import java.util.*;
public record DashboardView(long relatedMessages,Map<String,Long> stages,List<DeliveryEventView> recentFailures){}