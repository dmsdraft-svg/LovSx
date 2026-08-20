package com.example.marriagegender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RequestManager {

    public static final class PendingRequest {

        private final UUID sender;
        private final UUID target;
        private final long expiresAt;

        public PendingRequest(UUID sender, UUID target, long expiresAt) {
            this.sender = sender;
            this.target = target;
            this.expiresAt = expiresAt;
        }

        public UUID getSender() {
            return sender;
        }

        public UUID getTarget() {
            return target;
        }

        public boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private static final long TIMEOUT_MS = 30_000L;

    private final Map<UUID, PendingRequest> sexRequests = new HashMap<>();
    private final Map<UUID, PendingRequest> marryRequests = new HashMap<>();

    public void sendSexRequest(UUID sender, UUID target) {
        sexRequests.put(target, new PendingRequest(sender, target, System.currentTimeMillis() + TIMEOUT_MS));
    }

    public void sendMarryRequest(UUID sender, UUID target) {
        marryRequests.put(target, new PendingRequest(sender, target, System.currentTimeMillis() + TIMEOUT_MS));
    }

    public PendingRequest getSexRequest(UUID target) {
        PendingRequest request = sexRequests.get(target);

        if (request == null) {
            return null;
        }

        if (request.expired()) {
            sexRequests.remove(target);
            return null;
        }

        return request;
    }

    public PendingRequest getMarryRequest(UUID target) {
        PendingRequest request = marryRequests.get(target);

        if (request == null) {
            return null;
        }

        if (request.expired()) {
            marryRequests.remove(target);
            return null;
        }

        return request;
    }

    public void removeSexRequest(UUID target) {
        sexRequests.remove(target);
    }

    public void removeMarryRequest(UUID target) {
        marryRequests.remove(target);
    }

    public void cleanup() {
        sexRequests.values().removeIf(PendingRequest::expired);
        marryRequests.values().removeIf(PendingRequest::expired);
    }

    public void clearRequestsFor(UUID uuid) {
        sexRequests.remove(uuid);
        marryRequests.remove(uuid);

        sexRequests.values().removeIf(request -> request.getSender().equals(uuid));
        marryRequests.values().removeIf(request -> request.getSender().equals(uuid));
    }
}
