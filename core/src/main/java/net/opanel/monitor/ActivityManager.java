package net.opanel.monitor;

import net.opanel.OPanel;
import net.opanel.common.OPanelPlayer;
import net.opanel.event.EventManager;
import net.opanel.event.EventType;
import net.opanel.event.OPanelPlayerJoinEvent;
import net.opanel.storage.Storage;
import net.opanel.storage.StorageKey;
import net.opanel.utils.Utils;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

public class ActivityManager {
    public static final int MAX_ACTIVITY_SIZE = 30;

    private final OPanel plugin;
    private final List<ActivityData> activities;
    private final Consumer<OPanelPlayerJoinEvent> joinListener;

    public ActivityManager(OPanel plugin) {
        this.plugin = plugin;

        List<ActivityData> storedActivities = Storage.get().getStoredData(StorageKey.ACTIVITY);
        activities = storedActivities == null ? new ArrayList<>() : new ArrayList<>(storedActivities);
        activities.removeIf(Objects::isNull);
        sortActivities();
        if(trimActivities()) {
            saveActivities();
        }

        joinListener = event -> {
            try {
                recordPlayerActivity(event.getPlayer());
            } catch (Exception e) {
                plugin.logger.warn("Failed to record player activity: " + e.getMessage());
            }
        };

        EventManager.get().on(EventType.PLAYER_JOIN, joinListener);
    }

    public synchronized List<ActivityData> getActivities() {
        List<ActivityData> paddedActivities = new ArrayList<>(MAX_ACTIVITY_SIZE);
        for(int i = activities.size(); i < MAX_ACTIVITY_SIZE; i++) {
            paddedActivities.add(new ActivityData());
        }
        paddedActivities.addAll(activities);
        return paddedActivities;
    }

    public synchronized void recordPlayerActivity(OPanelPlayer player) {
        LocalDate today = LocalDate.now();
        ActivityData activityData = findOrCreateActivity(today);
        activityData.addPlayer(player);
        sortActivities();
        trimActivities();
        saveActivities();
    }

    /**
     * Finds the activity record for the target date. If duplicated records for the same day
     * exist in storage, their player sets are merged into the first matched entry and the
     * redundant entries are removed. If no record exists, a new one is created.
     */
    private ActivityData findOrCreateActivity(LocalDate date) {
        ActivityData matchedData = null;

        for(ActivityData activity : activities) {
            if(activity == null) {
                continue;
            }

            if(activity.players == null) {
                activity.players = new LinkedHashSet<>();
            }

            if(activity.isOnDate(date)) {
                if(matchedData == null) {
                    matchedData = activity;
                } else {
                    matchedData.players.addAll(activity.players);
                }
            }
        }

        if(matchedData != null) {
            final ActivityData mergedData = matchedData;
            activities.removeIf(activity -> activity != mergedData && activity != null && activity.isOnDate(date));
            return matchedData;
        }

        ActivityData newActivity = new ActivityData(Utils.localDateToDate(date));
        activities.add(newActivity);
        return newActivity;
    }

    private void sortActivities() {
        activities.sort(Comparator.comparing(activity -> (
            activity == null || activity.date == null
            ? new Date(0)
            : activity.date
        )));
    }

    private boolean trimActivities() {
        if(activities.size() <= MAX_ACTIVITY_SIZE) {
            return false;
        }

        activities.subList(0, activities.size() - MAX_ACTIVITY_SIZE).clear();
        return true;
    }

    private void saveActivities() {
        Storage.get().setStoredData(StorageKey.ACTIVITY, new ArrayList<>(activities));
    }

    public void shutdown() {
        EventManager.get().off(EventType.PLAYER_JOIN, joinListener);
    }
}
