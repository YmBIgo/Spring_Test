package com.example.demo.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping()
public class TestController {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@GetMapping("/index")
	public String index(Model model) {
		String sql = "SELECT * FROM test_table";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		model.addAttribute("testList", list);
		return "index";
	}
	
	// login signup get
	@GetMapping("/login")
	public String login(Model model) {
		return "login";
	}
	@GetMapping("/signup")
	public String signup(Model model) {
		return "signup";
	}
	
	// login signup post
	@PostMapping("/users/signup")
	public String usersSignup(@RequestParam String email,
							  @RequestParam String password,
							  Model model) {
		// TODO CSRF
		Pattern email_pattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
		Matcher email_matcher = email_pattern.matcher(email);
		boolean is_find = email_matcher.find();
		int password_length = password.length();
		System.out.println(email + " " + is_find);
		if (is_find == false || password_length < 8) {
			model.addAttribute("error_type", 0);
			return "users/sign_up/user_signup_fail";
		}
		//
		String check_user_sql = "SELECT * FROM test_table WHERE email='" + email + "'";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(check_user_sql);
		if (list.size() >= 1) {
			model.addAttribute("checked_user", list);
			model.addAttribute("error_type", 1);
			return "users/sign_up/user_signup_fail";
		}
		String masked_password = String.join("", Collections.nCopies(password_length, "*"));
		String hashed_new_password = DigestUtils.md5Hex(password);
//		String insert_new_user_sql = "INSERT INTO test_table(email, password) VALUES(" +
//				email + ", " + hashed_new_password  +")";
//		jdbcTemplate.update(insert_new_user_sql);
		//
		model.addAttribute("password", masked_password);
		model.addAttribute("email", email);
		return "users/sign_up/user_signup_success";
	}
}