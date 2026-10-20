package com.example.taskmanager.model;

public enum AgentLifecycleStage {
    DRAFT,
    DEVELOPMENT,
    TESTING,
    DEPLOYED,
    RETIRED;

    public AgentLifecycleStage next() {
        return switch (this) {
            case DRAFT -> DEVELOPMENT;
            case DEVELOPMENT -> TESTING;
            case TESTING -> DEPLOYED;
            case DEPLOYED -> RETIRED;
            case RETIRED -> null;
        };
    }
}
