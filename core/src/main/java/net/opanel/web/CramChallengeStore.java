package net.opanel.web;

import net.opanel.utils.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class CramChallengeStore {
    private static final int MAX_CHALLENGES_PER_IP = 16;
    private static final int MAX_CHALLENGES = 4096;
    private static final long CHALLENGE_TTL_MILLIS = 5 * 60 * 1000L;

    private final Map<ChallengeKey, ChallengeRecord> challenges = new HashMap<>();

    public synchronized CreateResult create(String ip, String id) {
        long now = System.currentTimeMillis();
        cleanupExpired(now);

        ChallengeKey key = new ChallengeKey(ip, id);
        if(challenges.containsKey(key)) {
            return CreateResult.duplicate();
        }

        long ipChallengeCount = challenges.keySet().stream()
                .filter(challengeKey -> challengeKey.ip().equals(ip))
                .count();
        if(ipChallengeCount >= MAX_CHALLENGES_PER_IP) {
            return CreateResult.capacityFull(retryAfterSeconds(ip, now));
        }
        if(challenges.size() >= MAX_CHALLENGES) {
            return CreateResult.capacityFull(retryAfterSeconds(null, now));
        }

        String challenge = Utils.generateRandomHex(16);
        while(containsChallenge(challenge)) {
            challenge = Utils.generateRandomHex(16);
        }
        challenges.put(key, new ChallengeRecord(challenge, now + CHALLENGE_TTL_MILLIS));
        return CreateResult.created(challenge);
    }

    public synchronized String consume(String ip, String id) {
        long now = System.currentTimeMillis();
        cleanupExpired(now);

        ChallengeRecord record = challenges.remove(new ChallengeKey(ip, id));
        return record == null ? null : record.challenge();
    }

    private void cleanupExpired(long now) {
        challenges.values().removeIf(record -> record.expiresAt() <= now);
    }

    private boolean containsChallenge(String challenge) {
        return challenges.values().stream().anyMatch(record -> record.challenge().equals(challenge));
    }

    private long retryAfterSeconds(String ip, long now) {
        long earliestExpiration = Long.MAX_VALUE;
        Iterator<Map.Entry<ChallengeKey, ChallengeRecord>> iterator = challenges.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<ChallengeKey, ChallengeRecord> entry = iterator.next();
            if(ip == null || entry.getKey().ip().equals(ip)) {
                earliestExpiration = Math.min(earliestExpiration, entry.getValue().expiresAt());
            }
        }
        if(earliestExpiration == Long.MAX_VALUE) return 1;
        return Math.max(1, (earliestExpiration - now + 999) / 1000);
    }

    private record ChallengeKey(String ip, String id) {}

    private record ChallengeRecord(String challenge, long expiresAt) {}

    public enum CreateStatus {
        CREATED,
        DUPLICATE,
        CAPACITY_FULL
    }

    public record CreateResult(CreateStatus status, String challenge, long retryAfterSeconds) {
        private static CreateResult created(String challenge) {
            return new CreateResult(CreateStatus.CREATED, challenge, 0);
        }

        private static CreateResult duplicate() {
            return new CreateResult(CreateStatus.DUPLICATE, null, 0);
        }

        private static CreateResult capacityFull(long retryAfterSeconds) {
            return new CreateResult(CreateStatus.CAPACITY_FULL, null, retryAfterSeconds);
        }
    }
}
