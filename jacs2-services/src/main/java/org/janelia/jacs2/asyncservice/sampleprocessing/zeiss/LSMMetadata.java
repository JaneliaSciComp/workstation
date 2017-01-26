package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.jacs2.cdi.ObjectMapperFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the JSON-formatted Zeiss LSM metadata that is output by lsm_json_dump.pl.
 */
public class LSMMetadata {
    private String stack;
    private LSMRecording recording;
    private List<LSMChannel> channels;
    private List<LSMTrack> tracks;
    private List<LSMLaser> lasers;
    private List<LSMMarker> markers;
    private List<LSMTimer> timers;

    public List<LSMChannel> getChannels() {
        return channels;
    }

    public List<LSMLaser> getLasers() {
        return lasers;
    }

    public String getStack() {
        return stack;
    }

    public LSMRecording getRecording() {
        return recording;
    }

    public List<LSMTrack> getTracks() {
        return tracks;
    }

    public List<LSMTrack> getNonBleachTracks() {
        List<LSMTrack> nonBleach = new ArrayList<LSMTrack>();
        for (LSMTrack track : tracks) {
            if ("0" .equals(track.getIsBleachTrack())) {
                nonBleach.add(track);
            }
        }
        return nonBleach;
    }

    public List<LSMMarker> getMarkers() {
        return markers;
    }

    public List<LSMTimer> getTimers() {
        return timers;
    }

    public LSMTrack getTrack(LSMChannel channel) {
        String parts[] = channel.getName().split("-");
        if (parts.length < 2) {
            List<LSMTrack> nonBleach = getNonBleachTracks();
            if (nonBleach.size() > 1) {
                throw new IllegalStateException("Channel name (" + channel.getName() + ") does not contain track name, and there is more than one non-bleach track (" + tracks.size() + ")");
            }
            return nonBleach.get(0);
        }
        String trackId = parts[1];
        for (LSMTrack track : tracks) {
            String thisTrackId = track.getName().replaceAll("rack\\s*", "");
            if (trackId.equals(thisTrackId)) {
                return track;
            }
        }
        // Couldn't find the track based on the track names.. let's try multiplex order instead
        for (LSMTrack track : tracks) {
            String thisTrackId = "T" + track.getMultiplexOrder();
            if (trackId.equals(thisTrackId)) {
                return track;
            }
        }
        return null;
    }

    public LSMDetectionChannel getDetectionChannel(LSMChannel channel) {
        LSMTrack track = getTrack(channel);
        if (track == null) return null;
        String parts[] = channel.getName().split("-");
        String chan = parts[0];
        if (track.getDetectionChannels() != null) {
            for (LSMDetectionChannel detChannel : track.getDetectionChannels()) {
                if (detChannel.getName().equals(chan)) {
                    return detChannel;
                }
            }
        }
        return null;
    }

    public LSMDataChannel getDataChannel(LSMChannel channel) {
        String parts[] = channel.getName().split("-");
        String chan = parts[0];
        LSMTrack track = getTrack(channel);
        if (track == null) return null;
        if (track.getDataChannels() != null) {
            for (LSMDataChannel dataChannel : track.getDataChannels()) {
                if (dataChannel.getName().equals(chan)) {
                    return dataChannel;
                }
            }
        }
        return null;
    }

    public static LSMMetadata fromFile(File file) throws IOException {
        ObjectMapper objectMapper = ObjectMapperFactory.instance().getDefaultObjectMapper();
        return objectMapper.readValue(Files.readAllBytes(file.toPath()), LSMMetadata.class);
    }

}
