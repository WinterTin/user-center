package com.dongdong.usercenter.controller;

import com.dongdong.usercenter.common.BaseResponse;
import com.dongdong.usercenter.exception.BusinessException;
import com.dongdong.usercenter.common.ErrorCode;
import com.dongdong.usercenter.constant.UserConstant;
import com.dongdong.usercenter.model.domain.User;
import com.dongdong.usercenter.model.domain.request.UserLoginRequest;
import com.dongdong.usercenter.model.domain.request.UserRegisterRequest;
import com.dongdong.usercenter.service.impl.UserServiceImpl;
import com.dongdong.usercenter.utils.ResponseUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @program: user-center
 * @description: 用户服务接口
 * @author: Mr.Ye
 * @create: 2022-05-14 17:43
 **/
@RestController
@RequestMapping("/user")
public class UserController {
	@Resource
	private UserServiceImpl userService;

	/**
	 * 用户注册接口
	 *
	 * @param userRegisterRequest 用户注册请求体参数
	 * @return 用户ID
	 */
	@PostMapping("/register")
	public BaseResponse<Long> userRegister(@Validated @RequestBody UserRegisterRequest userRegisterRequest) {
		if (userRegisterRequest == null) {
			throw new BusinessException(ErrorCode.NULL_ERROR, "请求对象为空");
		}
		// 简单的校验，不涉及业务逻辑
		String userAccount = userRegisterRequest.getUserAccount();
		String userPassword = userRegisterRequest.getUserPassword();
		String checkPassword = userRegisterRequest.getCheckPassword();
		Integer planetCode = userRegisterRequest.getPlanetCode();
		if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode + "")) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
		}
		Long id = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
		return ResponseUtils.success(id);
	}

	/**
	 * 用户登录接口
	 *
	 * @param userLoginRequest   用户登录请求体参数
	 * @param httpServletRequest 请求对象
	 * @return 用户基本信息
	 */
	@PostMapping("/login")
	public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest httpServletRequest) {
		if (userLoginRequest == null) {
			throw new BusinessException(ErrorCode.NULL_ERROR, "请求对象为空");
		}
		// 简单的校验，不涉及业务逻辑
		String userAccount = userLoginRequest.getUserAccount();
		String userPassword = userLoginRequest.getUserPassword();
		if (StringUtils.isAnyBlank(userAccount, userPassword)) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
		}
		User safeUser = userService.userLogin(userAccount, userPassword, httpServletRequest);
		return ResponseUtils.success(safeUser);
	}

	/**
	 * 用户注销接口
	 *
	 * @param httpServletRequest 请求对象
	 * @return 成功返回值
	 */
	@PostMapping("/logout")
	public BaseResponse<Integer> userLogin(HttpServletRequest httpServletRequest) {
		if (httpServletRequest == null) {
			throw new BusinessException(ErrorCode.NULL_ERROR, "请求对象为空");
		}
		Integer result = userService.userLogout(httpServletRequest);
		return ResponseUtils.success(result);
	}

	/**
	 * 查找所有用户
	 *
	 * @param request 请求对象
	 * @return 符合条件的用户列表
	 */
	@GetMapping("/search")
	public BaseResponse<List<User>> searchUsers(HttpServletRequest request) {
		// 校验是否是管理员
		if (!isAdmin(request)) {
			throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "用户非管理员");
		}
		List<User> users = userService.list();
		List<User> safeUsers = users.stream().map(user -> userService.getSafeUser(user)).collect(Collectors.toList());
		return ResponseUtils.success(safeUsers);
	}

	/**
	 * 根据ID删除用户
	 *
	 * @param id      用户ID
	 * @param request 请求对象
	 * @return 删除的用户数量
	 */
	@DeleteMapping("/delete")
	public BaseResponse<Integer> deleteUser(@RequestParam Long id, HttpServletRequest request) {
		if (!isAdmin(request)) {
			throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "用户非管理员");
		}
		Integer result = userService.deleteById(id);
		return ResponseUtils.success(result);
	}

	/**
	 * 获取当前用户
	 *
	 * @param request 请求对象
	 * @return 脱敏的当前用户信息
	 */
	@GetMapping("/current")
	public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
		// 获取当前用户信息的ID
		User originUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
		if (originUser == null) {
			throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
		}
		Long id = originUser.getId();
		// 利用ID查询数据库，获取最新的用户信息返回
		User user = userService.getById(id);
		User safeUser = userService.getSafeUser(user);
		return ResponseUtils.success(safeUser);
	}

	/**
	 * 判断是否是管理员
	 *
	 * @param request 请求对象
	 * @return 判断结果
	 */
	private static Boolean isAdmin(HttpServletRequest request) {
		User safeUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
		return UserConstant.ADMIN_ROLE.equals(safeUser.getUserRole());
	}
}