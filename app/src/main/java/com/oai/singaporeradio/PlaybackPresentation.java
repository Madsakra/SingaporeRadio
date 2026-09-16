package com.oai.singaporeradio;

/** Adapts the unchanged service's status messages into localized screen states. */
final class PlaybackPresentation {
    final Station station;
    final int label;
    final boolean active;
    final boolean live;
    final boolean error;

    private PlaybackPresentation(Station station, int label, boolean active, boolean live, boolean error) {
        this.station = station;
        this.label = label;
        this.active = active;
        this.live = live;
        this.error = error;
    }

    static PlaybackPresentation from(String status) {
        String message = status == null ? "Stopped" : status;
        // Compare complete names: POWER 98 must not capture its longer digital-channel names.
        for (Station station : StationData.ALL) {
            if (message.equals("Playing " + station.name))
                return new PlaybackPresentation(station, R.string.live, true, true, false);
            if (message.equals("Connecting to " + station.name + "…"))
                return new PlaybackPresentation(station, R.string.connecting, true, false, false);
            if (message.equals("Buffering " + station.name + "…"))
                return new PlaybackPresentation(station, R.string.buffering, true, false, false);
            if (message.equals("Paused " + station.name + ". Tap PLAY to resume."))
                return new PlaybackPresentation(station, R.string.paused, false, false, false);
            if (message.startsWith("Unable to play " + station.name + "."))
                return new PlaybackPresentation(station, R.string.stream_error, false, false, true);
        }
        if (message.equals("Stopped")) return new PlaybackPresentation(null, R.string.stopped, false, false, false);
        int label = message.startsWith("No internet") ? R.string.no_internet
            : message.startsWith("Connection timed out") ? R.string.timeout
            : message.startsWith("The station stopped") ? R.string.stream_ended : R.string.stream_error;
        return new PlaybackPresentation(null, label, false, false, true);
    }
}
