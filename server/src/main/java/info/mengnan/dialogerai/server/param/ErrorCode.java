package info.mengnan.dialogerai.server.param;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 通用
    BUSINESS_ERROR("0001", "业务异常"),

    // 文件
    FILE_EMPTY("1001", "文件不能为空"),
    FILE_INVALID("1002", "文件名无效"),
    FILE_UPLOAD_FAILED("1003", "文件上传失败"),

    // 知识库
    KB_NOT_FOUND("2001", "知识库不存在"),
    KB_NAME_EMPTY("2002", "知识库名称不能为空"),
    KB_WRITE_DENIED("2003", "无权操作该知识库"),

    // 文档
    DOC_DUPLICATE("3001", "该知识库中已存在同名文档"),
    DOC_NOT_FOUND("3002", "文档不存在"),
    DOC_NOT_READY("3003", "文档尚未处理完成，请稍后再试"),
    DOC_CONTENT_EMPTY("3004", "未找到可展示的文档内容"),

    // 会员
    MEMBER_CREDENTIALS_EMPTY("4001", "用户名和密码不能为空"),
    MEMBER_PASSWORD_EMPTY("4002", "原密码和新密码不能为空"),
    MEMBER_CANNOT_DELETE_SELF("4003", "不能删除当前登录的账号"),
    MEMBER_NOT_FOUND("4004", "用户不存在"),
    MEMBER_AUTH_FAILED("4005", "用户名或密码错误"),
    MEMBER_DISABLED("4006", "用户已被禁用"),
    MEMBER_OLD_PASSWORD_WRONG("4007", "原密码错误"),
    MEMBER_USERNAME_EXISTS("4008", "用户名已存在"),
    MEMBER_PHONE_EXISTS("4009", "手机号已被注册"),
    MEMBER_OWNER_REQUIRED("4010", "仅 Owner 可执行此操作"),
    MEMBER_MANAGE_DENIED("4011", "无权管理该用户"),
    ;

    private final String code;
    private final String message;
}
