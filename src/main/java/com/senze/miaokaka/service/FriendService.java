package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.entity.UserFriend;
import com.senze.miaokaka.model.vo.FriendApplicationVO;
import com.senze.miaokaka.model.vo.FriendCatVO;
import com.senze.miaokaka.model.vo.FriendFeedItemVO;
import com.senze.miaokaka.model.vo.FriendRankItemVO;
import com.senze.miaokaka.model.vo.FriendSearchVO;
import com.senze.miaokaka.model.vo.FriendVO;
import com.senze.miaokaka.model.vo.CheckInCalendarVO;

import java.util.List;

/**
 * 好友服务（申请-同意制；围观/点赞/动态/排行仅好友可见）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface FriendService extends IService<UserFriend> {

    /**
     * 发起好友申请（对方将收到通知）
     */
    void apply(Long userId, Long targetUserId);

    /**
     * 我收到的待审申请列表
     */
    List<FriendApplicationVO> applications(Long userId);

    /**
     * 同意申请：双向写入好友 + 通知申请人
     */
    void agree(Long userId, Long applicationId);

    /**
     * 拒绝申请（删除申请行，可重新申请）
     */
    void reject(Long userId, Long applicationId);

    /**
     * 我的好友列表（含全勤连击）
     */
    List<FriendVO> listFriends(Long userId);

    /**
     * 删除好友（双向删除）
     */
    void removeFriend(Long userId, Long friendUserId);

    /**
     * 搜索用户（账号精确匹配或用户 id），用于加好友前定位
     */
    FriendSearchVO search(String keyword);

    /**
     * 好友排行：好友 + 我，按全勤连击倒序
     */
    List<FriendRankItemVO> rank(Long userId);

    /**
     * 好友打卡动态：好友最近 7 天打卡（含点赞数与我的点赞状态）
     */
    List<FriendFeedItemVO> feed(Long userId);

    /**
     * 围观好友：其进行中计划的猫列表
     */
    FriendCatVO friendCats(Long userId, Long friendId);

    /**
     * 围观好友：其某计划的打卡日历（只读）
     */
    CheckInCalendarVO friendCalendar(Long userId, Long friendId, Long planId, String month);

    /**
     * 点赞好友打卡记录：同一记录仅一次、每日限 5 次；被赞者小鱼干 +1
     *
     * @return 该记录的累计点赞数
     */
    long like(Long userId, Long recordId);
}
