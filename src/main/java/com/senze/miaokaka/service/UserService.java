package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.dto.admin.UserBanRequest;
import com.senze.miaokaka.model.dto.admin.UserPageQueryRequest;
import com.senze.miaokaka.model.dto.user.UserLoginRequest;
import com.senze.miaokaka.model.dto.user.UserRegisterRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.LoginResponseVO;
import com.senze.miaokaka.model.vo.LoginUserVO;
import com.senze.miaokaka.model.vo.RankBoardVO;
import com.senze.miaokaka.model.vo.RankItemVO;
import com.senze.miaokaka.model.vo.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户服务
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface UserService extends IService<User> {

    /**
     * 注册
     *
     * @return 新用户id
     */
    long register(UserRegisterRequest request);

    /**
     * 账号密码登录，签发 JWT
     */
    LoginResponseVO login(UserLoginRequest request);

    /**
     * 从请求属性中取当前登录用户（由 JwtInterceptor 写入）
     */
    User getLoginUser(HttpServletRequest request);

    LoginUserVO toLoginUserVO(User user);

    /**
     * 管理端分页查用户
     */
    Page<UserVO> pageUserVOs(UserPageQueryRequest request);

    /**
     * 封禁/解封用户（admin 不可被封禁）
     */
    boolean banUser(UserBanRequest request);

    /**
     * 全勤连击榜：Top 50（走缓存）+ 当前用户排名 myRank
     * （Top 50 内取榜单名次；50 外按同口径实时补算；零连击为 null）
     */
    RankBoardVO streakBoard(Long userId);
}
