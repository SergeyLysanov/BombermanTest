package frontend;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import utils.UserSession;
import base.AccountService;
import base.AddressService;
import base.MessageSystem;
import base.Address;
import messageSystem.MessageSystemImpl;
import utils.SessionStatus;

import org.json.simple.JSONObject;
import org.junit.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class FrontendImplTest 
{
	private MessageSystem 	messageSystem;
	private AddressService	addressService;
	
	HttpServletResponse		resp;
	HttpServletRequest 		req;
	
	public FrontendImplTest()
	{
		this.messageSystem = mock(MessageSystemImpl.class);
		this.addressService = mock(AddressService.class);
		
		when(messageSystem.getAddressService()).thenReturn(addressService);
		when(addressService.getAddress(AccountService.class)).thenReturn(new Address());
		
		req = mock(HttpServletRequest.class);
		resp = mock(HttpServletResponse.class);
	}

	@Test
	public void testFrontendImpl()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		assertNotNull(frontend);
	}


	@Test
	public void testParseGetMainUrlWithSession() throws IOException 
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		PageGenerator pageGenerator = new PageGenerator();
		String sessionId = "sessionId";
		UserSession userSession = frontend.CreateUserSession("name", sessionId, "pass", "email");
		
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn(sessionId);
		
		when(req.toString()).thenReturn("");
		when(req.getRequestURI()).thenReturn("/");
		when(req.getSession(false)).thenReturn(session);
		
		String result = frontend.ParseGetRequestUrl(req, resp);
		
		verify(resp).setContentType("text/html; charset=UTF-8");
		assertEquals(pageGenerator.getMainPage(userSession), result);
	}
	
	@Test
	public void testParseGetMainUrlWithNullSession() throws IOException 
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		PageGenerator pageGenerator = new PageGenerator();
		
		when(req.toString()).thenReturn("");
		when(req.getRequestURI()).thenReturn("/");
		when(req.getSession(false)).thenReturn(null);
		
		String result = frontend.ParseGetRequestUrl(req, resp);
		
		verify(resp).setContentType("text/html; charset=UTF-8");
		assertEquals(pageGenerator.getMainPage(null), result);
	}
	
	@Test
	public void testParseGetRequestSessionStatus() throws IOException
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		
		String sessionId = "sessionId";
		UserSession userSession = frontend.CreateUserSession("name", sessionId, "pass", "email");
		userSession.eSessionStatus.set(SessionStatus.eRegistered);
		
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn(sessionId);
		
		when(req.toString()).thenReturn("");
		when(req.getRequestURI()).thenReturn("/register/status.json");
		when(req.getSession(false)).thenReturn(session);
		
		frontend.ParseGetRequestUrl(req, resp);
		
		verify(resp).setContentType("application/json; charset=UTF-8");
	}
	
	@Test
	public void testParseGetRequestLogout() throws IOException
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		
		String sessionId = "sessionId";
		UserSession userSession = frontend.CreateUserSession("name", sessionId, "pass", "email");
		userSession.eSessionStatus.set(SessionStatus.eAuthorized);
		
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn(sessionId);
		
		when(req.toString()).thenReturn("");
		when(req.getRequestURI()).thenReturn("/logout");
		when(req.getSession(false)).thenReturn(session);
		
		frontend.ParseGetRequestUrl(req, resp);
		
		verify(session).invalidate();
		verify(resp).sendRedirect("/");
	}
	
	@Test
	public void testParseGetRequestUnknownUrl() throws IOException
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		
		when(req.toString()).thenReturn("");
		when(req.getRequestURI()).thenReturn("/hlhjklh");
		when(req.getSession(false)).thenReturn(null);
		
		frontend.ParseGetRequestUrl(req, resp);
		verify(resp).sendRedirect("/");
	}
	
	@Test
	public void testAuthorizeUser()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		
		UserSession userSession = new UserSession();
		userSession.userName.set("name");

		frontend.AuthorizeUser(userSession);
		verify(messageSystem).sendMessage(any(MsgAuthorizeUser.class));
	}
	
	@Test
	public void testCreateAndGetUserSession()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		
		String userName = "name";
		String sessionId = "sessionId";
		String password = "123";
		String email = "email";

		UserSession result = frontend.CreateUserSession(userName, sessionId, password, email);
		UserSession result2 = frontend.GetUserSession(sessionId);
		
		assertNotNull(result);
		assertNotNull(result2);
		assertEquals(result, result2);
	}

	@Test
	public void testParsePostRegisterUrl()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);

		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionId");
		when(session.isNew()).thenReturn(true);
		
		when(req.toString()).thenReturn("");
		when(req.getRequestURI()).thenReturn("/register");
		when(req.getSession(true)).thenReturn(session);
		when(req.getParameter("name")).thenReturn("userName");
		when(req.getParameter("password")).thenReturn("password");
		when(req.getParameter("email")).thenReturn("email");
		
		String result = frontend.ParsePostRequestUrl(req);

		verify(messageSystem).sendMessage(any(MsgRegisterUser.class));
		assertEquals("wait registration", result);
	}

	@Test
	public void testParsePostLoginUrl()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		frontend.CreateUserSession("name", "sessionId", "pass", "mail");

		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionId");
		when(session.isNew()).thenReturn(false);
		
		when(req.toString()).thenReturn("");
		when(req.getRequestURI()).thenReturn("/login");
		when(req.getSession(true)).thenReturn(session);
		when(req.getParameter("name")).thenReturn("userName");
		when(req.getParameter("password")).thenReturn("password");
		when(req.getParameter("email")).thenReturn("email");
	
		String result = frontend.ParsePostRequestUrl(req);

		verify(messageSystem).sendMessage(any(MsgAuthorizeUser.class));
		assertEquals("wait authorization", result);
	}

	@Test
	public void testParsePostUnknownUrl()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		frontend.CreateUserSession("name", "sessionId", "pass", "mail");

		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionId");
		when(session.isNew()).thenReturn(false);
		
		when(req.toString()).thenReturn("");
		when(req.getRequestURI()).thenReturn("/unknown");
		when(req.getSession(true)).thenReturn(session);
		when(req.getParameter("name")).thenReturn("userName");
		when(req.getParameter("password")).thenReturn("password");
		when(req.getParameter("email")).thenReturn("email");
		
		String result = frontend.ParsePostRequestUrl(req);
		assertEquals("", result);
	}


	@Test
	public void testGetAddress()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		Address result = frontend.getAddress();
		assertNotNull(result);
	}

	@Test
	public void testGetUserSessionStatusWithNullSession()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		HttpSession session = null;

		String result = frontend.getUserSessionStatus(session);
		assertEquals("{\"status\": null}", result);
	}
	
	@Test
	public void testGetUserSessionStatus()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionId");

		UserSession userSession = frontend.CreateUserSession("name", "sessionId", "pass", "email");
		String jsonResult = frontend.getUserSessionStatus(session);

		JSONObject expectedResult = new JSONObject();
		expectedResult.put("userName", userSession.userName.get());
		expectedResult.put("userId", userSession.userId.get());
		expectedResult.put("status", "inProcess");
		  
		assertEquals(expectedResult.toString(), jsonResult);
	}

	@Test
	public void testLogout()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		String sessionId = "sessionId";
		
		UserSession result = frontend.CreateUserSession("name", sessionId, "pass", "email");
		frontend.logout(sessionId);
		UserSession result2 = frontend.GetUserSession(sessionId);

		assertNull(result2);
	}

	@Test
	public void testUpdateUserSession()
	{
		FrontendImpl frontend = new FrontendImpl(messageSystem);
		String sessionId = "sessionId";
		
		//Create user session
		UserSession userSession = frontend.CreateUserSession("name", sessionId, "pass", "email");
		
		//Updated user session
		UserSession newUserSession = new UserSession("name", sessionId, "pass");
		newUserSession.eSessionStatus.set(SessionStatus.eAuthorized);

		frontend.updateUserSession(newUserSession);
		UserSession result = frontend.GetUserSession(sessionId);
		
		assertEquals(SessionStatus.eAuthorized, result.eSessionStatus.get());
	}


	public static void main(String[] args) {
		new org.junit.runner.JUnitCore().run(FrontendImplTest.class);
	}
}