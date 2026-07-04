package info.mengnan.dialogerai.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;


@Data
@TableName(value = "chat_agent_option", autoResultMap = true)
public class ChatAgentOption {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 指定用户配置
     */
    private Long memberId;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 最大消息窗口数
     */
    private Integer maxMessages;

    /**
     * 是否启用
     */
    private Boolean enabled;

    private Boolean rag;

    /**
     * Query Transformer 类型
     */
    private String transform;

    /**
     * Content Aggregator 类型
     */
    private Boolean contentAggregator;

    private Boolean tools;

    /**
     * Content Injector 提示词模板
     */
    private String contentInjectorPrompt;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    @TableField("in_DB")
    private Boolean inDB;

    /**
     * 备注
     */
    private String remark;

    /**
     * 绑定的知识库 ID 列表，如 [1,2,3]
     */
    @TableField(value = "kb_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> kbIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}