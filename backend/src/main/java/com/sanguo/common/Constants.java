package com.sanguo.common;

/**
 * 常量：评论/收藏 target_type、文章状态等
 */
public class Constants {

    // 评论目标类型
    public static final int TARGET_ARTICLE = 1;
    public static final int TARGET_RESOURCE = 2;
    public static final int TARGET_COMMENT = 3;

    // 收藏目标类型
    public static final int COLLECT_ARTICLE = 1;
    public static final int COLLECT_PERSON = 2;
    public static final int COLLECT_EVENT = 3;
    public static final int COLLECT_LITERATURE = 4;
    public static final int COLLECT_ALLUSION = 5;
    public static final int COLLECT_AI_ANSWER = 6;
    public static final int COLLECT_FORUM_TOPIC = 7;

    // 文章状态
    public static final int ARTICLE_PENDING = 0;
    public static final int ARTICLE_APPROVED = 1;
    public static final int ARTICLE_REJECTED = 2;

    // 用户状态
    public static final int USER_NORMAL = 1;
    public static final int USER_DISABLED = 0;

    // 论坛主题状态
    public static final int TOPIC_NORMAL = 1;
    public static final int TOPIC_CLOSED = 0;
    public static final int TOPIC_HIDDEN = 2;

    // 角色编码
    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_CONTENT_ADMIN = "content_admin";
    public static final String ROLE_REVIEW_ADMIN = "review_admin";
    public static final String ROLE_SUPER_ADMIN = "super_admin";
}
