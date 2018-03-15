package zs.live.entity

import org.apache.commons.lang.StringUtils

/**
 * Created by Administrator on 2016/12/14.
 */
class LiveForeshow {

    long foreshowId;
    String title = StringUtils.EMPTY
    String imgUrl = StringUtils.EMPTY

    long userId;
    String userName = StringUtils.EMPTY
    String nickname = StringUtils.EMPTY
    String userImage = StringUtils.EMPTY
    Date beginTime

    int urlTag;

    String url = StringUtils.EMPTY
    int sortNum
    String srpId = StringUtils.EMPTY
    int isTop
    int isRecommend
    int isPush
    Date createTime
    Date updateTime
    String appId = StringUtils.EMPTY
    int foreshowStatus
    String keyword = StringUtils.EMPTY
    String liveRecordInfo = StringUtils.EMPTY
    int newsId
    int foreshowType
    long parentId
    String description = StringUtils.EMPTY
    String descriptionHtml = StringUtils.EMPTY
    long cateId
    Date endTime

    String pgcTitle
    int liveNum //第几期
    int childSort //系列包含的子记录排序
    int showTitleInList //5.6新增 在列表中进行展示 1:展示,0:不展示
    int notShowInClient //5.6 新增，展示规则，0:全部,1:h5显示，2：客户端展示

    int isCost
    int isSale
    int liveRecommend
    Date recommendTime
    String ruleJson
}
