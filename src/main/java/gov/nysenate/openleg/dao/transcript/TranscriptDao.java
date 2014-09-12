package gov.nysenate.openleg.dao.transcript;

import gov.nysenate.openleg.model.transcript.Transcript;
import gov.nysenate.openleg.model.transcript.TranscriptFile;
import gov.nysenate.openleg.model.transcript.TranscriptId;
import org.springframework.dao.DataAccessException;

import java.util.List;

public interface TranscriptDao
{

    /**
     * Retrieves all TranscriptId's for a year.
     * @param year
     * @return
     */
    public List<TranscriptId> getTranscriptIds(int year);

    /**
     * Retrieves a Transcript via its TranscriptId.
     * @param transcriptId The transcriptId of the Transcript to return
     * @return The Transcript belonging to the transcriptId
     * @see gov.nysenate.openleg.model.transcript.TranscriptId
     * @see Transcript
     */
    public Transcript getTranscript(TranscriptId transcriptId);

    /**
     * Updates the backing store with the given instance or inserts it
     * if the record doesn't already exist.
     * @param transcript The {@link Transcript} to update.
     * @param transcriptFile The {@link TranscriptFile} which updated the Transcript.
     */
    public void updateTranscript(Transcript transcript, TranscriptFile transcriptFile);
}