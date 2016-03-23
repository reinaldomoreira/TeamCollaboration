package com.uww.messaging.contract;

import com.uww.messaging.display.TeamInvitationResponse;
import com.uww.messaging.model.Team;

import java.util.List;

/**
 * Created by horvste on 2/19/16.
 */
public interface TeamService {
    void save(int creatorUserId, String teamName, String teamDescription);
    List<Team> findTeamsByUserId(int userId);
	List<TeamInvitationResponse> findAllInvitations();
	void addTeamMember(int teamId, int currentUserId, int invitedUserId);
	void inviteMemberToTeam(int teamId, int fromUserId, int invitedUserId, String message);
	void acceptTeamInvitation(int teamInvitationId);
	void rejectTeamInvitation(int teamInvitationId);

}
