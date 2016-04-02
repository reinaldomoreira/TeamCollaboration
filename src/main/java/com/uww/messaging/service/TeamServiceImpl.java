package com.uww.messaging.service;

import com.google.common.base.Preconditions;
import com.uww.messaging.contract.TeamService;
import com.uww.messaging.display.TeamInvitationResponse;
import com.uww.messaging.model.Team;
import com.uww.messaging.model.TeamInvitation;
import com.uww.messaging.model.TeamMember;
import com.uww.messaging.model.TeamMessageChat;
import com.uww.messaging.model.User;
import com.uww.messaging.repository.TeamInvitationRepository;
import com.uww.messaging.repository.TeamMemberRepository;
import com.uww.messaging.repository.TeamMessageChatRepository;
import com.uww.messaging.repository.TeamRepository;
import com.uww.messaging.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by horvste on 2/19/16.
 */
@Service
public class TeamServiceImpl implements TeamService {
	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	@Autowired
	private TeamMessageChatRepository teamMessageChatRepository;

	@Autowired
	private TeamInvitationRepository teamInvitationRepository;

	@Autowired
	private UserRepository userRepository;

	@Transactional
	@Override
	public void save(int creatorUserId, String teamName, String teamDescription) {
		Preconditions.checkNotNull(teamName);
		Preconditions.checkNotNull(teamDescription);
		Team team = new Team();
		team.setTeamName(teamName);
		team.setTeamDescription(teamDescription);
		Date currentDate = new Date();
		team.setCreatedTime(currentDate);
		teamRepository.save(team);

		TeamMessageChat teamMessageChat = new TeamMessageChat(team.getTeamId(), null);
		teamMessageChatRepository.save(teamMessageChat);

		TeamMember teamMember = new TeamMember();
		teamMember.setTeamId(team.getTeamId());
		teamMember.setUserId(creatorUserId);
		teamMemberRepository.save(teamMember);
	}

	@Override
	public List<Team> findTeamsByUserId(int userId) {
		List<TeamMember> teamMemberByUserId = teamMemberRepository.findTeamMemberByUserId(userId);
		List<Integer> teamIds = new ArrayList<>();
		teamMemberByUserId.forEach(teamMem -> teamIds.add(teamMem.getTeamId())
		);
		return teamRepository.findByTeamIdIn(teamIds);
	}

	@Override
	public List<TeamInvitationResponse> findAllInvitationsToUser(final int toUserId) {

		Iterable<TeamInvitation> teamInvitations = teamInvitationRepository.findByToUserId(toUserId);

		List<TeamInvitationResponse> responses = new ArrayList<>();

		teamInvitations.forEach(teamInvitation -> {
			TeamInvitationResponse response = new TeamInvitationResponse();

			User invited = userRepository.findOne(teamInvitation.getFromUserId());
			Team toTeam = teamRepository.findOne(teamInvitation.getToTeamId());

			response.setTeamInvitationId(teamInvitation.getTeamInvitationId());

			response.setToUserName(invited.getUsername());

			response.setTeamName(toTeam.getTeamName());

			response.setInvitationTime(teamInvitation.getInvitationTime());

			response.setMessage(teamInvitation.getMessage());

			response.setStatus(teamInvitation.getStatus());

			responses.add(response);
		});

		return responses;
	}


	@Transactional
	@Override
	public void addTeamMember(final int teamId, final int currentUserId, final int invitedUserId) {
		List<Team> teams = teamRepository.findByTeamIdIn(new ArrayList<Integer>() {{add(teamId);}});

		// makes sure there is one and only one team
		if (teams.size() > 1) { throw new RuntimeException("Size greater than one"); } else if (teams.size() == 0) {
			throw new RuntimeException("No team with this id: " + teamId);
		}

		Team team = teams.get(0);
		System.out.println("team : " + team.getTeamName() + " desc: " + team.getTeamDescription());

		// makes sure the currentUser is in the team so it can invite
		if (!isMemberIsInTeam(teamId, currentUserId)) {
			throw new RuntimeException("Member is not in this team " + teamId);
		}

		TeamMember teamMember = new TeamMember(invitedUserId, teamId);
		teamMemberRepository.save(teamMember);
	}

	@Transactional
	@Override
	public void inviteMemberToTeam(final int teamId, final int fromUserId, final int invitedUserId, String message) {

		List<TeamInvitation> toUserId = teamInvitationRepository.findByToUserId(invitedUserId);

		if (toUserId.size() > 0) {
			throw new IllegalArgumentException("User already has an invitation. Id: " + invitedUserId);
		}


		TeamInvitation teamInvitation = new TeamInvitation();
		teamInvitation.setFromUserId(fromUserId);
		teamInvitation.setToUserId(invitedUserId);
		teamInvitation.setToTeamId(teamId);
		teamInvitation.setMessage(message);
		teamInvitation.setInvitationTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
		teamInvitation.setStatus(TeamInvitation.STATUS_PENDING);

		teamInvitationRepository.save(teamInvitation);

	}

	@Transactional
	@Override
	public void acceptTeamInvitation(final int teamInvitationId) {
		TeamInvitation teamInvitation = teamInvitationRepository.findOne(teamInvitationId);

		teamInvitation.setStatus(TeamInvitation.STATUS_ACCEPTED);

		teamInvitationRepository.save(teamInvitation);

		addTeamMember(teamInvitation.getToTeamId(),teamInvitation.getFromUserId(),teamInvitation.getToUserId());
	}

	@Transactional
	@Override
	public void rejectTeamInvitation(final int teamInvitationId) {
		TeamInvitation teamInvitation = teamInvitationRepository.findOne(teamInvitationId);

		teamInvitation.setStatus(TeamInvitation.STATUS_REJECTED);

		teamInvitationRepository.save(teamInvitation);
	}

	public boolean isMemberIsInTeam(int teamId, int userId) {

		List<TeamMember> teamMemberByUserId = teamMemberRepository.findTeamMemberByUserId(userId);

		for (TeamMember member : teamMemberByUserId) {
			if (member.getTeamId() == teamId) {
				return true;
			}
		}

		return false;
	}
}
