package ise.ct.messages;

import presage.Conversation;

public class ARBPunishmentGiven extends ARBPunishment {

	public ARBPunishmentGiven(String myId, Conversation conv, String punishmentGiven, Integer chipColour, Integer numberOfChips ) {
		super(myId,conv,punishmentGiven, chipColour, numberOfChips);
		this.performative = ARBPunishmentGiven.class.getCanonicalName();
	}

	public ARBPunishmentGiven(String myId, Conversation conv, String punishmentGiven) {
		super(myId,conv,punishmentGiven);
		this.performative = ARBPunishmentGiven.class.getCanonicalName();
	}
	
}
