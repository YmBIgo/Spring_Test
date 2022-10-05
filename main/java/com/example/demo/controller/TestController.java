package com.example.demo.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
		String sql = "SELECT * FROM users";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		model.addAttribute("testList", list);
		return "index";
	}
	
	// login signup get
	@GetMapping("/login")
	public String login(Model model) {
		String csrf_key = generate_csrf();
		model.addAttribute("csrf_key", csrf_key);
		return "login";
	}
	@GetMapping("/signup")
	public String signup(Model model) {
		String csrf_key = generate_csrf();
		model.addAttribute("csrf_key", csrf_key);
		return "signup";
	}
	
	// login signup post
	@PostMapping("/users/login")
	public String usersLogin(@RequestParam String email,
							 @RequestParam String password,
							 @RequestParam String csrf_key,
							 Model model) {
		// TODO CSRF
		boolean csrf_result = check_csrf(csrf_key);
		if (csrf_result == false) {
			model.addAttribute("error_type", 2);
			return "users/log_in/user_login_fail";
		}
		// Email Check
		Pattern email_pattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
		Matcher email_matcher = email_pattern.matcher(email);
		boolean is_find = email_matcher.find();
		int password_length = password.length();
		if (is_find == false || password_length < 8) {
			model.addAttribute("error_type", 0);
			return "users/log_in/user_login_fail";
		}
		String hashed_password = DigestUtils.md5Hex(password);
		String check_user_sql = "SELECT * FROM users WHERE email='" + email +
								"' AND hashed_password='" + hashed_password + "';" ;
		List<Map<String, Object>> list = jdbcTemplate.queryForList(check_user_sql);
		System.out.println(list);
		if (list.size() == 0) {
			model.addAttribute("error_type", 1);
			return "users/log_in/user_login_fail";
		}
		return "users/log_in/user_login_success";
	}
	@PostMapping("/users/signup")
	public String usersSignup(@RequestParam String email,
							  @RequestParam String password,
							  @RequestParam String csrf_key,
							  Model model) {
		// TODO CSRF
		boolean csrf_result = check_csrf(csrf_key);
		if (csrf_result == false) {
			model.addAttribute("error_type", 2);
			return "users/sign_up/user_signup_fail";
		}
		// Email Check
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
		String check_user_sql = "SELECT * FROM users WHERE email='" + email + "'";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(check_user_sql);
		if (list.size() >= 1) {
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
	
	public String generate_csrf() {
		// generate random value
		Random random = new Random();
		int random_number = random.nextInt(10000000);
		String hashed_key_before = String.valueOf(random_number);
		String hashed_key = DigestUtils.md5Hex(hashed_key_before);
		// generate csfr checker sql
		String csrf_sql = "INSERT INTO csrf_checker(hashed_key) " +
						"VALUES('" + hashed_key + "');";
		jdbcTemplate.update(csrf_sql);
		return hashed_key;
	}
	
	public boolean check_csrf(String csrf_key) {
		String csrf_check_sql = "SELECT created_at FROM csrf_checker WHERE hashed_key='" +
								csrf_key + "' " +
								"ORDER BY created_at DESC";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(csrf_check_sql);
		if (list.size() == 0) return false;
		String csrf_created_at_str = list.get(0).get("created_at").toString();
		SimpleDateFormat csrf_timestamp_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date csrf_created_at;
		try {
			csrf_created_at = csrf_timestamp_format.parse(csrf_created_at_str);
		} catch(ParseException e) {
			csrf_created_at = new Date(0);
			System.out.println(csrf_created_at_str + " -> " + e.toString());
		}
		Date current_time = new Date(System.currentTimeMillis());
		long diffMills = current_time.getTime() - csrf_created_at.getTime();
		int diffSeconds = (int)diffMills/1000;
		// System.out.println(diffMills);
		System.out.println(diffSeconds);
		if (diffSeconds < 86400) {
			return true;
		}
		return false;
	}
}