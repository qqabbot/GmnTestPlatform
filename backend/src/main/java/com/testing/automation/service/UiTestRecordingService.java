package com.testing.automation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
public class UiTestRecordingService {
    
    // 管理录制进程
    private final Map<Long, Process> recordingProcesses = new ConcurrentHashMap<>();
    
    // 存储录制的代码
    private final Map<Long, StringBuilder> recordedCode = new ConcurrentHashMap<>();
    
    // 录制状态
    private final Map<Long, RecordingStatus> recordingStatus = new ConcurrentHashMap<>();
    
    // 代码回调函数（用于 WebSocket 推送）
    private final Map<Long, Consumer<String>> codeCallbacks = new ConcurrentHashMap<>();
    
    @lombok.Data
    public static class RecordingStatus {
        private boolean recording;
        private LocalDateTime startTime;
        private int codeLines;
        private String targetUrl;
        
        public boolean isRecording() {
            return recording;
        }
        
        public void setRecording(boolean recording) {
            this.recording = recording;
        }
    }
    
    /**
     * 启动录制
     * @param caseId 测试用例ID
     * @param targetUrl 目标URL
     */
    public void startRecording(Long caseId, String targetUrl) {
        if (recordingProcesses.containsKey(caseId)) {
            log.warn("Recording already started for case: {}", caseId);
            return;
        }
        
        try {
            log.info("Starting recording for case: {}, targetUrl: {}", caseId, targetUrl);
            
            // 初始化状态
            RecordingStatus status = new RecordingStatus();
            status.setRecording(true);
            status.setStartTime(LocalDateTime.now());
            status.setCodeLines(0);
            status.setTargetUrl(targetUrl);
            recordingStatus.put(caseId, status);
            
            // 清空之前的代码
            recordedCode.put(caseId, new StringBuilder());
            
            // 启动 Playwright codegen 进程
            ProcessBuilder pb = new ProcessBuilder(
                "npx", "playwright", "codegen",
                targetUrl,
                "--target", "javascript",
                "--output", "-" // 输出到 stdout
            );
            
            // 设置工作目录（可选）
            pb.directory(new java.io.File(System.getProperty("user.dir")));
            
            // 合并错误流到标准输出
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            recordingProcesses.put(caseId, process);
            
            // 启动线程读取输出
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder code = recordedCode.get(caseId);
                    RecordingStatus currentStatus = recordingStatus.get(caseId);
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) {
                            continue;
                        }
                        
                        // 跳过注释和空行（可选）
                        if (line.trim().startsWith("//") || line.trim().startsWith("*")) {
                            continue;
                        }
                        
                        // 添加代码行
                        if (code != null) {
                            code.append(line).append("\n");
                        }
                        
                        // 更新状态
                        if (currentStatus != null) {
                            currentStatus.setCodeLines(currentStatus.getCodeLines() + 1);
                        }
                        
                        // 通过回调发送代码（用于 WebSocket）
                        Consumer<String> callback = codeCallbacks.get(caseId);
                        if (callback != null) {
                            callback.accept(line);
                        }
                        
                        log.debug("Recording code line: {}", line);
                    }
                } catch (IOException e) {
                    log.error("Error reading codegen output for case: {}", caseId, e);
                } finally {
                    // 进程结束时清理
                    recordingProcesses.remove(caseId);
                    RecordingStatus finalStatus = recordingStatus.get(caseId);
                    if (finalStatus != null) {
                        finalStatus.setRecording(false);
                    }
                }
            });
            
            readerThread.setDaemon(true);
            readerThread.start();
            
            log.info("Recording started successfully for case: {}", caseId);
            
        } catch (IOException e) {
            log.error("Failed to start recording for case: {}", caseId, e);
            recordingProcesses.remove(caseId);
            recordingStatus.remove(caseId);
            recordedCode.remove(caseId);
            throw new RuntimeException("Failed to start recording: " + e.getMessage(), e);
        }
    }
    
    /**
     * 停止录制
     * @param caseId 测试用例ID
     */
    public void stopRecording(Long caseId) {
        Process process = recordingProcesses.remove(caseId);
        if (process != null) {
            log.info("Stopping recording for case: {}", caseId);
            process.destroy();
            
            // 等待进程结束（最多等待5秒）
            try {
                boolean terminated = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (!terminated) {
                    log.warn("Recording process did not terminate, forcing destroy");
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for recording process to stop", e);
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
            
            // 更新状态
            RecordingStatus status = recordingStatus.get(caseId);
            if (status != null) {
                status.setRecording(false);
            }
            
            // 清理回调
            codeCallbacks.remove(caseId);
            
            log.info("Recording stopped for case: {}", caseId);
        } else {
            log.warn("No active recording found for case: {}", caseId);
        }
    }
    
    /**
     * 获取录制的代码
     * @param caseId 测试用例ID
     * @return 录制的代码
     */
    public String getRecordedCode(Long caseId) {
        StringBuilder code = recordedCode.get(caseId);
        return code != null ? code.toString() : "";
    }
    
    /**
     * 获取录制状态
     * @param caseId 测试用例ID
     * @return 录制状态
     */
    public RecordingStatus getRecordingStatus(Long caseId) {
        return recordingStatus.get(caseId);
    }
    
    /**
     * 检查是否正在录制
     * @param caseId 测试用例ID
     * @return 是否正在录制
     */
    public boolean isRecording(Long caseId) {
        RecordingStatus status = recordingStatus.get(caseId);
        return status != null && status.isRecording();
    }
    
    /**
     * 注册代码回调（用于 WebSocket 推送）
     * @param caseId 测试用例ID
     * @param callback 回调函数
     */
    public void registerCodeCallback(Long caseId, Consumer<String> callback) {
        codeCallbacks.put(caseId, callback);
    }
    
    /**
     * 移除代码回调
     * @param caseId 测试用例ID
     */
    public void removeCodeCallback(Long caseId) {
        codeCallbacks.remove(caseId);
    }
    
    /**
     * 清理录制数据
     * @param caseId 测试用例ID
     */
    public void cleanup(Long caseId) {
        stopRecording(caseId);
        recordedCode.remove(caseId);
        recordingStatus.remove(caseId);
        codeCallbacks.remove(caseId);
    }
}
