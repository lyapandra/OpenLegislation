package gov.nysenate.openleg.service.spotcheck.agenda;

import com.google.common.collect.Range;
import gov.nysenate.openleg.client.view.oldapi.OldMeetingView;
import gov.nysenate.openleg.dao.agenda.oldapi.OldApiMeetingDao;
import gov.nysenate.openleg.dao.base.OldApiDocumentNotFoundEx;
import gov.nysenate.openleg.model.agenda.*;
import gov.nysenate.openleg.model.base.Version;
import gov.nysenate.openleg.model.bill.BaseBillId;
import gov.nysenate.openleg.model.bill.BillId;
import gov.nysenate.openleg.model.entity.Chamber;
import gov.nysenate.openleg.model.entity.CommitteeId;
import gov.nysenate.openleg.model.spotcheck.ReferenceDataNotFoundEx;
import gov.nysenate.openleg.model.spotcheck.agenda.AgendaAlertInfoCommittee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.List;

@Service
public class OldApiAgendaCheckReportService extends BaseAgendaCheckReportService {

    private int dummyAgendaNo = 0;

    @Autowired
    OldApiMeetingDao meetingDao;

    @Override
    protected List<AgendaAlertInfoCommittee> getReferences(LocalDateTime start, LocalDateTime end) throws ReferenceDataNotFoundEx {
        List<AgendaAlertInfoCommittee> refs =  agendaAlertDao.getProdUncheckedAgendaAlertReferences(Range.closed(start, end));
        if (refs.isEmpty()) {
            throw new ReferenceDataNotFoundEx(
                    String.format("no 1.9.2 agenda references were found within the given range %s to %s", start, end));
        }
        // Set all messages as blank since 1.9.2 doesn't present these
        refs.stream()
                .flatMap(aaic -> aaic.getItems().stream())
                .forEach(item -> item.setMessage(""));
        return refs;
    }

    @Override
    protected Agenda getAgenda(AgendaAlertInfoCommittee aaic) throws AgendaNotFoundEx {
        AgendaId agendaId = new AgendaId(aaic.getMeetingDateTime().toLocalDate().atStartOfDay(ZoneId.of("GMT")).toInstant().toEpochMilli(), -1);
        try {
            OldMeetingView meetingView = meetingDao.getMeeting(aaic.getCommitteeId(), aaic.getMeetingDateTime().toLocalDate());
            AgendaInfoCommittee meetingInfo = getAgendaInfoCommittee(meetingView, agendaId, aaic.getAddendum());
            Agenda agenda = new Agenda(agendaId);
            AgendaInfoAddendum addendum = new AgendaInfoAddendum(agendaId, aaic.getAddendum().toString(), aaic.getWeekOf(), LocalDateTime.now());
            addendum.putCommittee(meetingInfo);
            agenda.putAgendaInfoAddendum(addendum);
            return agenda;
        } catch (OldApiDocumentNotFoundEx ex) {
            throw new AgendaNotFoundEx(new AgendaId(agendaId.getNumber(), 0), ex);
        }
    }

    @Override
    protected void setReferenceChecked(AgendaAlertInfoCommittee reference) {
        agendaAlertDao.setAgendaAlertProdChecked(reference, true);
    }

    @Override
    protected String getNotes() {
        return "1.9.2";
    }

    /** --- Internal Methods --- */

    private AgendaInfoCommittee getAgendaInfoCommittee(OldMeetingView meetingView, AgendaId agendaId, Version addendum) {
        LocalDateTime meetingDateTime = null;
        try {
            meetingDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(meetingView.getMeetingDateTime())), ZoneId.of("GMT"));
        } catch (NumberFormatException ignored) {}
        AgendaInfoCommittee aic = new AgendaInfoCommittee(
                new CommitteeId(Chamber.SENATE, meetingView.getCommitteeName()),
                agendaId, addendum, meetingView.getCommitteeChair(), meetingView.getLocation(), meetingView.getNotes(),
                meetingDateTime
        );
        meetingView.getBills().stream()
                .map(obi -> new AgendaInfoCommitteeItem(obi.getBillId(), ""))
                .forEach(aic::addCommitteeItem);
        return aic;
    }
}
