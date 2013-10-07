package databaseService;

import utils.UserSession;
import base.DatabaseService;
import base.Address;
 // $codepro.audit.disable unnecessaryImport

public class MsgRegisterUser extends MessageToDatabaseService
{
	final private UserSession userSession;
	
	public MsgRegisterUser(Address from, Address to, UserSession userSession) 
	{
		super(from, to);
		this.userSession = userSession;
	}
	
	@Override
	public void exec(DatabaseService databaseService, Address from) 
	{
		databaseService.registerUser(userSession);
	}
}
