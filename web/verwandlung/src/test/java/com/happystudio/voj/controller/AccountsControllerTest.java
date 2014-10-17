package com.happystudio.voj.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.happystudio.voj.model.Language;
import com.happystudio.voj.model.User;
import com.happystudio.voj.model.UserGroup;
import com.happystudio.voj.service.UserService;
import com.happystudio.voj.util.DigestUtils;

/**
 * AccountsController的测试类.
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class AccountsControllerTest {
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(accountsController).build();
	}
	
	/**
	 * 测试用例: 测试loginView方法
	 * 测试数据: 请求/accounts/login
	 * 预期结果: 加载/WEB-INF/views/accounts/login.jsp
	 */
	@Test
	public void testLoginView() throws Exception {
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/accounts/login"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("accounts/login"))
			.andExpect(MockMvcResultMatchers.forwardedUrl("accounts/login"))
			.andReturn();
		Assert.assertNotNull(result.getModelAndView());
	}
	
	/**
	 * 测试用例: 测试loginAction方法
	 * 测试数据: 使用有效的用户名和密码登录
	 * 预期结果: 返回包含{isSuccessful: true}的JSON数据
	 */
	@Test
	public void testLoginActionSuccessful() throws Exception {
		String username = "zjhzxhz";
        String password = DigestUtils.md5Hex("zjhzxhz");
        Language preferLanguage = new Language(2, "text/x-c++-src", "C++");
        UserGroup userGroup = new UserGroup(3, "administrators", "Administrators");
        User user = new User(username, password, "zjhzxhz@gmail.com", userGroup, preferLanguage);
		
		Mockito.when(userService.isAccountValid(username, password)).thenReturn(user);
		
		MvcResult result = mockMvc.perform(
				MockMvcRequestBuilders.post("/accounts/login.action")
					.param("username", "zjhzxhz")
					.param("password", "zjhzxhz"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType("application/json;charset=UTF-8"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isSuccessful").value(new Boolean(true)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isUsernameEmpty").value(new Boolean(false)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isPasswordEmpty").value(new Boolean(false)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isAccountValid").value(new Boolean(true)))
				.andReturn();
		Assert.assertNotNull(result);
	}
	
	/**
	 * 测试用例: 测试loginAction方法
	 * 测试数据: 使用无效的用户名和密码
	 * 预期结果: 返回包含{isSuccessful: false}的JSON数据
	 */
	@Test
	public void testLoginActionInvalidAccount() throws Exception {
		Mockito.when(userService.isAccountValid("InvalidUsername", "InvalidPassword"))
				.thenReturn(null);
		
		MvcResult result = mockMvc.perform(
				MockMvcRequestBuilders.post("/accounts/login.action")
					.param("username", "InvalidUsername")
					.param("password", "InvalidPassword"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType("application/json;charset=UTF-8"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isSuccessful").value(new Boolean(false)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isUsernameEmpty").value(new Boolean(false)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isPasswordEmpty").value(new Boolean(false)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isAccountValid").value(new Boolean(false)))
				.andReturn();
		Assert.assertNotNull(result);
	}
	
	/**
	 * 测试用例: 测试loginAction方法
	 * 测试数据: 使用空用户名
	 * 预期结果: 返回包含{isUsernameEmpty: true}的JSON数据
	 */
	@Test
	public void testLoginActionEmptyUsername() throws Exception {
		Mockito.when(userService.isAccountValid("", "Password"))
				.thenReturn(null);
		
		MvcResult result = mockMvc.perform(
				MockMvcRequestBuilders.post("/accounts/login.action")
					.param("username", "")
					.param("password", "Password"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType("application/json;charset=UTF-8"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isSuccessful").value(new Boolean(false)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isUsernameEmpty").value(new Boolean(true)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isPasswordEmpty").value(new Boolean(false)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isAccountValid").value(new Boolean(false)))
				.andReturn();
		Assert.assertNotNull(result);
	}
	
	/**
	 * 测试用例: 测试loginAction方法
	 * 测试数据: 使用空密码
	 * 预期结果: 返回包含{isPasswordEmpty: true}的JSON数据
	 */
	@Test
	public void testLoginActionEmptyPassword() throws Exception {
		Mockito.when(userService.isAccountValid("Username", ""))
				.thenReturn(null);
		
		MvcResult result = mockMvc.perform(
				MockMvcRequestBuilders.post("/accounts/login.action")
					.param("username", "Username")
					.param("password", ""))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType("application/json;charset=UTF-8"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isSuccessful").value(new Boolean(false)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isUsernameEmpty").value(new Boolean(false)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isPasswordEmpty").value(new Boolean(true)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.isAccountValid").value(new Boolean(false)))
				.andReturn();
		Assert.assertNotNull(result);
	}
	
	/**
	 * MVC事件驱动对象.
	 */
	private MockMvc mockMvc;
	
	/**
	 * 待测试的AccountsController对象.
	 */
	@InjectMocks
    private AccountsController accountsController;
	
	/**
	 * Mock的UserService对象.
	 */
	@Mock
	private UserService userService;
}
