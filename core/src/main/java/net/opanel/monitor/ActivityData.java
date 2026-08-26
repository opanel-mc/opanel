package net.opanel.monitor;

import net.opanel.common.OPanelPlayer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class ActivityData {
    public record PlayerEntry(String name, String uuid) {
        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(!(obj instanceof PlayerEntry player)) return false;
            return Objects.equals(uuid, player.uuid());
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }
    }

    public Date date;
    public Set<PlayerEntry> players;

    public ActivityData() {
        players = new LinkedHashSet<>();
    }

    public ActivityData(Date date) {
        this();
        this.date = date;
    }

    public boolean isOnDate(LocalDate targetDate) {
        if(date == null) return false;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().equals(targetDate);
    }

    public void addPlayer(OPanelPlayer player) {
        if(player == null) return;
        if(players == null) players = new LinkedHashSet<>();

        players.remove(new PlayerEntry(null, player.getUUID()));
        players.add(new PlayerEntry(player.getName(), player.getUUID()));
    }
}
