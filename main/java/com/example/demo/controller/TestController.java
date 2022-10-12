package com.example.demo.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping()
public class TestController {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	// User
	@GetMapping("/index")
	public String index(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
						@CookieValue(name = "email", required = false, defaultValue = "") String email,
						Model model) {
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id.equals("")) {
			String csrf_key = generate_csrf();
			model.addAttribute("csrf_key", csrf_key);
			return "login";
		}
		List<Map<String, Object>> list = get_timeline_tweets();
		List<Map<String, Object>> list_with_user_id = new ArrayList();
		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> current_list = list.get(i);
			String current_tweet_user_id_str = current_list.get("user_id").toString();
			String get_user_name_sql = "SELECT name FROM users WHERE id = ?";
			Map<String, Object> user_result = jdbcTemplate.queryForMap(get_user_name_sql, current_tweet_user_id_str);
			current_list.put("user_name", user_result.get("name"));
			list_with_user_id.add(current_list);
		}
		model.addAttribute("tweets", list_with_user_id);
		String csrf_key = generate_csrf();
		model.addAttribute("csrf_key", csrf_key);
		return "index";
	}
	
	// login signup get
	@GetMapping("/login")
	public String login(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
						Model model) {
		// cookie check
		if (!cookie_value.equals("")) {
			return "index_redirect";
		}
		// csrf
		String csrf_key = generate_csrf();
		model.addAttribute("csrf_key", csrf_key);
		return "login";
	}
	@GetMapping("/signup")
	public String signup(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
						 Model model) {
		// cookie check
		if (!cookie_value.equals("")) {
			return "index_redirect";
		}
		// csrf
		String csrf_key = generate_csrf();
		model.addAttribute("csrf_key", csrf_key);
		return "signup";
	}
	
	// login signup post
	@PostMapping("/users/login")
	public String usersLogin(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
							 @RequestParam String email,
							 @RequestParam String password,
							 @RequestParam String csrf_key,
							 HttpServletResponse response,
							 Model model) {
		// cookie check
		if (!cookie_value.equals("")) {
			return "index_redirect";
		}
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
		// String check_user_sql = "SELECT * FROM users WHERE email='" + email +
		//						"' AND hashed_password='" + hashed_password + "';" ;
		String check_user_sql = "SELECT * FROM users WHERE email= ? AND hashed_password = ?;" ;
		List<Map<String, Object>> list = jdbcTemplate.queryForList(check_user_sql, email, hashed_password);
		System.out.println(list);
		if (list.size() == 0) {
			model.addAttribute("error_type", 1);
			return "users/log_in/user_login_fail";
		}

		// cookie
		String random_cookie_value = generate_cookie();
		String update_user_sql = "UPDATE users SET cookie_value = ? WHERE email = ? AND hashed_password = ?;";
		jdbcTemplate.update(update_user_sql, random_cookie_value, email, hashed_password);
		Cookie cookie = new Cookie("id", random_cookie_value);
		cookie.setMaxAge(365 * 24 * 60 * 60);
		cookie.setPath("/");
		response.addCookie(cookie);
		Cookie email_cookie = new Cookie("email", email);
		email_cookie.setMaxAge(365 * 24 * 60 * 60);
		email_cookie.setPath("/");
		response.addCookie(email_cookie);
		//
		return "users/log_in/user_login_success";
	}
	@PostMapping("/users/signup")
	public String usersSignup(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
							  @RequestParam String email,
							  @RequestParam String password,
							  @RequestParam String csrf_key,
							  HttpServletResponse response,
							  Model model) {
		// cookie check
		if (!cookie_value.equals("")) {
			return "index_redirect";
		}
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
		if (is_find == false || password_length < 8) {
			model.addAttribute("error_type", 0);
			return "users/sign_up/user_signup_fail";
		}
		//
		// String check_user_sql = "SELECT * FROM users WHERE email='" + email + "'";
		String check_user_sql = "SELECT * FROM users WHERE email = ?;";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(check_user_sql, email);
		if (list.size() >= 1) {
			model.addAttribute("error_type", 1);
			return "users/sign_up/user_signup_fail";
		}
		String masked_password = String.join("", Collections.nCopies(password_length, "*"));
		String hashed_new_password = DigestUtils.md5Hex(password);
		
		// String insert_new_user_sql = "INSERT INTO test_table(email, password) VALUES(" +
		// email + ", " + hashed_new_password  +")";
		// jdbcTemplate.update(insert_new_user_sql);

		// cookie
		String random_cookie_value = generate_cookie();
		String update_user_sql = "UPDATE users SET cookie_value = ? WHERE email = ? AND hashed_password = ?;";
		jdbcTemplate.update(update_user_sql, random_cookie_value, email, hashed_new_password);
		Cookie cookie = new Cookie("id", random_cookie_value);
		cookie.setMaxAge(365 * 24 * 60 * 60);
		cookie.setPath("/");
		response.addCookie(cookie);
		Cookie email_cookie = new Cookie("email", email);
		email_cookie.setMaxAge(365 * 24 * 60 * 60);
		email_cookie.setPath("/");
		response.addCookie(email_cookie);
		
		model.addAttribute("password", masked_password);
		model.addAttribute("email", email);
		return "users/sign_up/user_signup_success";
	}
	
	// Tweets
	@GetMapping("/tweets/new")
	public String tweet_new(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
							@CookieValue(name = "email", required = false, defaultValue = "") String email,
							Model model) {
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id == "") {
			String csrf_key = generate_csrf();
			model.addAttribute("csrf_key", csrf_key);
			return "login";
		}
		String csrf_key = generate_csrf();
		model.addAttribute("csrf_key", csrf_key);
		return "tweets/tweet_new";
	}
	
	@PostMapping("/tweets/create")
	public String tweet_create(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
							   @CookieValue(name = "email", required = false, defaultValue = "") String email,
							   @RequestParam String tweet,
							   @RequestParam String csrf_key,
							   Model model) {
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id == "") {
			String csrf_key_login = generate_csrf();
			model.addAttribute("csrf_key", csrf_key_login);
			return "login";
		}
		boolean csrf_result = check_csrf(csrf_key);
		if (csrf_result == false) {
			return "index_redirect";
		}
		String create_tweet_sql = "INSERT INTO tweets(text, user_id, is_reply) VALUES(?, ?, 0)";
		jdbcTemplate.update(create_tweet_sql, tweet, current_user_id);
		return "index_redirect";
	}
	
	@GetMapping("/tweets/{tweetId}")
	public String tweet_show(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
							 @CookieValue(name = "email", required = false, defaultValue = "") String email,
							 @PathVariable String tweetId,
							 Model model) {
		if (tweetId == "") {
			return "/tweets/tweet_notfound";
		}
		String find_tweet_sql = "SELECT * FROM tweets WHERE id = ?";
		List<Map<String, Object>> tweet_list = jdbcTemplate.queryForList(find_tweet_sql, tweetId);
		if (tweet_list.size() == 0) {
			return "/tweets/tweet_notfound";
		}
		Map<String, Object> tweet_map = tweet_list.get(0);
		String tweet_user_sql = "SELECT email, id, name, old FROM users WHERE id = ?";
		List<Map<String, Object>> user_list = jdbcTemplate.queryForList(tweet_user_sql, tweet_map.get("user_id"));
		// current_user
		String current_user_id = current_user_id(cookie_value, email);
		// likes
		String find_like_sql = "SELECT * FROM likes WHERE tweet_id = ?;";
		List<Map<String, Object>> like_list = jdbcTemplate.queryForList(find_like_sql, tweetId);
		int like_count = like_list.size();
		int user_like_flag = -1;
		if (!current_user_id.equals("")) {
			String find_user_like_sql = "SELECT * FROM likes WHERE user_id = ? AND tweet_id = ?;";
			List<Map<String, Object>> user_like_list = jdbcTemplate.queryForList(find_user_like_sql, current_user_id, tweetId);
			user_like_flag = user_like_list.size();
		}
		String csrf_key = generate_csrf();
		//
		model.addAttribute("tweet", tweet_map);
		model.addAttribute("user", user_list.get(0));
		model.addAttribute("like_count", like_count);
		model.addAttribute("user_like", user_like_flag);
		model.addAttribute("csrf_key", csrf_key);
		return "tweets/tweet_show";
	}
	
	// User Profile
	@GetMapping("/users/{userId}")
	public String user_profile_show(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
			   						@CookieValue(name = "email", required = false, defaultValue = "") String email,
									@PathVariable String userId,
									Model model) {
		if (userId == "") {
			return "users/profiles/user_profile_notfound";
		}
		String find_user_sql = "SELECT name, id, email, old FROM users WHERE id = ?;";
		List<Map<String, Object>> user_list = jdbcTemplate.queryForList(find_user_sql, userId);
		if (user_list.size() == 0) {
			return "/users/profiles/user_profile_notfound";
		}
		String find_tweets_sql = "SELECT * FROM tweets WHERE user_id = ? ORDER BY created_at DESC;";
		List<Map<String, Object>> tweets_list = jdbcTemplate.queryForList(find_tweets_sql, userId);
		//
		String following_user_sql = "SELECT * FROM user_follow_relationships WHERE following_user_id = ?;";
		String followed_user_sql  = "SELECT * FROM user_follow_relationships WHERE followed_user_id = ?;";
		List<Map<String, Object>> following_user = jdbcTemplate.queryForList(following_user_sql, userId);
		List<Map<String, Object>> followed_user  = jdbcTemplate.queryForList(followed_user_sql, userId);
		int following_user_length = following_user.size();
		int followed_user_length  = followed_user.size();
		//
		// 
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id != "") {
			if (userId.equals(current_user_id)) {
				model.addAttribute("follow_or_edit_button", 0);
			} else {
				String check_is_follow_sql = "SELECT * FROM user_follow_relationships WHERE following_user_id = ? AND followed_user_id = ?;";
				List<Map<String, Object>> is_follow_relationship = jdbcTemplate.queryForList(check_is_follow_sql, current_user_id, userId);
				if (is_follow_relationship.size() == 0) {
					model.addAttribute("follow_or_edit_button", 1);
				} else {
					model.addAttribute("follow_or_edit_button", 2);
				}
			}
		}
		//
		model.addAttribute("user", user_list.get(0));
		model.addAttribute("tweets", tweets_list);
		model.addAttribute("following", following_user_length);
		model.addAttribute("followed", followed_user_length);
		return "users/profiles/user_profile_show";
	}
	
	@GetMapping("/users")
	public String user_profile_index(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
			   						@CookieValue(name = "email", required = false, defaultValue = "") String email,
									Model model) {
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id.equals("")) {
			String csrf_key = generate_csrf();
			model.addAttribute("csrf_key", csrf_key);
			return "login";
		}
		String get_users_sql = "SELECT * FROM users;";
		List<Map<String, Object>> users_list = jdbcTemplate.queryForList(get_users_sql);
		model.addAttribute("users", users_list);
		model.addAttribute("current_user_id", current_user_id);
		return "users/profiles/user_profile_index";
	}
	
	@GetMapping("/users/{userId}/followings")
	public String user_profile_following(@PathVariable String userId,
										 Model model) {
		if (userId.equals("")) {
			return "/users/profiles/user_profile_notfound";
		}
		String find_user_sql = "SELECT id, name, email, old FROM users WHERE id = ?;";
		List<Map<String, Object>> user_list = jdbcTemplate.queryForList(find_user_sql, userId);
		if (user_list.size() == 0) {
			return "/users/profiles/user_profile_notfound";
		}
		model.addAttribute("current_user", user_list.get(0));
		//
		String following_user_sql = "SELECT * FROM user_follow_relationships WHERE following_user_id = ?;";
		List<Map<String, Object>> following_user = jdbcTemplate.queryForList(following_user_sql, userId);
		List<Map<String, Object>> following_user_array = new ArrayList(); 
		for (int i = 0; i < following_user.size(); i++) {
			String get_following_user_info_sql = "SELECT name, old, id, email FROM users WHERE id = ?;";
			Map<String, Object> following_user_info = jdbcTemplate.queryForMap(get_following_user_info_sql, following_user.get(i).get("followed_user_id"));
			following_user_array.add(following_user_info);
		}
		//
		model.addAttribute("following_user", following_user_array);
		return "/users/profiles/user_profile_following";
	}
	
	@GetMapping("/users/{userId}/followers")
	public String user_profile_follower(@PathVariable String userId,
										Model model) {
		if (userId.equals("")) {
			return "/users/profiles/user_profile_notfound";
		}
		String find_user_sql = "SELECT id, name, email, old FROM users WHERE id = ?;";
		List<Map<String, Object>> user_list = jdbcTemplate.queryForList(find_user_sql, userId);
		if (user_list.size() == 0) {
			return "/users/profiles/user_profile_notfound";
		}
		model.addAttribute("current_user", user_list.get(0));
		//
		String followed_user_sql  = "SELECT * FROM user_follow_relationships WHERE followed_user_id = ?;";
		List<Map<String, Object>> followed_user  = jdbcTemplate.queryForList(followed_user_sql, userId);
		if (followed_user.size() == 0) {
			return "/users/profiles/user_profile_follower";
		}
		String get_users_info_sql = "SELECT name, id, old, email FROM users WHERE id IN(";
		String get_user_info_middle_sql = "";
		for (int i = 0; i < followed_user.size(); i++) {
			if (followed_user.size() == i+1) {
				get_user_info_middle_sql += followed_user.get(i).get("following_user_id");
			} else {
				get_user_info_middle_sql += followed_user.get(i).get("following_user_id") + ", ";
			}
		}
		get_users_info_sql += get_user_info_middle_sql;
		get_users_info_sql += ");";
		List<Map<String, Object>> user_info = jdbcTemplate.queryForList(get_users_info_sql);
		//
		model.addAttribute("followed_user", user_info);
		return "/users/profiles/user_profile_follower";
	}
	@GetMapping("/users/{userId}/follow")
	public String user_profile_follow(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
			   						  @CookieValue(name = "email", required = false, defaultValue = "") String email, 
			   						  @PathVariable String userId,
			   						  Model model) {
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id.equals("")) {
			String csrf_key = generate_csrf();
			model.addAttribute("csrf_key", csrf_key);
			return "login";
		}
		model.addAttribute("userId", "http://localhost:8080/users/" + userId);
		if (userId.equals(current_user_id)) {
			return "users/profiles/user_profile_redirect";
		}
		//
		String check_is_follow_sql = "SELECT * FROM user_follow_relationships WHERE following_user_id = ? AND followed_user_id = ?;";
		List<Map<String, Object>> is_follow_relationship = jdbcTemplate.queryForList(check_is_follow_sql, current_user_id, userId);
		if (is_follow_relationship.size() != 0) {
			return "users/profiles/user_profile_redirect";
		}
		String insert_user_relationship_sql = "INSERT INTO user_follow_relationships(following_user_id, followed_user_id) VALUES(?, ?)";
		jdbcTemplate.update(insert_user_relationship_sql, current_user_id, userId);
		return "users/profiles/user_profile_redirect";
	}
	@GetMapping("/users/{userId}/unfollow")
	public String user_profile_unfollow(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
			   						  @CookieValue(name = "email", required = false, defaultValue = "") String email, 
			   						  @PathVariable String userId,
			   						  Model model) {
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id.equals("")) {
			String csrf_key = generate_csrf();
			model.addAttribute("csrf_key", csrf_key);
			return "login";
		}
		model.addAttribute("userId", "http://localhost:8080/users/" + userId);
		if (userId.equals(current_user_id)) {
			return "users/profiles/user_profile_redirect";
		}
		//
		String check_is_follow_sql = "SELECT * FROM user_follow_relationships WHERE following_user_id = ? AND followed_user_id = ?;";
		List<Map<String, Object>> is_follow_relationship = jdbcTemplate.queryForList(check_is_follow_sql, current_user_id, userId);
		if (is_follow_relationship.size() == 0) {
			return "users/profiles/user_profile_redirect";
		}
		String delete_user_relationship_sql = "DELETE FROM user_follow_relationships WHERE following_user_id = ? AND followed_user_id = ?;";
		jdbcTemplate.update(delete_user_relationship_sql, current_user_id, userId);
		return "users/profiles/user_profile_redirect";
	}
	@GetMapping("/users/profile/edit")
	public String user_profile_edit(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
				  					@CookieValue(name = "email", required = false, defaultValue = "") String email, 
									Model model) {
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id.equals("")) {
			String csrf_key = generate_csrf();
			model.addAttribute("csrf_key", csrf_key);
			return "login";
		}
		List<Map<String, Object>> user_list = current_user_object(cookie_value, email);
		model.addAttribute("user_info", user_list.get(0));
		String csrf_key = generate_csrf();
		model.addAttribute("csrf_key", csrf_key);
		return "users/profiles/user_profile_edit";
	}
	@PostMapping("/users/profile/update")
	public String user_profile_update(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
									  @CookieValue(name = "email", required = false, defaultValue = "") String email,
									  @RequestParam String old,
									  @RequestParam String name,
									  @RequestParam String csrf_key,
									  Model model) {
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id.equals("")) {
			String csrf_key_login = generate_csrf();
			model.addAttribute("csrf_key", csrf_key_login);
			return "login";
		}
		model.addAttribute("userId", "http://localhost:8080/users/" + current_user_id);
		boolean csrf_result = check_csrf(csrf_key);
		if (csrf_result == false) {
			return "users/profiles/user_profile_redirect";
		}
		String update_user_info_sql = "UPDATE users SET name = ?, old = ? WHERE id = ?";
		jdbcTemplate.update(update_user_info_sql, name, old, current_user_id);
		return "users/profiles/user_profile_redirect";
	}
	
	// Likes
	@PostMapping("/tweets/{tweetId}/like")
	public String tweet_like(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
							 @CookieValue(name = "email", required = false, defaultValue = "") String email,
							 @RequestParam String tweet_id,
							 @RequestParam String csrf_key,
							 Model model) {
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id.equals("")) {
			String csrf_key_login = generate_csrf();
			model.addAttribute("csrf_key", csrf_key_login);
			return "login";
		}
		String find_tweet_sql = "SELECT * FROM tweets WHERE id = ?;";
		List<Map<String, Object>> tweet_list = jdbcTemplate.queryForList(find_tweet_sql, tweet_id);
		if (tweet_list.size() == 0) {
			String tweet_url = "/tweets/" + tweet_id;
			model.addAttribute("tweetUrl", tweet_url);
			return "tweets/tweet_redirect";
		}
		String find_like_sql = "SELECT * FROM likes WHERE tweet_id = ? AND user_id = ?;";
		List<Map<String, Object>> like_list = jdbcTemplate.queryForList(find_like_sql, tweet_id, current_user_id);
		if (like_list.size() != 0) {
			String tweet_url = "/tweets/" + tweet_id;
			model.addAttribute("tweetUrl", tweet_url);
			return "tweets/tweet_redirect";
		}
		String insert_like_sql = "INSERT INTO likes (tweet_id, user_id) VALUES (?, ?)";
		jdbcTemplate.update(insert_like_sql, tweet_id, current_user_id);
		//
		String tweet_url = "/tweets/" + tweet_id;
		model.addAttribute("tweetUrl", tweet_url);
		return "tweets/tweet_redirect";
	}
	
	// unlike
	@PostMapping("/tweets/{tweetId}/unlike")
	public String tweet_unlike(@CookieValue(name = "id", required = false, defaultValue = "") String cookie_value,
			 				   @CookieValue(name = "email", required = false, defaultValue = "") String email,
			 				   @RequestParam String tweet_id,
			 				   @RequestParam String csrf_key,
			 				   Model model) {
		String current_user_id = current_user_id(cookie_value, email);
		if (current_user_id.equals("")) {
			String csrf_key_login = generate_csrf();
			model.addAttribute("csrf_key", csrf_key_login);
			return "login";
		}
		String find_like_sql = "SELECT * FROM likes WHERE tweet_id = ? AND user_id = ?;";
		List<Map<String, Object>> like_list = jdbcTemplate.queryForList(find_like_sql, tweet_id, current_user_id);
		if (like_list.size() == 0) {
			String tweet_url = "/tweets/" + tweet_id;
			model.addAttribute("tweetUrl", tweet_url);
			return "tweets/tweet_redirect";
		}
		String delete_like_sql = "DELETE FROM likes WHERE tweet_id = ? AND user_id = ?;";
		jdbcTemplate.update(delete_like_sql, tweet_id, current_user_id);
		//
		String tweet_url = "/tweets/" + tweet_id;
		model.addAttribute("tweetUrl", tweet_url);
		return "tweets/tweet_redirect";
	}
	
	//
	private String current_user_id(String cookie, String email) {
		String check_user_cookie_sql = "SELECT * FROM users WHERE email = ? AND cookie_value = ?;";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(check_user_cookie_sql, email, cookie);
		if (list.size() > 0) {
			String current_user_id = list.get(0).get("id").toString();
			return current_user_id;
		}
		return "";
	}
	
	private List<Map<String, Object>> current_user_object(String cookie, String email) {
		String check_user_cookie_sql = "SELECT * FROM users WHERE email = ? AND cookie_value = ?;";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(check_user_cookie_sql, email, cookie);
		return list;
	}
	
	private String generate_csrf() {
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
	
	private String generate_cookie() {
		// generate random value
		Random random = new Random();
		int random_number = random.nextInt(1000000000);
		String hashed_key_before = String.valueOf(random_number);
		String hashed_key = DigestUtils.sha256Hex(hashed_key_before);
		return hashed_key;
	}
	
	private boolean check_csrf(String csrf_key) {
		// String csrf_check_sql = "SELECT created_at FROM csrf_checker WHERE hashed_key='" +
		//						csrf_key + "' " +
		//						"ORDER BY created_at DESC";
		String csrf_check_sql = "SELECT created_at FROM csrf_checker WHERE hashed_key = ? ORDER BY created_at DESC;";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(csrf_check_sql, csrf_key);
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
//		System.out.println(diffSeconds);
		if (diffSeconds < 86400) {
			return true;
		}
		return false;
	}
	
	//
	
	private List<Map<String, Object>> get_timeline_tweets() {
		// TODO get tweet whose user is following
		String get_timeline_tweets_sql = "SELECT * FROM tweets ORDER BY created_at DESC;";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(get_timeline_tweets_sql);
		return list;
	}
}