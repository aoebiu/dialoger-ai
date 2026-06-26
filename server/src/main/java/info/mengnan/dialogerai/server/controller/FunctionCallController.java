package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.common.util.JSONUtil;
import info.mengnan.dialogerai.rag.constant.promptTemplate.PromptTemplateConstant;
import info.mengnan.dialogerai.rag.service.DirectModelInvoker;
import info.mengnan.dialogerai.rag.service.PromptTemplateManager;
import info.mengnan.dialogerai.repository.entity.ChatToolDescription;
import info.mengnan.dialogerai.repository.enums.AsyncTaskType;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.functionCall.FunctionCallRequest;
import info.mengnan.dialogerai.server.param.functionCall.FunctionCallScriptGenerateRequest;
import info.mengnan.dialogerai.server.param.functionCall.FunctionCallTestCaseGenerateRequest;
import info.mengnan.dialogerai.server.param.functionCall.FunctionCallTestRequest;
import info.mengnan.dialogerai.server.param.functionCall.ToolCapabilityAnalysisResult;
import info.mengnan.dialogerai.server.service.AsyncTaskService;
import info.mengnan.dialogerai.server.service.FunctionCallService;
import info.mengnan.dialogerai.server.service.MemberService;
import info.mengnan.dialogerai.server.service.ToolAdapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static info.mengnan.dialogerai.server.param.ErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/api/functioncall")
@RequiredArgsConstructor
public class FunctionCallController {

    private final FunctionCallService functionCallService;
    private final MemberService memberService;
    private final DirectModelInvoker directModelInvoker;
    private final PromptTemplateManager promptTemplateManager;
    private final ToolAdapterService toolAdapterService;
    private final AsyncTaskService asyncTaskService;

    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        return R.ok(functionCallService.list(memberService.resolveTeamMemberIds(memberId)));
    }

    @GetMapping("/{id}")
    public R getById(@PathVariable("id") Long id) {
        ChatToolDescription tool = functionCallService.findById(id);
        if (tool == null)
            return R.error(FC_PARAM_INVALID);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!hasTeamAccess(memberId, tool.getMemberId()))
            return R.error(FC_ACCESS_DENIED);

        return R.ok(tool);
    }

    @PostMapping("/create")
    public R create(@RequestBody FunctionCallRequest request) {
        if (request.getName() == null || request.getName().isBlank())
            return R.error(FC_PARAM_INVALID);
        if (request.getDescription() == null || request.getDescription().isBlank())
            return R.error(FC_PARAM_INVALID);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (functionCallService.findByNameAndMemberId(request.getName().trim(), memberId) != null)
            return R.error(FC_PARAM_INVALID);

        return R.ok(functionCallService.create(memberId, request));
    }

    @PostMapping("/generate/script")
    public R generateScript(@RequestBody FunctionCallScriptGenerateRequest request) {
        if (request.getPrompt() == null || request.getPrompt().isBlank())
            return R.error(FC_PROMPT_EMPTY);

        Long memberId = StpUtil.getLoginIdAsLong();
        String taskId = asyncTaskService.createTask(memberId, AsyncTaskType.GENERATE_SCRIPT,
                List.of("能力分析", "生成工具元数据"));

        asyncTaskService.submitTask(taskId, () -> {
            String userPrompt = request.getPrompt();
            try {
                asyncTaskService.updateStepRunning(taskId, 1);
                Map<String, Object> phase1Variables = Map.of("prompt", userPrompt);
                String analysisResult = directModelInvoker.directInvoke(
                        "generateScript.capabilityAnalyzer",
                        "tool_capability_analysis", phase1Variables);
                ToolCapabilityAnalysisResult capabilities = JSONUtil.toBean(analysisResult, ToolCapabilityAnalysisResult.class);

                asyncTaskService.updateStepRunning(taskId, 2);
                String baseTemplate = promptTemplateManager.getTemplate("tool_metadata_generation").template();
                String result = directModelInvoker.directInvokeRaw(
                        "generateScript.toolMetadataGenerator",
                        generatePrompt(baseTemplate, capabilities, request.getPrompt()));

                asyncTaskService.completeTask(taskId, result);
            } catch (Exception e) {
                log.error("generateScript async task failed, taskId={}", taskId, e);
                asyncTaskService.failTask(taskId, e.getMessage());
            }
        });

        return R.ok(Map.of("taskId", taskId));
    }

    @PutMapping("/{id}")
    public R update(@PathVariable("id") Long id, @RequestBody FunctionCallRequest request) {
        ChatToolDescription tool = functionCallService.findById(id);
        if (tool == null)
            return R.error(FC_PARAM_INVALID);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!hasTeamAccess(memberId, tool.getMemberId()))
            return R.error(FC_ACCESS_DENIED);
        if (!hasWriteAccess(memberId, tool.getMemberId()))
            return R.error(FC_WRITE_DENIED);

        if (request.getName() == null || request.getName().isBlank())
            return R.error(FC_PARAM_INVALID);
        if (request.getDescription() == null || request.getDescription().isBlank())
            return R.error(FC_PARAM_INVALID);

        return R.ok(functionCallService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public R delete(@PathVariable("id") Long id) {
        ChatToolDescription tool = functionCallService.findById(id);
        if (tool == null)
            return R.error(FC_PARAM_INVALID);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!hasTeamAccess(memberId, tool.getMemberId()))
            return R.error(FC_ACCESS_DENIED);
        if (!hasWriteAccess(memberId, tool.getMemberId()))
            return R.error(FC_WRITE_DENIED);

        functionCallService.delete(id);
        log.info("function call tool deleted: memberId={}, toolId={}, name={}", memberId, id, tool.getName());
        return R.ok();
    }

    @PostMapping("/{id}/test")
    public R test(@PathVariable("id") Long id, @RequestBody FunctionCallTestRequest request) {
        ChatToolDescription tool = functionCallService.findById(id);
        if (tool == null)
            return R.error(FC_PARAM_INVALID);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!hasTeamAccess(memberId, tool.getMemberId()))
            return R.error(FC_ACCESS_DENIED);

        try {
            String result = toolAdapterService.executeTool(tool, request.getParameters());
            return R.ok(Map.of("result", result != null ? result : "null"));
        } catch (Exception e) {
            log.error("function call tool test failed: id={}, name={}", id, tool.getName(), e);
            return R.error(FC_EXECUTE_FAILED);
        }
    }

    @PostMapping("/{id}/generate/testcases")
    public R generateTestCases(@PathVariable("id") Long id,
                               @RequestBody FunctionCallTestCaseGenerateRequest request) {
        ChatToolDescription tool = functionCallService.findById(id);
        if (tool == null)
            return R.error(FC_PARAM_INVALID);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!hasTeamAccess(memberId, tool.getMemberId()))
            return R.error(FC_ACCESS_DENIED);

        if (request.getCount() < 1 || request.getCount() > 20)
            return R.error(FC_TEST_COUNT_INVALID);

        Map<String, Object> testCaseVars = new HashMap<>();
        testCaseVars.put("count", request.getCount());
        testCaseVars.put("toolName", tool.getName());
        testCaseVars.put("toolDescription", tool.getDescription() != null ? tool.getDescription() : "");
        testCaseVars.put("executeScript", tool.getExecute() != null ? tool.getExecute() : "");
        testCaseVars.put("toolProperty", tool.getProperty() != null ? tool.getProperty() : "{}");
        testCaseVars.put("toolRequired", tool.getRequired() != null ? tool.getRequired() : "[]");

        try {
            String result = directModelInvoker.directInvoke(
                    "generateScript.testCases",
                    "tool_case_generation",
                    testCaseVars);
            return R.ok(Map.of("testCases", result));
        } catch (Exception e) {
            log.error("function call test case generation failed: id={}, name={}", id, tool.getName(), e);
            return R.error(FC_GENERATE_FAILED);
        }
    }

    private String generatePrompt(String baseTemplate, ToolCapabilityAnalysisResult capabilities, String prompt) {
        StringBuilder composedPrompt = new StringBuilder(baseTemplate);

        int rulesIndex = composedPrompt.indexOf("## Rules");
        if (rulesIndex == -1)
            rulesIndex = composedPrompt.length();

        StringBuilder capabilitySection = new StringBuilder();
        if (capabilities.isNeedsHttp())
            capabilitySection.append(PromptTemplateConstant.TOOL_HTTP_CAPABILITY_SNIPPET);
        if (capabilities.isNeedsConfig())
            capabilitySection.append(PromptTemplateConstant.TOOL_CONFIG_CAPABILITY_SNIPPET);
        if (capabilities.isNeedsJwt())
            capabilitySection.append(PromptTemplateConstant.TOOL_JWT_CAPABILITY_SNIPPET);

        composedPrompt.insert(rulesIndex, capabilitySection);
        return composedPrompt.toString().replace("{{prompt}}", prompt);
    }

    private boolean hasTeamAccess(Long memberId, Long toolOwnerMemberId) {
        return memberService.resolveTeamMemberIds(memberId).contains(toolOwnerMemberId);
    }

    private boolean hasWriteAccess(Long memberId, Long toolOwnerMemberId) {
        if (memberService.isOwner(memberId))
            return true;
        return memberId.equals(toolOwnerMemberId);
    }
}
