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
    MEMBER_CANNOT_DELETE_SELF("4003", "不能删除当前登录的账号"),
    MEMBER_NOT_FOUND("4004", "用户不存在或认证失败"),
    MEMBER_DISABLED("4006", "用户已被禁用"),
    MEMBER_OLD_PASSWORD_WRONG("4007", "原密码错误"),
    MEMBER_USERNAME_EXISTS("4008", "用户名已存在"),
    MEMBER_PHONE_EXISTS("4009", "手机号已被注册"),
    MEMBER_MANAGE_DENIED("4011", "无权执行此操作"),

    // 模型 API Key
    MODEL_KEY_NOT_FOUND("5001", "模型 API Key 不存在"),
    MODEL_PARAM_INVALID("5002", "缺少必要参数或参数格式不合法"),
    MODEL_KEY_DELETE_DENIED("5003", "无权删除该 API Key"),
    MODEL_DEFAULT_INVALID("5004", "仅 chat 类型模型可设为默认对话模型"),
    MODEL_DEFAULT_REQUIRED("5005", "当前组织未配置默认对话模型"),

    // 应用配置
    CONFIG_NOT_FOUND("7001", "配置不存在"),

    // Agent 配置
    AGENT_OPTION_NOT_FOUND("8001", "Agent 配置不存在"),
    AGENT_OPTION_WRITE_DENIED("8002", "无权操作该 Agent 配置"),
    AGENT_OPTION_NAME_EMPTY("8003", "Agent 配置名称不能为空"),
    AGENT_OPTION_MODEL_INVALID("8004", "绑定的模型不存在或不属于当前组织"),
    AGENT_OPTION_KB_INVALID("8005", "绑定的知识库不存在或无权访问"),

    // Function Call
    FC_PARAM_INVALID("6001", "工具不存在或参数不合法"),
    FC_ACCESS_DENIED("6002", "无权访问该工具"),
    FC_WRITE_DENIED("6003", "无权修改或删除该工具"),
    FC_PROMPT_EMPTY("6004", "提示词不能为空"),
    FC_TEST_COUNT_INVALID("6005", "测试用例数量需在 1~20 之间"),
    FC_EXECUTE_FAILED("6006", "工具执行失败"),
    FC_GENERATE_FAILED("6007", "生成失败"),
    ;

    private final String code;
    private final String message;
}
